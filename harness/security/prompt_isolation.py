"""Prompt Injection & External Untrusted Data Boundary.

Demarcates all external data (from websites, Notion, files, GitHub, Telegram)
as untrusted data so the LLM cannot be hijacked to execute unauthorized instructions.
"""

from __future__ import annotations

import re
from typing import Any, Dict

# Regex patterns commonly seen in prompt injection payloads
INJECTION_SIGNATURES = [
    re.compile(r"ignore\s+(all\s+)?(previous|prior)\s+instructions", re.IGNORECASE),
    re.compile(r"system\s*:\s*you\s+are\s+now", re.IGNORECASE),
    re.compile(r"you\s+are\s+no\s+longer\s+hermes", re.IGNORECASE),
    re.compile(r"override\s+(system|developer|safety)\s+policy", re.IGNORECASE),
    re.compile(r"<\|\s*im_start\s*\|>", re.IGNORECASE),
    re.compile(r"<\|\s*im_end\s*\|>", re.IGNORECASE),
]


def wrap_untrusted_content(content: str, source_type: str = "external_data", source_id: str = "") -> str:
    """Wraps external content into strict structural data tags and neutralizes command directives."""
    if not content:
        return content

    # Neutralize raw injection signatures
    sanitized = content
    for pattern in INJECTION_SIGNATURES:
        sanitized = pattern.sub("[filtered directive attempt]", sanitized)

    meta_attr = f' source="{source_type}"'
    if source_id:
        meta_attr += f' id="{source_id}"'

    return f"<untrusted_data{meta_attr}>\n{sanitized}\n</untrusted_data>"
