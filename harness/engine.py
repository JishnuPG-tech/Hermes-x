"""
Agent Harness Core Engine
Orchestrates the end-to-end task lifecycle:
Intent -> Context -> Plan -> Policy Guard -> Act -> Verify -> Recover -> Checkpoint -> Complete.
"""
from __future__ import annotations

import asyncio
import time
from typing import Dict, Any, List, Optional

from harness.kernel.models import (
    Task,
    Subtask,
    RunStep,
    Checkpoint,
    TaskStatus,
    RiskLevel,
    PermissionLevel,
    ApprovalStatus,
    FailureClass,
)
from harness.kernel.task_db import TaskDB
from harness.kernel.state_machine import transition_task
from harness.policy.guard import PolicyGuard, PolicyViolation
from harness.policy.approval_service import ApprovalService
from harness.orchestration.planner import Planner
from harness.orchestration.dag_engine import DAGEngine
from harness.orchestration.workforce import get_role
from harness.orchestration.isolated_workspace import WorkspaceManager
from harness.verifier.runner import VerificationRunner, VerificationResult
from harness.recovery.recovery_manager import RecoveryManager, RecoveryStrategy
from harness.events.event_bus import EventBus
from harness.connectors.obsidian_connector import ObsidianConnector
from harness.connectors.secrets_boundary import SecretsBoundary


class HarnessEngine:
    def __init__(
        self,
        db: Optional[TaskDB] = None,
        event_bus: Optional[EventBus] = None,
        approval_service: Optional[ApprovalService] = None,
        policy_guard: Optional[PolicyGuard] = None,
        workspace_manager: Optional[WorkspaceManager] = None,
        verification_runner: Optional[VerificationRunner] = None,
        recovery_manager: Optional[RecoveryManager] = None,
        obsidian_connector: Optional[ObsidianConnector] = None,
        secrets_boundary: Optional[SecretsBoundary] = None,
    ):
        self.db = db or TaskDB()
        self.event_bus = event_bus or EventBus(self.db)
        self.approval_service = approval_service or ApprovalService(self.db)
        self.policy_guard = policy_guard or PolicyGuard(self.approval_service)
        self.workspace_manager = workspace_manager or WorkspaceManager()
        self.verifier = verification_runner or VerificationRunner()
        self.recovery = recovery_manager or RecoveryManager()
        self.obsidian = obsidian_connector or ObsidianConnector()
        self.secrets = secrets_boundary or SecretsBoundary()
        self.planner = Planner()

    async def create_and_run_task(
        self,
        objective: str,
        project_id: str = "default",
        idempotency_key: Optional[str] = None,
        risk_level: RiskLevel = RiskLevel.MEDIUM,
        allowed_tools: Optional[List[str]] = None,
    ) -> Task:
        """Entry point to create and start a new durable task."""
        # 1. Check idempotency
        if idempotency_key:
            existing = self.db.get_task_by_idempotency_key(idempotency_key)
            if existing:
                return existing

        # 2. Create task record
        task = Task(
            project_id=project_id,
            objective=objective,
            idempotency_key=idempotency_key,
            risk_level=risk_level,
            allowed_tools=allowed_tools or ["bash_exec", "read_file", "write_file", "edit_file", "list_directory"],
        )
        # Create TaskContract
        from harness.kernel.models import TaskContract
        contract = TaskContract(
            task_id=task.task_id,
            objective=objective,
            scope=f"project:{project_id}",
            risk_level=risk_level,
            required_tools=task.allowed_tools,
        )
        task.metadata["task_contract"] = contract.to_dict()

        # Create dedicated workspace
        workspace = self.workspace_manager.create_workspace(task.task_id)
        task.workspace_path = str(workspace)
        self.db.save_task(task)

        await self.event_bus.emit(task.task_id, "task.created", "harness", "success", {"objective": objective, "workspace": str(workspace), "contract_id": contract.contract_id})

        # 3. Transition to INTAKE -> CONTEXT_READY -> PLANNED
        transition_task(task, TaskStatus.INTAKE)
        self.db.save_task(task)

        transition_task(task, TaskStatus.CONTEXT_READY)
        self.db.save_task(task)

        # 4. Generate Subtasks Plan
        subtasks = self.planner.create_fallback_plan(task)
        self.db.save_subtasks(subtasks)

        transition_task(task, TaskStatus.PLANNED)
        self.db.save_task(task)
        await self.event_bus.emit(task.task_id, "plan.created", "planner", "success", {"subtasks_count": len(subtasks)})

        # 5. Transition to READY
        transition_task(task, TaskStatus.READY)
        self.db.save_task(task)

        # 6. Execute task asynchronously
        asyncio.create_task(self._run_task_loop(task.task_id))
        return task

    async def _run_task_loop(self, task_id: str) -> None:
        """Main execution loop driving subtasks through DAG order."""
        task = self.db.get_task(task_id)
        if not task or task.status not in (TaskStatus.READY, TaskStatus.RUNNING, TaskStatus.RECOVERING):
            return

        try:
            transition_task(task, TaskStatus.RUNNING)
            self.db.save_task(task)
            await self.event_bus.emit(task.task_id, "task.started", "harness", "running", {})

            while True:
                # Reload task in case status changed (paused/cancelled)
                task = self.db.get_task(task_id)
                if not task or task.status in (TaskStatus.PAUSED, TaskStatus.CANCELLED, TaskStatus.FAILED_FINAL):
                    break

                subtasks = self.db.get_subtasks(task_id)
                dag = DAGEngine(subtasks)

                if dag.is_complete():
                    # All subtasks complete -> Verification & Finalization
                    await self._finalize_task(task, subtasks)
                    break

                if dag.has_failures():
                    transition_task(task, TaskStatus.FAILED_FINAL)
                    self.db.save_task(task)
                    await self.event_bus.emit(task.task_id, "task.failed", "harness", "error", {"reason": "One or more subtasks failed permanently."})
                    break

                ready_subtasks = dag.get_ready_subtasks()
                if not ready_subtasks:
                    # Stalled DAG or unresolved dependencies
                    transition_task(task, TaskStatus.FAILED_FINAL)
                    self.db.save_task(task)
                    await self.event_bus.emit(task.task_id, "task.failed", "harness", "error", {"reason": "DAG deadlock: no subtasks ready to execute."})
                    break

                # Execute ready subtasks concurrently via TeamCoordinator
                from harness.orchestration.team_coordinator import TeamCoordinator
                coordinator = TeamCoordinator(max_concurrent_workers=3)
                worker_results = await coordinator.execute_parallel_workers(
                    task=task,
                    ready_subtasks=ready_subtasks,
                    worker_func=self._execute_worker,
                )

                # Check if any subtask requested owner approval
                if task.status == TaskStatus.WAITING_APPROVAL:
                    return

                # If any worker failed, break cycle to allow recovery
                if any(not r.success for r in worker_results):
                    break

        except Exception as e:
            print(f"[HarnessEngine] Unhandled error in task loop {task_id}: {e}")
            if task:
                task.status = TaskStatus.FAILED_FINAL
                self.db.save_task(task)
                await self.event_bus.emit(task.task_id, "task.failed", "harness", "error", {"error": str(e)})

    async def _execute_worker(self, task: Task, subtask: Subtask, role: SpecialistRole) -> bool:
        """Explicit worker executor invoked by TeamCoordinator with specialist role."""
        return await self._execute_subtask(task, subtask, role=role)

    async def _execute_subtask(self, task: Task, subtask: Subtask, role: Optional[SpecialistRole] = None) -> bool:
        """Execute a single subtask with role persona, policy guard, verification, and recovery."""
        subtask.status = TaskStatus.RUNNING
        self.db.save_subtasks([subtask])
        worker_role = role or get_role(subtask.role)

        await self.event_bus.emit(
            task.task_id,
            "subtask.started",
            worker_role.name,
            "running",
            {"subtask_id": subtask.subtask_id, "title": subtask.title},
        )

        step_index = len(self.db.get_steps(task.task_id)) + 1
        step = RunStep(
            task_id=task.task_id,
            subtask_id=subtask.subtask_id,
            step_index=step_index,
            action_type="execution",
            action_name=f"{worker_role.name}:{subtask.title}",
        )

        # 1. Verification command check if present
        if subtask.verification_command:
            # Check hierarchical policy guard with worker role
            from harness.policy.trust_hierarchy import get_trust_engine
            eval_res = get_trust_engine().evaluate(
                tool_name="bash_exec",
                arguments={"command": subtask.verification_command},
                task_id=task.task_id,
                worker_role=worker_role.name,
                task_allowed_tools=task.allowed_tools,
            )
            if eval_res.requires_approval:
                task.status = TaskStatus.WAITING_APPROVAL
                self.db.save_task(task)
                subtask.status = TaskStatus.WAITING_APPROVAL
                self.db.save_subtasks([subtask])
                await self.event_bus.emit(task.task_id, "approval.requested", "policy_guard", "pending", {"reason": eval_res.reason})
                return False
            elif eval_res.decision.value == "DENY":
                subtask.status = TaskStatus.FAILED_FINAL
                self.db.save_subtasks([subtask])
                return False

            # Run deterministic verification
            await self.event_bus.emit(task.task_id, "verification.started", "verifier", "running", {"command": subtask.verification_command})
            v_result = await self.verifier.verify(subtask.verification_command, cwd=task.workspace_path)
            step.output_payload = v_result.to_dict()

            if not v_result.passed:
                # Handle recovery
                f_class = FailureClass(v_result.failure_class) if v_result.failure_class else FailureClass.UNKNOWN
                strategy, reason, backoff = self.recovery.determine_strategy(
                    task=task,
                    failure_class=f_class,
                    error_signature=v_result.stderr[:200],
                    recent_failures=[],
                )
                await self.event_bus.emit(task.task_id, "verification.failed", "verifier", "failed", {"error": v_result.stderr, "strategy": strategy, "reason": reason})

                if strategy == RecoveryStrategy.ESCALATE_OWNER:
                    subtask.status = TaskStatus.FAILED_FINAL
                    self.db.save_subtasks([subtask])
                    return False

                task.retry_count += 1
                self.db.save_task(task)
                if backoff > 0:
                    await asyncio.sleep(backoff)
                return False

            await self.event_bus.emit(task.task_id, "verification.passed", "verifier", "success", {"command": subtask.verification_command})

        # Subtask succeeded
        subtask.status = TaskStatus.COMPLETED
        subtask.result = f"Completed successfully by {role.name}."
        self.db.save_subtasks([subtask])

        step.status = "success"
        self.db.record_step(step)

        # Save Checkpoint
        chk = Checkpoint(
            task_id=task.task_id,
            completed_subtasks=[s.subtask_id for s in self.db.get_subtasks(task.task_id) if s.status == TaskStatus.COMPLETED],
            active_subtask_id=subtask.subtask_id,
            retry_count=task.retry_count,
        )
        self.db.save_checkpoint(chk)
        await self.event_bus.emit(task.task_id, "checkpoint.saved", "harness", "success", {"checkpoint_id": chk.checkpoint_id})

        return True

    async def _finalize_task(self, task: Task, subtasks: List[Subtask]) -> None:
        """Finalize task, transition to COMPLETED, and record knowledge in Obsidian vault."""
        transition_task(task, TaskStatus.VERIFYING)
        self.db.save_task(task)
        await self.event_bus.emit(task.task_id, "task.verifying", "harness", "running", {})

        transition_task(task, TaskStatus.COMPLETED)
        self.db.save_task(task)

        # Record verified task summary in Notion (primary) and Obsidian
        try:
            from harness.knowledge.notion_connector import NotionConnector
            from harness.knowledge.models import WriteIntent
            import uuid
            notion_conn = NotionConnector()
            if notion_conn.is_configured:
                await notion_conn.write(WriteIntent(
                    intent_id=f"w_{uuid.uuid4().hex[:8]}",
                    source="notion",
                    document_id="",
                    title=f"Task: {task.objective[:50]}",
                    content=f"## Objective\n{task.objective}\n\n## Outcome\nAll {len(subtasks)} subtasks successfully verified and completed.\n\n- Task ID: `{task.task_id}`\n- Workspace: `{task.workspace_path}`",
                ))
        except Exception as e:
            print(f"[HarnessEngine] Warning: could not sync task note to Notion: {e}")

        try:
            artifacts = [f"Workspace: {task.workspace_path}"]
            self.obsidian.record_task_summary(
                task_id=task.task_id,
                title=task.objective[:50],
                objective=task.objective,
                outcome=f"All {len(subtasks)} subtasks successfully verified and completed.",
                artifacts=artifacts,
            )
        except Exception as e:
            print(f"[HarnessEngine] Warning: could not write Obsidian note: {e}")

        await self.event_bus.emit(task.task_id, "task.completed", "harness", "success", {
            "task_id": task.task_id,
            "subtasks_completed": len(subtasks),
        })

    def resume_task(self, task_id: str) -> bool:
        """Resume a paused, interrupted, or waiting-approval task."""
        task = self.db.get_task(task_id)
        if not task:
            return False

        if task.status in (TaskStatus.PAUSED, TaskStatus.WAITING_APPROVAL, TaskStatus.READY):
            task.status = TaskStatus.READY
            self.db.save_task(task)
            asyncio.create_task(self._run_task_loop(task_id))
            return True
        return False

    def pause_task(self, task_id: str) -> bool:
        task = self.db.get_task(task_id)
        if not task or task.status not in (TaskStatus.RUNNING, TaskStatus.READY):
            return False
        task.status = TaskStatus.PAUSED
        self.db.save_task(task)
        return True

    def cancel_task(self, task_id: str) -> bool:
        task = self.db.get_task(task_id)
        if not task or task.status in (TaskStatus.COMPLETED, TaskStatus.FAILED_FINAL, TaskStatus.CANCELLED):
            return False
        task.status = TaskStatus.CANCELLED
        self.db.save_task(task)
        return True
