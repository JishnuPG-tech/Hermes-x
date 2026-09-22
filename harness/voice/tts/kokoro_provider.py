"""Kokoro Local TTS Provider implementation.

Primary local open-weight 82M TTS engine. Runs in-process on CPU to provide
fast, local, zero-network speech synthesis. Gracefully reports status if
kokoro packages are not present so failover to Edge-TTS is seamless.
"""

from __future__ import annotations

import asyncio
import io
import logging
import time
from typing import AsyncIterator, List, Optional

from harness.voice.models import AudioFormat, ProviderHealth, TTSGenerationRequest, TTSGenerationResult
from harness.voice.tts.base import TTSProvider

logger = logging.getLogger(__name__)


class KokoroProvider(TTSProvider):
    """Local Kokoro TTS engine implementation."""

    def __init__(
        self,
        default_voice: str = "am_adam",
        lang_code: str = "a",
        sample_rate: int = 24000,
    ) -> None:
        self._default_voice = default_voice
        self._lang_code = lang_code
        self._sample_rate = sample_rate
        self._pipeline = None
        self._is_ready = False
        self._last_error: Optional[str] = None
        self._consecutive_failures = 0
        self._last_latency_ms: Optional[float] = None

    @property
    def name(self) -> str:
        return "kokoro"

    @property
    def is_local(self) -> bool:
        return True

    @property
    def supported_formats(self) -> List[AudioFormat]:
        return [AudioFormat.WAV, AudioFormat.PCM_16K]

    async def initialize(self) -> bool:
        """Warm up Kokoro pipeline if dependencies are available."""
        start = time.perf_counter()
        try:
            try:
                from kokoro import KPipeline
                self._pipeline = KPipeline(lang_code=self._lang_code)
                self._is_ready = True
                logger.info("Kokoro KPipeline initialized in %0.2fms", (time.perf_counter() - start) * 1000)
                return True
            except ImportError:
                try:
                    import kokoro_onnx
                    self._pipeline = kokoro_onnx.Kokoro()
                    self._is_ready = True
                    logger.info("Kokoro ONNX initialized in %0.2fms", (time.perf_counter() - start) * 1000)
                    return True
                except ImportError:
                    self._is_ready = False
                    self._last_error = "Kokoro package not installed in environment; will failover to EdgeTTS"
                    logger.info(self._last_error)
                    return False
        except Exception as exc:
            self._is_ready = False
            self._last_error = f"Kokoro initialization error: {exc}"
            logger.warning(self._last_error)
            return False

    async def check_health(self) -> ProviderHealth:
        return ProviderHealth(
            provider=self.name,
            healthy=self._is_ready,
            consecutive_failures=self._consecutive_failures,
            last_latency_ms=self._last_latency_ms,
            circuit_state="CLOSED" if self._is_ready else "OPEN",
            is_local=True,
            error=self._last_error,
        )

    async def synthesize(self, request: TTSGenerationRequest) -> TTSGenerationResult:
        """Synthesize text using local Kokoro pipeline."""
        if not self._is_ready:
            return TTSGenerationResult(
                request_id=request.request_id,
                audio_bytes=b"",
                format=AudioFormat.WAV,
                sample_rate=self._sample_rate,
                latency_ms=0.0,
                provider=self.name,
                voice_used=request.voice or self._default_voice,
                success=False,
                error=self._last_error or "Kokoro not ready",
            )

        start = time.perf_counter()
        voice = request.voice or self._default_voice

        try:
            loop = asyncio.get_running_loop()

            def _run():
                if hasattr(self._pipeline, "__call__"):
                    req_speed = request.speed if request.speed is not None else 1.0
                    generator = self._pipeline(request.text, voice=voice, speed=req_speed, split_pattern=r"\n+")
                    for _, _, audio in generator:
                        return audio
                return None

            audio_data = await loop.run_in_executor(None, _run)
            if audio_data is None:
                raise RuntimeError("Kokoro synthesis returned empty audio")

            import soundfile as sf
            wav_buf = io.BytesIO()
            sf.write(wav_buf, audio_data, self._sample_rate, format="WAV")
            audio_bytes = wav_buf.getvalue()

            latency = (time.perf_counter() - start) * 1000.0
            self._last_latency_ms = round(latency, 2)
            self._consecutive_failures = 0

            return TTSGenerationResult(
                request_id=request.request_id,
                audio_bytes=audio_bytes,
                format=AudioFormat.WAV,
                sample_rate=self._sample_rate,
                latency_ms=round(latency, 2),
                provider=self.name,
                voice_used=voice,
                success=True,
            )
        except Exception as exc:
            self._consecutive_failures += 1
            self._last_error = str(exc)
            latency = (time.perf_counter() - start) * 1000.0
            return TTSGenerationResult(
                request_id=request.request_id,
                audio_bytes=b"",
                format=AudioFormat.WAV,
                sample_rate=self._sample_rate,
                latency_ms=round(latency, 2),
                provider=self.name,
                voice_used=voice,
                success=False,
                error=str(exc),
            )

    async def synthesize_stream(
        self, request: TTSGenerationRequest
    ) -> AsyncIterator[bytes]:
        res = await self.synthesize(request)
        if res.success and res.audio_bytes:
            yield res.audio_bytes
