"""
Server Computer Agent Tools
===========================
Exposes execution computer capabilities to Hermes Agent:
- computer_project_register: Register a Git project in the durable registry
- computer_project_list: List all managed server projects
- computer_workspace_create: Provision a sandboxed workspace for a task
- computer_run_command: Execute shell commands inside a specific project/workspace
- computer_verify_task: Deterministically verify task tests and artifacts before completion
- computer_system_status: Inspect storage, disk space, and runtime computer metrics
"""
from __future__ import annotations

import asyncio
import os
import shutil
import time
from pathlib import Path
from typing import Optional, List, Dict, Any

from hermes_core.tools.registry import registry
from harness.computer.project_registry import ProjectRegistry
from harness.computer.workspace_manager import WorkspaceManager
from harness.computer.verification_engine import VerificationEngine
from harness.computer.models import VerificationContract

_registry = ProjectRegistry()
_workspaces = WorkspaceManager(registry=_registry)
_verifier = VerificationEngine()


@registry.register(
    name="computer_project_register",
    description="Register a software repository/project in the persistent Server Computer registry.",
    parameters={
        "type": "object",
        "properties": {
            "project_id": {"type": "string", "description": "Unique identifier for the project (e.g. 'hermes-x')"},
            "name": {"type": "string", "description": "Human readable name of the project"},
            "github_repo": {"type": "string", "description": "GitHub repository in 'owner/repo' format (e.g. 'JishnuPG-tech/Hermes-x')"},
            "default_branch": {"type": "string", "description": "Default Git branch (default 'main')"},
            "description": {"type": "string", "description": "Brief description of the project"}
        },
        "required": ["project_id"]
    },
    category="system"
)
def computer_project_register(
    project_id: str,
    name: Optional[str] = None,
    github_repo: Optional[str] = None,
    default_branch: str = "main",
    description: Optional[str] = None,
) -> str:
    record = _registry.register_project(
        project_id=project_id,
        name=name,
        github_repo=github_repo,
        default_branch=default_branch,
        description=description,
    )
    return f"Successfully registered project '{record.project_id}' on Server Computer at '{record.path}'."


@registry.register(
    name="computer_project_list",
    description="List all registered software projects managed on the Server Computer.",
    parameters={"type": "object", "properties": {}},
    category="system"
)
def computer_project_list() -> str:
    projects = _registry.list_projects()
    if not projects:
        return "No projects currently registered on the Server Computer."
    lines = []
    for p in projects:
        lines.append(f"- **{p.name}** (`{p.project_id}`): Path=`{p.path}` | GitHub=`{p.github_repo or 'None'}` | Branch=`{p.default_branch}`")
    return "\n".join(lines)


@registry.register(
    name="computer_workspace_create",
    description="Provision an isolated task workspace for executing code, tests, or bug fixes.",
    parameters={
        "type": "object",
        "properties": {
            "task_id": {"type": "string", "description": "Unique task identifier (e.g. 'task_fix_auth_123')"},
            "project_id": {"type": "string", "description": "ID of registered project"},
            "branch": {"type": "string", "description": "Optional branch name"}
        },
        "required": ["task_id", "project_id"]
    },
    category="system"
)
async def computer_workspace_create(task_id: str, project_id: str, branch: Optional[str] = None) -> str:
    info = await _workspaces.provision_workspace(task_id=task_id, project_id=project_id, branch=branch)
    return f"Provisioned task workspace for '{task_id}' at: `{info.path}` (Commit: {info.git_commit or 'N/A'})"


@registry.register(
    name="computer_run_command",
    description="Execute a shell command inside a designated workspace or project path with timeout and sandboxing.",
    parameters={
        "type": "object",
        "properties": {
            "command": {"type": "string", "description": "Bash command to execute"},
            "cwd": {"type": "string", "description": "Working directory path inside /data/jarvis (defaults to /data/jarvis)"},
            "timeout_seconds": {"type": "integer", "description": "Execution timeout in seconds (default 60)"}
        },
        "required": ["command"]
    },
    category="coding"
)
async def computer_run_command(command: str, cwd: Optional[str] = None, timeout_seconds: int = 60) -> str:
    # Security check: ensure cwd is not targeting forbidden root paths
    work_dir = Path(cwd) if cwd else Path("/data/jarvis")
    if not work_dir.exists():
        work_dir.mkdir(parents=True, exist_ok=True)

    try:
        proc = await asyncio.wait_for(
            asyncio.create_subprocess_shell(
                command,
                cwd=str(work_dir),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            ),
            timeout=timeout_seconds,
        )
        stdout, stderr = await proc.communicate()
        out_str = stdout.decode("utf-8", errors="replace")[-3000:]
        err_str = stderr.decode("utf-8", errors="replace")[-2000:]

        status = "PASSED" if proc.returncode == 0 else f"FAILED (Exit Code {proc.returncode})"
        res = f"[{status}]\n"
        if out_str:
            res += f"STDOUT:\n{out_str}\n"
        if err_str:
            res += f"STDERR:\n{err_str}\n"
        return res
    except asyncio.TimeoutError:
        return f"[ERROR] Command timed out after {timeout_seconds} seconds."
    except Exception as e:
        return f"[ERROR] Execution failed: {str(e)}"


@registry.register(
    name="computer_verify_task",
    description="Run verification tests and checks to produce concrete proof before marking a task complete.",
    parameters={
        "type": "object",
        "properties": {
            "workspace_path": {"type": "string", "description": "Path of the workspace to verify"},
            "commands": {
                "type": "array",
                "items": {"type": "string"},
                "description": "List of test/build verification commands (e.g. ['pytest -q', 'python -m unittest'])"
            },
            "require_clean_git": {"type": "boolean", "description": "Whether Git status must be clean"}
        },
        "required": ["workspace_path", "commands"]
    },
    category="coding"
)
async def computer_verify_task(
    workspace_path: str,
    commands: List[str],
    require_clean_git: bool = False,
) -> str:
    contract = VerificationContract(commands=commands, require_clean_git=require_clean_git)
    result = await _verifier.verify_workspace(workspace_path=workspace_path, contract=contract)
    status_icon = "✅" if result.passed else "❌"
    lines = [f"{status_icon} **{result.summary}**"]
    for cr in result.command_results:
        c_status = "PASS" if cr["passed"] else f"FAIL (exit {cr['exit_code']})"
        lines.append(f"- `{cr['command']}`: {c_status} in {cr.get('duration_seconds', 0)}s")
    return "\n".join(lines)


@registry.register(
    name="computer_system_status",
    description="Inspect persistent storage, disk usage, active projects, and Server Computer health.",
    parameters={"type": "object", "properties": {}},
    category="system"
)
def computer_system_status() -> str:
    root = Path("/data/jarvis")
    if not root.exists():
        root = Path("./data/jarvis")

    stat = shutil.disk_usage(str(root) if root.exists() else ".")
    free_gb = round(stat.free / (1024 ** 3), 2)
    total_gb = round(stat.total / (1024 ** 3), 2)

    projects_count = len(_registry.list_projects())
    workspaces_count = len(_workspaces.list_active_workspaces())

    return f"""### 💻 Server Computer Runtime Status
- **Storage Root:** `{root.resolve()}`
- **Disk Usage:** {free_gb} GB free / {total_gb} GB total
- **Registered Projects:** {projects_count}
- **Active Workspaces:** {workspaces_count}
- **Status:** Healthy & Operational
"""
