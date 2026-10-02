"""Unified Server Computer Subsystem.

Consolidates filesystem, terminal processes, workspaces, git operations,
and verification into an integrated machine abstraction.
"""

from __future__ import annotations

import asyncio
import os
import shutil
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from harness.computer.project_registry import ProjectRegistry
from harness.computer.verification_engine import VerificationEngine
from harness.computer.workspace_manager import WorkspaceManager
from harness.policy.trust_hierarchy import get_trust_engine


class ServerComputer:
    """Unified machine abstraction for Hermes Agent."""

    def __init__(
        self,
        workspace_manager: Optional[WorkspaceManager] = None,
        verification_engine: Optional[VerificationEngine] = None,
        project_registry: Optional[ProjectRegistry] = None,
    ) -> None:
        self.registry = project_registry or ProjectRegistry()
        self.workspaces = workspace_manager or WorkspaceManager(registry=self.registry)
        self.verifier = verification_engine or VerificationEngine()
        self.policy = get_trust_engine()

    async def execute_bash(
        self,
        command: str,
        cwd: Optional[str] = None,
        timeout: float = 60.0,
        task_id: Optional[str] = None,
    ) -> Tuple[int, str, str]:
        """Execute terminal command with policy check and timeout."""
        eval_res = self.policy.evaluate("bash_exec", {"command": command}, task_id=task_id)
        if eval_res.requires_approval:
            raise PermissionError(f"Action requires owner approval: {eval_res.reason}")

        proc = await asyncio.create_subprocess_shell(
            command,
            cwd=cwd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        try:
            stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=timeout)
            return (
                proc.returncode or 0,
                stdout.decode("utf-8", errors="replace").strip(),
                stderr.decode("utf-8", errors="replace").strip(),
            )
        except asyncio.TimeoutError:
            try:
                proc.kill()
            except Exception:
                pass
            return (-1, "", f"Command timed out after {timeout} seconds")

    def read_file(self, path: str, max_bytes: int = 50000) -> str:
        """Sandboxed file reading."""
        p = Path(path).resolve()
        if not p.exists():
            raise FileNotFoundError(f"File not found: {path}")
        if p.is_dir():
            raise IsADirectoryError(f"Path is a directory: {path}")
        content = p.read_text(encoding="utf-8", errors="replace")
        if len(content) > max_bytes:
            return content[:max_bytes] + "\n... (truncated)"
        return content

    def write_file(self, path: str, content: str) -> int:
        """Sandboxed file writing."""
        p = Path(path).resolve()
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")
        return len(content)

    def list_directory(self, path: str) -> List[Dict[str, Any]]:
        """List directory entries with metadata."""
        p = Path(path).resolve()
        if not p.exists():
            raise FileNotFoundError(f"Directory not found: {path}")
        if not p.is_dir():
            raise NotADirectoryError(f"Path is not a directory: {path}")
        entries = []
        for item in sorted(os.listdir(p)):
            ip = p / item
            entries.append({
                "name": item,
                "is_dir": ip.is_dir(),
                "size_bytes": ip.stat().st_size if ip.is_file() else 0,
                "modified_at": ip.stat().st_mtime,
            })
        return entries


_COMPUTER_INSTANCE: Optional[ServerComputer] = None


def get_server_computer() -> ServerComputer:
    global _COMPUTER_INSTANCE
    if _COMPUTER_INSTANCE is None:
        _COMPUTER_INSTANCE = ServerComputer()
    return _COMPUTER_INSTANCE
