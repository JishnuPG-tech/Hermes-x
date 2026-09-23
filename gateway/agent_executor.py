"""
Hermes Agent Executor Compatibility Adapter
Unifies all legacy tool execution and agent loops onto the canonical AgentRuntime.
Preserves backwards compatibility for existing imports and queue streaming.
"""
from __future__ import annotations

import asyncio
import json
import logging
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from gateway import anthropic_bridge as ab
from hermes_core.runtime.agent_runtime import AgentRuntime
from hermes_core.runtime.context import ContextBuilder
from hermes_core.runtime.models import ExecutionContext
from hermes_core.runtime.tool_controller import tool_executor
from hermes_core.tools.skill_tools import (
    ACTIVE_CONVERSATION_SKILLS,
    BUILTIN_SKILLS,
    SKILLS_DIR,
)

logger = logging.getLogger("hermes.gateway.agent_executor")

DEFAULT_WORKSPACE = Path("/data") if Path("/data").exists() else Path("/tmp")
DEFAULT_WORKSPACE.mkdir(parents=True, exist_ok=True)


def build_system_prompt_with_skills(chat_id: str) -> str:
    """Delegates to ContextBuilder to ensure a single canonical system prompt is used."""
    context = ExecutionContext(
        user_id="default_user",
        session_id=chat_id,
        project_id="default",
        is_admin=True,
    )
    return ContextBuilder.build_system_prompt(context)


async def execute_tool_call(
    tool_name: str,
    args: Dict[str, Any],
    chat_id: str = "default_chat",
) -> str:
    """Executes a tool call through the canonical ToolExecutor."""
    context = ExecutionContext(
        user_id="default_user",
        session_id=chat_id,
        project_id="default",
        is_admin=True,
    )
    res = await tool_executor.execute(tool_name, args, context)
    return res.to_content_string()


async def run_autonomous_agent(
    chat_id: str,
    prompt: str,
    messages: list,
    model: str,
    msg_id: str,
    queue: asyncio.Queue,
) -> Tuple[str, str]:
    """
    Autonomous agent execution bridge streaming Anthropic SSE blocks
    powered entirely by the canonical AgentRuntime.
    """
    runtime = AgentRuntime.get_instance()
    context = ExecutionContext(
        user_id="default_user",
        session_id=chat_id,
        project_id="default",
        is_admin=True,
    )

    # Normalize messages into OpenAI format
    normalized_messages: List[Dict[str, Any]] = []
    for m in messages:
        role = m.get("role") or m.get("sender") or "user"
        r = "user" if role in ["human", "user"] else ("assistant" if role in ["assistant", "ai"] else "system")
        txt = m.get("content") or m.get("text") or ""
        if isinstance(txt, list):
            txt = "".join(cb.get("text", "") for cb in txt if isinstance(cb, dict) and cb.get("type") == "text")
        txt_str = str(txt).strip()
        if txt_str:
            normalized_messages.append({"role": r, "content": txt_str})

    if not any(m["role"] == "user" for m in normalized_messages):
        normalized_messages.append({"role": "user", "content": prompt or "Hello"})

    accumulated_text = ""
    accumulated_reasoning = ""
    text_active = False

    try:
        async for event in runtime.stream_chat(
            messages=normalized_messages,
            context=context,
            model=model or "hermes-agent",
        ):
            ev_type = event.get("type")

            if ev_type == "text":
                content = event.get("content", "")
                if content:
                    accumulated_text += content
                    if not text_active:
                        await queue.put(ab.create_content_block_start(0))
                        text_active = True
                    await queue.put(ab.create_content_block_delta(content, 0))

            elif ev_type == "thinking":
                reasoning = event.get("content", "")
                if reasoning:
                    accumulated_reasoning += reasoning

            elif ev_type == "error":
                err_msg = event.get("error", "Execution failed")
                if not text_active:
                    await queue.put(ab.create_content_block_start(0))
                    text_active = True
                err_text = f"\n[Error: {err_msg}]\n"
                accumulated_text += err_text
                await queue.put(ab.create_content_block_delta(err_text, 0))

    except Exception as exc:
        logger.error(f"Error in run_autonomous_agent: {exc}", exc_info=True)
        if not text_active:
            await queue.put(ab.create_content_block_start(0))
            text_active = True
        err_msg = f"An error occurred during execution: {exc}"
        accumulated_text += err_msg
        await queue.put(ab.create_content_block_delta(err_msg, 0))

    finally:
        if text_active:
            await queue.put(ab.create_content_block_stop(0))
        elif not accumulated_text:
            # Report truthful minimal fallback
            fallback = "Task completed with no output."
            await queue.put(ab.create_content_block_start(0))
            await queue.put(ab.create_content_block_delta(fallback, 0))
            await queue.put(ab.create_content_block_stop(0))
            accumulated_text = fallback

        await queue.put(ab.create_message_delta("end_turn"))
        await queue.put(ab.create_message_stop())
        await queue.put(None)

    return accumulated_text, accumulated_reasoning


# Expose AGENT_TOOLS dynamically resolved from capability index for backward compatibility
AGENT_TOOLS = ContextBuilder.resolve_active_tools(
    ExecutionContext(user_id="compat", session_id="compat", is_admin=True)
)
