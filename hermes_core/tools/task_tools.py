"""
Task Agent Tools
================
Exposes 24/7 background task scheduling, management, and inspection to Hermes Agent.
"""
from __future__ import annotations

import logging
from typing import Optional, Dict, Any, List
from hermes_core.tools.registry import registry

logger = logging.getLogger("hermes.tools.tasks")


@registry.register(
    name="schedule_task",
    description="Schedule an autonomous 24/7 background task that runs continuously or periodically on the server.",
    parameters={
        "type": "object",
        "properties": {
            "name": {"type": "string", "description": "A short, descriptive name for the task."},
            "instruction": {
                "type": "string",
                "description": "The instruction for the agent to execute or the shell command to run on every interval.",
            },
            "interval_seconds": {
                "type": "integer",
                "description": "How often to run the task in seconds (default: 300).",
            },
            "task_type": {
                "type": "string",
                "enum": ["agent", "bash"],
                "description": "Use 'agent' for autonomous AI reasoning/tool execution, or 'bash' for direct shell script execution.",
            },
            "notify_channels": {
                "type": "boolean",
                "description": "Set to true to dispatch completion summaries to notification channels.",
            },
        },
        "required": ["name", "instruction"],
    },
    category="task",
    side_effects=True,
)
async def schedule_task(
    name: str,
    instruction: str,
    interval_seconds: int = 300,
    task_type: str = "agent",
    notify_channels: bool = False,
    context: Optional[Any] = None,
) -> str:
    from gateway import background_agent as bg
    chat_id = getattr(context, "session_id", "background_autonomous_session") if context else "background_autonomous_session"
    job = bg.schedule_job(
        name=name,
        instruction=instruction,
        interval_seconds=int(interval_seconds),
        task_type=task_type,
        notify_channels=bool(notify_channels),
        chat_id=chat_id,
    )
    return (
        f"**24/7 Background Task Registered**\n"
        f"- **Task ID**: `{job['id']}`\n"
        f"- **Name**: {job['name']}\n"
        f"- **Interval**: Every {interval_seconds} seconds ({int(interval_seconds)//60} mins)\n"
        f"- **Type**: {task_type.upper()}\n"
        f"- **Status**: Active (Running in background on server)"
    )


@registry.register(
    name="list_background_tasks",
    description="List all persistent 24/7 background tasks running on the server.",
    parameters={"type": "object", "properties": {}},
    category="task",
)
def list_background_tasks() -> str:
    from gateway import background_agent as bg
    jobs = bg.get_all_jobs()
    if not jobs:
        return "No 24/7 background tasks currently scheduled."
    lines = ["### 24/7 Persistent Background Tasks on Server\n"]
    for j in jobs:
        status_label = "[Active]" if j.get("enabled") and j.get("status") in ("RUNNING", "SCHEDULED", "SUCCESS") else "[Paused]"
        lines.append(
            f"- **{j.get('name')}** (`{j.get('id')}`) — {status_label}\n"
            f"  - Interval: Every {j.get('interval_seconds')}s\n"
            f"  - Runs completed: {j.get('run_count', 0)}\n"
            f"  - Last run: {j.get('last_run_at') or 'Pending first run'}\n"
            f"  - Status: {j.get('status')}\n"
        )
    return "\n".join(lines)


@registry.register(
    name="stop_background_task",
    description="Stop and cancel a 24/7 background task by its Task ID.",
    parameters={
        "type": "object",
        "properties": {
            "task_id": {"type": "string", "description": "The ID of the task to stop."}
        },
        "required": ["task_id"],
    },
    category="task",
    side_effects=True,
)
def stop_background_task(task_id: str) -> str:
    from gateway import background_agent as bg
    tid = str(task_id).strip()
    ok = bg.cancel_job(tid)
    if ok:
        return f"Task `{tid}` has been stopped."
    return f"Error: Task `{tid}` not found."


@registry.register(
    name="get_task_logs",
    description="Retrieve execution logs of a 24/7 background task.",
    parameters={
        "type": "object",
        "properties": {
            "task_id": {"type": "string", "description": "The ID of the task to inspect."}
        },
        "required": ["task_id"],
    },
    category="task",
)
def get_task_logs(task_id: str) -> str:
    from gateway import background_agent as bg
    tid = str(task_id).strip()
    logs = bg.get_job_logs(tid)
    return f"**Logs for `{tid}`:**\n```text\n{logs[-2000:]}\n```"
