"""
Hermes Canonical AgentRuntime
Single unified entry point governing all agent executions across Chat, Voice, Telegram, and REST APIs.
"""
from __future__ import annotations

import asyncio
import logging
import os
from typing import Any, AsyncGenerator, Dict, List, Optional

from hermes_core.runtime.agent_loop import AutonomousAgentLoop
from hermes_core.runtime.events import RuntimeEventBus
from hermes_core.runtime.models import ExecutionContext
from hermes_core.runtime.tool_controller import tool_executor
from hermes_core.runtime.tool_discovery import capability_index

logger = logging.getLogger("hermes.runtime.agent_runtime")

UPSTREAM_URL = os.getenv("UPSTREAM_OMNIROUTE_URL", "https://jishnupg-opencode-cli.hf.space/v1").rstrip("/")
UPSTREAM_API_KEY = os.getenv("UPSTREAM_API_KEY", os.getenv("API_KEY_SECRET", "Jishnu2005"))
DEFAULT_MODEL = os.getenv("HERMES_DEFAULT_MODEL", "nvidia/nvidia/nemotron-3-super-120b-a12b")


class AgentRuntime:
    """The central authority for executing Hermes autonomous agent reasoning & actions."""

    _instance: Optional[AgentRuntime] = None

    def __init__(
        self,
        upstream_url: str = UPSTREAM_URL,
        api_key: str = UPSTREAM_API_KEY,
        default_model: str = DEFAULT_MODEL,
    ):
        self.upstream_url = upstream_url
        self.api_key = api_key
        self.default_model = default_model
        self.event_bus = RuntimeEventBus.get_instance()
        self.tool_executor = tool_executor
        self.capability_index = capability_index
        self._ensure_tools_registered()

    @classmethod
    def get_instance(cls) -> AgentRuntime:
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def _ensure_tools_registered(self):
        """Ensure core system tools are registered into the CapabilityIndex."""
        try:
            import hermes_core.tools
            from hermes_core.tools.registry import registry
            for name, meta in registry._tools.items():
                if not self.capability_index.get_tool(name):
                    from hermes_core.runtime.models import ToolMetadata, ToolCategory
                    raw_cat = meta.get("category", "system")
                    cat_map = {
                        "files": ToolCategory.FILESYSTEM,
                        "system": ToolCategory.SYSTEM,
                        "web": ToolCategory.WEB,
                        "vault": ToolCategory.KNOWLEDGE,
                        "memory": ToolCategory.MEMORY,
                        "coding": ToolCategory.CODING,
                    }
                    cat_enum = cat_map.get(raw_cat, ToolCategory.SYSTEM)
                    schema = meta.get("schema", {}).get("function", {})
                    t_meta = ToolMetadata(
                        name=name,
                        description=schema.get("description", meta.get("description", "")),
                        input_schema=schema.get("parameters", {}),
                        category=cat_enum,
                        permissions=[f"{cat_enum.value}.read", f"{cat_enum.value}.write"],
                    )
                    self.capability_index.register_capability(t_meta)
                    if name in registry._handlers:
                        self.tool_executor.register_handler(name, registry._handlers[name])
        except Exception as e:
            logger.warning(f"Error registering legacy tools into CapabilityIndex: {e}")

    async def stream_chat(
        self,
        messages: List[Dict[str, Any]],
        context: Optional[ExecutionContext] = None,
        model: Optional[str] = None,
        system_instruction: Optional[str] = None,
        temperature: float = 0.7,
        max_iterations: int = 8,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        Stream an autonomous agent conversation turn.
        All interfaces (Chat, Voice, Telegram) consume this single canonical method.
        """
        # Ensure execution context
        if context is None:
            user_msg = ""
            for m in reversed(messages):
                if m.get("role") == "user":
                    user_msg = str(m.get("content", ""))
                    break
            session_id = f"sess_{hash(user_msg) % 1000000}"
            context = ExecutionContext(
                user_id="default_user",
                session_id=session_id,
                project_id="default",
                is_admin=True,
            )

        resolved_model = model or self.default_model

        loop = AutonomousAgentLoop(
            upstream_url=self.upstream_url,
            api_key=self.api_key,
            max_iterations=max_iterations,
            event_bus=self.event_bus,
        )

        async for event in loop.run(
            messages=messages,
            context=context,
            model_name=resolved_model,
            temperature=temperature,
            custom_instructions=system_instruction,
        ):
            yield event

    async def execute_turn(
        self,
        objective: str,
        context: ExecutionContext,
        model: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Synchronous/accumulated turn execution (used by voice and quick tool callers)."""
        messages = [{"role": "user", "content": objective}]
        accumulated_text = []
        thinking_text = []
        errors = []

        async for event in self.stream_chat(messages=messages, context=context, model=model):
            ev_type = event.get("type")
            if ev_type == "text":
                accumulated_text.append(event.get("content", ""))
            elif ev_type == "thinking":
                thinking_text.append(event.get("content", ""))
            elif ev_type == "error":
                errors.append(event.get("error", ""))

        final_content = "".join(accumulated_text).strip()
        return {
            "success": len(errors) == 0 and bool(final_content),
            "response": final_content,
            "reasoning": "".join(thinking_text).strip(),
            "errors": errors,
        }
