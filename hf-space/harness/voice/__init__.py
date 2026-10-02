"""Hermes Voice Autonomy Subsystem.

Exports public models, chunker, TTS manager, and voice gateway.
"""

from __future__ import annotations

from typing import Optional

from harness.voice.chunker import SentenceChunker
from harness.voice.models import (
    AudioFormat,
    AudioChunkMessage,
    AssistantStateMessage,
    AssistantTextMessage,
    CommandCancelMessage,
    ProviderHealth,
    SessionOpenMessage,
    TextInputMessage,
    TTSGenerationRequest,
    TTSGenerationResult,
    VoiceSession,
    VoiceTimingMetrics,
    VoiceTurn,
)
from harness.voice.session import VoiceSessionManager
from harness.voice.tts.manager import TTSManager
from harness.voice.voice_gateway import VoiceGateway

__all__ = [
    "SentenceChunker",
    "TTSManager",
    "VoiceGateway",
    "VoiceSessionManager",
    "AudioFormat",
    "SessionOpenMessage",
    "TextInputMessage",
    "CommandCancelMessage",
    "AssistantTextMessage",
    "AssistantStateMessage",
    "TTSGenerationRequest",
    "TTSGenerationResult",
    "ProviderHealth",
    "VoiceSession",
    "VoiceTurn",
    "VoiceTimingMetrics",
    "get_voice_gateway",
]

_GATEWAY_INSTANCE: Optional[VoiceGateway] = None


def get_voice_gateway() -> VoiceGateway:
    """Singleton getter for the global VoiceGateway."""
    global _GATEWAY_INSTANCE
    if _GATEWAY_INSTANCE is None:
        _GATEWAY_INSTANCE = VoiceGateway()
    return _GATEWAY_INSTANCE
