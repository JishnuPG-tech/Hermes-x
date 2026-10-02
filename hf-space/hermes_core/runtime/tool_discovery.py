"""
Hermes Dynamic Capability Discovery Engine
Discovers capabilities semantically via metadata and schemas rather than hardcoded prompt rules.
"""
from __future__ import annotations

import re
from typing import Any, Dict, List, Optional, Set
from hermes_core.runtime.models import ToolMetadata, ExecutionContext


class CapabilityIndex:
    """
    Inverted semantic and keyword index over registered tool capabilities.
    Allows LLMs to query `search_tools` dynamically without hardcoding intent strings in Python.
    """
    def __init__(self):
        self._tools: Dict[str, ToolMetadata] = {}
        self._register_meta_tools()

    def _register_meta_tools(self):
        from hermes_core.runtime.models import ToolCategory
        self.register_capability(
            ToolMetadata(
                name="search_tools",
                description="Search and discover available tools, capabilities, and system interfaces dynamically.",
                input_schema={
                    "type": "object",
                    "properties": {
                        "query": {"type": "string", "description": "Natural language description of capability needed"},
                        "category": {"type": "string", "description": "Optional category filter"},
                    },
                    "required": ["query"],
                },
                category=ToolCategory.SYSTEM,
            )
        )
        self.register_capability(
            ToolMetadata(
                name="create_durable_task",
                description="Dispatch a long-running, multi-step, background, or scheduled workflow to the persistent HarnessEngine.",
                input_schema={
                    "type": "object",
                    "properties": {
                        "objective": {"type": "string", "description": "Clear, detailed objective of the task"},
                        "risk_level": {"type": "string", "enum": ["low", "medium", "high", "critical"], "description": "Risk profile"},
                    },
                    "required": ["objective"],
                },
                category=ToolCategory.TASK,
                side_effects=True,
            )
        )

    def register_capability(self, metadata: ToolMetadata):
        self._tools[metadata.name] = metadata

    def unregister_capability(self, name: str):
        self._tools.pop(name, None)

    def get_tool(self, name: str) -> Optional[ToolMetadata]:
        return self._tools.get(name)

    def list_all_tools(self, context: Optional[ExecutionContext] = None) -> List[ToolMetadata]:
        tools = list(self._tools.values())
        if context:
            # Deterministic permission filtering
            tools = [
                t for t in tools
                if not t.permissions or any(p in context.permissions for p in t.permissions) or context.is_admin
            ]
        return tools

    def search_capabilities(
        self,
        query: str,
        category: Optional[str] = None,
        context: Optional[ExecutionContext] = None,
        limit: int = 8,
    ) -> List[ToolMetadata]:
        """
        Calculates semantic match scores between user/model capability query and tool metadata.
        No hardcoded Python intelligence branches: scores match against tool descriptions,
        names, parameters, and categories.
        """
        query_tokens = set(re.findall(r'[a-zA-Z0-9_-]+', query.lower()))
        if not query_tokens:
            return self.list_all_tools(context)[:limit]

        scored: List[tuple[float, ToolMetadata]] = []

        for tool in self._tools.values():
            if not tool.availability:
                continue

            # Deterministic permission check
            if context and tool.permissions and not context.is_admin:
                if not any(p in context.permissions for p in tool.permissions):
                    continue

            if category and tool.category.value.lower() != category.lower():
                continue

            score = 0.0
            tool_name_tokens = set(re.findall(r'[a-zA-Z0-9_-]+', tool.name.lower()))
            desc_tokens = set(re.findall(r'[a-zA-Z0-9_-]+', tool.description.lower()))
            param_tokens = set(re.findall(r'[a-zA-Z0-9_-]+', str(tool.input_schema).lower()))

            # Exact or substring match in name (high weight)
            for qt in query_tokens:
                if qt in tool.name.lower():
                    score += 5.0
                if qt in tool_name_tokens:
                    score += 4.0
                if qt in desc_tokens:
                    score += 2.0
                if qt in param_tokens:
                    score += 1.0

            if score > 0.0:
                scored.append((score, tool))

        # Sort descending by relevance score
        scored.sort(key=lambda x: x[0], reverse=True)
        return [item[1] for item in scored[:limit]]

    def get_search_tools_schema(self) -> Dict[str, Any]:
        """OpenAI tool schema for the dynamic capability discovery meta-tool."""
        return {
            "type": "function",
            "function": {
                "name": "search_tools",
                "description": (
                    "Search and discover available tools, capabilities, and system interfaces. "
                    "Use this when you need a capability that is not currently exposed in your active tool set "
                    "(e.g. searching connected notes/Notion, inspecting workspaces, file editing, web search, "
                    "scheduling tasks, or server actions)."
                ),
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "Natural language description of the capability needed (e.g. 'read files from workspace', 'search knowledge notes', 'run shell command')."
                        },
                        "category": {
                            "type": "string",
                            "enum": ["filesystem", "system", "web", "knowledge", "memory", "coding", "browser", "task", "skill"],
                            "description": "Optional category filter."
                        }
                    },
                    "required": ["query"]
                }
            }
        }


# Singleton capability index
capability_index = CapabilityIndex()
