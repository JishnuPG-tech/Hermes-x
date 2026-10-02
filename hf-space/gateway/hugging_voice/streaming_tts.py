"""Streaming PCM16 TTS engine for Hugging Voice.

Synthesizes speech in real-time with a sweet, calm, warm female persona
(en-US-JennyNeural) and yields raw 24kHz 16-bit mono PCM chunks.
"""

from __future__ import annotations
import asyncio
import io
import logging
import re
from typing import AsyncGenerator, Optional

import edge_tts
try:
    import soundfile as sf
    HAS_SOUNDFILE = True
except ImportError:
    HAS_SOUNDFILE = False

logger = logging.getLogger("hermes.hugging_voice.tts")

DEFAULT_FEMALE_VOICE = "en-US-JennyNeural"
PCM_CHUNK_BYTES = 2048


class HuggingVoiceTTS:
    """Low-latency streaming TTS engine yielding PCM16 frames for Hugging Voice."""

    def __init__(self, default_voice: str = DEFAULT_FEMALE_VOICE):
        self.default_voice = default_voice

    async def stream_sentence_pcm(
        self,
        text: str,
        voice: Optional[str] = None,
        speed: float = 1.0,
        cancel_event: Optional[asyncio.Event] = None,
    ) -> AsyncGenerator[bytes, None]:
        if not text or not text.strip():
            return
        if cancel_event and cancel_event.is_set():
            return

        target_voice = voice or self.default_voice
        speed_delta = f"{int((speed - 1.0) * 100):+d}%" if speed != 1.0 else "+0%"

        spoken_text = self._clean_for_speech(text)
        if not spoken_text.strip():
            return

        try:
            comm = edge_tts.Communicate(
                spoken_text,
                voice=target_voice,
                rate=speed_delta,
            )

            raw_mp3_buffer = bytearray()
            async for chunk in comm.stream():
                if cancel_event and cancel_event.is_set():
                    return
                if chunk.get("type") == "audio":
                    raw_mp3_buffer.extend(chunk.get("data", b""))

            if not raw_mp3_buffer or (cancel_event and cancel_event.is_set()):
                return

            pcm_bytes = await self._decode_mp3_to_pcm(bytes(raw_mp3_buffer))
            if not pcm_bytes or (cancel_event and cancel_event.is_set()):
                return

            offset = 0
            pcm_len = len(pcm_bytes)
            while offset < pcm_len:
                if cancel_event and cancel_event.is_set():
                    return
                end = min(offset + PCM_CHUNK_BYTES, pcm_len)
                yield pcm_bytes[offset:end]
                offset = end
                await asyncio.sleep(0.005)

        except Exception as e:
            logger.error(f"Error in Hugging Voice streaming TTS: {e}")

    async def _decode_mp3_to_pcm(self, mp3_data: bytes) -> bytes:
        if not mp3_data:
            return b""

        def _do_decode() -> bytes:
            # 1. Try ffmpeg (standard in Linux container, decodes directly to 24kHz mono PCM16)
            try:
                import subprocess
                proc = subprocess.Popen(
                    ["ffmpeg", "-hide_banner", "-loglevel", "error", "-i", "pipe:0", "-f", "s16le", "-ac", "1", "-ar", "24000", "pipe:1"],
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                )
                pcm, err = proc.communicate(input=mp3_data, timeout=5)
                if proc.returncode == 0 and pcm:
                    return pcm
            except Exception as e:
                logger.debug(f"FFmpeg decode notice: {e}")

            # 2. Try soundfile
            if HAS_SOUNDFILE:
                try:
                    data, sr = sf.read(io.BytesIO(mp3_data), dtype="int16")
                    if len(data.shape) > 1 and data.shape[1] > 1:
                        data = data[:, 0]
                    return data.tobytes()
                except Exception as exc:
                    logger.debug(f"Soundfile decode notice: {exc}")

            # 3. Try miniaudio
            try:
                import miniaudio
                decoded = miniaudio.mp3_read_s16(mp3_data)
                return decoded.samples.tobytes()
            except Exception:
                pass

            logger.error("No audio decoder available to convert MP3 to PCM16")
            return b""

        return await asyncio.to_thread(_do_decode)

    @staticmethod
    def _clean_for_speech(text: str) -> str:
        t = re.sub(r"```[\s\S]*?```", " [code omitted] ", text)
        t = re.sub(r"`([^`]+)`", r"\1", t)
        t = re.sub(r"!\[([^\]]*)\]\([^)]+\)", "", t)
        t = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", t)
        t = re.sub(r"<[^>]+>", "", t)
        t = re.sub(r"[#*_~]", "", t)
        t = re.sub(r"\s+", " ", t)
        return t.strip()
