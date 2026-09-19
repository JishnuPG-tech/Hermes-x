"""
Isolated Workspace Manager
Provides isolated directory environments for tasks and sub-agent workers.
"""
from __future__ import annotations

import os
import shutil
from pathlib import Path
from typing import Optional


def get_default_workspaces_root() -> Path:
    env_root = os.getenv("HERMES_WORKSPACES_ROOT")
    if env_root:
        return Path(env_root)
    if Path("/data/hermes").is_dir() or (Path("/data").exists() and os.access("/data", os.W_OK)):
        return Path("/data/hermes/workspaces")
    return Path("./data/workspaces")


class WorkspaceManager:
    def __init__(self, root_dir: Optional[Path] = None):
        self.root_dir = root_dir or get_default_workspaces_root()
        self.root_dir.mkdir(parents=True, exist_ok=True)

    def create_workspace(self, task_id: str) -> Path:
        """Create an isolated workspace directory for a task."""
        workspace = self.root_dir / task_id
        workspace.mkdir(parents=True, exist_ok=True)
        return workspace

    def get_workspace(self, task_id: str) -> Path:
        """Get or create task workspace."""
        workspace = self.root_dir / task_id
        workspace.mkdir(parents=True, exist_ok=True)
        return workspace

    def clean_workspace(self, task_id: str) -> None:
        """Remove task workspace if needed."""
        workspace = self.root_dir / task_id
        if workspace.exists() and workspace.is_dir():
            shutil.rmtree(workspace, ignore_errors=True)
