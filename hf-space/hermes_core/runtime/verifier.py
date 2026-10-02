"""
Hermes Verification Gate
Prevents false completion claims. Ensures claimed state transitions and actions
are backed by empirical evidence before final completion claims.
"""
from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any, Dict, List, Optional
from hermes_core.runtime.models import ToolResult, VerificationResult

logger = logging.getLogger("hermes.runtime.verifier")


class VerificationGate:
    """Verifies empirical completion evidence before allowing task completion."""

    @staticmethod
    def verify_tool_result(result: ToolResult) -> VerificationResult:
        """Verifies if a tool execution truly produced valid evidence."""
        if not result.success:
            return VerificationResult(
                passed=False,
                evidence={"error": result.error},
                reason=f"Tool '{result.tool}' reported an error: {result.error.get('message') if result.error else 'Unknown'}",
            )

        tool_name = result.tool
        output = result.result

        # 1. File creation / modification verification
        if tool_name in ("write_file", "edit_file"):
            target_path = result.metadata.get("path")
            if target_path:
                p = Path(target_path).expanduser().resolve()
                if not p.exists():
                    return VerificationResult(
                        passed=False,
                        evidence={"path": str(p), "exists": False},
                        reason=f"Target file '{p}' does not exist on disk.",
                    )
                if p.stat().st_size == 0 and result.metadata.get("expected_non_empty", True):
                    return VerificationResult(
                        passed=False,
                        evidence={"path": str(p), "size_bytes": 0},
                        reason=f"Target file '{p}' was written but is empty (0 bytes).",
                    )
                return VerificationResult(
                    passed=True,
                    evidence={"path": str(p), "size_bytes": p.stat().st_size, "exists": True},
                    reason=f"File '{p}' verified successfully on disk ({p.stat().st_size} bytes).",
                )

        # 2. Bash / Terminal commands
        if tool_name in ("bash_exec", "bash", "computer_run_command"):
            if isinstance(output, str) and ("[ERROR" in output or "Command failed with exit code" in output):
                return VerificationResult(
                    passed=False,
                    evidence={"raw_output": output[:300]},
                    reason="Command exited with failure.",
                )
            return VerificationResult(
                passed=True,
                evidence={"output_preview": str(output)[:200]},
                reason="Command completed execution successfully.",
            )

        # 3. Knowledge / Memory records
        if tool_name in ("search_knowledge", "notion_search", "memory_recall"):
            count = len(output) if isinstance(output, list) else (1 if output else 0)
            return VerificationResult(
                passed=True,
                evidence={"retrieved_count": count},
                reason=f"Retrieved {count} knowledge items.",
            )

        return VerificationResult(
            passed=True,
            evidence={"tool": tool_name},
            reason="Generic tool execution verified.",
        )

    @staticmethod
    def audit_final_claim(
        assistant_text: str,
        tool_results: List[ToolResult]
    ) -> tuple[bool, Optional[str]]:
        """
        Ensures that if the assistant text claims completion of file writes, commands,
        or fixes, matching verified tool execution evidence exists in this turn.
        """
        claim_phrases = ["i have created the file", "i wrote the file", "i deleted the file", "the test passed", "i fixed the bug"]
        text_lower = assistant_text.lower()

        claims_action = any(cp in text_lower for cp in claim_phrases)
        if claims_action and not tool_results:
            return False, "Agent claimed an action occurred, but no tool was executed in this turn."

        failed_tools = [tr for tr in tool_results if not tr.success]
        if failed_tools and any(term in text_lower for term in ["successfully finished", "all done", "completely fixed"]):
            return False, f"Agent claimed complete success, but tools failed during execution: {[ft.tool for ft in failed_tools]}"

        return True, None

    async def verify_action_completion(
        self,
        objective: str,
        expected_files: Optional[List[str]] = None,
        last_exit_code: Optional[int] = None,
    ) -> VerificationResult:
        """Deterministically audits empirical action outcomes against completion requirements."""
        if last_exit_code is not None and last_exit_code != 0:
            return VerificationResult(
                passed=False,
                evidence={"exit_code": last_exit_code},
                reason=f"Command exited with non-zero code {last_exit_code}.",
            )
        if expected_files:
            missing = [f for f in expected_files if not os.path.exists(f)]
            if missing:
                return VerificationResult(
                    passed=False,
                    evidence={"missing_files": [f"Missing expected file: {f}" for f in missing]},
                    reason=f"Required files missing from disk: {missing}",
                )
        return VerificationResult(
            passed=True,
            evidence={"objective": objective, "verified_at": os.name},
            reason="All empirical verification checks passed.",
        )


verification_gate = VerificationGate()
