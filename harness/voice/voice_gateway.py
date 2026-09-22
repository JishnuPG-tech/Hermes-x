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
import math
import struct
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
    PlaybackStateMessage,
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
                    raw_bytes = message["bytes"]
                    # Support multiplexed 0x01 mic frame header
                    if len(raw_bytes) > 1 and raw_bytes[0] == 0x01 and (len(raw_bytes) - 1) % 2 == 0:
                        raw_bytes = raw_bytes[1:]

                    # ── Self-listening loop guard ──────────────────────────────────
                    # While assistant is actively speaking or playing out loud, do NOT accumulate
                    # loudspeaker audio into turns. Check only for high-energy user acoustic barge-in.
                    if session.is_speaking or session.assistant_audio_active:
                        count = len(raw_bytes) // 2
                        if count > 0:
                            shorts = struct.unpack(f"<{count}h", raw_bytes[: count * 2])
                            sum_sq = sum(s * s for s in shorts)
                            rms = math.sqrt(sum_sq / count)
                            if rms > 1200.0:  # Real human speech over loudspeaker
                                logger.info("Acoustic barge-in detected during speech (RMS=%.1f) in session %s", rms, sid)
                                self._trigger_barge_in(sid)
                                session.is_speaking = False
                                session.assistant_audio_active = False
                                audio_in_buffer.clear()
                                turn_detector.reset()
                        continue

                    audio_in_buffer.extend(raw_bytes)
                    is_turn_end = turn_detector.process_pcm16_chunk(raw_bytes)
                    if is_turn_end and len(audio_in_buffer) > 0:
                        pcm_payload = bytes(audio_in_buffer)
                        audio_in_buffer.clear()
                        turn_detector.reset()
                        # Process speech turn with fresh TurnContext
                        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                        turn_ctx = self.sessions.create_turn(sid, turn_id)
                        if active_turn_task and not active_turn_task.done():
                            active_turn_task.cancel()
                        active_turn_task = asyncio.create_task(
                            self._process_audio_turn(websocket, session, pcm_payload, turn_ctx)
                        )
                        turn_ctx.tasks.append(active_turn_task)

                elif "text" in message and message["text"]:
                    try:
                        data = json.loads(message["text"])
                    except Exception:
                        continue

                    mtype = data.get("type")

                    if mtype in ("session_open", "session_start"):
                        open_msg = SessionOpenMessage.model_validate(data)
                        session.voice = open_msg.voice or "en-US-ChristopherNeural"
                        session.speed = open_msg.speed if open_msg.speed is not None else 1.0
                        session.sample_rate = open_msg.sample_rate if open_msg.sample_rate is not None else 24000
                        session.client_metadata = open_msg.client_metadata or {}
                        user_name = session.client_metadata.get("user_name") or "Jishnu"
                        greet_requested = session.client_metadata.get("greet", False)

                        if greet_requested:
                            greeting_phrase = f"Hi {user_name}! How can I help you today?"
                            turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                            turn_ctx = self.sessions.create_turn(sid, turn_id)
                            metrics = VoiceTimingMetrics()
                            await self._send_json(
                                websocket,
                                AssistantStateMessage(
                                    session_id=sid,
                                    state="speaking",
                                    current_task_id=None,
                                    metrics=metrics,
                                ).model_dump(),
                            )
                            if active_turn_task and not active_turn_task.done():
                                active_turn_task.cancel()
                            active_turn_task = asyncio.create_task(
                                self._synthesize_and_send_phrase(
                                    websocket,
                                    session,
                                    greeting_phrase,
                                    turn_id,
                                    0,
                                    turn_ctx.cancel_event,
                                    metrics,
                                )
                            )
                            turn_ctx.tasks.append(active_turn_task)
                        else:
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
                            self._trigger_barge_in(sid)

                        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                        turn_ctx = self.sessions.create_turn(sid, turn_id)
                        if active_turn_task and not active_turn_task.done():
                            active_turn_task.cancel()
                        requested_model = data.get("model") or "hermes-agent"
                        active_turn_task = asyncio.create_task(
                            self._process_text_turn(websocket, session, inp_msg.text, turn_ctx, model=requested_model)
                        )
                        turn_ctx.tasks.append(active_turn_task)

                    elif mtype in ("command_cancel", "user_interrupt"):
                        cmd_scope = data.get("scope", "turn")
                        logger.info("Received cancellation (%s) for session %s", cmd_scope, sid)
                        self._trigger_barge_in(sid)
                        await self._send_json(
                            websocket,
                            AssistantStateMessage(
                                session_id=sid,
                                state="listening",
                                current_task_id=None,
                                metrics=VoiceTimingMetrics(),
                            ).model_dump(),
                        )

                    elif mtype == "playback_state":
                        pstate = data.get("state")
                        turn_id = data.get("turn_id")
                        logger.info("Client playback_state=%s (turn=%s, session=%s)", pstate, turn_id, sid)
                        if pstate == "started":
                            session.assistant_audio_active = True
                        elif pstate in ("completed", "interrupted"):
                            session.assistant_audio_active = False

                    elif mtype == "audio_chunk":
                        achunk = AudioChunkMessage.model_validate(data)
                        chunk_bytes = base64.b64decode(achunk.data)
                        audio_in_buffer.extend(chunk_bytes)
                        if turn_detector.process_pcm16_chunk(chunk_bytes):
                            pcm_payload = bytes(audio_in_buffer)
                            audio_in_buffer.clear()
                            turn_detector.reset()
                            turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                            turn_ctx = self.sessions.create_turn(sid, turn_id)
                            if active_turn_task and not active_turn_task.done():
                                active_turn_task.cancel()
                            active_turn_task = asyncio.create_task(
                                self._process_audio_turn(websocket, session, pcm_payload, turn_ctx)
                            )
                            turn_ctx.tasks.append(active_turn_task)

                    elif mtype in ("audio_end", "end_of_utterance"):
                        if len(audio_in_buffer) > 0:
                            pcm_payload = bytes(audio_in_buffer)
                            audio_in_buffer.clear()
                            turn_detector.reset()
                            turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                            turn_ctx = self.sessions.create_turn(sid, turn_id)
                            if active_turn_task and not active_turn_task.done():
                                active_turn_task.cancel()
                            active_turn_task = asyncio.create_task(
                                self._process_audio_turn(websocket, session, pcm_payload, turn_ctx)
                            )
                            turn_ctx.tasks.append(active_turn_task)

        except WebSocketDisconnect:
            logger.info("Voice WebSocket disconnected: %s", sid)
        except Exception as exc:
            logger.error("Error in Voice WebSocket %s: %s", sid, exc)
        finally:
            self.sessions.close_session(sid)

    def _trigger_barge_in(
        self,
        session_id: str,
    ) -> None:
        """Trigger barge-in cancellation within sub-200ms SLA."""
        start = time.perf_counter()
        session = self.sessions.get_session(session_id)
        if session:
            session.is_speaking = False
            session.assistant_audio_active = False
        self.sessions.cancel_active_turn(session_id)
        self.tts.cancel_generation(session_id)
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
        turn_ctx: Any,
    ) -> None:
        """Transcribe audio turn and run pipeline."""
        if turn_ctx.is_cancelled():
            return
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

        transcript = await self.stt.transcribe_audio_buffer(audio_pcm, 16000)
        metrics.stt_duration_ms = round((time.perf_counter() * 1000.0) - metrics.stt_start_ms, 2)

        if not transcript.strip() or turn_ctx.is_cancelled():
            logger.debug("No transcription obtained or turn cancelled for audio turn in %s", session.session_id)
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="listening",
                    current_task_id=None,
                    metrics=metrics,
                ).model_dump(),
            )
            return

        await self._process_text_turn(websocket, session, transcript, turn_ctx, metrics=metrics)

    async def _process_text_turn(
        self,
        websocket: WebSocket,
        session: VoiceSession,
        user_text: str,
        turn_ctx: Any,
        metrics: Optional[VoiceTimingMetrics] = None,
        model: str = "hermes-agent",
    ) -> None:
        """Drive full LLM generation, sentence chunking, TTS arbitration, and background task offloading."""
        if turn_ctx.is_cancelled():
            return
        metrics = metrics or VoiceTimingMetrics()
        turn_id = turn_ctx.turn_id
        cancel_event = turn_ctx.cancel_event
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
                    state="listening",
                    current_task_id=task.task_id,
                    metrics=metrics,
                ).model_dump(),
            )
            return

        # Regular conversational turn: Stream directly via agent_executor
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
            for prev_turn in session.turns[-5:]:
                if prev_turn.user_text:
                    messages.append({"role": "user", "content": prev_turn.user_text})
                if prev_turn.assistant_text:
                    messages.append({"role": "assistant", "content": prev_turn.assistant_text})

            queue: asyncio.Queue = asyncio.Queue()
            msg_id = f"msg_{uuid.uuid4().hex[:12]}"
            agent_model = model or "hermes-agent"

            # Run agent loop in background
            agent_task = asyncio.create_task(
                ae.run_autonomous_agent(
                    session.session_id,
                    user_text,
                    messages,
                    agent_model,
                    msg_id,
                    queue,
                )
            )
            turn_ctx.tasks.append(agent_task)


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

                # Extract text deltas from multi-line SSE frames
                if isinstance(item, str):
                    for line in item.splitlines():
                        line = line.strip()
                        if line.startswith("data: "):
                            data_part = line[6:].strip()
                            if data_part and data_part != "[DONE]":
                                try:
                                    parsed = json.loads(data_part)
                                    delta_text = ""
                                    if parsed.get("type") == "content_block_delta":
                                        delta_text = parsed.get("delta", {}).get("text", "")
                                    elif "choices" in parsed and len(parsed["choices"]) > 0:
                                        delta_text = parsed["choices"][0].get("delta", {}).get("content", "")

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
                                except Exception as err:
                                    logger.debug("Voice SSE parse error ignored: %s", err)

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

            # Fallback if assistant did not output any spoken audio phrase
            if not full_assistant_text and not cancel_event.is_set():
                fallback_phrase = "Task completed."
                turn_tts_index += 1
                await self._synthesize_and_send_phrase(
                    websocket,
                    session,
                    fallback_phrase,
                    turn_id,
                    turn_tts_index,
                    cancel_event,
                    metrics,
                )
                full_assistant_text.append(fallback_phrase)

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
            # Transition to idle, then immediately to listening so Android re-arms mic
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="idle",
                    current_task_id=turn.task_id,
                    metrics=metrics,
                ).model_dump(),
            )
            await self._send_json(
                websocket,
                AssistantStateMessage(
                    session_id=session.session_id,
                    state="listening",
                    current_task_id=None,
                    metrics=VoiceTimingMetrics(),
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

        session.is_speaking = True
        # Notify audio start with turn_id
        await self._send_json(
            websocket,
            AudioStartMessage(
                turn_id=turn_id,
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
                    start_baseline = metrics.llm_start_ms or tts_start
                    metrics.first_audio_latency_ms = round(now - start_baseline, 2)
                    metrics.first_audio_at_ms = now
                    metrics.tts_start_ms = tts_start
                    first_chunk = False

                # Send binary audio chunk
                await websocket.send_bytes(chunk)

            # Mark audio end with turn_id
            await self._send_json(websocket, AudioEndMessage(turn_id=turn_id).model_dump())

        except Exception as exc:
            logger.error("Error synthesizing audio chunk for phrase '%s': %s", phrase, exc)
        finally:
            session.is_speaking = False
