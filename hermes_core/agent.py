"""
Hermes Agent Core Service & API Gateway
Authoritative model-driven interface powered by the canonical AgentRuntime.
Eliminates hardcoded keyword routing, fake diagnostics, and canned fallback responses.
"""
from __future__ import annotations

import asyncio
import json
import logging
import os
import time
from typing import Any, AsyncGenerator, Dict, List, Optional

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, StreamingResponse

from hermes_core.omniroute_adapter import OmniRouteAdapter
from hermes_core.runtime.agent_runtime import AgentRuntime
from hermes_core.runtime.models import ExecutionContext
from hermes_core.tools.registry import registry
import hermes_core.tools  # Trigger tool discovery into capability index

logger = logging.getLogger("HermesAgent")

UPSTREAM_URL = os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1").rstrip("/")
UPSTREAM_API_KEY = os.getenv("UPSTREAM_API_KEY", os.getenv("API_KEY_SECRET", "Jishnu2005"))
DEFAULT_MODEL = os.getenv("HERMES_DEFAULT_MODEL", "nvidia/nvidia/nemotron-3-super-120b-a12b")


class HermesAgent:
    """Sovereign Hermes Agent facade delegating to the canonical AgentRuntime."""

    def __init__(self, upstream_url: str = UPSTREAM_URL, api_key: str = UPSTREAM_API_KEY):
        self.upstream_url = upstream_url
        self.api_key = (api_key or "").strip()
        self.omniroute = OmniRouteAdapter(upstream_url=self.upstream_url, api_key=self.api_key)
        self.runtime = AgentRuntime.get_instance()

    async def stream_chat(
        self,
        messages: List[Dict[str, Any]],
        model: Optional[str] = None,
        system: Optional[str] = None,
        temperature: float = 0.7,
        session_id: Optional[str] = None,
        user_id: Optional[str] = None,
        project_id: Optional[str] = None,
        enable_dynamic_tools: bool = True,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        Model-driven streaming execution.
        The model determines understanding, planning, tool discovery, and verification.
        No keyword branches or canned fallback responses.
        """
        # Build execution context
        ctx = ExecutionContext(
            user_id=user_id or "default_user",
            session_id=session_id or f"sess_{int(time.time()*1000)}",
            project_id=project_id or "default",
            is_admin=True,
        )

        chosen_model = model or DEFAULT_MODEL

        # Stream directly through the canonical AgentRuntime
        async for event in self.runtime.stream_chat(
            messages=messages,
            context=ctx,
            model=chosen_model,
            system_instruction=system,
            temperature=temperature,
        ):
            yield event


agent = HermesAgent()

# Internal FastAPI microservice on port 8642
app = FastAPI(title="Hermes Agent Core", version="3.0.0")


@app.get("/health")
async def health():
    from hermes_core.runtime.tool_discovery import capability_index
    return {
        "status": "ok",
        "service": "hermes_core",
        "runtime": "canonical_agent_runtime",
        "capabilities_count": len(capability_index._tools),
    }


@app.get("/v1/omniroute/telemetry")
async def omniroute_telemetry():
    """Exposes inference telemetry under Hermes authority."""
    return {
        "status": "ok",
        "upstream_url": agent.upstream_url,
        "recent_telemetry": agent.omniroute.get_recent_telemetry() if hasattr(agent, "omniroute") else [],
    }


@app.post("/v1/chat")
async def chat_endpoint(request: Request):
    """Direct agent stream endpoint."""
    data = await request.json()
    messages = data.get("messages", [])
    model = data.get("model")
    system = data.get("system")
    temperature = data.get("temperature", 0.7)
    session_id = data.get("session_id")
    user_id = data.get("user_id")

    async def event_generator():
        async for item in agent.stream_chat(
            messages,
            model=model,
            system=system,
            temperature=temperature,
            session_id=session_id,
            user_id=user_id,
        ):
            yield f"data: {json.dumps(item)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


@app.post("/v1/chat/completions")
async def openai_chat_completions(request: Request):
    """OpenAI-compatible adapter used across Telegram, Voice, and HTTP clients."""
    data = await request.json()
    messages = data.get("messages", [])
    model = data.get("model") or "hermes-agent"
    system = data.get("system")
    temperature = data.get("temperature", 0.7)
    stream = bool(data.get("stream", True))
    session_id = data.get("session_id") or request.headers.get("x-session-id")
    response_id = f"chatcmpl-hermes-{int(time.time() * 1000)}"

    if not stream:
        text_parts = []
        thinking_parts = []
        error = None
        async for item in agent.stream_chat(
            messages,
            model=model,
            system=system,
            temperature=temperature,
            session_id=session_id,
        ):
            if item.get("type") == "text":
                text_parts.append(item.get("content", ""))
            elif item.get("type") == "thinking":
                thinking_parts.append(item.get("content", ""))
            elif item.get("type") == "error":
                error = item.get("error")

        content = "".join(text_parts)
        if error and not content:
            content = f"Error: {error}"

        message = {"role": "assistant", "content": content}
        if thinking_parts:
            message["reasoning_content"] = "".join(thinking_parts)

        return JSONResponse({
            "id": response_id,
            "object": "chat.completion",
            "created": int(time.time()),
            "model": model,
            "choices": [{"index": 0, "message": message, "finish_reason": "stop"}],
        })

    async def completion_events():
        def chunk(delta: Dict[str, Any], finish_reason: Optional[str] = None) -> str:
            return (
                f"data: {json.dumps({'id': response_id, 'object': 'chat.completion.chunk', 'created': int(time.time()), 'model': model, 'choices': [{'index': 0, 'delta': delta, 'finish_reason': finish_reason}]}, ensure_ascii=False)}\n\n"
            )

        yield chunk({"role": "assistant", "content": ""})

        async for item in agent.stream_chat(
            messages,
            model=model,
            system=system,
            temperature=temperature,
            session_id=session_id,
        ):
            item_type = item.get("type")
            content = item.get("content", "")
            if item_type == "text" and content:
                yield chunk({"content": content})
            elif item_type == "thinking" and content:
                yield chunk({"reasoning_content": content})
            elif item_type == "error":
                yield chunk({"content": f"\n\n[Hermes Error: {item.get('error', 'Execution failed')}]"})

        yield chunk({}, "stop")
        yield "data: [DONE]\n\n"

    return StreamingResponse(completion_events(), media_type="text/event-stream")


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8642)
