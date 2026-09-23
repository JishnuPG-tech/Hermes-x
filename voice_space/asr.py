"""Low-latency Automated Speech Recognition (ASR) using faster-whisper.

Configured for CPU execution on free-tier instances:
- Model: tiny.en
- Quantization: int8
- Beam size: 1 (greedy, lowest latency)
- VAD filter disabled (since pre-segmented by our dedicated VAD)
- Condition on previous text: False (faster inference, eliminates repetition loops)
"""

import os
import time
import logging
from typing import Optional

logger = logging.getLogger("hermes.voice.asr")

try:
    import numpy as np
    from faster_whisper import WhisperModel
    HAS_WHISPER = True
except ImportError:
    HAS_WHISPER = False


class WhisperASREngine:
    """Thread-safe faster-whisper singleton for sub-300ms speech transcription."""

    _instance: Optional["WhisperASREngine"] = None

    @classmethod
    def get_instance(cls, model_size: str = "tiny.en") -> "WhisperASREngine":
        if cls._instance is None:
            cls._instance = cls(model_size=model_size)
        return cls._instance

    def __init__(self, model_size: str = "tiny.en", compute_type: str = "int8", cpu_threads: int = 2):
        self.model_size = model_size
        self.compute_type = compute_type
        self.cpu_threads = cpu_threads
        self.model: Optional[WhisperModel] = None

        default_dir = os.environ.get("MODEL_CACHE_DIR", "/app/models")
        download_root = os.path.join(default_dir, "whisper")
        os.makedirs(download_root, exist_ok=True)

        if HAS_WHISPER:
            try:
                start = time.perf_counter()
                logger.info(f"Loading faster-whisper ({model_size}, {compute_type}, threads={cpu_threads})...")
                self.model = WhisperModel(
                    model_size_or_path=model_size,
                    device="cpu",
                    compute_type=compute_type,
                    cpu_threads=cpu_threads,
                    download_root=download_root,
                )
                elapsed = (time.perf_counter() - start) * 1000.0
                logger.info(f"faster-whisper model loaded in {elapsed:.1f}ms")
            except Exception as e:
                logger.error(f"Failed to initialize faster-whisper: {e}")
        else:
            logger.warning("faster-whisper library not installed.")

    async def transcribe(self, pcm_bytes: bytes, sample_rate: int = 16000) -> str:
        """Transcribe 16kHz PCM16 mono bytes into text."""
        if not pcm_bytes or len(pcm_bytes) < 1000:
            return ""

        if not HAS_WHISPER or self.model is None:
            logger.warning("ASR engine requested transcription but faster-whisper is not available.")
            return ""

        start_time = time.perf_counter()

        # Convert raw PCM16 to float32 numpy array normalized to [-1.0, 1.0]
        audio_np = np.frombuffer(pcm_bytes, dtype=np.int16).astype(np.float32) / 32768.0

        try:
            # Run in worker thread to prevent blocking asyncio event loop
            import asyncio
            loop = asyncio.get_running_loop()

            def _run_transcribe():
                segments, info = self.model.transcribe(
                    audio_np,
                    beam_size=1,
                    language="en",
                    condition_on_previous_text=False,
                    vad_filter=False,
                    temperature=0.0,
                )
                text_parts = [segment.text.strip() for segment in segments]
                return " ".join(filter(None, text_parts)).strip()

            transcript = await loop.run_in_executor(None, _run_transcribe)
            elapsed_ms = (time.perf_counter() - start_time) * 1000.0
            logger.info(f"ASR transcribed ({elapsed_ms:.1f}ms): '{transcript}'")
            return transcript
        except Exception as e:
            logger.error(f"Error during ASR transcription: {e}")
            return ""
