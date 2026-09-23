"""Fast streaming LLM client connecting to OmniRoute with sentence-level chunking.

Pipelined architecture:
Yields sentence and clause chunks immediately as they are completed by the model,
allowing TTS synthesis to start within 200-400ms of first token arrival.
"""

import os
import re
import json
import asyncio
import logging
from typing import AsyncGenerator, Dict, List, Optional
import httpx

logger = logging.getLogger("hermes.voice.llm")

OMNIROUTE_URL = os.environ.get("OMNIROUTE_URL", "http://127.0.0.1:20128/v1/chat/completions")
DEFAULT_VOICE_PROMPT = (
    "You are Hermes Agent speaking live through realtime voice. "
    "Keep responses concise, natural, direct, and conversational (1-2 sentences maximum). "
    "Do NOT output markdown, markdown tables, lists, or code blocks; summarize clearly in plain speech. "
    "If the user asks to perform an action or tool task, acknowledge immediately in one short sentence."
)

# Clause / sentence split regex
PUNCT_SPLIT = re.compile(r'([.!?\n]+|[,;:—]+)')


class StreamingLLMClient:
    """Streams LLM tokens from OmniRoute and delivers chunked phrases for TTS pipelining."""

    def __init__(self, endpoint_url: str = OMNIROUTE_URL, timeout: float = 12.0):
        self.endpoint_url = endpoint_url
        self.timeout = timeout

    async def stream_sentences(
        self,
        messages: List[Dict[str, str]],
        model: str = "hermes-agent",
        max_tokens: int = 96,
        temperature: float = 0.2,
        cancel_event: Optional[asyncio.Event] = None,
    ) -> AsyncGenerator[str, None]:
        """Stream conversational phrases/clauses from OmniRoute LLM endpoint.

        Yields:
            individual speakable phrases as soon as punctuation boundary is detected.
        """
        payload = {
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
            "stream": True,
        }

        headers = {
            "Content-Type": "application/json",
            "Authorization": "Bearer hermes-voice-agent",
        }

        buffer = ""

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                async with client.stream("POST", self.endpoint_url, json=payload, headers=headers) as response:
                    if response.status_code != 200:
                        err_text = await response.aread()
                        logger.error(f"OmniRoute error ({response.status_code}): {err_text.decode('utf-8', errors='ignore')}")
                        yield "I had trouble contacting the language model."
                        return

                    async for line in response.aiter_lines():
                        if cancel_event and cancel_event.is_set():
                            logger.info("LLM generation stream interrupted by user.")
                            break

                        line = line.strip()
                        if not line or not line.startswith("data: "):
                            continue

                        data_str = line[6:].strip()
                        if data_str == "[DONE]":
                            break

                        try:
                            data_json = json.loads(data_str)
                            choices = data_json.get("choices", [])
                            if not choices:
                                continue
                            delta = choices[0].get("delta", {})
                            token = delta.get("content", "")
                            if not token:
                                continue

                            buffer += token

                            # Check for sentence or clause boundaries
                            # First check sentence ends (. ! ? \n)
                            sentence_match = re.search(r'([.!?\n]+)\s*', buffer)
                            if sentence_match:
                                end_pos = sentence_match.end()
                                phrase = buffer[:end_pos].strip()
                                buffer = buffer[end_pos:]
                                clean_phrase = self._clean_for_speech(phrase)
                                if clean_phrase:
                                    yield clean_phrase
                                continue

                            # Clause boundary break for early TTS: , ; : if buffer is >= 35 characters
                            if len(buffer) >= 35:
                                clause_match = re.search(r'([,;:—]+)\s*', buffer)
                                if clause_match:
                                    end_pos = clause_match.end()
                                    phrase = buffer[:end_pos].strip()
                                    buffer = buffer[end_pos:]
                                    clean_phrase = self._clean_for_speech(phrase)
                                    if clean_phrase:
                                        yield clean_phrase
                                    continue

                        except Exception:
                            continue

            # Flush remaining buffer at end of stream
            if buffer.strip() and not (cancel_event and cancel_event.is_set()):
                clean_remainder = self._clean_for_speech(buffer.strip())
                if clean_remainder:
                    yield clean_remainder

        except httpx.RequestError as e:
            logger.error(f"HTTP connection to OmniRoute failed: {e}")
            yield "Sorry, I lost connection to the agent engine."
        except asyncio.CancelledError:
            logger.info("LLM sentence streaming cancelled.")

    def _clean_for_speech(self, text: str) -> str:
        """Strip markdown and special formatting so TTS sounds natural."""
        clean = re.sub(r'```[\s\S]*?```', '', text)
        clean = re.sub(r'`([^`]+)`', r'\1', clean)
        clean = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', clean)
        clean = re.sub(r'[#*_~>]+', '', clean)
        clean = re.sub(r'<[^>]+>', '', clean)
        return clean.strip()
