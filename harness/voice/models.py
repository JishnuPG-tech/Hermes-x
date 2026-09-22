"""Voice Autonomy Protocol & Data Contracts
========================================
Defines message schemas, state enums, telemetry models, and session entities
for the Voice-First Jarvis runtime.
"""

from __future__ import annotations

import asyncio
import time
import uuid
from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class AudioFormat(str, Enum):
    MP3 = "mp3"
    PCM_16K = "pcm_16k"
    WAV = "wav"
    OPUS = "opus"


class VoiceState(str, Enum):
    IDLE = "idle"
    LISTENING = "listening"
    THINKING = "thinking"
    SPEAKING = "speaking"
    INTERRUPTED = "interrupted"
    BACKGROUND_RUNNING = "background_running"
    DEGRADED = "degraded"


class CancellationScope(str, Enum):
    SPEECH_ONLY = "speech_only"
    TURN = "turn"
    TASK = "task"


class ProviderState(str, Enum):
    UNKNOWN = "unknown"
    HEALTHY = "healthy"
    WARMING = "warming"
    BUSY = "busy"
    DEGRADED = "degraded"
    UNHEALTHY = "unhealthy"
    COOLDOWN = "cooldown"


class ProviderHealth(BaseModel):
    provider: str
    healthy: bool
    status: ProviderState = ProviderState.HEALTHY
    consecutive_failures: int = 0
    last_latency_ms: Optional[float] = None
    circuit_state: str = "CLOSED"
    is_local: bool = False
    error: Optional[str] = None
    last_check: float = Field(default_factory=time.time)


class VoiceTimingMetrics(BaseModel):
    request_id: str = Field(default_factory=lambda: uuid.uuid4().hex)
    turn_id: str = ""
    wake_detected_at: Optional[float] = None
    speech_start_at: Optional[float] = None
    speech_end_at: Optional[float] = None
    stt_start_ms: Optional[float] = None
    stt_duration_ms: Optional[float] = None
    llm_start_ms: Optional[float] = None
    first_audio_latency_ms: Optional[float] = None
    e2e_turn_ms: Optional[float] = None
    interrupt_at: Optional[float] = None


class TTSGenerationRequest(BaseModel):
    model_config = {"arbitrary_types_allowed": True}

    request_id: str = Field(default_factory=lambda: f"tts_{uuid.uuid4().hex[:8]}")
    tts_generation_id: str = Field(default_factory=lambda: f"gen_{uuid.uuid4().hex[:8]}")
    text: str
    voice: Optional[str] = None
    speed: float = 1.0
    cancel_event: Optional[asyncio.Event] = None


class TTSGenerationResult(BaseModel):
    request_id: str
    audio_bytes: bytes
    format: AudioFormat = AudioFormat.MP3
    sample_rate: int = 24000
    latency_ms: float = 0.0
    provider: str = "unknown"
    voice_used: str = "default"
    success: bool = True
    error: Optional[str] = None


class VoiceTurn(BaseModel):
    turn_id: str = Field(default_factory=lambda: f"turn_{uuid.uuid4().hex[:8]}")
    session_id: str = ""
    user_text: str = ""
    assistant_text: str = ""
    provider: str = "kokoro"
    fallback_used: bool = False
    task_id: Optional[str] = None
    interrupted: bool = False
    metrics: VoiceTimingMetrics = Field(default_factory=VoiceTimingMetrics)


class VoiceSession(BaseModel):
    session_id: str = Field(default_factory=lambda: f"vs_{uuid.uuid4().hex[:12]}")
    voice: str = "en-US-ChristopherNeural"
    speed: float = 1.0
    sample_rate: int = 24000
    user_id: str = "default_user"
    device_id: str = "default_device"
    active_task_id: Optional[str] = None
    state: VoiceState = VoiceState.IDLE
    is_speaking: bool = False
    created_at: float = Field(default_factory=time.time)
    last_active: float = Field(default_factory=time.time)
    client_metadata: Dict[str, Any] = Field(default_factory=dict)
    turns: List[VoiceTurn] = Field(default_factory=list)


# ---------------- Client to Server WebSocket Messages ----------------

class SessionOpenMessage(BaseModel):
    type: str = "session_open"
    session_id: Optional[str] = None
    voice: Optional[str] = None
    speed: Optional[float] = None
    sample_rate: Optional[int] = None
    client_metadata: Dict[str, Any] = Field(default_factory=dict)


class AudioStartMessage(BaseModel):
    type: str = "audio_start"
    session_id: Optional[str] = None
    turn_id: Optional[str] = None
    format: AudioFormat = AudioFormat.MP3
    sample_rate: int = 24000


class AudioChunkMessage(BaseModel):
    type: str = "audio_chunk"
    session_id: Optional[str] = None
    turn_id: Optional[str] = None
    sequence: int = 0
    data: str = ""


class AudioEndMessage(BaseModel):
    type: str = "audio_end"
    session_id: Optional[str] = None
    turn_id: Optional[str] = None


class TextInputMessage(BaseModel):
    type: str = "text_input"
    session_id: Optional[str] = None
    text: str
    barge_in: bool = False


class CommandCancelMessage(BaseModel):
    type: str = "command_cancel"
    session_id: Optional[str] = None
    scope: str = "speech_only"
    task_id: Optional[str] = None


# ---------------- Server to Client WebSocket Messages ----------------

class AssistantTextMessage(BaseModel):
    type: str = "assistant_text"
    text: str
    is_final: bool = False
    turn_id: Optional[str] = None


class AssistantAudioMessage(BaseModel):
    type: str = "assistant_audio"
    chunk_index: int = 0
    data: str = ""
    provider: str = ""
    format: str = "mp3"
    is_final: bool = False


class AssistantStateMessage(BaseModel):
    type: str = "assistant_state"
    session_id: str
    state: str = "idle"
    current_task_id: Optional[str] = None
    metrics: Optional[VoiceTimingMetrics] = None


class TaskUpdateMessage(BaseModel):
    type: str = "task_update"
    task_id: str
    status: str
    action_summary: Optional[str] = None


class ErrorMessage(BaseModel):
    type: str = "error"
    error: str
    code: str = "VOICE_ERROR"
    fatal: bool = False
