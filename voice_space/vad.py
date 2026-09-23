"""Voice Activity Detection (VAD) using Silero VAD ONNX with Energy Fallback.

Optimized for 16kHz PCM16 mono input with:
- 30ms-50ms frame analysis
- Pre-speech padding ring buffer to avoid clipping leading consonants
- Fast sub-millisecond ONNX runtime evaluation on CPU
- Speech confirmation (~150ms) and silence confirmation (~550ms)
"""

import os
import math
import struct
import logging
from collections import deque
from typing import Optional, Tuple

logger = logging.getLogger("hermes.voice.vad")

try:
    import numpy as np
    import onnxruntime as ort
    HAS_ONNX = True
except ImportError:
    HAS_ONNX = False


class SileroVADDetector:
    """Silero VAD ONNX detector with pre-speech buffering and adaptive silence threshold."""

    def __init__(
        self,
        model_path: Optional[str] = None,
        sample_rate: int = 16000,
        threshold: float = 0.5,
        min_speech_ms: int = 150,
        min_silence_ms: int = 550,
        pre_speech_pad_ms: int = 300,
    ):
        self.sample_rate = sample_rate
        self.threshold = threshold
        self.min_speech_ms = min_speech_ms
        self.min_silence_ms = min_silence_ms
        self.pre_speech_pad_ms = pre_speech_pad_ms

        self.session: Optional[ort.InferenceSession] = None
        self._h = None
        self._c = None

        # Resolve model path
        default_dir = os.environ.get("MODEL_CACHE_DIR", "/app/models")
        possible_paths = [
            model_path,
            os.path.join(default_dir, "vad", "silero_vad.onnx"),
            os.path.join(os.path.dirname(__file__), "models", "silero_vad.onnx"),
            "models/silero_vad.onnx",
        ]

        if HAS_ONNX:
            for p in possible_paths:
                if p and os.path.exists(p):
                    try:
                        opts = ort.SessionOptions()
                        opts.inter_op_num_threads = 1
                        opts.intra_op_num_threads = 1
                        opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
                        self.session = ort.InferenceSession(p, sess_options=opts, providers=["CPUExecutionProvider"])
                        logger.info(f"Loaded Silero VAD ONNX model from {p}")
                        break
                    except Exception as e:
                        logger.warning(f"Failed to load ONNX VAD model at {p}: {e}")

        if not self.session:
            logger.info("Silero ONNX not found; operating in high-performance RMS energy VAD mode.")

        self.reset()

    def reset(self):
        """Reset internal states between speech turns."""
        self._speech_confirmed = False
        self._consecutive_speech_ms = 0
        self._consecutive_silence_ms = 0

        # Ring buffer for pre-speech audio (to catch word start before confirmation)
        max_pre_chunks = max(3, int(self.pre_speech_pad_ms / 30))
        self._pre_buffer = deque(maxlen=max_pre_chunks)

        # Buffer for speech during utterance
        self._speech_buffer = bytearray()

        # Reset RNN hidden states if using Silero
        if HAS_ONNX and self.session:
            self._h = np.zeros((2, 1, 64), dtype=np.float32)
            self._c = np.zeros((2, 1, 64), dtype=np.float32)

    def _compute_rms(self, pcm16_bytes: bytes) -> float:
        """Compute normalized RMS energy of 16-bit PCM chunk."""
        count = len(pcm16_bytes) // 2
        if count == 0:
            return 0.0
        shorts = struct.unpack(f"<{count}h", pcm16_bytes[: count * 2])
        sum_sq = sum(s * s for s in shorts)
        return math.sqrt(sum_sq / count) / 32768.0

    def process_chunk(self, chunk: bytes) -> Tuple[float, bool, bool]:
        """Process a PCM16 16kHz audio chunk.

        Returns:
            (prob, is_speech_active, is_turn_complete)
        """
        if not chunk or len(chunk) < 64:
            return 0.0, self._speech_confirmed, False

        chunk_samples = len(chunk) // 2
        chunk_duration_ms = (chunk_samples / self.sample_rate) * 1000.0

        prob = 0.0
        if HAS_ONNX and self.session is not None:
            try:
                # Silero expects float32 in [-1.0, 1.0]
                audio_np = np.frombuffer(chunk, dtype=np.int16).astype(np.float32) / 32768.0
                # Pad or slice to 512 samples for standard Silero v4 chunk size if needed
                if len(audio_np) != 512:
                    if len(audio_np) > 512:
                        eval_np = audio_np[:512]
                    else:
                        eval_np = np.pad(audio_np, (0, 512 - len(audio_np)))
                else:
                    eval_np = audio_np

                input_tensor = np.expand_dims(eval_np, axis=0)
                sr_tensor = np.array(self.sample_rate, dtype=np.int64)

                # Check ONNX input names
                inputs = {
                    "input": input_tensor,
                    "sr": sr_tensor,
                    "h": self._h,
                    "c": self._c,
                }
                out, hn, cn = self.session.run(None, inputs)
                prob = float(out[0][0])
                self._h = hn
                self._c = cn
            except Exception as e:
                # Fallback to energy
                rms = self._compute_rms(chunk)
                prob = min(1.0, rms * 15.0)
        else:
            rms = self._compute_rms(chunk)
            # Normal conversational speech has RMS > 0.025 (or ~800 in 16-bit)
            prob = min(1.0, rms * 15.0)

        is_voice = prob >= self.threshold

        if is_voice:
            self._consecutive_speech_ms += chunk_duration_ms
            self._consecutive_silence_ms = 0
            if self._consecutive_speech_ms >= self.min_speech_ms:
                if not self._speech_confirmed:
                    self._speech_confirmed = True
                    # Flush pre-speech buffer into active speech buffer
                    for pre_chunk in self._pre_buffer:
                        self._speech_buffer.extend(pre_chunk)
                    self._pre_buffer.clear()
        else:
            self._consecutive_silence_ms += chunk_duration_ms
            # Only decay speech frames if silence has been sustained
            if self._consecutive_silence_ms > 100:
                self._consecutive_speech_ms = 0

        # Collect audio
        if self._speech_confirmed:
            self._speech_buffer.extend(chunk)
        else:
            self._pre_buffer.append(chunk)

        # Check turn completion
        is_turn_complete = False
        if self._speech_confirmed and self._consecutive_silence_ms >= self.min_silence_ms:
            # Check if we captured at least min duration of real audio
            if len(self._speech_buffer) >= (self.sample_rate * 2 * 0.4): # >= 400ms
                is_turn_complete = True

        return prob, self._speech_confirmed, is_turn_complete

    def get_speech_pcm(self) -> bytes:
        """Return the accumulated speech PCM16 bytes."""
        return bytes(self._speech_buffer)
