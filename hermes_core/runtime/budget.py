"""
Adaptive execution budget for Hermes autonomous agent runs.

The budget is a safety boundary, not a completion condition. A task completes
when the agent reaches a valid stopping condition. Productive work may extend
the soft budget up to the hard ceiling, while stalled work is stopped early.
"""
from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ExecutionBudget:
    initial_iterations: int = 12
    hard_ceiling: int = 48
    warning_ratio: float = 0.70
    wrapup_ratio: float = 0.85
    force_synthesis_ratio: float = 0.95

    def __post_init__(self) -> None:
        self.initial_iterations = max(4, min(self.initial_iterations, self.hard_ceiling))
        self.hard_ceiling = max(self.initial_iterations, min(self.hard_ceiling, 64))

    @classmethod
    def for_request(cls, messages, requested: int | None = None) -> "ExecutionBudget":
        if requested is not None:
            requested = max(4, min(int(requested), 64))
            return cls(initial_iterations=requested, hard_ceiling=max(requested, 48))

        user_chars = sum(
            len(str(m.get("content", "")))
            for m in messages
            if m.get("role") == "user"
        )
        message_count = len(messages)
        if user_chars <= 400 and message_count <= 4:
            initial = 8
        elif user_chars <= 1600 and message_count <= 10:
            initial = 12
        elif user_chars <= 6000:
            initial = 20
        else:
            initial = 28
        return cls(initial_iterations=initial, hard_ceiling=min(64, max(32, initial * 2)))

    @property
    def warning_at(self) -> int:
        return max(1, int(self.initial_iterations * self.warning_ratio))

    @property
    def wrapup_at(self) -> int:
        return max(self.warning_at + 1, int(self.initial_iterations * self.wrapup_ratio))

    @property
    def force_synthesis_at(self) -> int:
        return max(self.wrapup_at + 1, int(self.initial_iterations * self.force_synthesis_ratio))

    def can_extend(self, current_iteration: int, has_progress: bool) -> bool:
        return has_progress and current_iteration < self.hard_ceiling

    def limit_reached(self, current_iteration: int) -> bool:
        return current_iteration >= self.hard_ceiling
