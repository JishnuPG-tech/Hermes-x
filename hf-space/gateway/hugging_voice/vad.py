"""Voice Activity Detection for Hugging Voice.

Implements frame-based speech onset and silence offset detection
with pre-speech ring buffer to prevent cutting initial consonants.
"""

from __future__ import annotations
import math
import struct
import logging
from collections import deque
from typing import Tuple

logger = logging.getLogger("hermes.hugging_voice.vad")


class HuggingVoiceVAD:
    """Voice Activity Detector optimized for 16kHz PCM16 streaming chunks."""

    def __init__(
        self,
        sample_rate: int = 16000,
        energy_threshold: float = 0.025,
        min_speech_ms: int = 150,
        min_silence_ms: int = 400,
        pre_speech_pad_ms: int = 250,
    ):
        self.sample_rate = sample_rate
        self.energy_threshold = energy_threshold
        self.min_speech_ms = min_speech_ms
        self.min_silence_ms = min_silence_ms
        self.pre_speech_pad_ms = pre_speech_pad_ms

        self._speech_confirmed = False
        self._consecutive_speech_ms = 0
        self._consecutive_silence_ms = 0

        max_pre_chunks = max(3, int(self.pre_speech_pad_ms / 30))
        self._pre_buffer = deque(maxlen=max_pre_chunks)
        self._speech_buffer = bytearray()

    def reset(self):
        self._speech_confirmed = False
        self._consecutive_speech_ms = 0
        self._consecutive_silence_ms = 0
        self._pre_buffer.clear()
        self._speech_buffer.clear()

    def compute_rms(self, pcm16_bytes: bytes) -> float:
        count = len(pcm16_bytes) // 2
        if count == 0:
            return 0.0
        try:
            shorts = struct.unpack(f"<{count}h", pcm16_bytes[: count * 2])
            sum_sq = sum(s * s for s in shorts)
            mean_sq = sum_sq / count
            return math.sqrt(mean_sq) / 32768.0
        except Exception:
            return 0.0

    def process_chunk(self, pcm_chunk: bytes) -> Tuple[float, bool, bool]:
        if not pcm_chunk:
            return 0.0, False, False

        chunk_dur_ms = int((len(pcm_chunk) / (self.sample_rate * 2)) * 1000)
        rms = self.compute_rms(pcm_chunk)
        is_chunk_speech = rms >= self.energy_threshold

        if not self._speech_confirmed:
            self._pre_buffer.append(pcm_chunk)
            if is_chunk_speech:
                self._consecutive_speech_ms += chunk_dur_ms
                if self._consecutive_speech_ms >= self.min_speech_ms:
                    self._speech_confirmed = True
                    self._consecutive_silence_ms = 0
                    for b in self._pre_buffer:
                        self._speech_buffer.extend(b)
                    self._pre_buffer.clear()
                    return rms, True, False
            else:
                self._consecutive_speech_ms = max(0, self._consecutive_speech_ms - chunk_dur_ms)
            return rms, False, False

        self._speech_buffer.extend(pcm_chunk)
        if not is_chunk_speech:
            self._consecutive_silence_ms += chunk_dur_ms
            if self._consecutive_silence_ms >= self.min_silence_ms:
                return rms, False, True
        else:
            self._consecutive_silence_ms = 0

        return rms, True, False

    def get_speech_pcm(self) -> bytes:
        return bytes(self._speech_buffer)
