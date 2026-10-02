"""FastAPI WebSocket server for Hermes Voice Agent.

Multiplexed Protocol:
- Binary frames:
  - 0x01 + PCM16 16kHz audio: Client -> Server microphone chunk
  - 0x02 + Audio payload: Server -> Client synthesized speech chunk
- JSON text frames:
  - session_start, session_ready, assistant_state
  - transcript_partial, transcript_final, assistant_text
  - tts_start, tts_end, user_interrupt, agent_interrupted
"""

import os
import uuid
import json
import time
import asyncio
import logging
from typing import Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from vad import SileroVADDetector
from asr import WhisperASREngine
from llm import StreamingLLMClient, DEFAULT_VOICE_PROMPT
from tts import StreamingTTSEngine
from state import VoiceSessionState, VoiceSessionContext

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("hermes.voice.app")

app = FastAPI(title="Hermes Voice Agent Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Singletons initialized at startup
asr_engine: Optional[WhisperASREngine] = None
tts_engine: Optional[StreamingTTSEngine] = None
llm_client: Optional[StreamingLLMClient] = None

TAG_MIC_CHUNK = 0x01
TAG_TTS_CHUNK = 0x02


@app.on_event("startup")
async def on_startup():
    global asr_engine, tts_engine, llm_client
    logger.info("Initializing Hermes Voice Agent components...")
    asr_engine = WhisperASREngine.get_instance()
    tts_engine = StreamingTTSEngine()
    llm_client = StreamingLLMClient()
    logger.info("Hermes Voice Agent ready to serve voice traffic.")


@app.get("/health")
async def health():
    return JSONResponse({
        "status": "healthy",
        "service": "hermes-voice-agent",
        "has_whisper": asr_engine is not None and asr_engine.model is not None,
        "has_piper": tts_engine is not None and tts_engine.has_piper,
    })


@app.websocket("/v1/voice/ws")
@app.websocket("/ws/voice")
async def websocket_voice_endpoint(websocket: WebSocket):
    await websocket.accept()
    session_id = f"v_sess_{uuid.uuid4().hex[:10]}"
    ctx = VoiceSessionContext(session_id)
    vad = SileroVADDetector(sample_rate=16000)

    logger.info(f"Voice WebSocket connected: {session_id}")

    async def send_json(data: dict):
        try:
            await websocket.send_text(json.dumps(data))
        except Exception:
            pass

    async def send_state(state: VoiceSessionState):
        ctx.state = state
        await send_json({"type": "assistant_state", "state": state.value})

    async def execute_turn(audio_pcm: bytes, turn_id: str, cancel_event: asyncio.Event):
        """Pipeline: Whisper ASR -> OmniRoute LLM streaming -> Piper/Edge TTS."""
        if cancel_event.is_set():
            return

        t_start = time.perf_counter()
        await send_state(VoiceSessionState.TRANSCRIBING)

        # 1. ASR
        transcript = ""
        if asr_engine:
            transcript = await asr_engine.transcribe(audio_pcm, sample_rate=16000)

        if not transcript.strip() or cancel_event.is_set():
            logger.info(f"Empty transcript or cancelled for turn {turn_id}")
            vad.reset()
            await send_state(VoiceSessionState.LISTENING)
            return

        await send_json({
            "type": "transcript_final",
            "text": transcript,
            "turn_id": turn_id,
        })

        # Append to conversational context
        ctx.history.append({"role": "user", "content": transcript})
        if len(ctx.history) > 10:
            ctx.history = ctx.history[-10:]

        # 2. LLM Streaming
        await send_state(VoiceSessionState.THINKING)
        messages = [{"role": "system", "content": DEFAULT_VOICE_PROMPT}] + ctx.history

        full_reply_parts = []
        is_first_phrase = True

        async for phrase in llm_client.stream_sentences(
            messages=messages,
            model=ctx.model,
            cancel_event=cancel_event,
        ):
            if cancel_event.is_set():
                break

            full_reply_parts.append(phrase)

            if is_first_phrase:
                is_first_phrase = False
                await send_state(VoiceSessionState.SPEAKING)

            # Send text accompaniment
            await send_json({
                "type": "assistant_text",
                "text": phrase,
                "turn_id": turn_id,
            })

            # 3. TTS Synthesis
            await send_json({"type": "tts_start", "turn_id": turn_id})

            async for audio_chunk in tts_engine.stream_audio(
                text=phrase,
                voice=ctx.voice,
                speed=ctx.speed,
                cancel_event=cancel_event,
            ):
                if cancel_event.is_set():
                    break
                # Multiplexed frame with 0x02 header
                tagged_packet = bytes([TAG_TTS_CHUNK]) + audio_chunk
                try:
                    await websocket.send_bytes(tagged_packet)
                except Exception:
                    break

            await send_json({"type": "tts_end", "turn_id": turn_id})

        if full_reply_parts and not cancel_event.is_set():
            assistant_reply = " ".join(full_reply_parts)
            ctx.history.append({"role": "assistant", "content": assistant_reply})

        t_end = time.perf_counter()
        logger.info(f"Turn {turn_id} completed in {(t_end - t_start):.2f}s")

        vad.reset()
        if not cancel_event.is_set():
            await send_state(VoiceSessionState.LISTENING)

    try:
        # Initial ready handshake
        await send_json({
            "type": "session_ready",
            "session_id": session_id,
            "sample_rate": 16000,
            "format": "pcm16",
        })
        await send_state(VoiceSessionState.LISTENING)

        while True:
            message = await websocket.receive()
            if "bytes" in message and message["bytes"]:
                raw_bytes = message["bytes"]
                # Check for multiplexed protocol header 0x01
                if len(raw_bytes) > 1 and raw_bytes[0] == TAG_MIC_CHUNK:
                    pcm_chunk = raw_bytes[1:]
                else:
                    # Fallback for raw untagged PCM16 chunks
                    pcm_chunk = raw_bytes

                # If speaking, check for barge-in energy
                if ctx.state == VoiceSessionState.SPEAKING:
                    # Check acoustic barge-in
                    prob, _, _ = vad.process_chunk(pcm_chunk)
                    if prob > 0.8:
                        logger.info(f"Barge-in speech detected during speaking in {session_id}")
                        ctx.cancel_active_turn()
                        await send_json({"type": "agent_interrupted", "turn_id": ctx.active_turn_id or ""})
                        vad.reset()
                        await send_state(VoiceSessionState.CAPTURING)
                    continue

                prob, is_speech_active, is_turn_complete = vad.process_chunk(pcm_chunk)

                if is_speech_active and ctx.state == VoiceSessionState.LISTENING:
                    await send_state(VoiceSessionState.CAPTURING)

                if is_turn_complete:
                    speech_pcm = vad.get_speech_pcm()
                    vad.reset()
                    turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                    cancel_event = ctx.start_new_turn(turn_id)
                    ctx.active_pipeline_task = asyncio.create_task(
                        execute_turn(speech_pcm, turn_id, cancel_event)
                    )

            elif "text" in message and message["text"]:
                try:
                    data = json.loads(message["text"])
                except Exception:
                    continue

                msg_type = data.get("type")

                if msg_type in ("session_start", "session_open"):
                    ctx.voice = data.get("voice", ctx.voice)
                    ctx.speed = float(data.get("speed", 1.0))
                    ctx.sample_rate = int(data.get("sample_rate", 16000))
                    metadata = data.get("client_metadata", {})
                    ctx.user_name = metadata.get("user_name", "Jishnu")

                    greet = metadata.get("greet", False)
                    if greet:
                        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                        cancel_event = ctx.start_new_turn(turn_id)

                        async def do_greet():
                            greeting = f"Hi {ctx.user_name}, I'm Hermes. How can I help you today?"
                            await send_state(VoiceSessionState.SPEAKING)
                            await send_json({"type": "assistant_text", "text": greeting, "turn_id": turn_id})
                            await send_json({"type": "tts_start", "turn_id": turn_id})
                            async for chunk in tts_engine.stream_audio(greeting, voice=ctx.voice, speed=ctx.speed, cancel_event=cancel_event):
                                if cancel_event.is_set():
                                    break
                                tagged = bytes([TAG_TTS_CHUNK]) + chunk
                                await websocket.send_bytes(tagged)
                            await send_json({"type": "tts_end", "turn_id": turn_id})
                            vad.reset()
                            await send_state(VoiceSessionState.LISTENING)

                        ctx.active_pipeline_task = asyncio.create_task(do_greet())
                    else:
                        await send_state(VoiceSessionState.LISTENING)

                elif msg_type in ("user_interrupt", "command_cancel"):
                    logger.info(f"Explicit user interrupt for session {session_id}")
                    ctx.cancel_active_turn()
                    await send_json({"type": "agent_interrupted", "turn_id": ctx.active_turn_id or ""})
                    vad.reset()
                    await send_state(VoiceSessionState.LISTENING)

                elif msg_type in ("end_of_utterance", "audio_end"):
                    speech_pcm = vad.get_speech_pcm()
                    if speech_pcm and len(speech_pcm) > 1000:
                        vad.reset()
                        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                        cancel_event = ctx.start_new_turn(turn_id)
                        ctx.active_pipeline_task = asyncio.create_task(
                            execute_turn(speech_pcm, turn_id, cancel_event)
                        )

                elif msg_type == "text_input":
                    text = data.get("text", "").strip()
                    if text:
                        ctx.cancel_active_turn()
                        vad.reset()
                        turn_id = f"turn_{uuid.uuid4().hex[:8]}"
                        cancel_event = ctx.start_new_turn(turn_id)

                        async def do_text_turn():
                            await send_state(VoiceSessionState.THINKING)
                            ctx.history.append({"role": "user", "content": text})
                            messages = [{"role": "system", "content": DEFAULT_VOICE_PROMPT}] + ctx.history
                            full_reply_parts = []
                            is_first_phrase = True

                            async for phrase in llm_client.stream_sentences(
                                messages=messages,
                                model=data.get("model", ctx.model),
                                cancel_event=cancel_event,
                            ):
                                if cancel_event.is_set():
                                    break
                                full_reply_parts.append(phrase)
                                if is_first_phrase:
                                    is_first_phrase = False
                                    await send_state(VoiceSessionState.SPEAKING)

                                await send_json({"type": "assistant_text", "text": phrase, "turn_id": turn_id})
                                await send_json({"type": "tts_start", "turn_id": turn_id})
                                async for chunk in tts_engine.stream_audio(phrase, voice=ctx.voice, speed=ctx.speed, cancel_event=cancel_event):
                                    if cancel_event.is_set():
                                        break
                                    tagged = bytes([TAG_TTS_CHUNK]) + chunk
                                    await websocket.send_bytes(tagged)
                                await send_json({"type": "tts_end", "turn_id": turn_id})

                            if full_reply_parts and not cancel_event.is_set():
                                ctx.history.append({"role": "assistant", "content": " ".join(full_reply_parts)})

                            vad.reset()
                            if not cancel_event.is_set():
                                await send_state(VoiceSessionState.LISTENING)

                        ctx.active_pipeline_task = asyncio.create_task(do_text_turn())

                elif msg_type == "playback_state":
                    pstate = data.get("state")
                    if pstate == "interrupted":
                        ctx.cancel_active_turn()
                        await send_state(VoiceSessionState.LISTENING)

    except WebSocketDisconnect:
        logger.info(f"WebSocket client disconnected: {session_id}")
    except Exception as e:
        logger.error(f"WebSocket error in {session_id}: {e}", exc_info=True)
    finally:
        ctx.cancel_active_turn()


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 7860))
    uvicorn.run("app:app", host="0.0.0.0", port=port, reload=False)
