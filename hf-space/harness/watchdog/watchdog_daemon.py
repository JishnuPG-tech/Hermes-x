"""
Agent Harness Watchdog Daemon
Monitors task liveness, recovers stale tasks after restarts, and executes autonomous health checks.
"""
from __future__ import annotations

import asyncio
import time
from typing import List
from harness.kernel.models import Task, TaskStatus
from harness.kernel.task_db import TaskDB
from harness.engine import HarnessEngine


class WatchdogDaemon:
    def __init__(self, engine: HarnessEngine, check_interval: int = 60, stale_threshold_seconds: int = 300):
        self.engine = engine
        self.db = engine.db
        self.check_interval = check_interval
        self.stale_threshold = stale_threshold_seconds
        self._running = False

    async def start(self) -> None:
        """Start background watchdog loop."""
        self._running = True
        print(f"[Watchdog] Starting Agent Harness Watchdog (interval={self.check_interval}s, stale_threshold={self.stale_threshold}s)...")
        while self._running:
            try:
                await self.sweep_stale_tasks()
            except Exception as e:
                print(f"[Watchdog] Error in sweep: {e}")
            await asyncio.sleep(self.check_interval)

    def stop(self) -> None:
        self._running = False

    async def sweep_stale_tasks(self) -> List[str]:
        """Detect tasks in RUNNING status that have not been updated for > stale_threshold and resume them."""
        recovered = []
        now = time.time()
        running_tasks = self.db.list_tasks(status=TaskStatus.RUNNING)

        for task in running_tasks:
            time_since_update = now - task.updated_at
            if time_since_update > self.stale_threshold:
                print(f"[Watchdog] Detected stale task {task.task_id} (inactive for {int(time_since_update)}s). Resuming from last checkpoint...")
                chk = self.db.get_latest_checkpoint(task.task_id)
                await self.engine.event_bus.emit(
                    task.task_id,
                    "watchdog.stale_detected",
                    "watchdog",
                    "recovering",
                    {"checkpoint_id": chk.checkpoint_id if chk else "none", "inactive_seconds": int(time_since_update)},
                )
                self.engine.resume_task(task.task_id)
                recovered.append(task.task_id)

        return recovered
