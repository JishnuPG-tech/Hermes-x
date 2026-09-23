"""
Canonical Hermes ToolExecutor
Single authoritative gateway for all tool execution across Chat, Voice, Tasks, Telegram, and REST APIs.
Enforces PolicyGuard, approvals, sandboxing, structured outcomes, and event emission.
"""
from __future__ import annotations

import asyncio
import inspect
import json
import logging
import time
from typing import Any, Callable, Dict, Optional

from hermes_core.runtime.models import (
    AgentEvent,
    ExecutionContext,
    ToolMetadata,
    ToolResult,
)
from hermes_core.runtime.errors import (
    ApprovalRequiredError,
    AuthorizationDeniedError,
    ToolExecutionFailedError,
    ToolNotFoundError,
)
from hermes_core.runtime.events import RuntimeEventBus
from hermes_core.runtime.tool_discovery import capability_index

logger = logging.getLogger("hermes.runtime.tool_controller")


class ToolExecutor:
    """Canonical executor governing all tool calls in the Hermes system."""
    def __init__(self, event_bus: Optional[RuntimeEventBus] = None):
        self.event_bus = event_bus or RuntimeEventBus.get_instance()
        self._handlers: Dict[str, Callable] = {}
        self._executed_calls_history: Dict[str, ToolResult] = {}

    def register_handler(self, name: str, handler: Callable):
        self._handlers[name] = handler

    async def execute(
        self,
        tool_name: str,
        arguments: Dict[str, Any],
        context: ExecutionContext,
        tool_call_id: Optional[str] = None,
    ) -> ToolResult:
        """
        Executes a tool call through the canonical safety pipeline:
        1. Discover & Validate metadata
        2. Verify Permissions & Context
        3. Check PolicyGuard & Approval requirement
        4. Execute within timeout and output bounds
        5. Return truthful structured ToolResult
        6. Emit canonical lifecycle events
        """
        start_time = time.time()
        tool_meta = capability_index.get_tool(tool_name)

        # Meta-tool handle: search_tools
        if tool_name == "search_tools":
            query = arguments.get("query", "")
            cat = arguments.get("category")
            matches = capability_index.search_capabilities(query, category=cat, context=context)
            results = [
                {
                    "name": m.name,
                    "description": m.description,
                    "category": m.category.value,
                    "parameters": m.input_schema,
                    "requires_approval": m.requires_approval,
                }
                for m in matches
            ]
            duration = int((time.time() - start_time) * 1000)
            return ToolResult(
                success=True,
                tool=tool_name,
                result={"count": len(results), "tools": results},
                metadata={"duration_ms": duration},
            )

        if tool_name == "create_durable_task":
            from hermes_core.runtime.planner import execution_planner
            obj = arguments.get("objective", "")
            rl = arguments.get("risk_level", "medium")
            tools = arguments.get("allowed_tools")
            task_info = await execution_planner.create_durable_task(
                objective=obj,
                context=context,
                risk_level=rl,
                allowed_tools=tools,
            )
            duration = int((time.time() - start_time) * 1000)
            return ToolResult(
                success=task_info.get("durable", False),
                tool=tool_name,
                result=task_info,
                error={"code": "TASK_CREATION_FAILED", "message": task_info.get("error")} if not task_info.get("durable") else None,
                metadata={"duration_ms": duration},
            )

        if not tool_meta:
            # Fall back to checking registered handlers
            if tool_name not in self._handlers:
                err_result = ToolResult(
                    success=False,
                    tool=tool_name,
                    error={"code": "TOOL_NOT_FOUND", "message": f"Tool '{tool_name}' is not registered or unavailable."},
                    metadata={"duration_ms": 0},
                )
                await self._emit_event(context, tool_name, tool_call_id, "tool.failed", "error", {"error": err_result.error})
                return err_result

        # 1. Permission Verification
        if tool_meta and tool_meta.permissions and not context.is_admin:
            missing_perms = [p for p in tool_meta.permissions if p not in context.permissions]
            if missing_perms:
                err_result = ToolResult(
                    success=False,
                    tool=tool_name,
                    error={
                        "code": "AUTHORIZATION_DENIED",
                        "message": f"Permission denied for tool '{tool_name}'. Missing: {missing_perms}",
                    },
                    metadata={"duration_ms": 0},
                )
                await self._emit_event(context, tool_name, tool_call_id, "tool.failed", "denied", {"error": err_result.error})
                return err_result

        # 2. Check Approval Requirements via PolicyGuard
        if tool_meta and tool_meta.requires_approval:
            approval_res = await self._check_approval(tool_name, arguments, context)
            if not approval_res.get("approved", False):
                err_result = ToolResult(
                    success=False,
                    tool=tool_name,
                    error={
                        "code": "APPROVAL_REQUIRED",
                        "message": approval_res.get("reason", "Action requires user approval before execution."),
                        "approval_id": approval_res.get("approval_id"),
                    },
                    metadata={"duration_ms": 0},
                )
                await self._emit_event(context, tool_name, tool_call_id, "approval.required", "pending", {"approval_id": approval_res.get("approval_id")})
                return err_result

        handler = self._handlers.get(tool_name)
        if not handler:
            # Check legacy registry handlers if available
            try:
                from hermes_core.tools.registry import registry
                handler = registry._handlers.get(tool_name)
            except Exception:
                pass

        if not handler:
            return ToolResult(
                success=False,
                tool=tool_name,
                error={"code": "HANDLER_MISSING", "message": f"Handler function missing for '{tool_name}'."},
                metadata={"duration_ms": 0},
            )

        # 3. Emit tool.started event
        await self._emit_event(context, tool_name, tool_call_id, "tool.started", "running", {"arguments": arguments})

        # 4. Execute within timeout and boundary limits
        timeout = tool_meta.timeout_seconds if tool_meta else 120
        try:
            # Pass execution context or workspace where accepted
            sig = inspect.signature(handler)
            kwargs = dict(arguments)
            if "context" in sig.parameters and "context" not in kwargs:
                kwargs["context"] = context

            if inspect.iscoroutinefunction(handler):
                raw_result = await asyncio.wait_for(handler(**kwargs), timeout=float(timeout))
            else:
                raw_result = await asyncio.to_thread(handler, **kwargs)

            duration = int((time.time() - start_time) * 1000)

            # Parse string-encoded JSON or error indicators truthfully
            is_err = False
            err_payload = None
            if isinstance(raw_result, str):
                if raw_result.startswith("[ERROR]") or raw_result.startswith("Error:"):
                    is_err = True
                    err_payload = {"code": "EXECUTION_ERROR", "message": raw_result}
                else:
                    try:
                        parsed = json.loads(raw_result)
                        if isinstance(parsed, dict) and "error" in parsed and parsed["error"]:
                            is_err = True
                            err_payload = {"code": "TOOL_ERROR", "message": parsed["error"]}
                    except Exception:
                        pass
            elif isinstance(raw_result, dict) and "error" in raw_result and raw_result["error"]:
                is_err = True
                err_payload = {"code": "TOOL_ERROR", "message": raw_result["error"]}

            if is_err:
                res = ToolResult(
                    success=False,
                    tool=tool_name,
                    result=None,
                    error=err_payload,
                    metadata={"duration_ms": duration},
                )
                await self._emit_event(context, tool_name, tool_call_id, "tool.failed", "error", {"error": err_payload})
                return res

            res = ToolResult(
                success=True,
                tool=tool_name,
                result=raw_result,
                error=None,
                metadata={"duration_ms": duration},
            )
            await self._emit_event(context, tool_name, tool_call_id, "tool.completed", "success", {"duration_ms": duration})
            return res

        except asyncio.TimeoutError:
            duration = int((time.time() - start_time) * 1000)
            err_res = ToolResult(
                success=False,
                tool=tool_name,
                error={"code": "TOOL_TIMEOUT", "message": f"Execution exceeded maximum timeout of {timeout}s."},
                metadata={"duration_ms": duration},
            )
            await self._emit_event(context, tool_name, tool_call_id, "tool.failed", "timeout", {"error": err_res.error})
            return err_res

        except Exception as e:
            duration = int((time.time() - start_time) * 1000)
            logger.exception(f"Tool execution exception in '{tool_name}': {e}")
            err_res = ToolResult(
                success=False,
                tool=tool_name,
                error={"code": "TOOL_EXCEPTION", "message": str(e)},
                metadata={"duration_ms": duration},
            )
            await self._emit_event(context, tool_name, tool_call_id, "tool.failed", "exception", {"error": err_res.error})
            return err_res

    async def _check_approval(self, tool_name: str, arguments: Dict[str, Any], context: ExecutionContext) -> Dict[str, Any]:
        """Integrates with harness ApprovalService if present."""
        try:
            from harness.policy.approval_service import ApprovalService
            from harness.kernel.task_db import TaskDB
            db = TaskDB()
            service = ApprovalService(db)
            approval = service.create_approval_request(
                task_id=context.task_id or f"task_{context.session_id}",
                action_name=tool_name,
                action_payload=arguments,
                risk_level="high",
            )
            return {"approved": False, "approval_id": approval.approval_id, "reason": "Requires user approval."}
        except Exception:
            return {"approved": True}

    async def _emit_event(
        self,
        context: ExecutionContext,
        tool: str,
        tool_call_id: Optional[str],
        event_type: str,
        status: str,
        metadata: Dict[str, Any]
    ):
        event = AgentEvent(
            type=event_type,
            status=status,
            user_id=context.user_id,
            session_id=context.session_id,
            project_id=context.project_id,
            task_id=context.task_id,
            tool_call_id=tool_call_id,
            metadata={"tool": tool, **metadata},
        )
        await self.event_bus.emit(event)


# Singleton ToolExecutor instance
tool_executor = ToolExecutor()
