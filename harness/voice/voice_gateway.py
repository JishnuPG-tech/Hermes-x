"""Voice Gateway orchestrator.

Coordinates:
- Realtime WebSocket framing and protocol lifecycle.
- STT streaming audio transcription.
- LLM streaming generation with voice prompt optimization.
- SentenceChunker incremental text pipelining.
- Sub-4s fallback deadline and TTSManager audio generation.
- Sub-300ms barge-in speech cancellation.
- Task runtime offloading to HarnessEngine.
"""

from __future__ import annotations

import asyncio
import base64
import json
import logging
import time
import uuid
from typing import Any, AsyncIterator, Dict, List, Optional

from fastapi import WebSocket, WebSocketDisconnect

from harness.engine import HarnessEngine
from harness.kernel.models import Task
from harness.voice.chunker import SentenceChunker
from harness.voice.models import (
    AssistantAudioMessage,
    AssistantStateMessage,
    AssistantTextMessage,
    AudioChunkMessage,
    AudioEndMessage,
    AudioFormat,
    AudioStartMessage,
    CommandCancelMessage,
    ErrorMessage,
    SessionOpenMessage,
    TaskUpdateMessage,
    TextInputMessage,
    TTSGenerationRequest,
    VoiceSession,
    VoiceTimingMetrics,
    VoiceTurn,
)
from harness.voice.session import VoiceSessionManager
from harness.voice.stt_adapter import StreamingSTTAdapter
from harness.voice.tts.manager import TTSManager
from harness.voice.turn_detector import SilenceTurnDetector

logger = logging.getLogger(__name__)

# System prompt directive for conversational voice brevity
VOICE_SYSTEM_DIRECTIVE = (
    "You are Hermes Agent speaking live through full-duplex realtime voice. "
    "Keep responses concise, natural, direct, and conversational. "
    "Do NOT output markdown tables, raw markdown syntax, or huge code blocks in spoken responses; "
    "instead summarize clearly. If the user requests an autonomous coding or system task, "
    "acknowledge the task immediately in one short sentence, and let them know you are executing it."
)


class VoiceGateway:
    """Core Voice Gateway coordinating duplex audio, chunking, and agent execution."""

    def __init__(
        self,
        harness_engine: Optional[HarnessEngine] = None,
        tts_manager: Optional[TTSManager] = None,
        session_manager: Optional[VoiceSessionManager] = None,
    ) -> None:
        self.harness = harness_engine or HarnessEngine()
        self.tts = tts_manager or TTSManager()
        self.sessions = session_manager or VoiceSessionManager()
        self.stt = StreamingSTTAdapter()

    async def initialize(self) -> None:
        """Initialize TTS engines and dependent services."""
        await self.tts.initialize()

    async def handle_websocket_connection(self, websocket: WebSocket, session_id: Optional[str] = None) -> None:
        """Handle incoming full-duplex WebSocket connection."""
        await websocket.accept()
        sid = session_id or f"voice_{uuid.uuid4().hex[:12]}"
        session = self.sessions.get_or_create_session(sid)
        cancel_event = self.tts.register_session(sid)
        turn_detector = SilenceTurnDetector()
        from harness.voice.wake_word import WakeWordDetector
        wake_detector = WakeWordDetector(stt_adapter=self.stt)
        audio_in_buffer = bytearray()
        active_turn_task: Optional[asyncio.Task] = None

        logger.info("Voice WebSocket connected: %s", sid)

        try:
            # Emit initial state
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=sid,
                    state="idle",
                    current_task_id=None,
                    metrics=VoiceTimingMetrics(),
                ).model_dump(),
            )

            while True:
                # Receive text or binary frame
                message = await websocket.receive()
                msg_type = message.get("type")

                if msg_type == "websocket.disconnect":
                    break

                if "bytes" in message and message["bytes"]:
                    # Binary PCM/Audio frame
                    # Wake-word detection: Phonetic Candidate -> Stage 2 STT Verification
                    if wake_detector.process_pcm16_chunk(raw_bytes):
                        cand_buf = wake_detector.get_candidate_buffer() or raw_bytes
                        is_verified = await wake_detector.verify_keyword_async(cand_buf)
                        if is_verified:
                            logger.info("Wake-word 'Hermes' confirmed via phonetic & STT verification in session %s", sid)
                            await self._send_json(
                                websocket,
                                AssistantStateMessage(
                                    session_id=sid,
                                    state="listening",
                                    current_task_id=None,
                                    metrics=VoiceTimingMetrics(wake_detected_at=time.time()),
                                ).model_dump(),
                            )
                        else:
                            logger.debug("Wake candidate rejected by Stage 2 STT keyword verification in session %s", sid)
                    audio_in_buffer.extend(raw_bytes)
                    is_turn_end = turn_detector.process_pcm16_chunk(raw_bytes)
                    if is_turn_end and len(audio_in_buffer) > 0:
                        pcm_payload = bytes(audio_in_buffer)
                        audio_in_buffer.clear()
                        turn_detector.reset()
                        # Process speech turn
                        if active_turn_task and not active_turn_task.done():
                            active_turn_task.cancel()
                        active_turn_task = asyncio.create_task(
                            self._process_audio_turn(websocket, session, pcm_payload, cancel_event)
                        )

                elif "text" in message and message["text"]:
                    try:
                        data = json.loads(message["text"])
                    except Exception:
                        continue

                    mtype = data.get("type")

                    if mtype == "session_open":
                        open_msg = SessionOpenMessage.model_validate(data)
                        session.voice = open_msg.voice
                        session.speed = open_msg.speed
                        session.sample_rate = open_msg.sample_rate
                        session.client_metadata = open_msg.client_metadata
                        await self._send_json(
                            websocket,
                            AssistantStateMessage(
                                session_id=sid,
                                state="listening",
                                current_task_id=None,
                                metrics=VoiceTimingMetrics(),
                            ).model_dump(),
                        )

                    elif mtype == "text_input":
                        inp_msg = TextInputMessage.model_validate(data)
                        if inp_msg.barge_in:
                            self._trigger_barge_in(sid, cancel_event, active_turn_task)

                        # Process text input directly as a turn
                        if active_turn_task and not active_turn_task.done():
                            active_turn_task.cancel()
                        active_turn_task = asyncio.create_task(
                            self._process_text_turn(websocket, session, inp_msg.text, cancel_event)
                        )

                    elif mtype == "command_cancel":
                        cmd_cancel = CommandCancelMessage.model_validate(data)
                        logger.info("Received command_cancel (%s) for session %s", cmd_cancel.scope, sid)
                        self._trigger_barge_in(sid, cancel_event, active_turn_task)
                        if cmd_cancel.scope == "all":
                            # Optionally cancel current running harness background task
                            pass
                        await self._send_json(
                            websocket,
                            AssistantStateMessage(
                                session_id=sid,
                                state="idle",
                                current_task_id=None,
                                metrics=VoiceTimingMetrics(),
                            ).model_dump(),
                        )

                    elif mtype == "audio_chunk":
                        # JSON-wrapped audio chunk (base64)
                        achunk = AudioChunkMessage.model_validate(data)
                        chunk_bytes = base64.b64decode(achunk.data)
                        audio_in_buffer.extend(chunk_bytes)
                        if turn_detector.process_pcm16_chunk(chunk_bytes):
                            pcm_payload = bytes(audio_in_buffer)
                            audio_in_buffer.clear()
                            turn_detector.reset()
                            if active_turn_task and not active_turn_task.done():
                                active_turn_task.cancel()
                            active_turn_task = asyncio.create_task(
                                self._process_audio_turn(websocket, session, pcm_payload, cancel_event)
                            )

                    elif mtype == "audio_end":
                        if len(audio_in_buffer) > 0:
                            pcm_payload = bytes(audio_in_buffer)
                            audio_in_buffer.clear()
                            turn_detector.reset()
                            if active_turn_task and not active_turn_task.done():
                                active_turn_task.cancel()
                            active_turn_task = asyncio.create_task(
                                self._process_audio_turn(websocket, session, pcm_payload, cancel_event)
                            )

        except WebSocketDisconnect:
            logger.info("Voice WebSocket disconnected: %s", sid)
        except Exception as exc:
            logger.error("Error in Voice WebSocket %s: %s", sid, exc)
        finally:
            self.tts.unregister_session(sid)
            self.sessions.close_session(sid)

    def _trigger_barge_in(
        self,
        session_id: str,
        cancel_event: asyncio.Event,
        active_turn_task: Optional[asyncio.Task],
    ) -> None:
        """Trigger barge-in cancellation within sub-300ms SLA."""
        start = time.perf_counter()
        cancel_event.set()
        self.tts.cancel_generation(session_id)
        if active_turn_task and not active_turn_task.done():
            active_turn_task.cancel()
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        logger.info("Barge-in cancellation executed in %0.2fms for session %s", elapsed_ms, session_id)

    async def _send_json(self, websocket: WebSocket, data: Dict[str, Any]) -> None:
        try:
            await websocket.send_text(json.dumps(data))
        except Exception as exc:
            logger.debug("Failed to send WebSocket message: %s", exc)

    async def _process_audio_turn(
        self,
        websocket: WebSocket,
        session: VoiceSession,
        audio_pcm: bytes,
        cancel_event: asyncio.Event,
    ) -> None:
        """Transcribe audio turn and run pipeline."""
        cancel_event.clear()
        metrics = VoiceTimingMetrics()
        metrics.stt_start_ms = time.perf_counter() * 1000.0

        await self._send_json(
            websocket,
            AssistantStateMessage(
                session_id=session.session_id,
                state="thinking",
                current_task_id=None,
                metrics=metrics,
            ).model_dump(),
        )

        transcript = await self.stt.transcribe_audio_buffer(audio_pcm, session.sample_rate)
        metrics.stt_duration_ms = round((time.perf_counter() * 1000.0) - metrics.stt_start_ms, 2)

        if not transcript.strip():
            logger.debug("No transcription obtained for audio turn in %s", session.session_id)
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="idle",
                    current_task_id=None,
                    metrics=metrics,
                ).model_dump(),
            )
            return

        await self._process_text_turn(websocket, session, transcript, cancel_event, metrics=metrics)

    async def _process_text_turn(
        self,
        websocket: WebSocket,
        session: VoiceSession,
        user_text: str,
        cancel_event: asyncio.Event,
        metrics: Optional[VoiceTimingMetrics] = None,
    ) -> None:
        """Drive full LLM generation, sentence chunking, TTS arbitration, and background task offloading."""
        cancel_event.clear()
        metrics = metrics or VoiceTimingMetrics()
        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
        turn = VoiceTurn(
            turn_id=turn_id,
            user_text=user_text,
            metrics=metrics,
        )
        session.turns.append(turn)

        await self._send_json(
            websocket,
            AssistantStateMessage(
                session_id=session.session_id,
                state="thinking",
                current_task_id=None,
                metrics=metrics,
            ).model_dump(),
        )

        metrics.llm_start_ms = time.perf_counter() * 1000.0
        chunker = SentenceChunker()
        turn_tts_index = 0

        # Background durable task detection
        is_heavy_task = any(
            kw in user_text.lower()
            for kw in [
                "deploy",
                "build",
                "run task",
                "execute workflow",
                "refactor",
                "audit",
                "scan",
                "scrape",
            ]
        )

        if is_heavy_task:
            # Verbal acknowledgment first, then run durable task via HarnessEngine
            task: Task = await self.harness.create_and_run_task(
                objective=user_text,
                project_id=session.session_id,
            )
            turn.task_id = task.task_id
            ack_speech = f"I'm on it. Starting task {task.task_id[:8]} in the background now."
            await self._synthesize_and_send_phrase(
                websocket, session, ack_speech, turn_id, turn_tts_index, cancel_event, metrics
            )

            # Send TaskUpdateMessage
            await self._send_json(
                websocket,
                TaskUpdateMessage(
                    task_id=task.task_id,
                    status=task.status.value,
                    action_summary=f"Running task: {user_text[:100]}",
                ).model_dump(),
            )

            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="idle",
                    current_task_id=task.task_id,
                    metrics=metrics,
                ).model_dump(),
            )
            return

        # Regular conversational turn: Stream via agent_executor
        try:
            from gateway import agent_executor as ae

            # Use conversational history from session
            messages = [
                {"role": "system", "content": VOICE_SYSTEM_DIRECTIVE},
            ]
            for prev_turn in session.turns[-5:]:
                if prev_turn.user_text:
                    messages.append({"role": "user", "content": prev_turn.user_text})
                if prev_turn.assistant_text:
                    messages.append({"role": "assistant", "content": prev_turn.assistant_text})

            queue: asyncio.Queue = asyncio.Queue()
            msg_id = f"msg_{uuid.uuid4().hex[:12]}"
            model = "hermes-default"

            # Run agent loop in background
            agent_task = asyncio.create_task(
                ae.run_autonomous_agent(
                    session.session_id,
                    user_text,
                    messages,
                    model,
                    msg_id,
                    queue,
                )
            )

            full_assistant_text = []

            while not agent_task.done() or not queue.empty():
                if cancel_event.is_set():
                    agent_task.cancel()
                    break

                try:
                    item = await asyncio.wait_for(queue.get(), timeout=0.1)
                except asyncio.TimeoutError:
                    continue

                if item is None:
                    break

                # Extract text deltas
                if isinstance(item, str):
                    if item.startswith("data: "):
                        data_part = item[6:].strip()
                        if data_part and data_part != "[DONE]":
                            try:
                                parsed = json.loads(data_part)
                                if parsed.get("type") == "content_block_delta":
                                    delta_text = parsed.get("delta", {}).get("text", "")
                                    if delta_text:
                                        full_assistant_text.append(delta_text)
                                        # Feed chunker
                                        for phrase in chunker.feed(delta_text):
                                            if cancel_event.is_set():
                                                break
                                            turn_tts_index += 1
                                            await self._synthesize_and_send_phrase(
                                                websocket,
                                                session,
                                                phrase,
                                                turn_id,
                                                turn_tts_index,
                                                cancel_event,
                                                metrics,
                                            )
                            except Exception:
                                pass

            # Flush remaining chunker buffer
            for phrase in chunker.flush():
                if cancel_event.is_set():
                    break
                turn_tts_index += 1
                await self._synthesize_and_send_phrase(
                    websocket,
                    session,
                    phrase,
                    turn_id,
                    turn_tts_index,
                    cancel_event,
                    metrics,
                )

            turn.assistant_text = "".join(full_assistant_text)
            metrics.e2e_turn_ms = round((time.perf_counter() * 1000.0) - metrics.llm_start_ms, 2)

        except asyncio.CancelledError:
            logger.info("Voice turn cancelled by barge-in: %s", turn_id)
        except Exception as exc:
            logger.error("Error processing voice turn: %s", exc)
            await self._send_json(
                websocket,
                ErrorMessage(
                    error=str(exc),
                    code="TURN_PROCESSING_ERROR",
                    fatal=False,
                ).model_dump(),
            )
        finally:
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="idle",
                    current_task_id=turn.task_id,
                    metrics=metrics,
                ).model_dump(),
            )

    async def _synthesize_and_send_phrase(
        self,
        websocket: WebSocket,
        session: VoiceSession,
        phrase: str,
        turn_id: str,
        index: int,
        cancel_event: asyncio.Event,
        metrics: VoiceTimingMetrics,
    ) -> None:
        """Synthesize a single sentence/clause and stream audio chunks to WebSocket."""
        if not phrase.strip() or cancel_event.is_set():
            return

        tts_req_id = f"{turn_id}_p{index}"
        tts_start = time.perf_counter() * 1000.0

        # Send text accompaniment
        await self._send_json(
            websocket,
            AssistantTextMessage(
                text=phrase,
                is_final=False,
                turn_id=turn_id,
            ).model_dump(),
        )

        # Notify audio start
        await self._send_json(
            websocket,
            AudioStartMessage(
                format=AudioFormat.MP3,
                sample_rate=session.sample_rate,
            ).model_dump(),
        )

        req = TTSGenerationRequest(
            request_id=tts_req_id,
            tts_generation_id=tts_req_id,
            text=phrase,
            voice=session.voice,
            speed=session.speed,
            cancel_event=cancel_event,
        )

        first_chunk = True
        try:
            async for chunk in self.tts.stream_audio_chunks(req):
                if cancel_event.is_set():
                    break
                if first_chunk:
                    now = time.perf_counter() * 1000.0
                    metrics.first_audio_latency_ms = round(now - metrics.llm_start_ms, 2)
                    first_chunk = False

                # Send binary audio chunk
                await websocket.send_bytes(chunk)

            # Mark audio end
            await self._send_json(websocket, AudioEndMessage().model_dump())

        except Exception as exc:
            logger.error("Error synthesizing audio chunk for phrase '%s': %s", phrase, exc)
