"""
Server Computer Workspace Manager
=================================
Provisions and manages sandboxed working directories for autonomous tasks:
- Workspaces live under /data/jarvis/workspaces/<task-id>
- Supports Git worktrees and isolated project checkouts
- Enforces strict path isolation preventing tasks from escaping their workspace
"""
from __future__ import annotations

import asyncio
import os
import shutil
import time
from pathlib import Path
from typing import Dict, Any, List, Optional

from harness.computer.models import WorkspaceInfo
from harness.computer.project_registry import ProjectRegistry


def get_workspaces_root() -> Path:
    env_root = os.getenv("JARVIS_WORKSPACES_DIR")
    if env_root:
        return Path(env_root)
    base = Path("/data/jarvis/workspaces")
    if not base.exists() and not (Path("/data").exists() and os.access("/data", os.W_OK)):
        base = Path("./data/jarvis/workspaces")
    base.mkdir(parents=True, exist_ok=True)
    return base


class WorkspaceManager:
    def __init__(self, workspaces_root: Optional[Path] = None, registry: Optional[ProjectRegistry] = None):
        self.workspaces_root = workspaces_root or get_workspaces_root()
        self.workspaces_root.mkdir(parents=True, exist_ok=True)
        self.registry = registry or ProjectRegistry()

    def get_workspace_path(self, task_id: str) -> Path:
        """Resolves the sandboxed absolute path for a task workspace."""
        clean_id = task_id.replace("/", "_").replace("\\", "_").strip()
        target = (self.workspaces_root / clean_id).resolve()
        # Verify boundary
        try:
            target.relative_to(self.workspaces_root.resolve())
        except ValueError:
            raise ValueError(f"Task ID '{task_id}' creates invalid path outside workspaces directory")
        return target

    async def provision_workspace(
        self,
        task_id: str,
        project_id: str,
        branch: Optional[str] = None,
    ) -> WorkspaceInfo:
        """Provisions an isolated workspace for a task, checking out the project repository if present."""
        ws_path = self.get_workspace_path(task_id)
        ws_path.mkdir(parents=True, exist_ok=True)

        project = self.registry.get_project(project_id)
        proj_path = Path(project.path) if project else None

        git_commit = None
        is_worktree = False

        if proj_path and (proj_path / ".git").exists():
            # Try git worktree or isolated checkout
            target_branch = branch or (project.default_branch if project else "main")
            worktree_cmd = f"git -C \"{proj_path}\" worktree add -b \"task-{task_id}\" \"{ws_path}\" \"{target_branch}\""
            proc = await asyncio.create_subprocess_shell(
                worktree_cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            stdout, stderr = await proc.communicate()
            if proc.returncode == 0:
                is_worktree = True
            else:
                # Fallback to copy if worktree cannot be created
                try:
                    for item in proj_path.iterdir():
                        if item.name == ".git":
                            continue
                        dest = ws_path / item.name
                        if item.is_dir():
                            shutil.copytree(item, dest, dirs_exist_ok=True)
                        else:
                            shutil.copy2(item, dest)
                except Exception:
                    pass

        # Check git commit if git available
        try:
            git_proc = await asyncio.create_subprocess_shell(
                f"git -C \"{ws_path}\" rev-parse HEAD",
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            out, _ = await git_proc.communicate()
            if git_proc.returncode == 0:
                git_commit = out.decode("utf-8").strip()[:10]
        except Exception:
            pass

        info = WorkspaceInfo(
            task_id=task_id,
            project_id=project_id,
            path=str(ws_path).replace("\\", "/"),
            git_commit=git_commit,
            git_branch=branch,
            is_worktree=is_worktree,
            created_at=time.time(),
            status="active",
        )
        return info

    def cleanup_workspace(self, task_id: str, archive: bool = False) -> bool:
        """Cleans up or archives a finished task workspace."""
        ws_path = self.get_workspace_path(task_id)
        if not ws_path.exists():
            return True

        if archive:
            archive_dir = self.workspaces_root / "_archived"
            archive_dir.mkdir(parents=True, exist_ok=True)
            dest = archive_dir / f"{task_id}_{int(time.time())}"
            try:
                shutil.move(str(ws_path), str(dest))
                return True
            except Exception:
                return False
        else:
            try:
                shutil.rmtree(str(ws_path), ignore_errors=True)
                return True
            except Exception:
                return False

    def list_active_workspaces(self) -> List[Dict[str, Any]]:
        """Lists active workspace directories on disk."""
        active = []
        for p in self.workspaces_root.iterdir():
            if p.is_dir() and not p.name.startswith("_") and not p.name.startswith("."):
                active.append({
                    "task_id": p.name,
                    "path": str(p).replace("\\", "/"),
                    "created_at": p.stat().st_ctime,
                    "size_bytes": sum(f.stat().st_size for f in p.glob("**/*") if f.is_file()),
                })
        return active
