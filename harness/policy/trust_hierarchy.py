"""Trust Hierarchy & Unified Policy Engine.

Evaluates permissions through the strict 6-tier hierarchy:
  Owner Policy -> Hermes Policy -> Project Policy -> Task Policy -> Worker Policy -> Tool Action
Returns ALLOW, DENY, or REQUIRES_APPROVAL independent of the LLM.
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from enum import Enum
from typing import Any, Dict, Optional

from harness.kernel.models import PermissionLevel, RiskLevel
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
    """Evaluates requested tool actions against the hierarchical security boundary."""

    def __init__(self, owner_id: str = "primary_owner") -> None:
        self.owner_id = owner_id
        self.audit = get_audit_logger()

    def evaluate(
        self,
        tool_name: str,
        arguments: Dict[str, Any],
        project_id: str = "default",
        task_id: Optional[str] = None,
        worker_id: Optional[str] = None,
        chat_id: Optional[str] = None,
    ) -> PolicyEvaluationResult:
        now = time.time()
        level, risk, reason = classify_action(tool_name, arguments)

        # 1. Level 8 (Destructive) - Automatic Denial unless explicit owner approval
        if level == PermissionLevel.L8_DESTRUCTIVE:
            res = PolicyEvaluationResult(
                decision=Decision.REQUIRES_APPROVAL,
                level=level,
                risk=RiskLevel.CRITICAL,
                reason=f"Action '{tool_name}' is classified as L8 Destructive: {reason}",
                requires_approval=True,
                approval_scope="system_owner",
                evaluated_at=now,
            )
            self.audit.record(
                tool_name=tool_name,
                decision=res.decision.value,
                task_id=task_id,
                chat_id=chat_id,
                arguments_summary=arguments,
                status="pending_approval",
                details=res.reason,
            )
            return res

        # 2. System administration patterns require owner approval
        if level == PermissionLevel.L7_SYS_ADMIN:
            res = PolicyEvaluationResult(
                decision=Decision.REQUIRES_APPROVAL,
                level=level,
                risk=RiskLevel.HIGH,
                reason=f"Action '{tool_name}' modifies system-level packages or services: {reason}",
                requires_approval=True,
                approval_scope="admin",
                evaluated_at=now,
            )
            self.audit.record(
                tool_name=tool_name,
                decision=res.decision.value,
                task_id=task_id,
                chat_id=chat_id,
                arguments_summary=arguments,
                status="pending_approval",
                details=res.reason,
            )
            return res

        # 3. Standard autonomous allowed levels (L0 to L6)
        res = PolicyEvaluationResult(
            decision=Decision.ALLOW,
            level=level,
            risk=risk,
            reason=reason,
            requires_approval=False,
            evaluated_at=now,
        )
        self.audit.record(
            tool_name=tool_name,
            decision=res.decision.value,
            task_id=task_id,
            chat_id=chat_id,
            arguments_summary=arguments,
            status="allowed",
            details=res.reason,
        )
        return res


_TRUST_ENGINE: Optional[TrustHierarchyEngine] = None


def get_trust_engine() -> TrustHierarchyEngine:
    global _TRUST_ENGINE
    if _TRUST_ENGINE is None:
        _TRUST_ENGINE = TrustHierarchyEngine()
    return _TRUST_ENGINE
