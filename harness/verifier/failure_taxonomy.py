"""
Failure Taxonomy & Error Classifier
Classifies tool and command failures into actionable categories.
"""
from __future__ import annotations

import re
from typing import Tuple
from harness.kernel.models import FailureClass


TRANSIENT_PATTERNS = [
    re.compile(r"(connection\s+(reset|refused|timed\s*out)|timeout|502\s+bad\s+gateway|503\s+service\s+unavailable|rate\s+limit|429)", re.IGNORECASE),
    re.compile(r"(network\s+is\s+unreachable|temporary\s+failure\s+in\s+name\s+resolution)", re.IGNORECASE),
]

DEPENDENCY_PATTERNS = [
    re.compile(r"(ModuleNotFoundError|No\s+module\s+named|command\s+not\s+found|package\s+.*not\s+found|Cannot\s+find\s+module)", re.IGNORECASE),
    re.compile(r"(ImportError|pip\s+install|npm\s+install)", re.IGNORECASE),
]

PERMISSION_PATTERNS = [
    re.compile(r"(PermissionError|Permission\s+denied|Access\s+is\s+denied|EACCES|Operation\s+not\s+permitted)", re.IGNORECASE),
    re.compile(r"(Authentication\s+failed|Unauthorized|401|403\s+Forbidden)", re.IGNORECASE),
]

CODE_DEFECT_PATTERNS = [
    re.compile(r"(SyntaxError|IndentationError|TypeError|ValueError|IndexError|KeyError|AttributeError|AssertionError|NameError)", re.IGNORECASE),
    re.compile(r"(FAILED\s+\(failures=\d+\)|FAILURES|tests\s+failed)", re.IGNORECASE),
]


def classify_failure(error_text: str, exit_code: int = 1) -> Tuple[FailureClass, str]:
    """Classify an error string or stack trace into a FailureClass."""
    if not error_text:
        return FailureClass.UNKNOWN, "Command returned non-zero exit status without error output."

    for pattern in TRANSIENT_PATTERNS:
        if pattern.search(error_text):
            return FailureClass.TRANSIENT, "Network or transient service error detected. Backoff and retry recommended."

    for pattern in DEPENDENCY_PATTERNS:
        if pattern.search(error_text):
            return FailureClass.DEPENDENCY, "Missing module, executable, or dependency detected."

    for pattern in PERMISSION_PATTERNS:
        if pattern.search(error_text):
            return FailureClass.PERMISSION, "Permission or access restriction encountered."

    for pattern in CODE_DEFECT_PATTERNS:
        if pattern.search(error_text):
            return FailureClass.CODE_DEFECT, "Code defect, test failure, or syntax error detected in workspace."

    return FailureClass.UNKNOWN, "Unclassified execution error."
