"""
Hermes Agent Planner & Loop Protector
Tracks execution progress, detects pathological tool loops, handles dynamic re-planning,
and bridges complex/background objectives to HarnessEngine.
"""
from __future__ import annotations

import hashlib
import json
import logging
import time
from typing import Any, Dict, List, Optional, Set

from hermes_core.runtime.models import (
    ExecutionContext,
    PlanStep,
    TaskPlan,
    ToolResult,
)
from hermes_core.runtime.errors import ToolLoopDetectedError

logger = logging.getLogger("hermes.runtime.planner")


class ExecutionPlanner:
    """Manages multi-step plans, loop detection, and durable task creation."""

    def __init__(self, max_consecutive_identical_failures: int = 3):
        self.max_failures = max_consecutive_identical_failures
        self._call_history: List[Dict[str, Any]] = []

    def record_tool_call(self, tool_name: str, arguments: Dict[str, Any], result: ToolResult):
        """Records an executed tool call and checks for pathological loops."""
        call_signature = self._hash_call(tool_name, arguments)
        self._call_history.append({
            "tool": tool_name,
            "signature": call_signature,
            "success": result.success,
            "error": result.error,
            "timestamp": time.time(),
        })

        # Check for loop: identical tool and arguments failing repeatedly
        if not result.success:
            recent_identical = [
                h for h in self._call_history[-self.max_failures:]
                if h["signature"] == call_signature and not h["success"]
            ]
            if len(recent_identical) >= self.max_failures:
                raise ToolLoopDetectedError(tool_name, iterations=len(recent_identical))

    def record_call_and_check_loop(self, tool_name: str, arguments: Dict[str, Any], success: bool) -> bool:
        dummy_res = ToolResult(success=success, tool=tool_name, error={"code": "FAIL"} if not success else None)
        try:
            self.record_tool_call(tool_name, arguments, dummy_res)
            return False
        except ToolLoopDetectedError:
            return True

    def get_loop_breaker_guidance(self, tool_name: str) -> str:
        return f"[SYSTEM ALERT: TOOL LOOP DETECTED] Repeated failing calls to '{tool_name}' detected. Re-evaluate your strategy and plan an alternative approach."


    def formulate_replan_context(self, completed_steps: List[Dict[str, Any]], failed_results: List[ToolResult]) -> str:
        """Constructs concise re-planning guidance for the model after failures."""
        if not failed_results:
            return ""

        summary_lines = ["[System Notice: Tool Failure & Re-Plan Required]:"]
        for fr in failed_results:
            err_msg = fr.error.get("message") if fr.error else "Unknown error"
            summary_lines.append(f"- Tool '{fr.tool}' failed: {err_msg}")
        summary_lines.append(
            "Analyze the failure reason above, adjust your parameters or choose a complementary capability to resolve the problem. Do not repeat the exact same failed action."
        )
        return "\n".join(summary_lines)

    async def create_durable_harness_task(
        self,
        objective: str,
        context: ExecutionContext,
        risk_level: str = "medium",
        allowed_tools: Optional[List[str]] = None,
    ) -> Dict[str, Any]:
        """Bridges a long-running, multi-step, background task into HarnessEngine."""
        try:
            from gateway.harness_api import get_harness_engine
            from harness.kernel.models import RiskLevel
            engine = get_harness_engine()
            rl = RiskLevel.MEDIUM
            try:
                rl = RiskLevel(risk_level.lower())
            except Exception:
                pass

            task = await engine.create_and_run_task(
                objective=objective,
                project_id=context.project_id,
                risk_level=rl,
                allowed_tools=allowed_tools,
            )
            return {
                "durable": True,
                "task_id": task.task_id,
                "status": "scheduled",
                "objective": objective,
                "workspace": task.workspace_path,
                "message": f"Durable task {task.task_id} successfully dispatched to HarnessEngine.",
            }
        except Exception as e:
            logger.error(f"Failed to create durable task: {e}")
            return {
                "durable": False,
                "error": str(e),
                "message": f"Could not create durable task: {e}",
            }

    @staticmethod
    def _hash_call(tool_name: str, arguments: Dict[str, Any]) -> str:
        try:
            sorted_args = json.dumps(arguments, sort_keys=True, ensure_ascii=False)
        except Exception:
            sorted_args = str(arguments)
        return hashlib.sha256(f"{tool_name}:{sorted_args}".encode("utf-8")).hexdigest()

    def get_create_task_schema(self) -> Dict[str, Any]:
        """OpenAI tool schema for durable background task creation."""
        return {
            "type": "function",
            "function": {
                "name": "create_durable_task",
                "description": (
                    "Dispatch a long-running, multi-step, background, or scheduled workflow to the persistent "
                    "HarnessEngine. Use this when the user's objective requires multi-step autonomous execution "
                    "(e.g. repo-wide refactorings, deep audits, test suite repairs, scheduled monitoring) that should "
                    "persist across restarts and disconnects."
                ),
                "parameters": {
                    "type": "object",
                    "properties": {
                        "objective": {
                            "type": "string",
                            "description": "Clear, detailed objective of the task."
                        },
                        "risk_level": {
                            "type": "string",
                            "enum": ["low", "medium", "high", "critical"],
                            "description": "Risk profile of the task operations (default: medium)."
                        }
                    },
                    "required": ["objective"]
                }
            }
        }


# Singleton execution planner
execution_planner = ExecutionPlanner()

