"""
Progress tracking and stall detection for Hermes autonomous execution.

This is deliberately model-agnostic. It detects repeated execution states and
lack of observable progress instead of trying to infer user intent with rules.
"""
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field
from typing import Any, Dict, List


@dataclass
class ProgressSnapshot:
    iteration: int
    signature: str
    progressed: bool
    reason: str


@dataclass
class ProgressTracker:
    stall_threshold: int = 3
    _previous_signature: str | None = None
    _stall_count: int = 0
    snapshots: List[ProgressSnapshot] = field(default_factory=list)

    @staticmethod
    def _compact(value: Any, limit: int = 1200) -> str:
        try:
            raw = json.dumps(value, sort_keys=True, ensure_ascii=False, default=str)
        except Exception:
            raw = str(value)
        return raw[:limit]

    def observe(
        self,
        iteration: int,
        tool_calls: List[Dict[str, Any]],
        tool_results: List[Any],
        assistant_text: str,
    ) -> ProgressSnapshot:
        state = {
            "tools": [
                {
                    "name": c.get("name"),
                    "arguments": c.get("arguments", {}),
                }
                for c in tool_calls
            ],
            "results": [
                {
                    "tool": getattr(r, "tool", None),
                    "success": getattr(r, "success", False),
                    "error": getattr(r, "error", None),
                    "result": self._compact(getattr(r, "result", None)),
                }
                for r in tool_results
            ],
            "text": self._compact(assistant_text),
        }
        signature = hashlib.sha256(
            self._compact(state).encode("utf-8")
        ).hexdigest()

        if self._previous_signature == signature:
            self._stall_count += 1
            progressed = False
            reason = f"execution state repeated ({self._stall_count} consecutive repeats)"
        else:
            self._stall_count = 0
            progressed = True
            reason = "observable execution state changed"

        self._previous_signature = signature
        snapshot = ProgressSnapshot(iteration, signature, progressed, reason)
        self.snapshots.append(snapshot)
        return snapshot

    @property
    def stalled(self) -> bool:
        return self._stall_count >= self.stall_threshold

    @property
    def stall_count(self) -> int:
        return self._stall_count

    def guidance(self) -> str:
        return (
            "[SYSTEM ALERT: NO EXECUTION PROGRESS] The execution state has repeated "
            f"{self._stall_count} consecutive times. Stop repeating the same approach. "
            "Inspect the latest result, choose a materially different authorized action, "
            "ask for clarification if a decision is genuinely blocking progress, or "
            "finish with the truthful current status."
        )
