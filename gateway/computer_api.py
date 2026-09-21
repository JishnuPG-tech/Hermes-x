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


# ── File System Exploration ───────────────────────────────────

@router.get("/v1/computer/files")
async def list_computer_files(path: Optional[str] = None):
    """List files in the specified computer directory."""
    root = Path("/data/jarvis")
    if not root.exists():
        root = Path("./data/jarvis")
    root.mkdir(parents=True, exist_ok=True)

    target = (root / path.lstrip("/\\")) if path else root
    if not target.exists() or not target.is_dir():
        return {"status": "ok", "path": str(target), "files": []}

    items = []
    for entry in target.iterdir():
        try:
            stat = entry.stat()
            items.append({
                "name": entry.name,
                "path": str(entry.relative_to(root)).replace("\\", "/"),
                "is_dir": entry.is_dir(),
                "size": stat.st_size if entry.is_file() else 0,
                "modified": stat.st_mtime,
            })
        except Exception:
            continue
    items.sort(key=lambda x: (not x["is_dir"], x["name"].lower()))
    rel_path = ""
    try:
        rel_path = str(target.relative_to(root)).replace("\\", "/")
    except Exception:
        rel_path = "/"
    return {"status": "ok", "path": rel_path, "files": items}


@router.get("/v1/computer/file/content")
async def get_computer_file_content(path: str = Query(...)):
    """Read content of a file on the server computer."""
    root = Path("/data/jarvis")
    if not root.exists():
        root = Path("./data/jarvis")
    target = (root / path.lstrip("/\\")).resolve()
    if not str(target).startswith(str(root.resolve())):
        raise HTTPException(status_code=403, detail="Path traversal forbidden")
    if not target.exists() or not target.is_file():
        raise HTTPException(status_code=404, detail="File not found")
    try:
        content = target.read_text(encoding="utf-8", errors="replace")
        return {"status": "ok", "path": path, "content": content}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Browser Live View ─────────────────────────────────────────

import base64
from harness.browser.browser_manager import BrowserManager
_browser_mgr = BrowserManager()


@router.get("/v1/browser/status")
async def get_browser_status():
    """Returns status of the active headless browser session."""
    session = _browser_mgr._sessions.get("default", {})
    return {
        "status": "ok",
        "current_url": session.get("current_url", "about:blank"),
        "title": session.get("title", "Ready"),
        "is_active": _browser_mgr._browser is not None,
    }


@router.post("/v1/browser/navigate")
async def navigate_browser(request: Request):
    """Navigate the server browser to a URL."""
    data = await request.json()
    url = data.get("url", "https://google.com")
    res = await _browser_mgr.navigate(url)
    return {
        "status": "ok",
        "url": res.url,
        "title": res.title,
        "status_code": res.status_code,
        "content_snippet": res.text_content[:2000] if res.text_content else "",
    }


@router.get("/v1/browser/screenshot")
async def get_browser_screenshot():
    """Captures and returns base64 PNG screenshot of current browser viewport."""
    res = await _browser_mgr.screenshot()
    if res.get("status") == "success" and "file_path" in res:
        try:
            with open(res["file_path"], "rb") as f:
                b64 = base64.b64encode(f.read()).decode("utf-8")
            return {"status": "ok", "screenshot_base64": b64, "url": res.get("current_url", "")}
        except Exception as e:
            return {"status": "error", "message": str(e)}
    return {"status": "fallback", "message": res.get("message", "Screenshot unavailable"), "screenshot_base64": ""}

