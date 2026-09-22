"""
OmniRoute Inference Adapter
===========================
Implements the interface contract between Hermes Agent and OmniRoute:
- "Hermes is the king. OmniRoute powers the king."
- Semantic routing profiles (fast, coding, reasoning, default).
- Dynamic provider failover cascade without resetting Hermes state.
- Normalized streaming events, tool calls, and reasoning deltas.
- Safe telemetry recording without secret exposure.
"""
from __future__ import annotations

import asyncio
import json
import logging
import os
import time
from dataclasses import dataclass, field, asdict
from typing import AsyncGenerator, Dict, Any, List, Optional, Tuple
import httpx

logger = logging.getLogger("OmniRouteAdapter")


@dataclass
class InferenceTelemetry:
    request_id: str
    requested_model: str
    routed_model: Optional[str] = None
    first_token_latency_ms: Optional[int] = None
    total_latency_ms: Optional[int] = None
    prompt_tokens: int = 0
    completion_tokens: int = 0
    failover_attempts: int = 0
    status: str = "pending"  # success, failover, error
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


# Semantic routing profiles mapping task needs to priority model cascades
ROUTING_PROFILES: Dict[str, List[str]] = {
    "fast": [
        "antigravity/gemini-2.5-flash",
        "auto/fast",
        "auto/best-chat",
    ],
    "coding": [
        "antigravity/gemini-2.5-flash",
        "auto/best-coding",
        "nvidia/nvidia/nemotron-3-super-120b-a12b",
        "groq/llama-3.3-70b-versatile",
    ],
    "reasoning": [
        "antigravity/gemini-2.5-flash",
        "auto/best-reasoning",
        "nvidia/nvidia/nemotron-3-super-120b-a12b",
    ],
    "default": [
        "antigravity/gemini-2.5-flash",
        "auto/best-coding",
        "auto/best-chat",
    ],
}


class OmniRouteAdapter:
    def __init__(
        self,
        upstream_url: Optional[str] = None,
        api_key: Optional[str] = None,
        timeout_seconds: float = 60.0,
    ):
        raw_url = upstream_url or os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1")
        self.upstream_url = raw_url.rstrip("/")
        self.api_key = (api_key or os.getenv("UPSTREAM_API_KEY", os.getenv("API_KEY_SECRET", "Jishnu2005"))).strip()
        self.timeout = timeout_seconds

        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        self.client = httpx.AsyncClient(
            base_url=self.upstream_url,
            headers=headers,
            timeout=httpx.Timeout(self.timeout, connect=5.0),
            follow_redirects=True,
        )
        self._telemetry_history: List[InferenceTelemetry] = []
        self._max_history = 100

    def resolve_model_cascade(self, requested_model: Optional[str], profile: Optional[str] = None) -> List[str]:
        """Resolves candidate model cascade based on request and routing profile."""
        cascade: List[str] = []
        if requested_model and requested_model not in ("default", "hermes", "hermes-agent"):
            cascade.append(requested_model)

        profile_models = ROUTING_PROFILES.get(profile or "default", ROUTING_PROFILES["default"])
        for m in profile_models:
            if m not in cascade:
                cascade.append(m)

        return cascade

    async def stream_inference(
        self,
        messages: List[Dict[str, Any]],
        requested_model: Optional[str] = None,
        profile: Optional[str] = None,
        tools: Optional[List[Dict[str, Any]]] = None,
        temperature: float = 0.7,
        request_id: Optional[str] = None,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """Streams inference through OmniRoute with automatic provider failover and telemetry."""
        req_id = request_id or f"req_{int(time.time() * 1000)}"
        model_cascade = self.resolve_model_cascade(requested_model, profile)

        telemetry = InferenceTelemetry(
            request_id=req_id,
            requested_model=requested_model or model_cascade[0],
        )

        stream_succeeded = False
        last_error = None
        start_time = time.time()

        for attempt_idx, candidate_model in enumerate(model_cascade):
            req_body: Dict[str, Any] = {
                "model": candidate_model,
                "messages": messages,
                "temperature": temperature,
                "stream": True,
            }
            if tools:
                req_body["tools"] = tools
                req_body["tool_choice"] = "auto"

            try:
                first_token = True
                telemetry.routed_model = candidate_model
                telemetry.failover_attempts = attempt_idx

                async with self.client.stream(
                    "POST",
                    "/chat/completions",
                    json=req_body,
                    timeout=httpx.Timeout(20.0, connect=5.0),
                ) as response:
                    if response.status_code != 200:
                        err_body = await response.aread()
                        last_error = f"HTTP {response.status_code} from OmniRoute on model '{candidate_model}': {err_body.decode('utf-8', errors='ignore')[:150]}"
                        logger.warning(f"[OmniRoute] Failover from {candidate_model}: {last_error}")
                        continue

                    async for line in response.aiter_lines():
                        line = line.strip()
                        if not line or not line.startswith("data:"):
                            continue
                        data_str = line[5:].strip()
                        if data_str == "[DONE]":
                            break

                        try:
                            chunk = json.loads(data_str)
                        except Exception:
                            continue

                        choices = chunk.get("choices", [])
                        if not choices:
                            continue

                        delta = choices[0].get("delta", {})

                        if first_token:
                            first_token = False
                            telemetry.first_token_latency_ms = int((time.time() - start_time) * 1000)

                        # 1. Reasoning content (thinking models)
                        if "reasoning_content" in delta and delta["reasoning_content"]:
                            yield {"type": "thinking", "content": delta["reasoning_content"]}

                        # 2. Regular content text
                        if "content" in delta and delta["content"]:
                            yield {"type": "text", "content": delta["content"]}

                        # 3. Native tool calls
                        if "tool_calls" in delta and delta["tool_calls"]:
                            for tc in delta["tool_calls"]:
                                yield {"type": "tool_delta", "tool_call": tc}

                    # If we reached here without exception, stream succeeded
                    stream_succeeded = True
                    telemetry.status = "success"
                    telemetry.total_latency_ms = int((time.time() - start_time) * 1000)
                    self._record_telemetry(telemetry)
                    yield {"type": "telemetry", "data": telemetry.to_dict()}
                    return

            except (httpx.ConnectError, httpx.ConnectTimeout, httpx.ReadTimeout, httpx.RemoteProtocolError) as exc:
                last_error = f"Network exception on model '{candidate_model}': {exc}"
                logger.warning(f"[OmniRoute] Transient failure on {candidate_model}: {exc}. Trying next candidate...")
                await asyncio.sleep(0.5)

            except Exception as exc:
                last_error = f"Unexpected error on model '{candidate_model}': {exc}"
                logger.error(f"[OmniRoute] Error on {candidate_model}: {exc}")

        # All models exhausted
        telemetry.status = "error"
        telemetry.total_latency_ms = int((time.time() - start_time) * 1000)
        self._record_telemetry(telemetry)

        yield {
            "type": "error",
            "error": f"All OmniRoute candidate models exhausted. Last error: {last_error}",
        }

    def _record_telemetry(self, item: InferenceTelemetry):
        self._telemetry_history.append(item)
        if len(self._telemetry_history) > self._max_history:
            self._telemetry_history.pop(0)

    def get_recent_telemetry(self, limit: int = 20) -> List[Dict[str, Any]]:
        return [t.to_dict() for t in self._telemetry_history[-limit:]]
