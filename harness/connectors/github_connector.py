"""
GitHub Connector
Provides capability-based Git and GitHub operations without exposing tokens to models.
"""
from __future__ import annotations

import asyncio
import os
from typing import Dict, Any, Optional, Tuple


class GitHubConnector:
    def __init__(self, token_env: str = "GITHUB_TOKEN"):
        self.token_env = token_env

    async def _run_git(self, args: list[str], cwd: Optional[str] = None) -> Tuple[int, str, str]:
        proc = await asyncio.create_subprocess_exec(
            "git",
            *args,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
            cwd=cwd,
        )
        stdout, stderr = await proc.communicate()
        return (
            proc.returncode or 0,
            stdout.decode("utf-8", errors="replace").strip(),
            stderr.decode("utf-8", errors="replace").strip(),
        )

    async def get_status(self, repo_dir: str) -> Dict[str, Any]:
        """Get git status of a repository."""
        code, out, err = await self._run_git(["status", "--porcelain"], cwd=repo_dir)
        branch_code, branch_out, _ = await self._run_git(["rev-parse", "--abbrev-ref", "HEAD"], cwd=repo_dir)
        return {
            "is_clean": (code == 0 and len(out) == 0),
            "current_branch": branch_out if branch_code == 0 else "unknown",
            "modified_files": out.splitlines() if out else [],
        }

    async def create_branch(self, repo_dir: str, branch_name: str) -> Tuple[bool, str]:
        """Create and switch to a new branch."""
        code, out, err = await self._run_git(["checkout", "-b", branch_name], cwd=repo_dir)
        return (code == 0, out if code == 0 else err)

    async def commit_changes(self, repo_dir: str, message: str) -> Tuple[bool, str]:
        """Add all files and commit with a structured message."""
        add_code, _, add_err = await self._run_git(["add", "."], cwd=repo_dir)
        if add_code != 0:
            return False, f"Git add failed: {add_err}"
        commit_code, commit_out, commit_err = await self._run_git(["commit", "-m", message], cwd=repo_dir)
        return (commit_code == 0, commit_out if commit_code == 0 else commit_err)
