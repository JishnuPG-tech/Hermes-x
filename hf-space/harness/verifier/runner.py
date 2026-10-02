"""
Verification Runner
Executes deterministic verification commands and produces verifiable evidence.
"""
from __future__ import annotations

import asyncio
import shlex
import time
from dataclasses import dataclass, asdict
from typing import Dict, Any, Optional, List
from harness.kernel.models import FailureClass
from harness.verifier.failure_taxonomy import classify_failure


@dataclass
class VerificationResult:
    passed: bool
    command: str
    exit_code: int
    stdout: str
    stderr: str
    duration_ms: int
    failure_class: Optional[str] = None
    failure_reason: Optional[str] = None
    timestamp: float = 0.0

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class VerificationRunner:
    def __init__(self, default_timeout: int = 60):
        self.default_timeout = default_timeout

    async def verify(self, command: str, cwd: Optional[str] = None, timeout: Optional[int] = None) -> VerificationResult:
        """Run a verification command and produce structured evidence."""
        start = time.time()
        timeout = timeout or self.default_timeout
        proc = None
        try:
            proc = await asyncio.create_subprocess_shell(
                command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=cwd,
            )
            stdout_bytes, stderr_bytes = await asyncio.wait_for(proc.communicate(), timeout=timeout)
            duration_ms = int((time.time() - start) * 1000)
            exit_code = proc.returncode or 0
            stdout = stdout_bytes.decode("utf-8", errors="replace").strip()
            stderr = stderr_bytes.decode("utf-8", errors="replace").strip()

            passed = (exit_code == 0)
            failure_cls = None
            failure_reason = None
            if not passed:
                f_class, f_reason = classify_failure(stderr or stdout, exit_code)
                failure_cls = f_class.value
                failure_reason = f_reason

            return VerificationResult(
                passed=passed,
                command=command,
                exit_code=exit_code,
                stdout=stdout[:4000],  # bounded evidence
                stderr=stderr[:4000],
                duration_ms=duration_ms,
                failure_class=failure_cls,
                failure_reason=failure_reason,
                timestamp=time.time(),
            )

        except asyncio.TimeoutError:
            if proc:
                try:
                    proc.kill()
                except Exception:
                    pass
            duration_ms = int((time.time() - start) * 1000)
            return VerificationResult(
                passed=False,
                command=command,
                exit_code=-1,
                stdout="",
                stderr=f"Verification timed out after {timeout} seconds.",
                duration_ms=duration_ms,
                failure_class=FailureClass.TRANSIENT.value,
                failure_reason="Execution timed out",
                timestamp=time.time(),
            )
        except Exception as e:
            duration_ms = int((time.time() - start) * 1000)
            return VerificationResult(
                passed=False,
                command=command,
                exit_code=-1,
                stdout="",
                stderr=f"Verification execution error: {str(e)}",
                duration_ms=duration_ms,
                failure_class=FailureClass.UNKNOWN.value,
                failure_reason=str(e),
                timestamp=time.time(),
            )
