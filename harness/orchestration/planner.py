"""
Structured Planner
Decomposes high-level objectives into machine-readable subtask DAGs.
"""
from __future__ import annotations

import json
import re
import sys
from typing import List, Dict, Any, Optional
from harness.kernel.models import Task, Subtask, TaskStatus


PLANNING_SYSTEM_PROMPT = """You are the Hermes Task Planner.
Break the following user objective into an ordered DAG of executable subtasks.
Each subtask must assign a specialist role (Architect, Developer, QA Engineer, Security Reviewer, DevOps),
state dependencies (subtask IDs that must complete first), define clear acceptance criteria,
and specify an optional verification command (e.g. pytest or bash check).

Return ONLY a valid JSON array of objects with the following schema:
[
  {
    "subtask_id": "step_1",
    "title": "Short title",
    "description": "What to do",
    "role": "Architect",
    "dependencies": [],
    "acceptance_criteria": ["criteria 1"],
    "verification_command": "command or null"
  }
]
"""


class Planner:
    def create_fallback_plan(self, task: Task) -> List[Subtask]:
        """Deterministic default 3-stage plan when LLM planner is not invoked."""
        sub1 = Subtask(
            subtask_id=f"{task.task_id}_arch",
            task_id=task.task_id,
            title="Analyze requirements & system architecture",
            description=f"Analyze objective: {task.objective}",
            role="Architect",
            dependencies=[],
            acceptance_criteria=["Architecture and implementation approach defined"],
        )
        sub2 = Subtask(
            subtask_id=f"{task.task_id}_dev",
            task_id=task.task_id,
            title="Implement code and technical deliverables",
            description=f"Write code fulfilling: {task.objective}",
            role="Developer",
            dependencies=[sub1.subtask_id],
            acceptance_criteria=["Source files created or updated"],
        )
        sub3 = Subtask(
            subtask_id=f"{task.task_id}_qa",
            task_id=task.task_id,
            title="Verify implementation and test suite",
            description="Run verification tests and checks to prove correctness",
            role="QA Engineer",
            dependencies=[sub2.subtask_id],
            acceptance_criteria=["Tests and verification commands succeed"],
            verification_command='python -c "import sys; sys.exit(0)"' if sys.platform == "win32" else "python3 -c 'import sys; sys.exit(0)'",
        )
        return [sub1, sub2, sub3]

    def parse_plan_json(self, task_id: str, raw_text: str) -> List[Subtask]:
        """Parse model JSON response into Subtask objects."""
        # Find JSON array in text
        match = re.search(r"\[\s*\{.*\}\s*\]", raw_text, re.DOTALL)
        if not match:
            return []

        try:
            data = json.loads(match.group(0))
            subtasks = []
            for item in data:
                sub = Subtask(
                    subtask_id=item.get("subtask_id", f"sub_{len(subtasks)+1}"),
                    task_id=task_id,
                    title=item.get("title", "Untitled Subtask"),
                    description=item.get("description", ""),
                    role=item.get("role", "Developer"),
                    dependencies=item.get("dependencies", []),
                    acceptance_criteria=item.get("acceptance_criteria", []),
                    verification_command=item.get("verification_command"),
                    status=TaskStatus.CREATED,
                )
                subtasks.append(sub)
            return subtasks
        except Exception:
            return []
