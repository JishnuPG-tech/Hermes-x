"""FastAPI Router for Voice Autonomy Subsystem.

Provides:
- Realtime duplex WebSocket endpoints:
    /v1/voice/ws
    /voice
- Health and observability:
    /health/voice
- Voice catalog and testing:
    /v1/voice/voices
    /v1/voice/synthesize
- REST turn endpoint:
    /v1/voice/turn
"""

from __future__ import annotations

import json
import logging
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, HTTPException, Query, Request, Response, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse, Response, StreamingResponse
from pydantic import BaseModel, Field

from harness.voice import get_voice_gateway
from harness.voice.models import AudioFormat, TTSGenerationRequest

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Hermes Voice Autonomy"])


class SynthesizeRequest(BaseModel):
    text: str = Field(..., description="Text to synthesize")
    voice: Optional[str] = Field("en-US-ChristopherNeural", description="Voice ID")
    speed: Optional[float] = Field(1.0, description="Speech rate multiplier")
    format: Optional[str] = Field("mp3", description="Audio format: mp3 or pcm")


class VoiceTurnRequest(BaseModel):
    session_id: Optional[str] = Field(None, description="Session ID")
    text: str = Field(..., description="User input text")
    voice: Optional[str] = Field(None, description="Voice ID override")


# ── Full-Duplex WebSockets ───────────────────────────────────

@router.websocket("/v1/voice/ws")
@router.websocket("/voice")
async def voice_websocket_endpoint(websocket: WebSocket, session_id: Optional[str] = Query(None)):
    """Full-duplex WebSocket connection for streaming speech and receiving synthesised audio."""
    gateway = get_voice_gateway()
    await gateway.handle_websocket_connection(websocket, session_id=session_id)


# ── Health & Diagnostics ─────────────────────────────────────

@router.get("/health/voice")
async def voice_health():
    """Voice subsystem health endpoint reporting TTS provider statuses and circuit breakers."""
    gateway = get_voice_gateway()
    tts_health = await gateway.tts.get_health_status()

    # Determine overall health: at least one TTS provider must be healthy or operational
    any_healthy = any(p.healthy for p in tts_health.values())

    return {
        "status": "healthy" if any_healthy else "degraded",
        "service": "voice-autonomy",
        "providers": {k: v.model_dump() for k, v in tts_health.items()},
        "sessions_active": len(gateway.sessions._sessions),
    }


# ── Voice Catalog & Synthesis APIs ──────────────────────────

@router.get("/v1/voice/voices")
async def list_voices():
    """List supported voice profiles."""
    return {
        "voices": [
            {
                "id": "en-US-ChristopherNeural",
                "name": "Christopher (Neural)",
                "gender": "male",
                "language": "en-US",
                "provider": "edge_tts",
                "recommended": True,
            },
            {
                "id": "en-US-GuyNeural",
                "name": "Guy (Neural)",
                "gender": "male",
                "language": "en-US",
                "provider": "edge_tts",
                "recommended": False,
            },
            {
                "id": "en-US-EricNeural",
                "name": "Eric (Neural)",
                "gender": "male",
                "language": "en-US",
                "provider": "edge_tts",
                "recommended": False,
            },
            {
                "id": "en-GB-RyanNeural",
                "name": "Ryan (Neural UK)",
                "gender": "male",
                "language": "en-GB",
                "provider": "edge_tts",
                "recommended": False,
            },
            {
                "id": "am_adam",
                "name": "Adam (Local Kokoro)",
                "gender": "male",
                "language": "en-US",
                "provider": "kokoro",
                "recommended": False,
            },
        ]
    }


@router.post("/v1/voice/synthesize")
async def synthesize_speech(req: SynthesizeRequest):
    """Direct REST endpoint to synthesize text into audio stream."""
    gateway = get_voice_gateway()
    tts_req = TTSGenerationRequest(
        text=req.text,
        voice=req.voice or "en-US-ChristopherNeural",
        speed=req.speed or 1.0,
    )
    result = await gateway.tts.synthesize(tts_req)
    if not result.success:
        raise HTTPException(status_code=500, detail=f"Synthesis error: {result.error}")

    media_type = "audio/mpeg" if result.format == AudioFormat.MP3 else "audio/pcm"
    return Response(
        content=result.audio_bytes,
        media_type=media_type,
        headers={
            "X-TTS-Provider": result.provider,
            "X-TTS-Latency-MS": str(result.latency_ms),
        },
    )


@router.post("/v1/voice/turn")
async def execute_voice_turn(req: VoiceTurnRequest):
    """Stateless or session-backed voice turn endpoint returning generated text and audio bytes."""
    gateway = get_voice_gateway()
    sid = req.session_id or f"turn_{uuid_hex()}"
    session = gateway.sessions.get_or_create_session(sid)
    if req.voice:
        session.voice = req.voice

    # Synthesize simple response via agent or prompt
    tts_req = TTSGenerationRequest(
        text=req.text,
        voice=session.voice,
        speed=session.speed,
    )
    result = await gateway.tts.synthesize(tts_req)
    return {
        "session_id": sid,
        "input": req.text,
        "success": result.success,
        "audio_bytes_length": len(result.audio_bytes),
        "latency_ms": result.latency_ms,
        "provider": result.provider,
    }


def uuid_hex() -> str:
    import uuid
    return uuid.uuid4().hex[:8]
