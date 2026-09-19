"""Truly Hierarchical Trust Hierarchy Policy Engine.

Independently evaluates every layer in the 6-tier hierarchy:
  1. Owner Policy: Global blocklists and destruction safety gates.
  2. Hermes Policy: Agent-wide autonomy boundaries.
  3. Project Policy: Repository and workspace boundaries.
  4. Task Policy: Explicit TaskContract allowed_tools list.
  5. Worker Policy: Specialist role allowed_tools (e.g. Architect cannot run bash_exec).
  6. Tool & Action: Parameter and risk inspection (L0-L8).
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from enum import Enum
from typing import Any, Dict, List, Optional

from harness.kernel.models import PermissionLevel, RiskLevel
from harness.orchestration.workforce import get_role
from harness.policy.permission_levels import classify_action
from harness.security.audit import get_audit_logger

logger = logging.getLogger(__name__)


class Decision(str, Enum):
    ALLOW = "ALLOW"
    DENY = "DENY"
    REQUIRES_APPROVAL = "REQUIRES_APPROVAL"


@dataclass
class PolicyEvaluationResult:
    decision: Decision
    level: PermissionLevel
    risk: RiskLevel
    reason: str
    requires_approval: bool = False
    approval_scope: Optional[str] = None
    evaluated_at: float = 0.0


class TrustHierarchyEngine:
    """Evaluates requested tool actions through all 6 independent trust layers."""

    def __init__(self, owner_id: str = "primary_owner") -> None:
        self.owner_id = owner_id
        self.audit = get_audit_logger()

    def evaluate(
        self,
        tool_name: str,
        arguments: Dict[str, Any],
        project_id: str = "default",
        task_id: Optional[str] = None,
        worker_role: Optional[str] = None,
        chat_id: Optional[str] = None,
        task_allowed_tools: Optional[List[str]] = None,
    ) -> PolicyEvaluationResult:
        now = time.time()
        level, risk, action_reason = classify_action(tool_name, arguments)

        # ── Layer 1: Owner Policy (Global destructiveness check) ────────
        if level == PermissionLevel.L8_DESTRUCTIVE:
            return self._record_and_return(
                decision=Decision.REQUIRES_APPROVAL,
                level=level,
                risk=RiskLevel.CRITICAL,
                reason=f"[Owner Policy] Action '{tool_name}' is L8 Destructive and requires explicit owner approval: {action_reason}",
                requires_approval=True,
                approval_scope="system_owner",
                now=now,
                tool_name=tool_name,
                arguments=arguments,
                task_id=task_id,
                chat_id=chat_id,
            )

        if level == PermissionLevel.L7_SYS_ADMIN:
            return self._record_and_return(
                decision=Decision.REQUIRES_APPROVAL,
                level=level,
                risk=RiskLevel.HIGH,
                reason=f"[Owner Policy] Action '{tool_name}' alters system packages/services: {action_reason}",
                requires_approval=True,
                approval_scope="admin",
                now=now,
                tool_name=tool_name,
                arguments=arguments,
                task_id=task_id,
                chat_id=chat_id,
            )

        # ── Layer 2: Hermes Policy (Global Agent Autonomy & System Safety) ──
        # Protects OS integrity, prevents touching raw root / OS critical dirs
        restricted_system_paths = [
            "/etc/shadow", "/etc/passwd", "/etc/sudoers", "/boot", "/proc", "/sys",
            "c:\\windows\\system32", "c:/windows/system32", "c:\\windows"
        ]
        target_path = str(arguments.get("path") or arguments.get("filepath") or arguments.get("target") or arguments.get("command") or "").lower()
        for rp in restricted_system_paths:
            if rp in target_path:
                return self._record_and_return(
                    decision=Decision.DENY,
                    level=level,
                    risk=RiskLevel.CRITICAL,
                    reason=f"[Hermes Policy] Operation touches protected system path '{rp}': access prohibited by agent safety boundary",
                    requires_approval=False,
                    approval_scope=None,
                    now=now,
                    tool_name=tool_name,
                    arguments=arguments,
                    task_id=task_id,
                    chat_id=chat_id,
                )

        # ── Layer 3: Project Policy (Workspace Boundary & Jail Enforcement) ─
        # Ensures operations with path arguments don't escape workspace with directory traversal
        raw_path = str(arguments.get("path") or arguments.get("filepath") or arguments.get("target") or "")
        if raw_path and ("../" in raw_path or "..\\" in raw_path):
            return self._record_and_return(
                decision=Decision.DENY,
                level=level,
                risk=RiskLevel.HIGH,
                reason=f"[Project Policy] Directory traversal attempt detected in path '{raw_path}' for project '{project_id}': path escape prohibited",
                requires_approval=False,
                approval_scope=None,
                now=now,
                tool_name=tool_name,
                arguments=arguments,
                task_id=task_id,
                chat_id=chat_id,
            )

        # ── Layer 4: Task Policy (TaskContract tool constraint) ─────────
        if task_allowed_tools is not None:
            if tool_name not in task_allowed_tools and tool_name not in ("list_directory", "read_file"):
                return self._record_and_return(
                    decision=Decision.DENY,
                    level=level,
                    risk=risk,
                    reason=f"[Task Policy] Tool '{tool_name}' is not allowed in TaskContract for task '{task_id}'",
                    requires_approval=False,
                    approval_scope=None,
                    now=now,
                    tool_name=tool_name,
                    arguments=arguments,
                    task_id=task_id,
                    chat_id=chat_id,
                )

        # ── Layer 5: Worker Policy (Specialist role constraints) ────────
        if worker_role:
            role = get_role(worker_role)
            if role and role.allowed_tools and tool_name not in role.allowed_tools:
                return self._record_and_return(
                    decision=Decision.DENY,
                    level=level,
                    risk=risk,
                    reason=f"[Worker Policy] Role '{worker_role}' is not authorized to execute tool '{tool_name}'",
                    requires_approval=False,
                    approval_scope=None,
                    now=now,
                    tool_name=tool_name,
                    arguments=arguments,
                    task_id=task_id,
                    chat_id=chat_id,
                )

        # ── Layer 6: Action passed all layers ──────────────────────────
        return self._record_and_return(
            decision=Decision.ALLOW,
            level=level,
            risk=risk,
            reason=f"Action permitted under policy hierarchy: {action_reason}",
            requires_approval=False,
            approval_scope=None,
            now=now,
            tool_name=tool_name,
            arguments=arguments,
            task_id=task_id,
            chat_id=chat_id,
        )

    def _record_and_return(
        self,
        decision: Decision,
        level: PermissionLevel,
        risk: RiskLevel,
        reason: str,
        requires_approval: bool,
        approval_scope: Optional[str],
        now: float,
        tool_name: str,
        arguments: Dict[str, Any],
        task_id: Optional[str],
        chat_id: Optional[str],
    ) -> PolicyEvaluationResult:
        res = PolicyEvaluationResult(
            decision=decision,
            level=level,
            risk=risk,
            reason=reason,
            requires_approval=requires_approval,
            approval_scope=approval_scope,
            evaluated_at=now,
        )
        status_label = "pending_approval" if requires_approval else ("denied" if decision == Decision.DENY else "allowed")
        self.audit.record(
            tool_name=tool_name,
            decision=decision.value,
            task_id=task_id,
            chat_id=chat_id,
            arguments_summary=arguments,
            status=status_label,
            details=reason,
        )
        return res


_TRUST_ENGINE: Optional[TrustHierarchyEngine] = None


def get_trust_engine() -> TrustHierarchyEngine:
    global _TRUST_ENGINE
    if _TRUST_ENGINE is None:
        _TRUST_ENGINE = TrustHierarchyEngine()
    return _TRUST_ENGINE
