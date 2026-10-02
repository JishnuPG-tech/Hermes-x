"""
Task State Machine
Enforces valid task state transitions and emits state change events.
"""
from __future__ import annotations

from typing import Set, Dict, Optional
from harness.kernel.models import Task, TaskStatus


class InvalidStateTransition(Exception):
    def __init__(self, current: TaskStatus, target: TaskStatus, reason: str = ""):
        super().__init__(f"Invalid state transition from {current.value} to {target.value}. {reason}")
        self.current = current
        self.target = target
        self.reason = reason


# Valid transitions map
VALID_TRANSITIONS: Dict[TaskStatus, Set[TaskStatus]] = {
    TaskStatus.CREATED: {TaskStatus.INTAKE, TaskStatus.CANCELLED},
    TaskStatus.INTAKE: {TaskStatus.CONTEXT_READY, TaskStatus.CANCELLED, TaskStatus.FAILED_FINAL},
    TaskStatus.CONTEXT_READY: {TaskStatus.PLANNED, TaskStatus.CANCELLED, TaskStatus.FAILED_FINAL},
    TaskStatus.PLANNED: {TaskStatus.WAITING_APPROVAL, TaskStatus.READY, TaskStatus.CANCELLED, TaskStatus.FAILED_FINAL},
    TaskStatus.WAITING_APPROVAL: {TaskStatus.READY, TaskStatus.CANCELLED, TaskStatus.FAILED_FINAL},
    TaskStatus.READY: {TaskStatus.RUNNING, TaskStatus.PAUSED, TaskStatus.CANCELLED},
    TaskStatus.RUNNING: {
        TaskStatus.WAITING_TOOL,
        TaskStatus.WAITING_EXTERNAL,
        TaskStatus.VERIFYING,
        TaskStatus.COMPLETED,
        TaskStatus.FAILED_RECOVERABLE,
        TaskStatus.FAILED_FINAL,
        TaskStatus.PAUSED,
        TaskStatus.CANCELLED,
    },
    TaskStatus.WAITING_TOOL: {TaskStatus.RUNNING, TaskStatus.FAILED_RECOVERABLE, TaskStatus.CANCELLED},
    TaskStatus.WAITING_EXTERNAL: {TaskStatus.RUNNING, TaskStatus.FAILED_RECOVERABLE, TaskStatus.CANCELLED},
    TaskStatus.FAILED_RECOVERABLE: {TaskStatus.RECOVERING, TaskStatus.FAILED_FINAL, TaskStatus.CANCELLED},
    TaskStatus.RECOVERING: {TaskStatus.RUNNING, TaskStatus.FAILED_FINAL, TaskStatus.CANCELLED},
    TaskStatus.VERIFYING: {TaskStatus.COMPLETED, TaskStatus.FAILED_RECOVERABLE, TaskStatus.FAILED_FINAL, TaskStatus.CANCELLED},
    TaskStatus.PAUSED: {TaskStatus.READY, TaskStatus.RUNNING, TaskStatus.CANCELLED},
    # Terminal states
    TaskStatus.COMPLETED: set(),
    TaskStatus.FAILED_FINAL: set(),
    TaskStatus.CANCELLED: set(),
}


def transition_task(task: Task, target_status: TaskStatus, reason: str = "") -> Task:
    """Validate and transition task to target status."""
    if task.status == target_status:
        return task

    allowed = VALID_TRANSITIONS.get(task.status, set())
    if target_status not in allowed:
        raise InvalidStateTransition(task.status, target_status, reason)

    task.status = target_status
    return task


def can_transition(current: TaskStatus, target: TaskStatus) -> bool:
    """Check if transition is valid without raising exception."""
    if current == target:
        return True
    return target in VALID_TRANSITIONS.get(current, set())
