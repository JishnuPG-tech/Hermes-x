"""
Hermes Central Context Builder
Assembles structured, minimal, and secure context for the LLM Controller.
Separates Canonical System Prompt, Runtime Environment, Capabilities, Task History, and Tools.
"""
from __future__ import annotations

import os
import time
from pathlib import Path
from typing import Any, Dict, List, Optional
from hermes_core.runtime.models import ExecutionContext, ToolMetadata
from hermes_core.runtime.tool_discovery import capability_index

_MASTER_PROMPT_PATH = Path(__file__).resolve().parent.parent / "prompts" / "hermes_master.md"
_CACHED_MASTER_PROMPT: Optional[str] = None


def get_canonical_master_prompt() -> str:
    """Loads and caches the authoritative Hermes Master System Prompt from hermes_master.md."""
    global _CACHED_MASTER_PROMPT
    if _CACHED_MASTER_PROMPT is not None:
        return _CACHED_MASTER_PROMPT

    if _MASTER_PROMPT_PATH.exists():
        try:
            _CACHED_MASTER_PROMPT = _MASTER_PROMPT_PATH.read_text(encoding="utf-8").strip()
            return _CACHED_MASTER_PROMPT
        except Exception:
            pass

    # Fallback to root HERMES_MASTER_SYSTEM_PROMPT.md if available
    root_prompt = Path(__file__).resolve().parent.parent.parent / "HERMES_MASTER_SYSTEM_PROMPT.md"
    if root_prompt.exists():
        try:
            text = root_prompt.read_text(encoding="utf-8")
            start = text.find("<hermes_system>")
            end = text.find("</hermes_system>")
            if start != -1 and end != -1:
                _CACHED_MASTER_PROMPT = text[start : end + len("</hermes_system>")].strip()
                return _CACHED_MASTER_PROMPT
        except Exception:
            pass

    # Resilient fallback minimal definition
    return (
        "<hermes_system>\n"
        "<identity>\nYou are Hermes Agent, a general-purpose autonomous AI agent operating inside the Hermes runtime.\n</identity>\n"
        "</hermes_system>"
    )


class ContextBuilder:
    """Builds clean, structured context for model invocations."""

    @staticmethod
    def build_system_prompt(
        context: ExecutionContext,
        custom_instructions: Optional[str] = None,
        retrieved_memory: Optional[str] = None,
        retrieved_knowledge: Optional[str] = None,
    ) -> str:
        """
        Assembles the authoritative canonical master prompt dynamically followed by
        runtime-generated environment, skills, memory, and custom instructions.
        """
        master_prompt = get_canonical_master_prompt()
        blocks = [master_prompt]

        # 1. Environmental runtime context
        current_utc = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
        env_lines = [
            "<runtime_context>",
            f"- Current Time: {current_utc}",
            f"- Authenticated User: {context.user_id}",
            f"- Active Session: {context.session_id}",
            f"- Project Scope: {context.project_id}",
            f"- Workspace Path: {context.workspace_path}",
            f"- Admin Privileges: {'Enabled' if context.is_admin else 'Standard'}",
        ]
        if context.permissions:
            env_lines.append(f"- Granted Permissions: {', '.join(sorted(context.permissions))}")
        if context.task_id:
            env_lines.append(f"- Active Durable Task: {context.task_id}")
        env_lines.append("</runtime_context>")
        blocks.append("\n".join(env_lines))

        # 2. Active Skills context if present
        from hermes_core.tools.skill_tools import ACTIVE_CONVERSATION_SKILLS, BUILTIN_SKILLS
        session_skills = list(context.active_skills or [])
        if context.session_id in ACTIVE_CONVERSATION_SKILLS:
            for s in ACTIVE_CONVERSATION_SKILLS[context.session_id]:
                if s not in session_skills:
                    session_skills.append(s)

        if session_skills:
            skill_lines = ["<active_skills>"]
            for s in session_skills:
                info = BUILTIN_SKILLS.get(s, {})
                desc = info.get("description", "Domain specialized knowledge and procedures.")
                skill_lines.append(f"- Skill '{s}': {desc}")
            skill_lines.append("</active_skills>")
            blocks.append("\n".join(skill_lines))

        # 3. Memory injection if relevant
        if retrieved_memory and retrieved_memory.strip():
            blocks.append(f"<memory_context>\n{retrieved_memory.strip()}\n</memory_context>")

        # 4. Connected Knowledge Notes injection if relevant
        if retrieved_knowledge and retrieved_knowledge.strip():
            blocks.append(f"<knowledge_context>\n{retrieved_knowledge.strip()}\n</knowledge_context>")

        # 5. User Custom system instructions
        if custom_instructions and custom_instructions.strip():
            blocks.append(f"<custom_instructions>\n{custom_instructions.strip()}\n</custom_instructions>")

        return "\n\n".join(blocks)

    @staticmethod
    def assemble_messages(
        messages: List[Dict[str, Any]],
        context: ExecutionContext,
        custom_instructions: Optional[str] = None,
        retrieved_memory: Optional[str] = None,
        retrieved_knowledge: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """Assembles complete conversation payload prepending authoritative system prompt."""
        system_content = ContextBuilder.build_system_prompt(
            context,
            custom_instructions=custom_instructions,
            retrieved_memory=retrieved_memory,
            retrieved_knowledge=retrieved_knowledge,
        )
        assembled = [{"role": "system", "content": system_content}]
        assembled.extend([m for m in messages if m.get("role") != "system"])
        return assembled

    @staticmethod
    def resolve_active_tools(
        context: ExecutionContext,
        discovered_tools: Optional[List[ToolMetadata]] = None,
    ) -> List[Dict[str, Any]]:
        """
        Determines the list of tools exposed to the model for the current turn.
        Always includes meta-tools (search_tools, create_durable_task).
        Merges with dynamically discovered or core system capabilities.
        """
        tools_dict: Dict[str, Dict[str, Any]] = {}

        # 1. Meta-tools always present
        tools_dict["search_tools"] = capability_index.get_search_tools_schema()

        # 2. Add durable task creation
        from hermes_core.runtime.planner import ExecutionPlanner
        tools_dict["create_durable_task"] = ExecutionPlanner().get_create_task_schema()

        # 3. Add base capabilities matching context permissions
        core_tool_names = [
            "bash",
            "bash_exec",
            "read_file",
            "write_file",
            "edit_file",
            "list_directory",
            "list_dir",
            "search_knowledge",
            "notion_search",
            "notion_read_page",
            "notion_create_page",
            "web_search",
            "memory_recall",
            "schedule_task",
            "list_background_tasks",
            "activate_skill",
            "list_skills",
            "computer_system_status",
        ]
        for name in core_tool_names:
            t = capability_index.get_tool(name)
            if t and (not t.permissions or any(p in context.permissions for p in t.permissions) or context.is_admin):
                tools_dict[name] = t.to_openai_schema()

        # 4. Add dynamically discovered tools
        if discovered_tools:
            for dt in discovered_tools:
                tools_dict[dt.name] = dt.to_openai_schema()

        return list(tools_dict.values())
