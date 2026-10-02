"""Turn and silence detector for audio streams.

Calculates audio energy (RMS) to determine silence boundaries and turn completion.
"""

from __future__ import annotations

import math
import struct
from typing import Optional


class SilenceTurnDetector:
    """Detects end of utterance via silence threshold on incoming PCM audio chunks."""

    def __init__(
        self,
        silence_duration_ms: float = 700.0,
        energy_threshold: float = 300.0,
        sample_rate: int = 16000,
    ) -> None:
        self.silence_duration_ms = silence_duration_ms
        self.energy_threshold = energy_threshold
        self.sample_rate = sample_rate

        self._consecutive_silence_ms: float = 0.0
        self._has_speech_started: bool = False

    def reset(self) -> None:
        self._consecutive_silence_ms = 0.0
        self._has_speech_started = False

    def process_pcm16_chunk(self, chunk: bytes) -> bool:
        """Process 16-bit PCM chunk and returns True if turn is considered finished."""
        if len(chunk) < 2:
            return False

        # Compute RMS energy
        count = len(chunk) // 2
        shorts = struct.unpack(f"<{count}h", chunk[: count * 2])
        sum_sq = sum(s * s for s in shorts)
        rms = math.sqrt(sum_sq / count) if count > 0 else 0.0

        chunk_ms = (count / self.sample_rate) * 1000.0

        if rms >= self.energy_threshold:
            self._has_speech_started = True
            self._consecutive_silence_ms = 0.0
            return False
        else:
            if self._has_speech_started:
                self._consecutive_silence_ms += chunk_ms
                if self._consecutive_silence_ms >= self.silence_duration_ms:
                    return True
            return False
