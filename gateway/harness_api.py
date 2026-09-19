"""
Agent Harness & Agent OS REST & SSE Control Plane API
Exposes /v1/tasks, /v1/approvals, and real-time SSE event streams.
"""
from __future__ import annotations

import json
from typing import Optional, List, Dict, Any
from fastapi import APIRouter, Request, HTTPException, Query, Path
from fastapi.responses import JSONResponse, StreamingResponse

from harness.kernel.models import TaskStatus, RiskLevel
from harness.engine import HarnessEngine


router = APIRouter(tags=["Agent Harness OS"])

# Singleton engine instance
_ENGINE: Optional[HarnessEngine] = None


def get_harness_engine() -> HarnessEngine:
    global _ENGINE
    if _ENGINE is None:
        _ENGINE = HarnessEngine()
    return _ENGINE


# ── Task Management API ───────────────────────────────────────
@router.post("/v1/tasks")
async def create_task(request: Request):
    """Create and dispatch a new durable autonomous task."""
    body = await request.json()
    objective = body.get("objective", "").strip()
    if not objective:
        raise HTTPException(status_code=400, detail="Missing required field: 'objective'")

    project_id = body.get("project_id", "default")
    idempotency_key = body.get("idempotency_key")
    risk_level_str = body.get("risk_level", "medium")
    try:
        risk_level = RiskLevel(risk_level_str.lower())
    except Exception:
        risk_level = RiskLevel.MEDIUM

    allowed_tools = body.get("allowed_tools")
    engine = get_harness_engine()
    task = await engine.create_and_run_task(
        objective=objective,
        project_id=project_id,
        idempotency_key=idempotency_key,
        risk_level=risk_level,
        allowed_tools=allowed_tools,
    )
    return JSONResponse(task.to_dict(), status_code=201)


@router.get("/v1/tasks")
async def list_tasks(
    project_id: Optional[str] = Query(None),
    status: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=200),
):
    """List tasks with optional project_id and status filters."""
    engine = get_harness_engine()
    status_enum = None
    if status:
        try:
            status_enum = TaskStatus(status.upper())
        except Exception:
            pass
    tasks = engine.db.list_tasks(project_id=project_id, status=status_enum, limit=limit)
    return JSONResponse({"tasks": [t.to_dict() for t in tasks]})


@router.get("/v1/tasks/{task_id}")
async def get_task_details(task_id: str = Path(...)):
    """Get complete task details, subtasks, steps, and checkpoints."""
    engine = get_harness_engine()
    task = engine.db.get_task(task_id)
    if not task:
        raise HTTPException(status_code=404, detail=f"Task {task_id} not found")

    subtasks = engine.db.get_subtasks(task_id)
    steps = engine.db.get_steps(task_id)
    checkpoint = engine.db.get_latest_checkpoint(task_id)

    data = task.to_dict()
    data["subtasks"] = [s.to_dict() for s in subtasks]
    data["steps"] = [st.to_dict() for st in steps]
    data["latest_checkpoint"] = checkpoint.to_dict() if checkpoint else None
    return JSONResponse(data)


@router.post("/v1/tasks/{task_id}/pause")
async def pause_task(task_id: str = Path(...)):
    engine = get_harness_engine()
    if engine.pause_task(task_id):
        return JSONResponse({"status": "paused", "task_id": task_id})
    raise HTTPException(status_code=400, detail=f"Cannot pause task {task_id}")


@router.post("/v1/tasks/{task_id}/resume")
async def resume_task(task_id: str = Path(...)):
    engine = get_harness_engine()
    if engine.resume_task(task_id):
        return JSONResponse({"status": "resumed", "task_id": task_id})
    raise HTTPException(status_code=400, detail=f"Cannot resume task {task_id}")


@router.post("/v1/tasks/{task_id}/cancel")
async def cancel_task(task_id: str = Path(...)):
    engine = get_harness_engine()
    if engine.cancel_task(task_id):
        return JSONResponse({"status": "cancelled", "task_id": task_id})
    raise HTTPException(status_code=400, detail=f"Cannot cancel task {task_id}")


# ── Live Events SSE API ───────────────────────────────────────
@router.get("/v1/tasks/{task_id}/events")
async def stream_task_events(task_id: str = Path(...)):
    """Server-Sent Events (SSE) stream for real-time task execution updates."""
    engine = get_harness_engine()

    async def event_generator():
        # Yield historical events first
        past_events = engine.db.get_events(task_id)
        for pe in past_events:
            yield f"data: {json.dumps(pe)}\n\n"

        # Subscribe to live events
        async for live_event in engine.event_bus.subscribe(task_id=task_id):
            yield f"data: {json.dumps(live_event)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ── Approvals API ─────────────────────────────────────────────
@router.get("/v1/approvals")
async def list_approvals():
    """List all pending owner approval requests."""
    engine = get_harness_engine()
    pending = engine.approval_service.list_pending()
    return JSONResponse({"approvals": [a.to_dict() for a in pending]})


@router.get("/v1/approvals/{approval_id}")
async def get_approval(approval_id: str = Path(...)):
    engine = get_harness_engine()
    appr = engine.approval_service.get_approval(approval_id)
    if not appr:
        raise HTTPException(status_code=404, detail="Approval request not found")
    return JSONResponse(appr.to_dict())


@router.post("/v1/approvals/{approval_id}/approve")
async def approve_request(approval_id: str = Path(...), request: Request = None):
    engine = get_harness_engine()
    body = await request.json() if request else {}
    reason = body.get("reason", "Approved by owner")
    approved = engine.approval_service.approve(approval_id, approved_by="owner", reason=reason)
    if not approved:
        raise HTTPException(status_code=400, detail="Could not approve request (already decided or expired)")

    appr = engine.approval_service.get_approval(approval_id)
    if appr and appr.task_id:
        engine.resume_task(appr.task_id)

    return JSONResponse({"status": "approved", "approval_id": approval_id})


@router.post("/v1/approvals/{approval_id}/deny")
async def deny_request(approval_id: str = Path(...), request: Request = None):
    engine = get_harness_engine()
    body = await request.json() if request else {}
    reason = body.get("reason", "Denied by owner")
    denied = engine.approval_service.deny(approval_id, denied_by="owner", reason=reason)
    if not denied:
        raise HTTPException(status_code=400, detail="Could not deny request")
    return JSONResponse({"status": "denied", "approval_id": approval_id})


# ── Health & Diagnostics API ──────────────────────────────────
@router.get("/v1/harness/health")
async def harness_health():
    engine = get_harness_engine()
    all_tasks = engine.db.list_tasks(limit=100)
    running_count = sum(1 for t in all_tasks if t.status == TaskStatus.RUNNING)
    pending_approvals = len(engine.approval_service.list_pending())
    return JSONResponse({
        "status": "healthy",
        "service": "agent_harness_os",
        "database_path": engine.db.db_path,
        "active_tasks_count": running_count,
        "pending_approvals_count": pending_approvals,
    })
