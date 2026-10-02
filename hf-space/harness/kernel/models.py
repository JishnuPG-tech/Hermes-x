"""
Agent Harness Core Data Models
Implements typed contracts for Tasks, Subtasks, Steps, Checkpoints, and Approvals.
"""
from __future__ import annotations

import time
import uuid
from dataclasses import dataclass, field, asdict
from enum import Enum
from typing import Any, Dict, List, Optional


class TaskStatus(str, Enum):
    CREATED = "CREATED"
    INTAKE = "INTAKE"
    CONTEXT_READY = "CONTEXT_READY"
    PLANNED = "PLANNED"
    WAITING_APPROVAL = "WAITING_APPROVAL"
    READY = "READY"
    RUNNING = "RUNNING"
    WAITING_TOOL = "WAITING_TOOL"
    WAITING_EXTERNAL = "WAITING_EXTERNAL"
    FAILED_RECOVERABLE = "FAILED_RECOVERABLE"
    RECOVERING = "RECOVERING"
    VERIFYING = "VERIFYING"
    COMPLETED = "COMPLETED"
    FAILED_FINAL = "FAILED_FINAL"
    CANCELLED = "CANCELLED"
    PAUSED = "PAUSED"
    ARCHIVED = "ARCHIVED"


class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class PermissionLevel(str, Enum):
    L0_OBSERVE = "L0"
    L1_PROJECT_READ = "L1"
    L2_PROJECT_EDIT = "L2"
    L3_TERMINAL_EXEC = "L3"
    L4_NETWORK_API = "L4"
    L5_GITHUB_WRITE = "L5"
    L6_DEPLOY = "L6"
    L7_SYS_ADMIN = "L7"
    L8_DESTRUCTIVE = "L8"


class ApprovalStatus(str, Enum):
    PENDING = "pending"
    APPROVED = "approved"
    DENIED = "denied"
    EXPIRED = "expired"


class FailureClass(str, Enum):
    TRANSIENT = "transient"
    DEPENDENCY = "dependency"
    CODE_DEFECT = "code_defect"
    PERMISSION = "permission"
    ENVIRONMENT = "environment"
    UNKNOWN = "unknown"


@dataclass
class Task:
    task_id: str = field(default_factory=lambda: f"task_{uuid.uuid4().hex[:12]}")
    project_id: str = "default"
    objective: str = ""
    status: TaskStatus = TaskStatus.CREATED
    risk_level: RiskLevel = RiskLevel.MEDIUM
    idempotency_key: Optional[str] = None
    parent_task_id: Optional[str] = None
    assigned_worker: Optional[str] = None
    workspace_path: Optional[str] = None
    allowed_tools: List[str] = field(default_factory=lambda: ["bash_exec", "read_file", "write_file", "edit_file", "list_directory"])
    allowed_integrations: List[str] = field(default_factory=list)
    retry_budget: int = 5
    retry_count: int = 0
    checkpoint: Optional[str] = None
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["status"] = self.status.value
        d["risk_level"] = self.risk_level.value
        return d

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Task:
        data = data.copy()
        if "status" in data and isinstance(data["status"], str):
            data["status"] = TaskStatus(data["status"])
        if "risk_level" in data and isinstance(data["risk_level"], str):
            data["risk_level"] = RiskLevel(data["risk_level"])
        return cls(**data)


@dataclass
class Subtask:
    subtask_id: str = field(default_factory=lambda: f"sub_{uuid.uuid4().hex[:10]}")
    task_id: str = ""
    title: str = ""
    description: str = ""
    role: str = "Developer"
    dependencies: List[str] = field(default_factory=list)
    status: TaskStatus = TaskStatus.CREATED
    acceptance_criteria: List[str] = field(default_factory=list)
    verification_command: Optional[str] = None
    result: Optional[str] = None
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["status"] = self.status.value
        return d

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> Subtask:
        data = data.copy()
        if "status" in data and isinstance(data["status"], str):
            data["status"] = TaskStatus(data["status"])
        return cls(**data)


@dataclass
class RunStep:
    step_id: str = field(default_factory=lambda: f"step_{uuid.uuid4().hex[:10]}")
    task_id: str = ""
    subtask_id: Optional[str] = None
    step_index: int = 0
    action_type: str = "tool_call"  # plan, tool_call, verification, recovery, checkpoint
    action_name: str = ""
    input_payload: Dict[str, Any] = field(default_factory=dict)
    output_payload: Optional[Dict[str, Any]] = None
    status: str = "pending"  # pending, success, failed, skipped
    error: Optional[str] = None
    duration_ms: Optional[int] = None
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class Checkpoint:
    checkpoint_id: str = field(default_factory=lambda: f"chk_{uuid.uuid4().hex[:10]}")
    task_id: str = ""
    completed_subtasks: List[str] = field(default_factory=list)
    active_subtask_id: Optional[str] = None
    workspace_revision: Optional[str] = None
    known_failures: List[Dict[str, Any]] = field(default_factory=list)
    retry_count: int = 0
    state_payload: Dict[str, Any] = field(default_factory=dict)
    created_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class ApprovalRequest:
    approval_id: str = field(default_factory=lambda: f"appr_{uuid.uuid4().hex[:10]}")
    task_id: str = ""
    action: str = ""
    risk_level: RiskLevel = RiskLevel.HIGH
    permission_level: PermissionLevel = PermissionLevel.L5_GITHUB_WRITE
    reason: str = ""
    parameters: Dict[str, Any] = field(default_factory=dict)
    requested_by: str = "system"
    requested_at: float = field(default_factory=time.time)
    expires_at: float = field(default_factory=lambda: time.time() + 86400)
    status: ApprovalStatus = ApprovalStatus.PENDING
    decision_by: Optional[str] = None
    decision_at: Optional[float] = None
    decision_reason: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["risk_level"] = self.risk_level.value
        d["permission_level"] = self.permission_level.value
        d["status"] = self.status.value
        return d


@dataclass
class TaskContract:
    contract_id: str = field(default_factory=lambda: f"tc_{uuid.uuid4().hex[:10]}")
    task_id: str = ""
    objective: str = ""
    scope: str = "project"
    authorization_level: PermissionLevel = PermissionLevel.L3_TERMINAL_EXEC
    constraints: List[str] = field(default_factory=list)
    required_tools: List[str] = field(default_factory=list)
    risk_level: RiskLevel = RiskLevel.MEDIUM
    dependencies: List[str] = field(default_factory=list)
    success_conditions: List[str] = field(default_factory=list)
    rollback_strategy: str = "git_checkout"
    reporting_policy: str = "on_complete_or_failure"
    created_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["authorization_level"] = self.authorization_level.value
        d["risk_level"] = self.risk_level.value
        return d
