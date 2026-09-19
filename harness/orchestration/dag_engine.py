"""
DAG Engine
Manages subtask dependencies, topological ordering, and ready node identification.
"""
from __future__ import annotations

from typing import List, Dict, Set, Optional
from harness.kernel.models import Subtask, TaskStatus


class DAGEngine:
    def __init__(self, subtasks: List[Subtask]):
        self.subtasks = {s.subtask_id: s for s in subtasks}

    def get_ready_subtasks(self) -> List[Subtask]:
        """Find subtasks that are CREATED or READY and whose dependencies are all COMPLETED."""
        ready: List[Subtask] = []
        for s in self.subtasks.values():
            if s.status in (TaskStatus.CREATED, TaskStatus.READY):
                deps_met = True
                for dep_id in s.dependencies:
                    dep = self.subtasks.get(dep_id)
                    if not dep or dep.status != TaskStatus.COMPLETED:
                        deps_met = False
                        break
                if deps_met:
                    ready.append(s)
        return ready

    def is_complete(self) -> bool:
        """Check if all subtasks are COMPLETED."""
        if not self.subtasks:
            return True
        return all(s.status == TaskStatus.COMPLETED for s in self.subtasks.values())

    def has_failures(self) -> bool:
        """Check if any subtask has permanently failed or cancelled."""
        return any(s.status in (TaskStatus.FAILED_FINAL, TaskStatus.CANCELLED) for s in self.subtasks.values())

    def validate_no_cycles(self) -> bool:
        """Check for cycles using DFS topological sort."""
        visited: Dict[str, int] = {}  # 0: unvisited, 1: visiting, 2: visited

        def dfs(node_id: str) -> bool:
            visited[node_id] = 1
            node = self.subtasks.get(node_id)
            if node:
                for dep_id in node.dependencies:
                    if dep_id not in self.subtasks:
                        continue
                    if visited.get(dep_id, 0) == 1:
                        return False  # cycle
                    if visited.get(dep_id, 0) == 0:
                        if not dfs(dep_id):
                            return False
            visited[node_id] = 2
            return True

        for node_id in self.subtasks:
            if visited.get(node_id, 0) == 0:
                if not dfs(node_id):
                    return False
        return True
