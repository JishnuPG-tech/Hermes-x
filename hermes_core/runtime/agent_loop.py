"""
Hermes Canonical Autonomous Agent Loop
Multi-turn autonomous decision and execution engine driven by the LLM Controller.
No hardcoded intelligence branches, no keyword gating, no fake fallbacks.
"""
from __future__ import annotations

import asyncio
import json
import logging
import time
from typing import Any, AsyncGenerator, Dict, List, Optional

import httpx

from hermes_core.runtime.context import ContextBuilder
from hermes_core.runtime.errors import (
    HermesRuntimeError,
    ModelTimeoutError,
    ModelUnavailableError,
    ToolLoopDetectedError,
)
from hermes_core.runtime.events import RuntimeEventBus
from hermes_core.runtime.models import (
    AgentEvent,
    ExecutionContext,
    ToolMetadata,
    ToolResult,
)
from hermes_core.runtime.planner import ExecutionPlanner
from hermes_core.runtime.budget import ExecutionBudget
from hermes_core.runtime.progress import ProgressTracker
from hermes_core.runtime.tool_controller import tool_executor
from hermes_core.runtime.tool_discovery import capability_index
from hermes_core.runtime.verifier import verification_gate

logger = logging.getLogger("hermes.runtime.agent_loop")


class AutonomousAgentLoop:
    """The iterative loop driving autonomous plan-execute-observe-replan-verify cycles."""

    def __init__(
        self,
        upstream_url: str,
        api_key: str,
        max_iterations: Optional[int] = None,
        timeout_seconds: float = 120.0,
        event_bus: Optional[RuntimeEventBus] = None,
    ):
        self.upstream_url = upstream_url.rstrip("/")
        self.api_key = api_key
        self.requested_max_iterations = max_iterations
        self.timeout_seconds = timeout_seconds
        self.event_bus = event_bus or RuntimeEventBus.get_instance()
        self.planner = ExecutionPlanner()

    async def run(
        self,
        messages: List[Dict[str, Any]],
        context: ExecutionContext,
        model_name: str = "hermes-agent",
        temperature: float = 0.7,
        custom_instructions: Optional[str] = None,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        Executes the autonomous agent turn yielding streaming events:
        {"type": "thinking" | "text" | "tool_call" | "tool_result" | "error" | "done", ...}
        """
        start_time = time.time()
        session_id = context.session_id
        budget = ExecutionBudget.for_request(messages, self.requested_max_iterations)
        progress = ProgressTracker()
        emitted_budget_notices = set()

        # 1. Emit agent.started event
        await self.event_bus.emit(AgentEvent(
            type="agent.started",
            status="running",
            user_id=context.user_id,
            session_id=session_id,
            project_id=context.project_id,
            task_id=context.task_id,
            metadata={"model": model_name},
        ))

        # 2. Assemble context centrally
        system_content = ContextBuilder.build_system_prompt(
            context=context,
            custom_instructions=custom_instructions,
        )

        conversation_history: List[Dict[str, Any]] = []
        for m in messages:
            if m.get("role") != "system":
                conversation_history.append(dict(m))

        active_messages = [{"role": "system", "content": system_content}] + conversation_history
        discovered_tools: List[ToolMetadata] = []
        executed_tool_results: List[ToolResult] = []

        headers = {
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
        }
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        client_timeout = httpx.Timeout(connect=8.0, read=self.timeout_seconds, write=30.0, pool=30.0)

        # 3. Iterative Agent Loop
        for iteration in range(budget.hard_ceiling):
            iteration_number = iteration + 1
            if iteration_number >= budget.warning_at and "warning" not in emitted_budget_notices:
                emitted_budget_notices.add("warning")
                active_messages.append({
                    "role": "system",
                    "content": (
                        f"[SYSTEM NOTICE: EXECUTION BUDGET] Soft budget reached ({budget.initial_iterations}). "
                        "Continue only if meaningful progress is still required. Verify and finish when satisfied."
                    ),
                })
            if iteration_number >= budget.wrapup_at and "wrapup" not in emitted_budget_notices:
                emitted_budget_notices.add("wrapup")
                active_messages.append({
                    "role": "system",
                    "content": (
                        "[SYSTEM NOTICE: WRAP UP] Reassess the original objective. Complete essential remaining work, "
                        "verify it, and use finish_task. Avoid nonessential exploration."
                    ),
                })
            # Check total wall-clock timeout
            if (time.time() - start_time) > self.timeout_seconds:
                err_msg = f"Task exceeded maximum execution time of {self.timeout_seconds}s."
                yield {"type": "error", "error": err_msg}
                await self.event_bus.emit(AgentEvent(
                    type="agent.failed",
                    status="timeout",
                    user_id=context.user_id,
                    session_id=session_id,
                    metadata={"error": err_msg},
                ))
                return

            force_synthesis = iteration_number >= budget.force_synthesis_at and not progress.stalled
            active_tools = ContextBuilder.resolve_active_tools(context, discovered_tools)
            if force_synthesis:
                active_tools = []
                if "synthesis" not in emitted_budget_notices:
                    emitted_budget_notices.add("synthesis")
                    active_messages.append({
                        "role": "system",
                        "content": (
                            "[SYSTEM NOTICE: FINAL SYNTHESIS] Do not call tools. "
                            "Produce the best truthful final response from verified work so far. "
                            "Clearly state anything incomplete or unverified."
                        ),
                    })

            req_payload = {
                "model": model_name,
                "messages": active_messages,
                "temperature": temperature,
                "stream": True,
                "tools": active_tools,
                "tool_choice": "none" if force_synthesis else "auto",
            }

            accumulated_text = ""
            accumulated_reasoning = ""
            tool_calls_buffer: Dict[int, Dict[str, Any]] = {}
            stream_success = False


            try:
                async with httpx.AsyncClient(timeout=client_timeout) as client:
                    async with client.stream(
                        "POST",
                        f"{self.upstream_url}/chat/completions",
                        json=req_payload,
                        headers=headers,
                    ) as resp:
                        if resp.status_code != 200:
                            err_body = (await resp.aread()).decode("utf-8", errors="replace")
                            raise ModelUnavailableError(model_name, f"HTTP {resp.status_code}: {err_body}")

                        async for line in resp.aiter_lines():
                            line = line.strip()
                            if not line or not line.startswith("data:"):
                                continue
                            raw = line[5:].strip()
                            if raw == "[DONE]":
                                break

                            try:
                                chunk = json.loads(raw)
                                choices = chunk.get("choices", [])
                                if not choices:
                                    continue
                                delta = choices[0].get("delta", {})

                                # Stream internal reasoning (marked as thinking, never as assistant answer)
                                reasoning = delta.get("reasoning_content") or delta.get("reasoning")
                                if reasoning:
                                    accumulated_reasoning += reasoning
                                    yield {"type": "thinking", "content": reasoning}

                                # Stream text delta
                                content = delta.get("content") or delta.get("text")
                                if content:
                                    accumulated_text += content
                                    yield {"type": "text", "content": content}
                                    stream_success = True

                                # Buffer streamed tool calls
                                tcs = delta.get("tool_calls", [])
                                for tc in tcs:
                                    idx = tc.get("index", 0)
                                    if idx not in tool_calls_buffer:
                                        tool_calls_buffer[idx] = {
                                            "id": tc.get("id") or f"call_{idx}_{int(time.time()*1000)}",
                                            "name": tc.get("function", {}).get("name", ""),
                                            "arguments": "",
                                        }
                                    if "function" in tc:
                                        fn = tc["function"]
                                        if "name" in fn and fn["name"]:
                                            tool_calls_buffer[idx]["name"] = fn["name"]
                                        if "arguments" in fn and fn["arguments"]:
                                            tool_calls_buffer[idx]["arguments"] += fn["arguments"]

                            except Exception:
                                continue

            except Exception as e:
                logger.error(f"Inference stream failed: {e}")
                yield {"type": "error", "error": f"Model inference failed: {e}"}
                await self.event_bus.emit(AgentEvent(
                    type="agent.failed",
                    status="error",
                    user_id=context.user_id,
                    session_id=session_id,
                    metadata={"error": str(e)},
                ))
                return

            # Parse assembled tool calls
            parsed_tool_calls: List[Dict[str, Any]] = []
            for tc in tool_calls_buffer.values():
                args_dict = {}
                raw_args = tc.get("arguments", "").strip()
                if raw_args:
                    try:
                        args_dict = json.loads(raw_args)
                    except Exception:
                        args_dict = {"input": raw_args}
                parsed_tool_calls.append({
                    "id": tc["id"],
                    "name": tc["name"],
                    "arguments": args_dict,
                })

            # Case A: Model issued tool calls -> Execute, Observe, and Continue Loop
            if parsed_tool_calls:
                # Add assistant message with tool calls to conversation state
                active_messages.append({
                    "role": "assistant",
                    "content": accumulated_text or None,
                    "tool_calls": [
                        {
                            "id": ptc["id"],
                            "type": "function",
                            "function": {
                                "name": ptc["name"],
                                "arguments": json.dumps(ptc["arguments"], ensure_ascii=False),
                            }
                        }
                        for ptc in parsed_tool_calls
                    ]
                })

                round_tool_results: List[ToolResult] = []

                for ptc in parsed_tool_calls:
                    t_name = ptc["name"]
                    t_args = ptc["arguments"]
                    call_id = ptc["id"]

                    yield {
                        "type": "tool_call",
                        "tool": t_name,
                        "call_id": call_id,
                        "arguments": t_args,
                    }

                    # Runtime completion protocol. The runtime handles this meta-tool directly.
                    if t_name == "finish_task":
                        status = str(t_args.get("status", "completed")).lower()
                        summary = str(t_args.get("summary", "")).strip()
                        evidence = t_args.get("evidence", {})
                        if status not in {"completed", "blocked"}:
                            status = "blocked"
                        valid, reason = verification_gate.audit_completion_request(
                            status=status,
                            summary=summary,
                            tool_results=executed_tool_results,
                            evidence=evidence,
                        )
                        if valid:
                            final_status = "success" if status == "completed" else "blocked"
                            await self.event_bus.emit(AgentEvent(
                                type="agent.completed" if status == "completed" else "agent.failed",
                                status=final_status,
                                user_id=context.user_id,
                                session_id=session_id,
                                project_id=context.project_id,
                                task_id=context.task_id,
                                metadata={"iterations": iteration_number, "total_tools_executed": len(executed_tool_results),
                                          "completion_protocol": True, "evidence": evidence},
                            ))
                            yield {"type": "text", "content": summary}
                            yield {"type": "done", "iterations": iteration_number, "status": final_status}
                            return
                        active_messages.append({
                            "role": "system",
                            "content": f"[SYSTEM NOTICE: COMPLETION REJECTED] {reason} Continue with evidence or report a truthful blocked status.",
                        })
                        continue

                    # Meta-tool handle: search_tools updates discovered capabilities
                    if t_name == "search_tools":
                        s_res = await tool_executor.execute(t_name, t_args, context, tool_call_id=call_id)
                        round_tool_results.append(s_res)
                        executed_tool_results.append(s_res)
                        for item in s_res.result.get("tools", []):
                            meta = capability_index.get_tool(item["name"])
                            if meta and meta not in discovered_tools:
                                discovered_tools.append(meta)
                        active_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "name": t_name,
                            "content": s_res.to_content_string(),
                        })
                        yield {
                            "type": "tool_result",
                            "tool": t_name,
                            "call_id": call_id,
                            "success": s_res.success,
                            "result": s_res.result,
                        }
                        continue

                    # Meta-tool handle: create_durable_task bridges to HarnessEngine
                    if t_name == "create_durable_task":
                        task_obj = t_args.get("objective", "")
                        r_level = t_args.get("risk_level", "medium")
                        dt_res = await self.planner.create_durable_harness_task(task_obj, context, risk_level=r_level)
                        t_res = ToolResult(
                            success=dt_res.get("durable", False),
                            tool=t_name,
                            result=dt_res,
                            metadata={"task_id": dt_res.get("task_id")},
                        )
                        round_tool_results.append(t_res)
                        executed_tool_results.append(t_res)
                        active_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "name": t_name,
                            "content": t_res.to_content_string(),
                        })
                        yield {
                            "type": "tool_result",
                            "tool": t_name,
                            "call_id": call_id,
                            "success": t_res.success,
                            "result": t_res.result,
                        }
                        continue

                    # Canonical tool execution
                    try:
                        res = await tool_executor.execute(t_name, t_args, context, tool_call_id=call_id)
                    except Exception as ex:
                        res = ToolResult(
                            success=False,
                            tool=t_name,
                            error={"code": "TOOL_EXCEPTION", "message": str(ex)},
                        )

                    round_tool_results.append(res)
                    executed_tool_results.append(res)

                    # Loop protection tracking
                    try:
                        self.planner.record_tool_call(t_name, t_args, res)
                    except ToolLoopDetectedError as loop_err:
                        yield {"type": "error", "error": loop_err.message}
                        await self.event_bus.emit(AgentEvent(
                            type="agent.failed",
                            status="loop_detected",
                            user_id=context.user_id,
                            session_id=session_id,
                            metadata={"error": loop_err.message},
                        ))
                        return

                    # Verification of empirical results
                    v_res = verification_gate.verify_tool_result(res)
                    res.verification_evidence = v_res.evidence

                    active_messages.append({
                        "role": "tool",
                        "tool_call_id": call_id,
                        "name": t_name,
                        "content": res.to_content_string(),
                    })

                    yield {
                        "type": "tool_result",
                        "tool": t_name,
                        "call_id": call_id,
                        "success": res.success,
                        "result": res.result,
                        "error": res.error,
                    }

                progress_snapshot = progress.observe(
                    iteration=iteration_number,
                    tool_calls=parsed_tool_calls,
                    tool_results=round_tool_results,
                    assistant_text=accumulated_text,
                )
                if progress.stalled:
                    active_messages.append({"role": "system", "content": progress.guidance()})
                    if "stall" not in emitted_budget_notices:
                        emitted_budget_notices.add("stall")
                        yield {"type": "warning", "warning": progress.guidance(), "iteration": iteration_number}
                    if progress.stall_count >= progress.stall_threshold + 1:
                        blocked_msg = (
                            "Execution stopped because the agent repeated the same execution state without measurable progress. "
                            "Completed work was preserved, but the remaining objective was not verified."
                        )
                        yield {"type": "error", "error": blocked_msg, "status": "stalled"}
                        await self.event_bus.emit(AgentEvent(
                            type="agent.failed", status="stalled", user_id=context.user_id, session_id=session_id,
                            metadata={"iterations": iteration_number, "stall_count": progress.stall_count},
                        ))
                        return
                if progress_snapshot.progressed and iteration_number >= budget.initial_iterations:
                    budget.extend_window(8)

                # If any tool failed, inject structured re-planning prompt into context
                failed_tools = [r for r in round_tool_results if not r.success]
                if failed_tools:
                    replan_notice = self.planner.formulate_replan_context([], failed_tools)
                    active_messages.append({
                        "role": "system",
                        "content": replan_notice,
                    })

                # Loop continues to next iteration so model reasons over observation
                continue

            # Case B: Model did not issue tool calls -> final synthesis or direct answer.
            if accumulated_text:
                valid, reason = verification_gate.audit_final_claim(accumulated_text, executed_tool_results)
                if valid:
                    await self.event_bus.emit(AgentEvent(
                        type="agent.completed", status="success", user_id=context.user_id, session_id=session_id,
                        project_id=context.project_id, task_id=context.task_id,
                        metadata={"iterations": iteration_number, "total_tools_executed": len(executed_tool_results)},
                    ))
                    yield {"type": "done", "iterations": iteration_number, "status": "success"}
                    return
                active_messages.append({
                    "role": "system",
                    "content": f"[SYSTEM NOTICE: FINAL RESPONSE REJECTED] {reason} Do not claim unverified completion. Continue with evidence, then use finish_task.",
                })
                continue

            # If empty and no tool calls, report truthful error rather than fake diagnostics
            if not accumulated_text and not parsed_tool_calls:
                err_msg = "Model completed turn without providing text content or actions."
                yield {"type": "error", "error": err_msg}
                await self.event_bus.emit(AgentEvent(
                    type="agent.failed",
                    status="no_response",
                    user_id=context.user_id,
                    session_id=session_id,
                    metadata={"error": err_msg},
                ))
                return

        # Hard safety ceiling reached. Never pretend the task completed.
        timeout_msg = (
            f"Execution safety ceiling reached ({budget.hard_ceiling} iterations). "
            "The task was not verified as complete."
        )
        yield {
            "type": "error", "error": timeout_msg, "status": "budget_exhausted",
            "iterations": budget.hard_ceiling, "tools_executed": len(executed_tool_results),
        }
        await self.event_bus.emit(AgentEvent(
            type="agent.failed", status="budget_exhausted", user_id=context.user_id, session_id=session_id,
            metadata={"error": timeout_msg, "iterations": budget.hard_ceiling,
                      "total_tools_executed": len(executed_tool_results)},
        ))
