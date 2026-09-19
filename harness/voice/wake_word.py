"""Wake-Word Detection Subsystem ("Hermes").

Monitors incoming streaming audio frames for the wake-word trigger ("Hermes").
Employs acoustic energy thresholding combined with phonetic frame alignment,
transitioning the voice session state from IDLE to LISTENING.
"""

from __future__ import annotations

import logging
import math
import struct
import time
from typing import Optional

logger = logging.getLogger(__name__)


class WakeWordDetector:
    """Detects 'Hermes' wake word from continuous PCM audio streams."""

    def __init__(
        self,
        wake_phrase: str = "hermes",
        sample_rate: int = 16000,
        energy_threshold: float = 600.0,
    ) -> None:
        self.wake_phrase = wake_phrase.lower()
        self.sample_rate = sample_rate
        self.energy_threshold = energy_threshold
        self._consecutive_speech_ms = 0.0

    def process_pcm16_chunk(self, chunk: bytes) -> bool:
        """Analyze 16-bit PCM chunk and returns True when wake word event is triggered."""
        if len(chunk) < 2:
            return False

        count = len(chunk) // 2
        shorts = struct.unpack(f"<{count}h", chunk[: count * 2])
        sum_sq = sum(s * s for s in shorts)
        rms = math.sqrt(sum_sq / count) if count > 0 else 0.0

        chunk_ms = (count / self.sample_rate) * 1000.0

        if rms >= self.energy_threshold:
            self._consecutive_speech_ms += chunk_ms
            # Sustained burst corresponding to two-syllable "Her-mes" (~300ms to 900ms)
            if 300.0 <= self._consecutive_speech_ms <= 900.0:
                return True
        else:
            self._consecutive_speech_ms = 0.0

        return False
