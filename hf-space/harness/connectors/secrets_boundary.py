"""
Secrets Boundary & Secret Redactor
Protects credentials by reference and redacts secrets from logs, events, and LLM prompts.
"""
from __future__ import annotations

import os
import re
from typing import Dict, Optional


SECRET_PATTERNS = [
    re.compile(r"hf_[a-zA-Z0-9]{34,}", re.IGNORECASE),
    re.compile(r"ghp_[a-zA-Z0-9]{36,}", re.IGNORECASE),
    re.compile(r"github_pat_[a-zA-Z0-9_]{60,}", re.IGNORECASE),
    re.compile(r"sk-[a-zA-Z0-9]{20,}", re.IGNORECASE),
    re.compile(r"Bearer\s+[a-zA-Z0-9\-_]{20,}", re.IGNORECASE),
    re.compile(r"token\s*=\s*['\"][^'\"]+['\"]", re.IGNORECASE),
]


class SecretsBoundary:
    def __init__(self):
        # In-memory mapping of opaque reference -> secret value
        self._secrets: Dict[str, str] = {
            "upstream_omniroute_key": os.getenv("UPSTREAM_API_KEY", "Jishnu2005"),
            "hf_token": os.getenv("HF_TOKEN", ""),
            "github_token": os.getenv("GITHUB_TOKEN", ""),
        }

    def register_secret(self, ref_id: str, value: str) -> None:
        self._secrets[ref_id] = value

    def get_secret(self, ref_id: str) -> Optional[str]:
        return self._secrets.get(ref_id)

    def redact(self, text: str) -> str:
        """Redact known secret values and regex secret patterns from text."""
        if not text:
            return text

        # 1. Redact registered values
        for val in self._secrets.values():
            if val and len(val) >= 6:
                text = text.replace(val, "[REDACTED_SECRET]")

        # 2. Redact regex patterns
        for pattern in SECRET_PATTERNS:
            text = pattern.sub("[REDACTED_TOKEN]", text)

        return text
