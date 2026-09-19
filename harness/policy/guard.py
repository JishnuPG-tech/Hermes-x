"""
Policy Guard Pre-Execution Interceptor
Enforces tool access, workspace sandboxing, and human approval checks before any action runs.
"""
from __future__ import annotations

import os
from pathlib import Path
from typing import Dict, Any, Optional, Tuple

from harness.kernel.models import Task, PermissionLevel, RiskLevel, ApprovalRequest
from harness.policy.permission_levels import classify_action, requires_approval
from harness.policy.approval_service import ApprovalService


class PolicyViolation(Exception):
    def __init__(self, message: str, level: PermissionLevel, requires_approval: bool = False, approval_req: Optional[ApprovalRequest] = None):
        super().__init__(message)
        self.level = level
        self.requires_approval = requires_approval
        self.approval_req = approval_req


# Sensitive paths that can NEVER be written or deleted by an autonomous agent
PROTECTED_PATHS = [
    "/etc",
    "/root",
    "/bin",
    "/sbin",
    "/lib",
    "/usr",
    "/var/run",
    "/proc",
    "/sys",
    "~/.ssh",
    "~/.gnupg",
]


class PolicyGuard:
    def __init__(self, approval_service: ApprovalService, default_max_level: PermissionLevel = PermissionLevel.L4_NETWORK_API):
        self.approval_service = approval_service
        self.default_max_level = default_max_level

    def validate_action(self, task: Task, tool_name: str, arguments: Dict[str, Any]) -> Tuple[bool, Optional[str]]:
        """Validate if tool execution is permitted under current policy.
        Raises PolicyViolation if disallowed or requires approval.
        """
        # 1. Allowed tools check
        if tool_name not in task.allowed_tools:
            raise PolicyViolation(
                f"Tool '{tool_name}' is not in the allowed tools list for task {task.task_id}.",
                level=PermissionLevel.L3_TERMINAL_EXEC,
            )

        # 2. Filesystem sandbox check
        self._check_filesystem_sandbox(task, tool_name, arguments)

        # 3. Classify action permission and risk
        level, risk, reason = classify_action(tool_name, arguments)

        # 4. Check approval requirement
        if requires_approval(level, self.default_max_level):
            # Check if this action was already approved
            if not self.approval_service.is_action_approved(task.task_id, tool_name, arguments):
                # Issue approval request
                req = self.approval_service.request_approval(
                    task_id=task.task_id,
                    action=tool_name,
                    risk_level=risk,
                    permission_level=level,
                    reason=reason,
                    parameters=arguments,
                )
                raise PolicyViolation(
                    f"Action '{tool_name}' requires owner approval (Level: {level.value}, Risk: {risk.value}). Approval request created: {req.approval_id}",
                    level=level,
                    requires_approval=True,
                    approval_req=req,
                )

        return True, None

    def _check_filesystem_sandbox(self, task: Task, tool_name: str, arguments: Dict[str, Any]):
        """Ensure file operations stay inside the allowed workspace and do not mutate system files."""
        path_arg = arguments.get("file_path") or arguments.get("path") or arguments.get("target_path")
        if not path_arg:
            return

        resolved_path = Path(path_arg).resolve()
        resolved_str = str(resolved_path)

        # Check protected paths
        for p in PROTECTED_PATHS:
            expanded = Path(p).expanduser().resolve()
            if resolved_path == expanded or expanded in resolved_path.parents:
                raise PolicyViolation(
                    f"Access to protected system path '{p}' is forbidden.",
                    level=PermissionLevel.L8_DESTRUCTIVE,
                )

        # If task has a dedicated workspace, ensure file mutations are inside it
        if task.workspace_path and tool_name in ("write_file", "edit_file", "delete_file"):
            workspace = Path(task.workspace_path).resolve()
            if resolved_path != workspace and workspace not in resolved_path.parents:
                # Allowed only if specifically permitted or inside /data
                data_root = Path("/data").resolve()
                if not (data_root.exists() and (resolved_path == data_root or data_root in resolved_path.parents)):
                    raise PolicyViolation(
                        f"Mutation to '{resolved_str}' is outside task workspace '{task.workspace_path}'.",
                        level=PermissionLevel.L7_SYS_ADMIN,
                    )
