"""
Server Computer Verification Engine
===================================
Enforces the core Agent OS rule:
"No task may be marked complete merely because the model says it is complete.
Completion requires the task's verification contract to pass."
"""
from __future__ import annotations

import asyncio
import os
import time
from pathlib import Path
from typing import Dict, Any, List, Optional

from harness.computer.models import VerificationContract, VerificationResult


class VerificationEngine:
    def __init__(self, default_timeout_seconds: int = 180):
        self.default_timeout = default_timeout_seconds

    async def verify_workspace(
        self,
        workspace_path: str,
        contract: VerificationContract,
    ) -> VerificationResult:
        """Executes the verification contract commands inside the workspace."""
        ws = Path(workspace_path)
        if not ws.exists():
            return VerificationResult(
                passed=False,
                summary=f"Workspace path '{workspace_path}' does not exist on disk",
                verified_at=time.time(),
            )

        cmd_results: List[Dict[str, Any]] = []
        all_passed = True
        timeout = contract.timeout_seconds or self.default_timeout

        # 1. Execute verification commands
        for cmd in contract.commands:
            start_t = time.time()
            try:
                proc = await asyncio.wait_for(
                    asyncio.create_subprocess_shell(
                        cmd,
                        cwd=str(ws),
                        stdout=asyncio.subprocess.PIPE,
                        stderr=asyncio.subprocess.PIPE,
                    ),
                    timeout=timeout,
                )
                stdout_b, stderr_b = await proc.communicate()
                duration = time.time() - start_t
                exit_code = proc.returncode

                stdout_str = stdout_b.decode("utf-8", errors="replace")[-2000:]
                stderr_str = stderr_b.decode("utf-8", errors="replace")[-2000:]

                passed = (exit_code == 0)
                if not passed:
                    all_passed = False

                cmd_results.append({
                    "command": cmd,
                    "exit_code": exit_code,
                    "passed": passed,
                    "duration_seconds": round(duration, 2),
                    "stdout_tail": stdout_str,
                    "stderr_tail": stderr_str,
                })
            except asyncio.TimeoutError:
                all_passed = False
                cmd_results.append({
                    "command": cmd,
                    "exit_code": -1,
                    "passed": False,
                    "error": f"Command timed out after {timeout} seconds",
                })
            except Exception as e:
                all_passed = False
                cmd_results.append({
                    "command": cmd,
                    "exit_code": -1,
                    "passed": False,
                    "error": str(e),
                })

        # 2. Check Git status if required
        git_clean = None
        if contract.require_clean_git and (ws / ".git").exists():
            try:
                proc = await asyncio.create_subprocess_shell(
                    "git status --porcelain",
                    cwd=str(ws),
                    stdout=asyncio.subprocess.PIPE,
                    stderr=asyncio.subprocess.PIPE,
                )
                out, _ = await proc.communicate()
                git_clean = (len(out.strip()) == 0)
                if not git_clean:
                    all_passed = False
            except Exception:
                git_clean = False
                all_passed = False

        status_text = "PASSED" if all_passed else "FAILED"
        summary = f"Verification {status_text}: {len(cmd_results)} commands executed"
        if contract.require_clean_git:
            summary += f" | Git clean: {git_clean}"

        return VerificationResult(
            passed=all_passed,
            summary=summary,
            command_results=cmd_results,
            git_clean=git_clean,
            verified_at=time.time(),
        )
