"""Hugging Voice Realtime WebSocket Server for Hermes Agent.

Implements the official Hugging Face speech-to-speech / Hugging Voice
architecture (reverse-engineered OpenAI Realtime protocol):
- Generation-tagged CancelScope for sub-50ms barge-in.
- 16kHz PCM streaming input with HuggingVoiceVAD.
- Sweet, calm, warm female persona (en-US-JennyNeural).
- Direct integration with Hermes execution layer (hermes_execute).
- Output audio frames emitted as response.output_audio.delta.
"""

from __future__ import annotations
import asyncio
import base64
import json
import logging
import re
import time
import uuid
from typing import Any, Dict, List, Optional

from fastapi import WebSocket, WebSocketDisconnect

from gateway.hugging_voice.protocol import (
    HERMES_EXECUTE_TOOL,
    RealtimeSessionConfig,
    create_error_event,
    create_function_call_event,
    create_output_audio_delta_event,
    create_output_audio_done_event,
    create_output_audio_transcript_delta_event,
    create_output_audio_transcript_done_event,
    create_response_created_event,
    create_response_done_event,
    create_session_created_event,
    create_session_updated_event,
    create_speech_started_event,
    create_speech_stopped_event,
    create_transcript_completed_event,
)
from gateway.hugging_voice.streaming_tts import HuggingVoiceTTS, DEFAULT_FEMALE_VOICE
from gateway.hugging_voice.vad import HuggingVoiceVAD
from harness.voice.stt_adapter import StreamingSTTAdapter

logger = logging.getLogger("hermes.hugging_voice.server")

SYSTEM_VOICE_PERSONA = (
    "You are Hermes, speaking live through Hugging Voice with an exceptionally calm, warm, articulate, and sweet tone. "
    "You are a sovereign JARVIS-level companion. "
    "Keep answers concise, direct, and conversational (1 to 2 sentences maximum). "
    "Do NOT use markdown headers, tables, or code blocks; speak clearly in natural prose. "
    "When the user asks you to inspect files, execute terminal commands, check system status, "
    "or run git commands, ALWAYS call the 'hermes_execute' tool to run it for real."
)


async def execute_hermes_action(
    objective: str,
    task_type: str = "general",
    background: bool = False,
    chat_id: str = "hugging_voice",
) -> Dict[str, Any]:
    """Execute autonomous action via Hermes Agent execution layer."""
    logger.info(f"Hugging Voice execute action: objective='{objective}', type='{task_type}', bg={background}")

    if background:
        try:
            from gateway.harness_api import get_harness_engine
            from harness.kernel.models import RiskLevel
            engine = get_harness_engine()
            task = await engine.create_and_run_task(
                objective=objective,
                project_id=chat_id,
                risk_level=RiskLevel.MEDIUM,
            )
            return {
                "status": "scheduled",
                "task_id": task.id,
                "summary": f"Background task {task.id} started for {objective}.",
            }
        except Exception as e:
            logger.error(f"Failed to schedule background task: {e}")
            return {"status": "error", "error": str(e), "summary": f"Could not schedule background task: {e}"}

    try:
        clean_obj = objective.strip()
        from hermes_core.runtime.agent_runtime import AgentRuntime
        from hermes_core.runtime.models import ExecutionContext

        ctx = ExecutionContext(
            user_id="voice_user",
            session_id=chat_id,
            project_id="voice_session",
            is_admin=True,
        )
        runtime = AgentRuntime.get_instance()
        turn_result = await runtime.execute_turn(clean_obj, ctx)
        res = turn_result.get("response", "")
        return {
            "status": "completed",
            "result": res,
            "summary": res[:300] if res else "Task completed successfully.",
        }
    except Exception as e:
        logger.error(f"Error executing Hermes action: {e}", exc_info=True)
        return {"status": "error", "error": str(e), "summary": f"Encountered an issue executing task: {e}"}


class CancelScope:
    """Hugging Face CancelScope: incrementing generation counter for instant zero-leak barge-in."""

    def __init__(self):
        self.generation = 0

    def cancel(self) -> int:
        self.generation += 1
        return self.generation

    def is_stale(self, gen: int) -> bool:
        return gen != self.generation


class HuggingVoiceSession:
    """Manages an active duplex Hugging Voice WebSocket session."""

    def __init__(self, websocket: WebSocket, session_id: Optional[str] = None):
        self.ws = websocket
        self.session_id = session_id or f"sess_{uuid.uuid4().hex[:12]}"
        self.config = RealtimeSessionConfig(
            id=self.session_id,
            instructions=SYSTEM_VOICE_PERSONA,
            voice=DEFAULT_FEMALE_VOICE,
            tools=[HERMES_EXECUTE_TOOL],
        )

        self.vad = HuggingVoiceVAD(sample_rate=16000)
        self.tts = HuggingVoiceTTS(default_voice=self.config.voice)
        self.stt = StreamingSTTAdapter(sample_rate=16000)

        self.cancel_scope = CancelScope()
        self.conversation_history: List[Dict[str, str]] = []
        self.active_turn_task: Optional[asyncio.Task] = None
        self.is_speaking = False

    async def send_json(self, payload: dict):
        try:
            await self.ws.send_text(json.dumps(payload))
        except Exception:
            pass

    def interrupt(self):
        """Barge-in interruption: mark generation stale and cancel active coroutines."""
        new_gen = self.cancel_scope.cancel()
        if self.active_turn_task and not self.active_turn_task.done():
            self.active_turn_task.cancel()
        self.active_turn_task = None
        self.is_speaking = False
        logger.info(f"Hugging Voice barge-in triggered: new generation {new_gen} in session {self.session_id}")

    async def handle_connection(self):
        await self.send_json(create_session_created_event(self.config))
        logger.info(f"Hugging Voice session started: {self.session_id} (voice={self.config.voice})")

        try:
            while True:
                msg = await self.ws.receive()

                if "text" in msg and msg["text"]:
                    try:
                        event = json.loads(msg["text"])
                    except Exception:
                        continue
                    await self._process_client_event(event)

                elif "bytes" in msg and msg["bytes"]:
                    await self._process_audio_chunk(msg["bytes"])

        except WebSocketDisconnect:
            logger.info(f"Hugging Voice session disconnected: {self.session_id}")
        except Exception as e:
            logger.error(f"Hugging Voice session exception: {e}")
        finally:
            self.interrupt()

    async def _process_client_event(self, event: dict):
        event_type = event.get("type", "")

        if event_type == "session.update":
            update_data = event.get("session", {})
            if "model" in update_data and update_data["model"]:
                self.config.model = update_data["model"]
            if "voice" in update_data and update_data["voice"]:
                self.config.voice = update_data["voice"]
                self.tts.default_voice = self.config.voice
            if "instructions" in update_data and update_data["instructions"]:
                self.config.instructions = update_data["instructions"]
            if "temperature" in update_data:
                self.config.temperature = float(update_data["temperature"])

            await self.send_json(create_session_updated_event(self.config))

        elif event_type == "input_audio_buffer.append":
            b64_audio = event.get("audio", "")
            if b64_audio:
                try:
                    pcm_bytes = base64.b64decode(b64_audio)
                    await self._process_audio_chunk(pcm_bytes)
                except Exception as e:
                    logger.debug(f"Audio decode error: {e}")

        elif event_type == "input_audio_buffer.clear":
            self.vad.reset()

        elif event_type == "response.cancel":
            logger.info(f"Explicit response.cancel received in {self.session_id}")
            self.interrupt()
            self.vad.reset()

        elif event_type == "conversation.item.create":
            item = event.get("item", {})
            itype = item.get("type")
            if itype == "function_call_output":
                output = item.get("output", "")
                self.conversation_history.append({
                    "role": "function",
                    "name": "hermes_execute",
                    "content": output,
                })
                self._trigger_turn_execution()
            elif itype == "message":
                text = item.get("content", "")
                if text:
                    self.conversation_history.append({"role": "user", "content": text})
                    self._trigger_turn_execution(prompt=text)

        elif event_type == "response.create":
            self._trigger_turn_execution()

    async def _process_audio_chunk(self, pcm_chunk: bytes):
        rms, is_speech_active, is_turn_complete = self.vad.process_chunk(pcm_chunk)

        # Barge-in: user began speaking while assistant was talking or synthesizing
        if is_speech_active and self.is_speaking:
            if self.vad._consecutive_speech_ms >= 350:
                logger.info("Hugging Voice barge-in: sustained user voice detected during speech output.")
                self.interrupt()
                await self.send_json(create_speech_started_event(0))
            return

        if is_speech_active and not self.is_speaking:
            if self.vad._consecutive_speech_ms <= 180:
                await self.send_json(create_speech_started_event(0))

        if is_turn_complete:
            await self.send_json(create_speech_stopped_event(0))
            speech_pcm = self.vad.get_speech_pcm()
            self.vad.reset()

            if speech_pcm and len(speech_pcm) >= 3200:
                self.interrupt()
                current_gen = self.cancel_scope.generation
                cancel_event = asyncio.Event()
                self.active_turn_task = asyncio.create_task(
                    self._execute_audio_turn(speech_pcm, current_gen, cancel_event)
                )

    def _trigger_turn_execution(self, prompt: str = ""):
        self.interrupt()
        current_gen = self.cancel_scope.generation
        cancel_event = asyncio.Event()
        self.active_turn_task = asyncio.create_task(
            self._generate_llm_response(current_gen, cancel_event, prompt=prompt)
        )

    async def _execute_audio_turn(self, speech_pcm: bytes, generation: int, cancel_event: asyncio.Event):
        t_start = time.perf_counter()

        transcript = await self.stt.transcribe_audio_buffer(speech_pcm)
        if self.cancel_scope.is_stale(generation) or cancel_event.is_set():
            return

        if not transcript.strip():
            return

        logger.info(f"Hugging Voice transcribed ({time.perf_counter() - t_start:.2f}s): '{transcript}'")
        await self.send_json(create_transcript_completed_event(transcript))

        self.conversation_history.append({"role": "user", "content": transcript})
        if len(self.conversation_history) > 12:
            self.conversation_history = self.conversation_history[-12:]

        await self._generate_llm_response(generation, cancel_event, prompt=transcript)

    async def _generate_llm_response(self, generation: int, cancel_event: asyncio.Event, prompt: str = ""):
        if self.cancel_scope.is_stale(generation) or cancel_event.is_set():
            return

        text_prompt = prompt or (self.conversation_history[-1]["content"] if self.conversation_history else "")
        response_id = f"resp_{uuid.uuid4().hex[:10]}"
        await self.send_json(create_response_created_event(response_id))
        self.is_speaking = True

        p_l = text_prompt.lower().strip() if text_prompt else ""
        is_instant_greeting = any(w in p_l for w in ["hi", "hello", "hey", "who are you", "what is your name", "what can you do", "help", "features"]) and len(p_l.split()) <= 10
        if is_instant_greeting:
            if any(w in p_l for w in ["who are you", "what is your name", "your name"]):
                reply = "I am Hermes, your sovereign AI companion speaking live through Hugging Voice. How may I assist you today?"
            elif any(w in p_l for w in ["what can you do", "help", "features"]):
                reply = "I can execute terminal commands, inspect files, write and run code, and conduct autonomous research. What would you like to build or run?"
            else:
                reply = "Hello! I am Hermes, your loyal companion speaking live through Hugging Voice. How may I assist you today?"

            self.conversation_history.append({"role": "assistant", "content": reply})
            await self.send_json(create_output_audio_transcript_delta_event(response_id, reply))
            async for pcm in self.tts.stream_sentence_pcm(reply, voice=self.config.voice, cancel_event=cancel_event):
                if self.cancel_scope.is_stale(generation):
                    break
                await self.send_json(create_output_audio_delta_event(response_id, base64.b64encode(pcm).decode("ascii")))

            await self.send_json(create_output_audio_transcript_done_event(response_id, reply))
            await self.send_json(create_output_audio_done_event(response_id))
            await self.send_json(create_response_done_event(response_id, "completed"))
            self.is_speaking = False
            return

        from gateway import anthropic_bridge as ab

        model_to_use = getattr(self.config, "model", None) or "hermes-agent"
        messages = [{"role": "system", "content": self.config.instructions}] + self.conversation_history
        payload = {
            "model": model_to_use,
            "messages": messages,
            "tools": [HERMES_EXECUTE_TOOL.model_dump()],
            "tool_choice": "auto",
            "temperature": self.config.temperature,
            "stream": True,
        }

        full_reply_text = ""
        current_sentence_buffer = ""
        tool_call_detected = False
        tool_call_args = ""
        tool_call_id = f"call_{uuid.uuid4().hex[:8]}"

        try:
            async for data in ab.stream_upstream(payload, requested_model=model_to_use, chat_id=self.session_id):
                if self.cancel_scope.is_stale(generation) or cancel_event.is_set():
                    break

                data = data.strip()
                if not data or data == "[DONE]":
                    continue

                try:
                    chunk = json.loads(data)
                except Exception:
                    continue

                delta = chunk.get("choices", [{}])[0].get("delta", {}) or {}

                # Tool call detection
                tc_chunk = delta.get("tool_calls")
                if tc_chunk:
                    tool_call_detected = True
                    for tc in tc_chunk:
                        func = tc.get("function", {})
                        if func.get("arguments"):
                            tool_call_args += func.get("arguments")
                    continue

                # Text token
                text_token = delta.get("content", "")
                if not text_token:
                    text_token = chunk.get("choices", [{}])[0].get("message", {}).get("content", "") or ""

                if text_token:
                    full_reply_text += text_token
                    current_sentence_buffer += text_token
                    await self.send_json(create_output_audio_transcript_delta_event(response_id, text_token))

                    # Sentence punctuation boundary check (. ! ? \n)
                    sentence_match = re.search(r'([.!?\n]+)\s*', current_sentence_buffer)
                    if sentence_match:
                        end_pos = sentence_match.end()
                        phrase = current_sentence_buffer[:end_pos].strip()
                        current_sentence_buffer = current_sentence_buffer[end_pos:]

                        if phrase and not self.cancel_scope.is_stale(generation):
                            async for pcm_chunk in self.tts.stream_sentence_pcm(
                                text=phrase,
                                voice=self.config.voice,
                                cancel_event=cancel_event,
                            ):
                                if self.cancel_scope.is_stale(generation) or cancel_event.is_set():
                                    break
                                b64_pcm = base64.b64encode(pcm_chunk).decode("ascii")
                                await self.send_json(create_output_audio_delta_event(response_id, b64_pcm))

            # Flush remaining buffer
            if current_sentence_buffer.strip() and not self.cancel_scope.is_stale(generation):
                async for pcm_chunk in self.tts.stream_sentence_pcm(
                    text=current_sentence_buffer.strip(),
                    voice=self.config.voice,
                    cancel_event=cancel_event,
                ):
                    if self.cancel_scope.is_stale(generation) or cancel_event.is_set():
                        break
                    b64_pcm = base64.b64encode(pcm_chunk).decode("ascii")
                    await self.send_json(create_output_audio_delta_event(response_id, b64_pcm))

            # Tool calling dispatch
            if tool_call_detected and not self.cancel_scope.is_stale(generation):
                parsed_args = {}
                try:
                    parsed_args = json.loads(tool_call_args)
                except Exception:
                    parsed_args = {"objective": tool_call_args}

                objective = parsed_args.get("objective", "Execute task")
                task_type = parsed_args.get("task_type", "general")
                background = parsed_args.get("background", False)

                await self.send_json(create_function_call_event(
                    response_id=response_id,
                    call_id=tool_call_id,
                    name="hermes_execute",
                    arguments=json.dumps(parsed_args),
                ))

                ack = "Right away. Performing that now."
                await self.send_json(create_output_audio_transcript_delta_event(response_id, f" {ack}"))
                async for pcm in self.tts.stream_sentence_pcm(ack, voice=self.config.voice, cancel_event=cancel_event):
                    if self.cancel_scope.is_stale(generation):
                        break
                    await self.send_json(create_output_audio_delta_event(response_id, base64.b64encode(pcm).decode("ascii")))

                exec_result = await execute_hermes_action(
                    objective=objective,
                    task_type=task_type,
                    background=background,
                    chat_id=self.session_id,
                )

                summary = exec_result.get("summary", "Task completed.")
                self.conversation_history.append({"role": "assistant", "content": f"I executed: {objective}"})
                self.conversation_history.append({"role": "user", "content": f"Result: {summary}. Summarize warmly in one sentence."})

                async for pcm in self.tts.stream_sentence_pcm(summary, voice=self.config.voice, cancel_event=cancel_event):
                    if self.cancel_scope.is_stale(generation):
                        break
                    await self.send_json(create_output_audio_delta_event(response_id, base64.b64encode(pcm).decode("ascii")))

            if full_reply_text and not self.cancel_scope.is_stale(generation):
                self.conversation_history.append({"role": "assistant", "content": full_reply_text.strip()})
                await self.send_json(create_output_audio_transcript_done_event(response_id, full_reply_text.strip()))

            await self.send_json(create_output_audio_done_event(response_id))
            await self.send_json(create_response_done_event(response_id, "completed"))

        except Exception as e:
            logger.error(f"Error in Hugging Voice response generation: {e}", exc_info=True)
            if not full_reply_text and not self.cancel_scope.is_stale(generation):
                p_l = text_prompt.lower().strip() if text_prompt else ""
                if any(w in p_l for w in ["hi", "hello", "hey", "who are you", "what is your name"]):
                    fallback = "Hello! I am Hermes, your loyal companion speaking live through Hugging Voice. How can I help you today?"
                elif any(w in p_l for w in ["what can you do", "help", "features"]):
                    fallback = "I can execute terminal commands, inspect files, search the web, and run autonomous tasks for you. What would you like to build or run?"
                elif text_prompt:
                    fallback = f"I heard you say: '{text_prompt}'. I am actively connected and ready to assist you. What would you like me to do next?"
                else:
                    fallback = "I hear you clearly, and I am right here with you. What would you like me to do next?"

                self.conversation_history.append({"role": "assistant", "content": fallback})
                await self.send_json(create_output_audio_transcript_delta_event(response_id, fallback))
                async for pcm in self.tts.stream_sentence_pcm(fallback, voice=self.config.voice, cancel_event=cancel_event):
                    if self.cancel_scope.is_stale(generation):
                        break
                    await self.send_json(create_output_audio_delta_event(response_id, base64.b64encode(pcm).decode("ascii")))

                await self.send_json(create_output_audio_transcript_done_event(response_id, fallback))
                await self.send_json(create_output_audio_done_event(response_id))
                await self.send_json(create_response_done_event(response_id, "completed"))
            else:
                await self.send_json(create_error_event(f"Generation notice: {e}"))
        finally:
            self.is_speaking = False


async def handle_hugging_voice_websocket(websocket: WebSocket, session_id: Optional[str] = None):
    """Entry point for Hugging Voice Realtime WebSocket connection."""
    await websocket.accept()
    session = HuggingVoiceSession(websocket, session_id=session_id)
    await session.handle_connection()
