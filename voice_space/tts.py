"""Streaming Text-To-Speech (TTS) engine with Piper and Edge-TTS fallback.

Supports:
1. Piper TTS ONNX (ultra-low CPU latency, direct 16kHz/22.05kHz PCM)
2. Edge-TTS neural streaming fallback (crisp natural voice, zero local CPU load)
3. Immediate barge-in cancellation via asyncio.Event
"""

import os
import io
import asyncio
import logging
from typing import AsyncGenerator, Optional

logger = logging.getLogger("hermes.voice.tts")


class StreamingTTSEngine:
    """TTS Engine producing audio frames with cancellation support."""

    def __init__(self, voice_model_path: Optional[str] = None):
        default_dir = os.environ.get("MODEL_CACHE_DIR", "/app/models")
        self.piper_onnx = voice_model_path or os.path.join(default_dir, "piper", "en_US-lessac-medium.onnx")
        self.piper_json = self.piper_onnx + ".json"
        self.has_piper = os.path.exists(self.piper_onnx) and os.path.exists(self.piper_json)

        if self.has_piper:
            logger.info(f"Piper TTS engine available with voice model: {self.piper_onnx}")
        else:
            logger.info("Piper voice model not found locally; Edge-TTS neural engine will be primary.")

    async def stream_audio(
        self,
        text: str,
        voice: str = "en-US-ChristopherNeural",
        speed: float = 1.0,
        cancel_event: Optional[asyncio.Event] = None,
    ) -> AsyncGenerator[bytes, None]:
        """Stream raw audio chunks for the given text.

        Yields:
            bytes containing audio (raw PCM or streaming MP3 frames)
        """
        clean_text = text.strip()
        if not clean_text:
            return

        # 1. Try Piper TTS if available
        if self.has_piper:
            try:
                async for chunk in self._stream_piper(clean_text, cancel_event):
                    yield chunk
                return
            except Exception as e:
                logger.warning(f"Piper TTS failed: {e}. Falling back to Edge-TTS.")

        # 2. Edge-TTS neural fallback
        async for chunk in self._stream_edge_tts(clean_text, voice, speed, cancel_event):
            yield chunk

    async def _stream_piper(
        self,
        text: str,
        cancel_event: Optional[asyncio.Event] = None,
    ) -> AsyncGenerator[bytes, None]:
        """Synthesize using Piper subprocess in streaming raw PCM format."""
        cmd = [
            "piper",
            "--model", self.piper_onnx,
            "--config", self.piper_json,
            "--output-raw",
        ]

        proc = await asyncio.create_subprocess_exec(
            *cmd,
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )

        try:
            if proc.stdin:
                proc.stdin.write(text.encode("utf-8"))
                await proc.stdin.drain()
                proc.stdin.close()

            chunk_size = 1920  # ~60ms at 16kHz 16-bit mono
            while proc.stdout:
                if cancel_event and cancel_event.is_set():
                    try:
                        proc.kill()
                    except Exception:
                        pass
                    break

                chunk = await proc.stdout.read(chunk_size)
                if not chunk:
                    break
                yield chunk
        finally:
            if proc.returncode is None:
                try:
                    proc.kill()
                except Exception:
                    pass

    async def _stream_edge_tts(
        self,
        text: str,
        voice: str,
        speed: float,
        cancel_event: Optional[asyncio.Event] = None,
    ) -> AsyncGenerator[bytes, None]:
        """Synthesize using Edge-TTS with streaming audio chunks."""
        try:
            import edge_tts

            rate_str = "+0%"
            if speed > 1.05:
                rate_str = f"+{int((speed - 1.0) * 100)}%"
            elif speed < 0.95:
                rate_str = f"-{int((1.0 - speed) * 100)}%"

            communicate = edge_tts.Communicate(text, voice, rate=rate_str)
            async for chunk in communicate.stream():
                if cancel_event and cancel_event.is_set():
                    break
                if chunk["type"] == "audio":
                    yield chunk["data"]
        except ImportError:
            logger.error("Neither Piper nor Edge-TTS is available.")
            yield b""
        except Exception as e:
            logger.error(f"Edge-TTS synthesis error: {e}")
            yield b""
