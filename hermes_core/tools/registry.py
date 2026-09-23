"""
Hermes Canonical Tool Registry & Capability Catalog
Registers tools with rich metadata, schemas, and permissions.
Integrates with CapabilityIndex and canonical ToolExecutor.
"""
from __future__ import annotations

import json
import inspect
from typing import Dict, Any, List, Callable, Optional
from hermes_core.runtime.models import ToolMetadata, ToolCategory, ExecutionContext
from hermes_core.runtime.tool_discovery import capability_index
from hermes_core.runtime.tool_controller import tool_executor


CATEGORY_MAP = {
    "files": ToolCategory.FILESYSTEM,
    "system": ToolCategory.SYSTEM,
    "web": ToolCategory.WEB,
    "vault": ToolCategory.KNOWLEDGE,
    "memory": ToolCategory.MEMORY,
    "coding": ToolCategory.CODING,
    "browser": ToolCategory.BROWSER,
    "task": ToolCategory.TASK,
    "skill": ToolCategory.SKILL,
}


class ToolRegistry:
    def __init__(self):
        self._tools: Dict[str, Dict[str, Any]] = {}
        self._handlers: Dict[str, Callable] = {}
        self._categories: Dict[str, List[str]] = {
            "web": [],
            "coding": [],
            "files": [],
            "vault": [],
            "memory": [],
            "system": [],
            "browser": [],
            "task": [],
            "skill": [],
        }
        self._enabled_categories: set = {"web", "coding", "files", "vault", "memory", "system", "browser", "task", "skill"}

    def register(
        self,
        name: str,
        description: str,
        parameters: Dict[str, Any],
        category: str = "system",
        permissions: Optional[List[str]] = None,
        requires_approval: bool = False,
        side_effects: bool = False,
        timeout_seconds: int = 120,
    ):
        """Decorator to register a tool with structured metadata and schema."""
        def decorator(fn: Callable):
            schema = {
                "type": "function",
                "function": {
                    "name": name,
                    "description": description,
                    "parameters": parameters,
                }
            }
            cat_enum = CATEGORY_MAP.get(category, ToolCategory.SYSTEM)
            perms = permissions or [f"{cat_enum.value}.read", f"{cat_enum.value}.write"]

            self._tools[name] = {
                "schema": schema,
                "category": category,
                "description": description,
                "permissions": perms,
                "requires_approval": requires_approval,
                "side_effects": side_effects,
                "timeout_seconds": timeout_seconds,
            }
            self._handlers[name] = fn

            if category not in self._categories:
                self._categories[category] = []
            if name not in self._categories[category]:
                self._categories[category].append(name)

            # Register into canonical CapabilityIndex and ToolExecutor
            meta = ToolMetadata(
                name=name,
                description=description,
                input_schema=parameters,
                category=cat_enum,
                permissions=perms,
                requires_approval=requires_approval,
                side_effects=side_effects,
                timeout_seconds=timeout_seconds,
            )
            capability_index.register_capability(meta)
            tool_executor.register_handler(name, fn)

            return fn
        return decorator

    def enable_category(self, category: str):
        self._enabled_categories.add(category)

    def disable_category(self, category: str):
        self._enabled_categories.discard(category)

    def get_all_tools(self) -> List[Dict[str, Any]]:
        return [
            meta["schema"] for name, meta in self._tools.items()
            if meta["category"] in self._enabled_categories
        ]

    def select_tools_for_prompt(
        self,
        prompt: str,
        user_requested_tools: Optional[List[str]] = None,
        context: Optional[ExecutionContext] = None,
        limit: int = 10,
    ) -> List[Dict[str, Any]]:
        """
        Model-driven, semantic capability discovery.
        Does NOT rely on keyword matching, greeting counters, or hardcoded branch lists.
        Always exposes `search_tools` and `create_durable_task` plus dynamically ranked capabilities.
        """
        if user_requested_tools:
            return [
                self._tools[name]["schema"] for name in user_requested_tools
                if name in self._tools and self._tools[name]["category"] in self._enabled_categories
            ]

        # Use CapabilityIndex to retrieve semantically relevant tools based on the objective
        matches = capability_index.search_capabilities(prompt, context=context, limit=limit)
        schemas = [m.to_openai_schema() for m in matches]

        # Always include meta-discovery tool if not already present
        search_schema = capability_index.get_search_tools_schema()
        if not any(s.get("function", {}).get("name") == "search_tools" for s in schemas):
            schemas.insert(0, search_schema)

        return schemas

    async def execute_tool(
        self,
        name: str,
        arguments: Dict[str, Any],
        context: Optional[ExecutionContext] = None,
    ) -> str:
        """Executes tool through the canonical ToolExecutor."""
        ctx = context or ExecutionContext(
            user_id="default_user",
            session_id="legacy_session",
            project_id="default",
            is_admin=True,
        )
        res = await tool_executor.execute(name, arguments, ctx)
        return res.to_content_string()


registry = ToolRegistry()
