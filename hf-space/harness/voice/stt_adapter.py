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
        self._local_model = None
        self._checked_local = False

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

        # 1. Local Faster-Whisper if available
        if not self._checked_local:
            try:
                from faster_whisper import WhisperModel
                self._local_model = WhisperModel("tiny", device="cpu", compute_type="int8")
                logger.info("Local faster-whisper STT initialized successfully")
            except Exception:
                self._local_model = None
            self._checked_local = True

        if self._local_model is not None:
            try:
                segments, _ = await asyncio.to_thread(self._local_model.transcribe, io.BytesIO(wav_bytes))
                text = " ".join([seg.text for seg in segments]).strip()
                if text:
                    return text
            except Exception as e:
                logger.debug("Local faster-whisper transcribe error: %s", e)

        # 2. Groq Whisper API
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

        # 3. OpenAI Whisper API
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

        # 4. Hugging Face Serverless Inference API
        hf_token = os.getenv("HF_TOKEN") or os.getenv("HUGGINGFACE_TOKEN") or os.getenv("HUGGING_FACE_HUB_TOKEN")
        if hf_token:
            try:
                import httpx
                async with httpx.AsyncClient(timeout=12.0) as client:
                    resp = await client.post(
                        "https://router.huggingface.co/hf-inference/models/openai/whisper-large-v3-turbo",
                        headers={"Authorization": f"Bearer {hf_token}", "Content-Type": "audio/wav"},
                        content=wav_bytes,
                    )
                    if resp.status_code == 200:
                        res_json = resp.json()
                        text = res_json.get("text", "").strip() if isinstance(res_json, dict) else ""
                        if text:
                            return text
            except Exception as e:
                logger.debug("Hugging Face Whisper STT failed: %s", e)

        # In pure offline mode or when STT keys are not present, log notice
        logger.debug("STT: No external cloud STT key configured or call fell through. Buffer bytes: %d", len(audio_pcm))
        return ""
