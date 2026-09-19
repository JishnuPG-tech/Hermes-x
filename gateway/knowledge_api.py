"""
Gateway Knowledge API Router
============================
Exposes HTTP endpoints for unified knowledge retrieval, note management,
Notion integration status, Obsidian vault status, and sync jobs.
"""
from __future__ import annotations

import time
from typing import Optional, List, Dict, Any
from fastapi import APIRouter, Request, Query, HTTPException
from fastapi.responses import JSONResponse

from harness.knowledge.router import KnowledgeRouter
from harness.knowledge.models import KnowledgeQuery, WriteIntent

router = APIRouter(prefix="/v1", tags=["Knowledge"])
_router = KnowledgeRouter()


@router.get("/knowledge/sources")
async def list_sources():
    """Lists configured knowledge sources and their real-time health."""
    health_map = await _router.get_health()
    sources = [
        {
            "name": "notion",
            "type": "cloud_database",
            "primary": True,
            "status": health_map.get("notion").status if "notion" in health_map else "unknown",
            "message": health_map.get("notion").message if "notion" in health_map else "",
            "capabilities": ["search", "pages", "databases", "blocks", "atomic_writes"],
        },
        {
            "name": "obsidian",
            "type": "local_markdown_vault",
            "primary": False,
            "status": health_map.get("obsidian").status if "obsidian" in health_map else "unknown",
            "message": health_map.get("obsidian").message if "obsidian" in health_map else "",
            "capabilities": ["search", "read", "atomic_writes", "wikilinks", "frontmatter", "uris"],
        },
    ]
    return {
        "status": "ok",
        "default_source": _router.default_source,
        "sources": sources,
    }


@router.get("/knowledge/search")
async def search_knowledge_endpoint(
    q: str = Query(..., description="Search query string"),
    sources: Optional[str] = Query(None, description="Comma-separated sources: notion,obsidian"),
    project: Optional[str] = Query(None, description="Project filter"),
    limit: int = Query(10, ge=1, le=50),
):
    """Unified hybrid search across Notion and Obsidian."""
    source_list = [s.strip() for s in sources.split(",")] if sources else None
    kq = KnowledgeQuery(query=q, sources=source_list, project=project, limit=limit)
    results = await _router.search(kq)
    return {
        "status": "ok",
        "query": q,
        "results_count": len(results),
        "results": [r.to_dict() for r in results],
    }


@router.get("/knowledge/notes/{source}/{source_id:path}")
async def get_note_endpoint(source: str, source_id: str):
    """Retrieves a note or document by provider source and identifier."""
    if source not in ("notion", "obsidian"):
        raise HTTPException(status_code=400, detail="Source must be 'notion' or 'obsidian'")

    doc = await _router.get_document(source, source_id)
    if not doc:
        raise HTTPException(status_code=404, detail=f"Note '{source_id}' not found in {source}")
    return {"status": "ok", "document": doc.to_dict()}


@router.post("/knowledge/notes")
async def create_note_endpoint(request: Request):
    """Creates a new note or decision record in Notion or Obsidian."""
    data = await request.json()
    title = data.get("title")
    content = data.get("content")
    if not title or not content:
        raise HTTPException(status_code=400, detail="'title' and 'content' are required fields")

    destination = data.get("destination", _router.default_source).lower()
    intent = WriteIntent(
        intent_id=f"api_{int(time.time() * 1000)}",
        destination=destination,
        title=title,
        content=content,
        parent_id=data.get("parent_id"),
        tags=data.get("tags", []),
        properties=data.get("properties", {}),
        operation="create",
    )

    result = await _router.write(intent, user_approved=True)
    if not result.success:
        return JSONResponse(status_code=400, content={"status": "error", "error": result.error})
    return {"status": "ok", "result": result.to_dict()}


@router.post("/knowledge/sync")
async def sync_knowledge_endpoint(request: Request):
    """Triggers an incremental pull synchronization across active sources."""
    from harness.knowledge.sync import KnowledgeSyncEngine
    sync_engine = KnowledgeSyncEngine(router=_router, audit_logger=_router.audit)
    data = await request.json() if request.headers.get("content-length") else {}
    connector = data.get("connector")
    if connector:
        res = await sync_engine.sync_source(connector)
    else:
        res = await sync_engine.sync_all()
    return {"status": "ok", "sync_result": res}


@router.get("/knowledge/audit")
async def get_audit_log(limit: int = Query(50, ge=1, le=100)):
    """Returns recent tamper-evident audit events from knowledge operations."""
    events = _router.audit.get_recent_events(limit=limit)
    return {"status": "ok", "events_count": len(events), "events": events}


@router.get("/notion/status")
async def notion_status():
    """Checks Notion integration status and configuration guidance."""
    health = await _router.notion.health()
    return {
        "status": health.status,
        "healthy": health.healthy,
        "message": health.message,
        "configured": _router.notion.is_configured,
        "api_version": "2022-06-28",
        "how_to_connect": (
            "Set Space secret 'NOTION_API_KEY' or 'NOTION_TOKEN' with an internal integration token "
            "from https://www.notion.so/my-integrations, and share relevant Notion pages with your integration."
        ),
    }


@router.get("/obsidian/status")
async def obsidian_status():
    """Checks Obsidian / Ignis vault status."""
    health = await _router.obsidian.health()
    return {
        "status": health.status,
        "healthy": health.healthy,
        "message": health.message,
        "vault_name": _router.obsidian.vault_name,
        "vault_root": str(_router.obsidian.vault_root),
        "webui_url": "/vault",
    }


@router.post("/obsidian/pair")
async def obsidian_pair(request: Request):
    """Pairing handshake for external companion Obsidian bridges."""
    data = await request.json()
    device_id = data.get("device_id", f"dev_{int(time.time())}")
    public_key = data.get("public_key", "")
    challenge = f"hermes_pair_{int(time.time())}_{device_id}"

    _router.audit.log(
        event_type="bridge_paired",
        connector="obsidian",
        operation="pair",
        details={"device_id": device_id, "has_pubkey": bool(public_key)},
    )

    return {
        "status": "ok",
        "device_id": device_id,
        "challenge": challenge,
        "paired_at": time.time(),
        "expires_in": 3600,
    }
