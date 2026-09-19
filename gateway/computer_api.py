"""
Server Computer Gateway API
===========================
Exposes REST endpoints for:
- /v1/computer/status: Storage metrics, disk space, and runtime health
- /v1/projects: Project registry management
- /v1/workspaces: Task workspace management
- /health/storage: Storage mount and write checks
"""
from __future__ import annotations

import os
import shutil
import time
from pathlib import Path
from typing import Optional, List, Dict, Any
from fastapi import APIRouter, Request, HTTPException
from fastapi.responses import JSONResponse

from harness.computer.project_registry import ProjectRegistry
from harness.computer.workspace_manager import WorkspaceManager

router = APIRouter(tags=["ServerComputer"])

_registry = ProjectRegistry()
_workspaces = WorkspaceManager(registry=_registry)


@router.get("/v1/computer/status")
async def computer_status():
    """Returns Server Computer runtime health, storage metrics, and active projects."""
    root = Path("/data/jarvis")
    if not root.exists():
        root = Path("./data/jarvis")

    is_writable = False
    try:
        root.mkdir(parents=True, exist_ok=True)
        test_file = root / f".write_test_{int(time.time())}"
        test_file.write_text("ok", encoding="utf-8")
        is_writable = test_file.exists()
        test_file.unlink(missing_ok=True)
    except Exception:
        is_writable = False

    stat = shutil.disk_usage(str(root) if root.exists() else ".")
    free_gb = round(stat.free / (1024 ** 3), 2)
    total_gb = round(stat.total / (1024 ** 3), 2)

    projects = _registry.list_projects()
    workspaces = _workspaces.list_active_workspaces()

    return {
        "status": "healthy" if is_writable else "degraded",
        "storage_root": str(root.resolve()).replace("\\", "/"),
        "writable": is_writable,
        "disk_free_gb": free_gb,
        "disk_total_gb": total_gb,
        "active_projects_count": len(projects),
        "active_workspaces_count": len(workspaces),
        "projects": [p.to_dict() for p in projects],
        "timestamp": time.time(),
    }


@router.get("/health/storage")
async def health_storage():
    """Specific health check verifying the /data persistent Storage Bucket mount."""
    data_dir = Path("/data")
    if not data_dir.exists():
        return JSONResponse(
            status_code=503,
            content={"status": "error", "error": "Persistent volume /data is not mounted"},
        )
    writable = os.access(str(data_dir), os.W_OK)
    return {
        "status": "ok" if writable else "read_only",
        "mount": "/data",
        "writable": writable,
        "service": "server_computer_storage",
    }


@router.get("/v1/projects")
async def list_projects():
    """Lists all registered software projects on the Server Computer."""
    projects = _registry.list_projects()
    return {
        "status": "ok",
        "count": len(projects),
        "projects": [p.to_dict() for p in projects],
    }


@router.post("/v1/projects")
async def register_project(request: Request):
    """Registers a software project in the persistent registry."""
    data = await request.json()
    project_id = data.get("project_id")
    if not project_id:
        raise HTTPException(status_code=400, detail="'project_id' is required")

    record = _registry.register_project(
        project_id=project_id,
        name=data.get("name"),
        path=data.get("path"),
        github_repo=data.get("github_repo"),
        default_branch=data.get("default_branch", "main"),
        policy=data.get("policy", "standard-development"),
        description=data.get("description"),
    )
    return {"status": "ok", "project": record.to_dict()}


@router.get("/v1/projects/{project_id}")
async def get_project_details(project_id: str):
    """Retrieves metadata and working copy status of a registered project."""
    record = _registry.get_project(project_id)
    if not record:
        raise HTTPException(status_code=404, detail=f"Project '{project_id}' not found")
    p_path = Path(record.path)
    exists = p_path.is_dir()
    is_git = (p_path / ".git").exists() if exists else False

    return {
        "status": "ok",
        "project": record.to_dict(),
        "filesystem": {
            "exists": exists,
            "is_git_repository": is_git,
        },
    }


@router.post("/v1/workspaces")
async def create_workspace(request: Request):
    """Provisions an isolated task workspace on the Server Computer."""
    data = await request.json()
    task_id = data.get("task_id")
    project_id = data.get("project_id")
    if not task_id or not project_id:
        raise HTTPException(status_code=400, detail="'task_id' and 'project_id' are required")

    info = await _workspaces.provision_workspace(
        task_id=task_id,
        project_id=project_id,
        branch=data.get("branch"),
    )
    return {"status": "ok", "workspace": info.to_dict()}
