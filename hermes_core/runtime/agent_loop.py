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
        max_iterations: int = 8,
        timeout_seconds: float = 120.0,
        event_bus: Optional[RuntimeEventBus] = None,
    ):
        self.upstream_url = upstream_url.rstrip("/")
        self.api_key = api_key
        self.max_iterations = max(1, min(max_iterations, 16))
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
        for iteration in range(self.max_iterations):
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

            active_tools = ContextBuilder.resolve_active_tools(context, discovered_tools)

            req_payload = {
                "model": model_name,
                "messages": active_messages,
                "temperature": temperature,
                "stream": True,
                "tools": active_tools,
                "tool_choice": "auto",
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

            # Case B: Model did not issue tool calls -> It produced a direct answer or final synthesis
            if accumulated_text:
                # Truthfulness & Verification Audit
                valid, reason = verification_gate.audit_final_claim(accumulated_text, executed_tool_results)
                if not valid:
                    logger.warning(f"Verification claim audit warning: {reason}")

                await self.event_bus.emit(AgentEvent(
                    type="agent.completed",
                    status="success",
                    user_id=context.user_id,
                    session_id=session_id,
                    project_id=context.project_id,
                    task_id=context.task_id,
                    metadata={"iterations": iteration + 1, "total_tools_executed": len(executed_tool_results)},
                ))
                yield {"type": "done", "iterations": iteration + 1}
                return

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

        # Exceeded max iterations
        timeout_msg = f"Task reached maximum allowed iterations ({self.max_iterations}) without concluding."
        yield {"type": "error", "error": timeout_msg}
        await self.event_bus.emit(AgentEvent(
            type="agent.failed",
            status="max_iterations",
            user_id=context.user_id,
            session_id=session_id,
            metadata={"error": timeout_msg},
        ))
