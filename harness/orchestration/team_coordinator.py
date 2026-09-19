"""Agent Teams & Concurrent Worker Pool.

Coordinates parallel multi-agent workers executing independent DAG subtasks
with isolated context, worker cancellation, and result merging.
"""

from __future__ import annotations

import asyncio
import logging
import time
from dataclasses import dataclass, field
from typing import Any, Callable, Coroutine, Dict, List, Optional

from harness.kernel.models import Subtask, Task, TaskStatus
from harness.orchestration.workforce import SpecialistRole, get_role

logger = logging.getLogger(__name__)


@dataclass
class WorkerContract:
    worker_id: str
    subtask_id: str
    role_name: str
    objective: str
    allowed_tools: List[str]
    timeout_seconds: float = 300.0


@dataclass
class WorkerResult:
    worker_id: str
    subtask_id: str
    success: bool
    result_text: str
    duration_seconds: float
    error: Optional[str] = None


class TeamCoordinator:
    """Manages parallel worker spawning and result aggregation."""

    def __init__(self, max_concurrent_workers: int = 3) -> None:
        self.max_concurrent_workers = max_concurrent_workers
        self.semaphore = asyncio.Semaphore(max_concurrent_workers)

    async def execute_parallel_workers(
        self,
        task: Task,
        ready_subtasks: List[Subtask],
        worker_func: Callable[[Task, Subtask, SpecialistRole], Coroutine[Any, Any, bool]],
    ) -> List[WorkerResult]:
        """Spawns parallel workers for independent subtasks and merges outcomes."""
        async def _run_single(sub: Subtask) -> WorkerResult:
            async with self.semaphore:
                role = get_role(sub.role)
                start_t = time.time()
                try:
                    success = await worker_func(task, sub, role)
                    duration = time.time() - start_t
                    return WorkerResult(
                        worker_id=f"worker_{sub.subtask_id}",
                        subtask_id=sub.subtask_id,
                        success=success,
                        result_text=sub.result or f"Finished by {role.name}",
                        duration_seconds=round(duration, 2),
                    )
                except Exception as exc:
                    duration = time.time() - start_t
                    logger.error("Worker for subtask %s failed: %s", sub.subtask_id, exc)
                    return WorkerResult(
                        worker_id=f"worker_{sub.subtask_id}",
                        subtask_id=sub.subtask_id,
                        success=False,
                        result_text="",
                        duration_seconds=round(duration, 2),
                        error=str(exc),
                    )

        results = await asyncio.gather(*[_run_single(s) for s in ready_subtasks], return_exceptions=False)
        return results
