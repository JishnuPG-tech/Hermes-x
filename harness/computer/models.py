"""
Server Computer Data Models
===========================
Defines data structures for the persistent execution computer:
- ProjectRecord (managed Git repositories)
- TaskSpec (machine-readable task contracts)
- WorkspaceInfo (sandboxed task working directories)
- VerificationContract & VerificationResult (evidence-based task completion)
- SystemStatus (storage and runtime metrics)
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field, asdict
from typing import Dict, Any, List, Optional


@dataclass
class ProjectRecord:
    project_id: str
    name: str
    path: str
    github_repo: Optional[str] = None
    default_branch: str = "main"
    policy: str = "standard-development"  # "standard-development" | "read-only" | "high-security"
    description: Optional[str] = None
    created_at: float = field(default_factory=time.time)
    last_active_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class WorkspaceInfo:
    task_id: str
    project_id: str
    path: str
    git_commit: Optional[str] = None
    git_branch: Optional[str] = None
    is_worktree: bool = True
    created_at: float = field(default_factory=time.time)
    status: str = "active"  # "active" | "archived" | "cleaned"

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class VerificationContract:
    commands: List[str] = field(default_factory=list)
    checks: List[str] = field(default_factory=list)
    require_clean_git: bool = False
    timeout_seconds: int = 180

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class VerificationResult:
    passed: bool
    summary: str
    command_results: List[Dict[str, Any]] = field(default_factory=list)
    git_clean: Optional[bool] = None
    verified_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class TaskSpec:
    id: str
    project_id: str
    objective: str
    workspace: str
    git_remote: Optional[str] = None
    git_base_ref: str = "main"
    status: str = "created"  # created, running, paused, verifying, completed, failed
    verification: VerificationContract = field(default_factory=VerificationContract)
    verification_result: Optional[VerificationResult] = None
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        if self.verification:
            data["verification"] = self.verification.to_dict()
        if self.verification_result:
            data["verification_result"] = self.verification_result.to_dict()
        return data


@dataclass
class SystemStatus:
    storage_root: str
    writable: bool
    disk_total_gb: float
    disk_free_gb: float
    active_projects_count: int
    active_workspaces_count: int
    status: str = "healthy"
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
