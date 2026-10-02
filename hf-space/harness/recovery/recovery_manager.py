"""
Recovery Manager
Controls bounded retries, strategy pivoting, and recovery escalation.
"""
from __future__ import annotations

import asyncio
from typing import Dict, Any, List, Optional
from harness.kernel.models import Task, FailureClass


class RecoveryStrategy:
    RETRY_IMMEDIATE = "retry_immediate"
    BACKOFF_RETRY = "backoff_retry"
    INSTALL_DEPENDENCY = "install_dependency"
    CODE_REPAIR = "code_repair"
    PIVOT_STRATEGY = "pivot_strategy"
    ESCALATE_OWNER = "escalate_owner"


class RecoveryManager:
    def __init__(self, max_retries_per_failure: int = 2):
        self.max_retries_per_failure = max_retries_per_failure

    def determine_strategy(
        self,
        task: Task,
        failure_class: FailureClass,
        error_signature: str,
        recent_failures: List[Dict[str, Any]],
    ) -> tuple[str, str, int]:
        """Determine next recovery action based on failure class and past attempts.
        Returns (strategy_name, explanation, backoff_seconds).
        """
        # 1. Check if retry budget is exhausted
        if task.retry_count >= task.retry_budget:
            return (
                RecoveryStrategy.ESCALATE_OWNER,
                f"Retry budget exhausted ({task.retry_count}/{task.retry_budget} attempts). Escalating to owner.",
                0,
            )

        # 2. Check for identical repeating failure signatures (prevent loops)
        same_error_count = sum(
            1 for f in recent_failures if f.get("error_signature") == error_signature
        )

        if same_error_count >= self.max_retries_per_failure:
            return (
                RecoveryStrategy.PIVOT_STRATEGY,
                f"Identical failure occurred {same_error_count} times. Changing execution strategy or alternative tool.",
                1,
            )

        # 3. Strategy based on failure classification
        if failure_class == FailureClass.TRANSIENT:
            backoff = min(2 ** task.retry_count, 15)
            return (
                RecoveryStrategy.BACKOFF_RETRY,
                f"Transient network/service issue. Retrying with exponential backoff ({backoff}s).",
                backoff,
            )

        elif failure_class == FailureClass.DEPENDENCY:
            return (
                RecoveryStrategy.INSTALL_DEPENDENCY,
                "Missing dependency identified. Attempting dependency resolution before re-executing.",
                0,
            )

        elif failure_class == FailureClass.CODE_DEFECT:
            return (
                RecoveryStrategy.CODE_REPAIR,
                "Code or test defect detected. Instructing Hermes to analyze error trace and edit code.",
                0,
            )

        elif failure_class == FailureClass.PERMISSION:
            return (
                RecoveryStrategy.ESCALATE_OWNER,
                "Permission denied or access restriction encountered. Escalating for owner authorization.",
                0,
            )

        return (
            RecoveryStrategy.PIVOT_STRATEGY,
            "Unknown error pattern. Attempting alternate diagnostic and execution route.",
            1,
        )
