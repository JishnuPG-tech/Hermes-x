"""Streaming speech-to-text adapter.

Supports passing PCM chunks to streaming Whisper/speech recognition endpoints,
or decoding pre-recorded utterances. Provides graceful fallback when external
STT backends are not configured.
"""

from __future__ import annotations

import asyncio
import io
import logging
import os
import wave
from typing import Optional

logger = logging.getLogger(__name__)


class StreamingSTTAdapter:
    """Adapts streaming audio chunks into transcribed text."""

    def __init__(self, sample_rate: int = 16000) -> None:
        self.sample_rate = sample_rate
        self.provider = os.getenv("VOICE_STT_PROVIDER", "whisper_api")
        self.api_key = os.getenv("OPENAI_API_KEY") or os.getenv("GROQ_API_KEY")

    async def transcribe_audio_buffer(
        self,
        audio_pcm: bytes,
        sample_rate: Optional[int] = None,
    ) -> str:
        """Transcribe a collected PCM audio buffer to text."""
        sr = sample_rate or self.sample_rate
        if len(audio_pcm) < 3200:  # less than ~0.1s
            return ""

        # Convert PCM to WAV in-memory
        wav_buf = io.BytesIO()
        with wave.open(wav_buf, "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(sr)
            wf.writeframes(audio_pcm)
        wav_bytes = wav_buf.getvalue()

        # If Groq or OpenAI key is available, we can query speech-to-text API
        if os.getenv("GROQ_API_KEY"):
            try:
                import httpx
                async with httpx.AsyncClient(timeout=10.0) as client:
                    files = {"file": ("audio.wav", wav_bytes, "audio/wav")}
                    data = {"model": "whisper-large-v3"}
                    resp = await client.post(
                        "https://api.groq.com/openai/v1/audio/transcriptions",
                        headers={"Authorization": f"Bearer {os.getenv('GROQ_API_KEY')}"},
                        files=files,
                        data=data,
                    )
                    if resp.status_code == 200:
                        return resp.json().get("text", "").strip()
            except Exception as e:
                logger.warning("Groq Whisper STT failed: %s", e)

        if os.getenv("OPENAI_API_KEY"):
            try:
                import httpx
                async with httpx.AsyncClient(timeout=10.0) as client:
                    files = {"file": ("audio.wav", wav_bytes, "audio/wav")}
                    data = {"model": "whisper-1"}
                    resp = await client.post(
                        "https://api.openai.com/v1/audio/transcriptions",
                        headers={"Authorization": f"Bearer {os.getenv('OPENAI_API_KEY')}"},
                        files=files,
                        data=data,
                    )
                    if resp.status_code == 200:
                        return resp.json().get("text", "").strip()
            except Exception as e:
                logger.warning("OpenAI Whisper STT failed: %s", e)

        # In pure offline mode or testing when STT keys are not present, log notice
        logger.info("STT: No external cloud STT key configured or call fell through. Buffer bytes: %d", len(audio_pcm))
        return ""
