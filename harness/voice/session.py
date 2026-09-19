"""Voice session lifecycle management.

Manages active WebSockets, conversation state, turn history, and cancellation tokens.
"""

from __future__ import annotations

import asyncio
import logging
import time
from typing import Dict, List, Optional

from harness.voice.models import SessionOpenMessage, VoiceSession, VoiceTurn

logger = logging.getLogger(__name__)


class VoiceSessionManager:
    """Manages active realtime voice sessions."""

    def __init__(self, session_ttl_seconds: float = 3600.0) -> None:
        self.session_ttl = session_ttl_seconds
        self._sessions: Dict[str, VoiceSession] = {}
        self._locks: Dict[str, asyncio.Lock] = {}

    def get_or_create_session(
        self,
        session_id: str,
        open_msg: Optional[SessionOpenMessage] = None,
    ) -> VoiceSession:
        if session_id in self._sessions:
            sess = self._sessions[session_id]
            sess.last_active = time.time()
            return sess

        voice = "en-US-ChristopherNeural"
        speed = 1.0
        sample_rate = 24000
        client_metadata = {}

        if open_msg:
            if open_msg.voice:
                voice = open_msg.voice
            if open_msg.speed:
                speed = open_msg.speed
            if open_msg.sample_rate:
                sample_rate = open_msg.sample_rate
            if open_msg.client_metadata:
                client_metadata = open_msg.client_metadata

        sess = VoiceSession(
            session_id=session_id,
            voice=voice,
            speed=speed,
            sample_rate=sample_rate,
            client_metadata=client_metadata,
        )
        self._sessions[session_id] = sess
        self._locks[session_id] = asyncio.Lock()
        logger.info("New voice session created: %s (voice: %s)", session_id, voice)
        return sess

    def get_session(self, session_id: str) -> Optional[VoiceSession]:
        return self._sessions.get(session_id)

    def get_lock(self, session_id: str) -> asyncio.Lock:
        if session_id not in self._locks:
            self._locks[session_id] = asyncio.Lock()
        return self._locks[session_id]

    def close_session(self, session_id: str) -> Optional[VoiceSession]:
        self._locks.pop(session_id, None)
        sess = self._sessions.pop(session_id, None)
        if sess:
            logger.info("Voice session closed: %s (turns: %d)", session_id, len(sess.turns))
        return sess

    def prune_expired(self) -> int:
        now = time.time()
        expired = [
            sid for sid, s in self._sessions.items()
            if now - s.last_active > self.session_ttl
        ]
        for sid in expired:
            self.close_session(sid)
        return len(expired)
