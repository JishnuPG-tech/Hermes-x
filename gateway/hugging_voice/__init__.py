"""Hugging Voice real-time full-duplex conversational voice package."""

from gateway.hugging_voice.hugging_voice_server import (
    handle_hugging_voice_websocket,
    execute_hermes_action,
)
from gateway.hugging_voice.protocol import (
    RealtimeSessionConfig,
    HERMES_EXECUTE_TOOL,
)
from gateway.hugging_voice.streaming_tts import (
    HuggingVoiceTTS,
    DEFAULT_FEMALE_VOICE,
)
from gateway.hugging_voice.vad import HuggingVoiceVAD

__all__ = [
    "handle_hugging_voice_websocket",
    "execute_hermes_action",
    "RealtimeSessionConfig",
    "HERMES_EXECUTE_TOOL",
    "HuggingVoiceTTS",
    "DEFAULT_FEMALE_VOICE",
    "HuggingVoiceVAD",
]
