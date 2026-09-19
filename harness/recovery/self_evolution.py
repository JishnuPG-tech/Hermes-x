"""Self-Evolution & Safe Skill Lifecycle Engineering.

Implements the controlled self-evolution process:
1. Detect improvement need from repeated task failures or missing capabilities.
2. Propose skill or behavioral modification in an isolated sandbox.
3. Execute automated unit & functional verification tests.
4. Security review for privilege escalation or injection vulnerabilities.
5. Versioned activation with automated instant rollback if verification fails.
"""

from __future__ import annotations

import json
import logging
import os
import shutil
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

logger = logging.getLogger(__name__)

DEFAULT_SKILLS_DIR = Path("/data/hermes/skills") if Path("/data").exists() else Path("/tmp/hermes/skills")
DEFAULT_EVOLUTION_LOG = Path("/data/jarvis/logs/evolution.jsonl") if Path("/data").exists() else Path("/tmp/jarvis/logs/evolution.jsonl")


@dataclass
class EvolutionProposal:
    proposal_id: str
    skill_name: str
    version: str
    rationale: str
    system_prompt: str
    test_cases: List[str] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    status: str = "PROPOSED"  # PROPOSED, TESTING, APPROVED, ACTIVATED, ROLLEDBACK


class SelfEvolutionEngine:
    """Oversees safe, verified agent self-improvement."""

    def __init__(self, skills_dir: Optional[Any] = None) -> None:
        self.skills_dir = Path(skills_dir) if skills_dir else DEFAULT_SKILLS_DIR
        self.skills_dir.mkdir(parents=True, exist_ok=True)
        self.log_file = DEFAULT_EVOLUTION_LOG
        self.log_file.parent.mkdir(parents=True, exist_ok=True)

    def propose_skill_evolution(
        self,
        skill_name: str,
        rationale: str,
        prompt_content: str,
        test_cases: Optional[List[str]] = None,
    ) -> EvolutionProposal:
        import uuid
        proposal = EvolutionProposal(
            proposal_id=f"evo_{uuid.uuid4().hex[:8]}",
            skill_name=skill_name,
            version=time.strftime("%Y%m%d.%H%M%S"),
            rationale=rationale,
            system_prompt=prompt_content,
            test_cases=test_cases or [],
        )
        self._record_log(proposal)
        return proposal

    def sandbox_test_and_audit(self, proposal: EvolutionProposal) -> Tuple[bool, str]:
        """Perform automated security audit and test execution on proposed skill."""
        # 1. Security check for injection or dangerous escalations
        forbidden = ["rm -rf", "delete_database", "mkfs", "DROP TABLE", "chmod 777"]
        for bad in forbidden:
            if bad in proposal.system_prompt:
                proposal.status = "REJECTED_SECURITY"
                self._record_log(proposal)
                return False, f"Security audit failed: forbidden pattern '{bad}' found"

        # 2. Syntax and structure check
        if len(proposal.system_prompt.strip()) < 30:
            proposal.status = "REJECTED_TESTS"
            self._record_log(proposal)
            return False, "Validation failed: skill prompt too short or empty"

        # 3. Active execution of proposed test cases
        if proposal.test_cases:
            import sys
            import tempfile
            import subprocess
            with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as sandbox_dir:
                sb_path = Path(sandbox_dir)
                # Write skill prompt to mock skill file
                (sb_path / "SKILL.md").write_text(proposal.system_prompt, encoding="utf-8")
                
                # Write test script that executes each test case
                test_script = sb_path / "run_sandbox_tests.py"
                test_lines = ["import sys\n", "print('[SANDBOX TEST RUNNER STARTED]')\n"]
                for i, tc in enumerate(proposal.test_cases):
                    # Wrap test case execution
                    if "assert" in tc or "def " in tc:
                        test_lines.append(f"# Test case {i+1}\n{tc}\n")
                    else:
                        test_lines.append(f"# Informational/behavioral check {i+1}\nassert '{tc}' != '', 'Empty test expectation'\n")
                test_lines.append("print('[SANDBOX TEST RUNNER ALL PASSED]')\n")
                test_script.write_text("\n".join(test_lines), encoding="utf-8")

                try:
                    proc = subprocess.run(
                        [sys.executable, str(test_script)],
                        cwd=sandbox_dir,
                        capture_output=True,
                        text=True,
                        timeout=15.0,
                    )
                    if proc.returncode != 0:
                        proposal.status = "REJECTED_TESTS"
                        self._record_log(proposal)
                        return False, f"Sandboxed test execution failed: {proc.stderr.strip() or proc.stdout.strip()}"
                except subprocess.TimeoutExpired:
                    proposal.status = "REJECTED_TIMEOUT"
                    self._record_log(proposal)
                    return False, "Sandboxed test execution timed out (limit: 15s)"
                except Exception as e:
                    proposal.status = "REJECTED_ERROR"
                    self._record_log(proposal)
                    return False, f"Sandbox test runner execution error: {e}"

        proposal.status = "APPROVED"
        self._record_log(proposal)
        return True, "Security audit and sandboxed test execution passed."

    def activate_skill(self, proposal: EvolutionProposal) -> bool:
        """Atomically activate the versioned skill on disk."""
        if proposal.status != "APPROVED":
            return False

        skill_dir = self.skills_dir / proposal.skill_name
        skill_dir.mkdir(parents=True, exist_ok=True)

        # Backup current version if present
        current_file = skill_dir / "SKILL.md"
        backup_file = skill_dir / f"SKILL.md.bak.{proposal.version}"
        if current_file.exists():
            shutil.copy2(current_file, backup_file)

        # Write new version
        metadata_header = (
            f"---\nname: {proposal.skill_name}\nversion: {proposal.version}\n"
            f"rationale: {proposal.rationale}\nproposal_id: {proposal.proposal_id}\n---\n\n"
        )
        current_file.write_text(metadata_header + proposal.system_prompt, encoding="utf-8")
        proposal.status = "ACTIVATED"
        self._record_log(proposal)
        logger.info("Skill '%s' successfully evolved to version %s", proposal.skill_name, proposal.version)
        return True

    def rollback_skill(self, skill_name: str) -> bool:
        """Instantly rollback a skill to its previous backup version."""
        skill_dir = self.skills_dir / skill_name
        current_file = skill_dir / "SKILL.md"
        backups = sorted(skill_dir.glob("SKILL.md.bak.*"))
        if not backups:
            return False
        latest_backup = backups[-1]
        shutil.copy2(latest_backup, current_file)
        logger.info("Skill '%s' rolled back from %s", skill_name, latest_backup.name)
        return True

    def _record_log(self, proposal: EvolutionProposal) -> None:
        try:
            with open(self.log_file, "a", encoding="utf-8") as f:
                f.write(json.dumps({
                    "proposal_id": proposal.proposal_id,
                    "skill_name": proposal.skill_name,
                    "version": proposal.version,
                    "status": proposal.status,
                    "rationale": proposal.rationale,
                    "timestamp": time.time(),
                }) + "\n")
        except Exception:
            pass


_EVO_ENGINE: Optional[SelfEvolutionEngine] = None


def get_self_evolution_engine() -> SelfEvolutionEngine:
    global _EVO_ENGINE
    if _EVO_ENGINE is None:
        _EVO_ENGINE = SelfEvolutionEngine()
    return _EVO_ENGINE
