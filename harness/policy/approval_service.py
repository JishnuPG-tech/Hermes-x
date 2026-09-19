"""
Approval Service
Manages owner approval requests, expiration, decisions, and policy gating.
"""
from __future__ import annotations

import time
from typing import Dict, Any, Optional, List
from harness.kernel.models import ApprovalRequest, ApprovalStatus, PermissionLevel, RiskLevel
from harness.kernel.task_db import TaskDB


class ApprovalService:
    def __init__(self, db: TaskDB):
        self.db = db

    def request_approval(
        self,
        task_id: str,
        action: str,
        risk_level: RiskLevel,
        permission_level: PermissionLevel,
        reason: str,
        parameters: Dict[str, Any],
        requested_by: str = "agent",
        ttl_seconds: int = 86400,
    ) -> ApprovalRequest:
        """Create a new pending approval request in database."""
        req = ApprovalRequest(
            task_id=task_id,
            action=action,
            risk_level=risk_level,
            permission_level=permission_level,
            reason=reason,
            parameters=parameters,
            requested_by=requested_by,
            requested_at=time.time(),
            expires_at=time.time() + ttl_seconds,
            status=ApprovalStatus.PENDING,
        )
        self.db.create_approval(req)
        return req

    def get_approval(self, approval_id: str) -> Optional[ApprovalRequest]:
        return self.db.get_approval(approval_id)

    def list_pending(self) -> List[ApprovalRequest]:
        return self.db.list_pending_approvals()

    def approve(self, approval_id: str, approved_by: str = "owner", reason: Optional[str] = None) -> bool:
        return self.db.update_approval(
            approval_id=approval_id,
            status=ApprovalStatus.APPROVED,
            decision_by=approved_by,
            reason=reason or "Approved by owner",
        )

    def deny(self, approval_id: str, denied_by: str = "owner", reason: Optional[str] = None) -> bool:
        return self.db.update_approval(
            approval_id=approval_id,
            status=ApprovalStatus.DENIED,
            decision_by=denied_by,
            reason=reason or "Denied by owner",
        )

    def is_action_approved(self, task_id: str, action: str, parameters: Dict[str, Any]) -> bool:
        """Check if an approved request exists for this exact task action."""
        # Query approvals for task
        with self.db._get_conn() as conn:
            row = conn.execute("""
            SELECT * FROM approvals
            WHERE task_id = ? AND action = ? AND status = ? AND expires_at > ?
            ORDER BY decision_at DESC LIMIT 1;
            """, (task_id, action, ApprovalStatus.APPROVED.value, time.time())).fetchone()
            if not row:
                return False
            # Check parameter match or loose match
            return True
