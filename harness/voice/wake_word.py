"""Wake-Word Detection Subsystem ("Hermes").

Monitors incoming streaming audio frames for the specific wake-word trigger ("Hermes").
Employs acoustic energy thresholding, zero-crossing rate (ZCR) phonetic profiling,
and optional rapid STT keyword verification, transitioning the voice session state from IDLE to LISTENING.
"""

from __future__ import annotations

import asyncio
import logging
import math
import struct
import time
from typing import Optional

logger = logging.getLogger(__name__)


class WakeWordDetector:
    """Detects 'Hermes' wake word from continuous PCM audio streams.
    
    Phonetic Profile of 'Hermes' (/ˈhɜːr.miːz/):
    1. Initial aspiration & open vowel (/h/, /ɜːr/): low-to-medium ZCR, moderate energy.
    2. Bilabial nasal closure (/m/): dip in high-frequency energy.
    3. Close front vowel (/iː/): harmonic formant peak.
    4. Terminal voiceless alveolar sibilant (/z/ or /s/): high ZCR (>0.15), sharp fricative tail.
    """

    def __init__(
        self,
        wake_phrase: str = "hermes",
        sample_rate: int = 16000,
        energy_threshold: float = 600.0,
        stt_adapter: Optional[Any] = None,
    ) -> None:
        self.wake_phrase = wake_phrase.lower()
        self.sample_rate = sample_rate
        self.energy_threshold = energy_threshold
        self.stt_adapter = stt_adapter
        
        self._consecutive_speech_ms = 0.0
        self._history_zcr: list[float] = []
        self._history_rms: list[float] = []
        self._buffer: bytearray = bytearray()
        self._last_trigger_time: float = 0.0

    def process_pcm16_chunk(self, chunk: bytes) -> bool:
        """Analyze 16-bit PCM chunk and returns True when 'Hermes' wake word is detected."""
        if len(chunk) < 2:
            return False

        count = len(chunk) // 2
        shorts = struct.unpack(f"<{count}h", chunk[: count * 2])
        if count == 0:
            return False

        # 1. Compute RMS Energy
        sum_sq = sum(s * s for s in shorts)
        rms = math.sqrt(sum_sq / count)

        # 2. Compute Zero-Crossing Rate (ZCR) for fricative/sibilant detection (/s/, /z/)
        crossings = sum(1 for i in range(1, count) if (shorts[i - 1] >= 0 > shorts[i]) or (shorts[i - 1] < 0 <= shorts[i]))
        zcr = crossings / count

        chunk_ms = (count / self.sample_rate) * 1000.0

        if rms >= self.energy_threshold:
            self._consecutive_speech_ms += chunk_ms
            self._history_zcr.append(zcr)
            self._history_rms.append(rms)
            self._buffer.extend(chunk)

            # Limit history to ~1.2s window
            max_frames = int((1200.0 / chunk_ms))
            if len(self._history_zcr) > max_frames:
                self._history_zcr.pop(0)
                self._history_rms.pop(0)
                del self._buffer[:count * 2]

            # Duration check: "Hermes" spoken clearly takes ~350ms to 950ms
            if 350.0 <= self._consecutive_speech_ms <= 950.0:
                # Sibilance check: the ending of "Hermes" (/z/ or /s/) exhibits high ZCR (>0.12)
                recent_zcr = self._history_zcr[-4:]
                has_sibilant_tail = any(z >= 0.12 for z in recent_zcr)
                
                # Vowel/nasal core check: beginning/middle should have moderate ZCR (0.02 - 0.10)
                early_zcr = self._history_zcr[:max(1, len(self._history_zcr) // 2)]
                has_vocalic_core = any(0.01 <= z <= 0.12 for z in early_zcr)

                if has_sibilant_tail and has_vocalic_core:
                    now = time.time()
                    # Cooldown to prevent multi-triggering
                    if now - self._last_trigger_time > 1.5:
                        self._last_trigger_time = now
                        self._reset_state()
                        return True
        else:
            # Silence / below threshold resets speech counter
            if self._consecutive_speech_ms > 0:
                self._reset_state()

        return False

    async def verify_keyword_async(self, audio_pcm: bytes) -> bool:
        """Stage 2: Verifies candidate audio buffer against STT keywords if STT adapter is available."""
        if not self.stt_adapter:
            return True

        try:
            transcript = await self.stt_adapter.transcribe_audio_buffer(audio_pcm, sample_rate=self.sample_rate)
            cleaned = transcript.lower().strip()
            # Fuzzy match for wake word
            return any(k in cleaned for k in ["hermes", "hermis", "her meez", "hermis"])
        except Exception as e:
            logger.warning("Wake word STT verification error: %s", e)
            return True

    def _reset_state(self) -> None:
        self._consecutive_speech_ms = 0.0
        self._history_zcr.clear()
        self._history_rms.clear()
        self._buffer.clear()
