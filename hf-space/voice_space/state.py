"""Session and Turn State Management for Hermes Voice Agent."""

import time
import asyncio
from enum import Enum
from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class VoiceSessionState(str, Enum):
    IDLE = "idle"
    LISTENING = "listening"
    CAPTURING = "capturing"
    TRANSCRIBING = "transcribing"
    THINKING = "thinking"
    SPEAKING = "speaking"
    MUTED = "muted"
    ERROR = "error"


class VoiceSessionContext:
    """State tracking for a single active client WebSocket voice session."""

    def __init__(self, session_id: str):
        self.session_id = session_id
        self.state: VoiceSessionState = VoiceSessionState.IDLE
        self.voice: str = "en-US-ChristopherNeural"
        self.speed: float = 1.0
        self.sample_rate: int = 16000
        self.user_name: str = "Jishnu"
        self.model: str = "hermes-agent"

        # Active turn synchronization
        self.active_turn_id: Optional[str] = None
        self.cancel_event: asyncio.Event = asyncio.Event()
        self.active_pipeline_task: Optional[asyncio.Task] = None

        # Turn conversation history for conversational context
        self.history: List[Dict[str, str]] = []

    def start_new_turn(self, turn_id: str) -> asyncio.Event:
        """Cancel any previous in-flight processing and start a fresh turn."""
        self.cancel_active_turn()
        self.active_turn_id = turn_id
        self.cancel_event = asyncio.Event()
        return self.cancel_event

    def cancel_active_turn(self):
        """Signal immediate cancellation to any active LLM or TTS generators."""
        if not self.cancel_event.is_set():
            self.cancel_event.set()
        if self.active_pipeline_task and not self.active_pipeline_task.done():
            self.active_pipeline_task.cancel()
            self.active_pipeline_task = None
        self.active_turn_id = None
