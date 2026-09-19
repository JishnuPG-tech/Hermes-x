"""
Permission Levels & Tool Action Classification
Maps tools and actions to strict hierarchical permission levels (L0-L8).
"""
from __future__ import annotations

import re
from typing import Dict, Any, Optional
from harness.kernel.models import PermissionLevel, RiskLevel


# Tool classification table
TOOL_PERMISSION_MAP: Dict[str, PermissionLevel] = {
    "list_directory": PermissionLevel.L1_PROJECT_READ,
    "read_file": PermissionLevel.L1_PROJECT_READ,
    "search_files": PermissionLevel.L1_PROJECT_READ,
    "write_file": PermissionLevel.L2_PROJECT_EDIT,
    "edit_file": PermissionLevel.L2_PROJECT_EDIT,
    "append_file": PermissionLevel.L2_PROJECT_EDIT,
    "bash": PermissionLevel.L3_TERMINAL_EXEC,
    "bash_exec": PermissionLevel.L3_TERMINAL_EXEC,
    "http_request": PermissionLevel.L4_NETWORK_API,
    "github_create_branch": PermissionLevel.L5_GITHUB_WRITE,
    "github_commit": PermissionLevel.L5_GITHUB_WRITE,
    "github_create_pr": PermissionLevel.L5_GITHUB_WRITE,
    "deploy_service": PermissionLevel.L6_DEPLOY,
    "system_service_control": PermissionLevel.L7_SYS_ADMIN,
    "delete_file": PermissionLevel.L8_DESTRUCTIVE,
    "delete_database": PermissionLevel.L8_DESTRUCTIVE,
    "git_force_push": PermissionLevel.L8_DESTRUCTIVE,
}

# Dangerous bash command regex patterns that elevate permission to L8_DESTRUCTIVE or L7_SYS_ADMIN
DESTRUCTIVE_PATTERNS = [
    re.compile(r"\brm\s+(-[rfRF]+\s+|--recursive\s+).*", re.IGNORECASE),
    re.compile(r"\b(mkfs|dd|fdisk|parted)\b", re.IGNORECASE),
    re.compile(r"\bdrop\s+(database|table)\b", re.IGNORECASE),
    re.compile(r"\bgit\s+push\s+.*--force\b", re.IGNORECASE),
    re.compile(r"\bgit\s+reset\s+--hard\b", re.IGNORECASE),
]

SYS_ADMIN_PATTERNS = [
    re.compile(r"\b(apt|apt-get|yum|apk)\s+(install|remove|purge)\b", re.IGNORECASE),
    re.compile(r"\b(systemctl|service|supervisorctl)\b", re.IGNORECASE),
    re.compile(r"\b(chmod|chown)\s+(-R\s+)?777\b", re.IGNORECASE),
    re.compile(r"\b(useradd|userdel|passwd)\b", re.IGNORECASE),
]


def classify_action(tool_name: str, arguments: Dict[str, Any]) -> tuple[PermissionLevel, RiskLevel, str]:
    """Classify a tool action and inspect arguments for dynamic permission elevation."""
    base_level = TOOL_PERMISSION_MAP.get(tool_name, PermissionLevel.L3_TERMINAL_EXEC)
    risk_level = RiskLevel.LOW
    reason = f"Standard action for tool {tool_name}"

    if base_level in (PermissionLevel.L0_OBSERVE, PermissionLevel.L1_PROJECT_READ):
        risk_level = RiskLevel.LOW
    elif base_level == PermissionLevel.L2_PROJECT_EDIT:
        risk_level = RiskLevel.MEDIUM
    elif base_level in (PermissionLevel.L3_TERMINAL_EXEC, PermissionLevel.L4_NETWORK_API):
        risk_level = RiskLevel.MEDIUM

    # Deep inspection for bash commands
    if tool_name in ("bash_exec", "bash"):
        cmd = arguments.get("command", "")
        for pattern in DESTRUCTIVE_PATTERNS:
            if pattern.search(cmd):
                return PermissionLevel.L8_DESTRUCTIVE, RiskLevel.CRITICAL, f"Command contains potentially destructive operation: '{cmd[:60]}...'"
        for pattern in SYS_ADMIN_PATTERNS:
            if pattern.search(cmd):
                return PermissionLevel.L7_SYS_ADMIN, RiskLevel.HIGH, f"Command requires system administration privileges: '{cmd[:60]}...'"

    # Inspection for file deletion
    if tool_name == "delete_file":
        return PermissionLevel.L8_DESTRUCTIVE, RiskLevel.HIGH, f"Destructive file deletion: {arguments.get('file_path')}"

    # Inspection for git writes
    if "github" in tool_name or "git" in tool_name:
        risk_level = RiskLevel.HIGH

    return base_level, risk_level, reason


def requires_approval(level: PermissionLevel, max_autonomous_level: PermissionLevel = PermissionLevel.L4_NETWORK_API) -> bool:
    """Determine if a permission level requires human owner approval before execution."""
    level_order = [
        PermissionLevel.L0_OBSERVE,
        PermissionLevel.L1_PROJECT_READ,
        PermissionLevel.L2_PROJECT_EDIT,
        PermissionLevel.L3_TERMINAL_EXEC,
        PermissionLevel.L4_NETWORK_API,
        PermissionLevel.L5_GITHUB_WRITE,
        PermissionLevel.L6_DEPLOY,
        PermissionLevel.L7_SYS_ADMIN,
        PermissionLevel.L8_DESTRUCTIVE,
    ]
    current_idx = level_order.index(level)
    max_idx = level_order.index(max_autonomous_level)
    return current_idx > max_idx
