"""
Knowledge Policy Guard & Sandboxing
===================================
Enforces access boundaries, path sandboxing, write intent verification,
and prompt injection defense.
"""
from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Tuple, Optional, List, Dict, Any

from harness.knowledge.models import WriteIntent


class KnowledgePolicyError(Exception):
    pass


class KnowledgePolicy:
    def __init__(
        self,
        vault_root: Optional[Path] = None,
        allow_autonomous_writes: bool = False,
    ):
        raw_root = vault_root or os.getenv("OBSIDIAN_VAULT_DIR", "/data/obsidian/vault")
        self.vault_root = Path(raw_root).resolve()
        self.allow_autonomous_writes = (
            allow_autonomous_writes
            or os.getenv("KNOWLEDGE_AUTONOMOUS_WRITES", "false").lower() in ("1", "true", "yes")
        )
        self.read_allowed = True
        self.create_requires_approval = not self.allow_autonomous_writes
        self.update_requires_approval = not self.allow_autonomous_writes
        self.delete_allowed = False  # Deletions denied by default per spec

        self.allowed_extensions = {".md", ".markdown", ".canvas", ".json"}
        self.denied_subdirs = {".obsidian/plugins", "Secrets", "Private", ".git"}

    def validate_obsidian_path(self, relative_path: str) -> Path:
        """Validates that a path is strictly inside the vault root and prevents directory traversal."""
        clean = relative_path.replace("\\", "/").strip().lstrip("/")
        
        # 1. Reject path traversal sequences
        if ".." in clean.split("/"):
            raise KnowledgePolicyError(f"Directory traversal detected and blocked: {relative_path}")

        # 2. Check deny-listed folders
        for denied in self.denied_subdirs:
            if clean.startswith(denied) or f"/{denied}" in clean:
                raise KnowledgePolicyError(f"Access to protected folder '{denied}' is forbidden: {relative_path}")

        # 3. Resolve absolute path
        target = (self.vault_root / clean).resolve()

        # 4. Strict parent boundary check
        try:
            target.relative_to(self.vault_root)
        except ValueError:
            raise KnowledgePolicyError(f"Target path escapes vault boundary: {relative_path}")

        # 5. Check extension if writing a file
        if target.suffix and target.suffix.lower() not in self.allowed_extensions:
            raise KnowledgePolicyError(f"File extension '{target.suffix}' is not permitted in vault: {relative_path}")

        return target

    def check_write_intent(self, intent: WriteIntent, user_approved: bool = False) -> Tuple[bool, Optional[str]]:
        """Evaluates write policy. Returns (allowed, reason_or_challenge)."""
        # 1. Operation check
        if intent.operation == "delete":
            if not self.delete_allowed:
                return False, "Deletion is disabled by default knowledge policy"

        # 2. Check autonomous write vs human approval
        if not user_approved and not self.allow_autonomous_writes:
            return False, f"Operation '{intent.operation}' to {intent.destination} requires explicit user approval"

        # 3. Destination validation
        if intent.destination == "obsidian":
            try:
                dest_path = intent.parent_id or intent.title
                if not dest_path.endswith(".md"):
                    dest_path += ".md"
                self.validate_obsidian_path(dest_path)
            except KnowledgePolicyError as e:
                return False, str(e)

        # 4. Inspect content for raw prompt injection markers
        suspicious = [
            "system: ignore all previous instructions",
            "sudo rm -rf",
            "execute arbitrary code",
            "curl -x post",
        ]
        low = intent.content.lower()
        if any(p in low for p in suspicious):
            return False, "Content contains prohibited instruction injection patterns"

        return True, None

    @staticmethod
    def sanitize_untrusted_note(content: str) -> str:
        """Sanitizes content retrieved from notes so it is treated as data, not instruction."""
        # Strip system instruction markers
        sanitized = re.sub(r"(?i)<\s*system\s*>", "[SYSTEM_NOTE]:", content)
        sanitized = re.sub(r"(?i)<\s*/\s*system\s*>", "", sanitized)
        return sanitized
