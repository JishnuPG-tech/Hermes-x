import os
import json
import uuid
import time
import asyncio
from pathlib import Path
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional, Set
from fastapi import APIRouter, Request, Header, HTTPException, Query, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse
from pydantic import BaseModel

router = APIRouter()

STORAGE_DIR = Path("/data/sessions") if Path("/data").exists() else Path("/tmp/sessions")
STORAGE_DIR.mkdir(parents=True, exist_ok=True)
SESSIONS_FILE = STORAGE_DIR / "sessions_db.json"

_SESSIONS: Dict[str, Dict[str, Any]] = {}
_MESSAGES: Dict[str, List[Dict[str, Any]]] = {}
_CONV_TO_SESSION: Dict[str, str] = {}

# Active WebSocket watchers per session
_ACTIVE_WATCHERS: Dict[str, Set[WebSocket]] = {}

def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def _load_data():
    global _SESSIONS, _MESSAGES, _CONV_TO_SESSION
    if SESSIONS_FILE.exists():
        try:
            raw = json.loads(SESSIONS_FILE.read_text(encoding="utf-8"))
            _SESSIONS = raw.get("sessions", {})
            _MESSAGES = raw.get("messages", {})
            for sess_id, sess in _SESSIONS.items():
                cuuid = sess.get("conversation_uuid")
                if cuuid:
                    _CONV_TO_SESSION[cuuid] = sess_id
        except Exception:
            _SESSIONS = {}
            _MESSAGES = {}

def _save_data():
    try:
        payload = {
            "sessions": _SESSIONS,
            "messages": _MESSAGES
        }
        SESSIONS_FILE.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    except Exception:
        pass

_load_data()

async def broadcast_session_event(session_id: str, event_type: str, payload: Dict[str, Any]):
    """Broadcasts real-time events to all active WebSocket listeners on a session."""
    if session_id in _ACTIVE_WATCHERS:
        event = {
            "type": event_type,
            "payload": payload
        }
        dead_sockets = set()
        for ws in _ACTIVE_WATCHERS[session_id]:
            try:
                await ws.send_json(event)
            except Exception:
                dead_sockets.add(ws)
        _ACTIVE_WATCHERS[session_id].difference_update(dead_sockets)

# -------------------------------------------------------------
# A. Session Management
# -------------------------------------------------------------
@router.post("/v1/sessions")
@router.post("/sessions")
@router.post("/api/v1/sessions")
@router.post("/hermes/v1/sessions")
async def create_session(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}
    
    session_id = body.get("id") or f"sess_{uuid.uuid4().hex[:24]}"
    conv_uuid = body.get("conversation_uuid") or str(uuid.uuid4())
    title = body.get("title")
    now = _now_iso()
    
    session_obj = {
        "id": session_id,
        "conversation_uuid": conv_uuid,
        "title": title,
        "created_at": now,
        "updated_at": now
    }
    
    _SESSIONS[session_id] = session_obj
    _CONV_TO_SESSION[conv_uuid] = session_id
    if session_id not in _MESSAGES:
        _MESSAGES[session_id] = []
        
    _save_data()
    asyncio.create_task(broadcast_session_event(session_id, "session.created", session_obj))
    return session_obj

@router.get("/v1/sessions")
@router.get("/sessions")
@router.get("/api/v1/sessions")
@router.get("/hermes/v1/sessions")
async def list_sessions(limit: int = Query(50, le=100)):
    items = list(_SESSIONS.values())
    sliced = items[-limit:]
    return {
        "data": sliced,
        "sessions": sliced,
        "has_more": len(items) > limit,
        "first_id": items[0]["id"] if items else None,
        "last_id": items[-1]["id"] if items else None
    }

@router.get("/v1/sessions/{session_id}")
@router.get("/sessions/{session_id}")
@router.get("/api/v1/sessions/{session_id}")
@router.get("/hermes/v1/sessions/{session_id}")
async def get_session(session_id: str):
    sess_id = session_id or ""
    if sess_id not in _SESSIONS:
        if sess_id in _CONV_TO_SESSION:
            sess_id = _CONV_TO_SESSION[sess_id]
        else:
            now = _now_iso()
            _SESSIONS[sess_id] = {
                "id": sess_id,
                "session_id": sess_id,
                "conversation_uuid": sess_id,
                "title": "Chat",
                "created_at": now,
                "updated_at": now
            }
            _MESSAGES[sess_id] = []
            _save_data()

    sess = dict(_SESSIONS[sess_id])
    sess["session_id"] = sess.get("session_id") or sess_id
    msgs = _MESSAGES.get(sess_id, [])
    sess["messages"] = msgs
    sess["message_count"] = len(msgs)

    try:
        from gateway.autonomous_chat import _ACTIVE_RUNS
        active_run = _ACTIVE_RUNS.get(sess_id)
        is_running = bool(active_run and not active_run.is_finished)
        sess["is_streaming"] = is_running
        sess["status"] = "running" if is_running else "idle"
        sess["has_active_run"] = is_running
    except Exception:
        sess["is_streaming"] = False
        sess["status"] = "idle"
        sess["has_active_run"] = False

    return {
        "session": sess,
        **sess
    }

@router.get("/api/session")
async def get_session_query(request: Request, session_id: Optional[str] = Query(None)):
    sid = session_id or request.query_params.get("session_id") or ""
    if not sid:
        raise HTTPException(status_code=400, detail="session_id is required")
    return await get_session(session_id=sid)

@router.get("/api/session/status")
@router.get("/v1/sessions/{session_id}/status")
async def get_session_status_endpoint(request: Request, session_id: Optional[str] = None):
    sid = session_id or request.query_params.get("session_id") or ""
    try:
        from gateway.autonomous_chat import _ACTIVE_RUNS
        active_run = _ACTIVE_RUNS.get(sid)
        is_running = bool(active_run and not active_run.is_finished)
        return {
            "session_id": sid,
            "is_streaming": is_running,
            "status": "running" if is_running else "idle",
            "current_text": active_run.accumulated_text if active_run else None,
            "has_active_run": is_running
        }
    except Exception:
        return {
            "session_id": sid,
            "is_streaming": False,
            "status": "idle",
            "current_text": None,
            "has_active_run": False
        }

@router.delete("/v1/sessions/{session_id}")
@router.delete("/sessions/{session_id}")
@router.delete("/api/v1/sessions/{session_id}")
@router.delete("/hermes/v1/sessions/{session_id}")
async def delete_session(session_id: str):
    if session_id in _SESSIONS:
        deleted = _SESSIONS.pop(session_id)
        _MESSAGES.pop(session_id, None)
        _save_data()
        asyncio.create_task(broadcast_session_event(session_id, "session.deleted", {"session_id": session_id}))
        return {"status": "deleted", "id": session_id}
    return {"status": "not_found", "id": session_id}

@router.post("/v1/sessions/{session_id}/rename")
@router.post("/sessions/{session_id}/rename")
@router.post("/api/v1/sessions/{session_id}/rename")
@router.post("/api/session/rename")
@router.patch("/v1/sessions/{session_id}")
async def rename_session(request: Request, session_id: Optional[str] = None):
    try:
        body = await request.json()
    except Exception:
        body = {}
    
    sid = session_id or body.get("session_id") or ""
    title = body.get("title") or body.get("name") or ""
    title = str(title).strip()
    if not title:
        raise HTTPException(status_code=400, detail="title is required")
    
    if sid not in _SESSIONS:
        if sid in _CONV_TO_SESSION:
            sid = _CONV_TO_SESSION[sid]
        else:
            now = _now_iso()
            _SESSIONS[sid] = {
                "id": sid,
                "session_id": sid,
                "conversation_uuid": sid,
                "title": title,
                "created_at": now,
                "updated_at": now
            }
    
    _SESSIONS[sid]["title"] = title[:120]
    _SESSIONS[sid]["updated_at"] = _now_iso()
    _save_data()
    asyncio.create_task(broadcast_session_event(sid, "session.updated", _SESSIONS[sid]))
    return {"ok": True, "status": "updated", "session": _SESSIONS[sid], **_SESSIONS[sid]}

# -------------------------------------------------------------
# B. Message History Management
# -------------------------------------------------------------
@router.get("/v1/sessions/{session_id}/messages")
@router.get("/sessions/{session_id}/messages")
@router.get("/api/v1/sessions/{session_id}/messages")
@router.get("/hermes/v1/sessions/{session_id}/messages")
async def list_session_messages(
    session_id: str,
    limit: int = Query(50, le=100),
    before_id: Optional[str] = None,
    after_id: Optional[str] = None
):
    if session_id not in _SESSIONS and session_id in _CONV_TO_SESSION:
        session_id = _CONV_TO_SESSION[session_id]
        
    msgs = _MESSAGES.get(session_id, [])
    
    # Cursor pagination
    if before_id:
        idx = next((i for i, m in enumerate(msgs) if m["id"] == before_id), None)
        if idx is not None:
            msgs = msgs[:idx]
    if after_id:
        idx = next((i for i, m in enumerate(msgs) if m["id"] == after_id), None)
        if idx is not None:
            msgs = msgs[idx+1:]
            
    sliced = msgs[-limit:]
    return {
        "data": sliced,
        "has_more": len(msgs) > limit,
        "first_id": sliced[0]["id"] if sliced else None,
        "last_id": sliced[-1]["id"] if sliced else None
    }

@router.get("/v1/sessions/{session_id}/messages/{message_id}")
@router.get("/sessions/{session_id}/messages/{message_id}")
@router.get("/api/v1/sessions/{session_id}/messages/{message_id}")
@router.get("/hermes/v1/sessions/{session_id}/messages/{message_id}")
async def get_single_message(session_id: str, message_id: str):
    if session_id not in _SESSIONS and session_id in _CONV_TO_SESSION:
        session_id = _CONV_TO_SESSION[session_id]
        
    msgs = _MESSAGES.get(session_id, [])
    msg = next((m for m in msgs if m["id"] == message_id), None)
    if not msg:
        raise HTTPException(status_code=404, detail="Message not found")
    return msg

@router.post("/v1/sessions/{session_id}/messages")
@router.post("/sessions/{session_id}/messages")
@router.post("/api/v1/sessions/{session_id}/messages")
@router.post("/hermes/v1/sessions/{session_id}/messages")
async def append_session_message(session_id: str, request: Request):
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid message payload")
        
    if session_id not in _SESSIONS:
        if session_id in _CONV_TO_SESSION:
            session_id = _CONV_TO_SESSION[session_id]
        else:
            # Auto-provision
            now = _now_iso()
            _SESSIONS[session_id] = {
                "id": session_id,
                "conversation_uuid": session_id,
                "title": "Chat",
                "created_at": now,
                "updated_at": now
            }
            _MESSAGES[session_id] = []
            
    msg_id = body.get("id") or f"msg_{uuid.uuid4().hex[:24]}"
    role = body.get("role", "user")
    model = body.get("model", "hermes-agent")
    raw_content = body.get("content", [])
    
    # Normalize content blocks
    if isinstance(raw_content, str):
        content_blocks = [{"type": "text", "text": raw_content}]
    elif isinstance(raw_content, list):
        content_blocks = raw_content
    else:
        content_blocks = [{"type": "text", "text": str(raw_content)}]
        
    now = _now_iso()
    msg_obj = {
        "id": msg_id,
        "type": "message",
        "role": role,
        "model": model,
        "content": content_blocks,
        "stop_reason": body.get("stop_reason"),
        "stop_sequence": body.get("stop_sequence"),
        "usage": body.get("usage", {"input_tokens": 10, "output_tokens": 0}),
        "created_at": body.get("created_at", now),
        "conversation_uuid": _SESSIONS[session_id].get("conversation_uuid", session_id)
    }
    
    # Idempotent deduplication: update in-place if ID matches
    existing_idx = next((i for i, m in enumerate(_MESSAGES[session_id]) if m["id"] == msg_id), None)
    if existing_idx is not None:
        _MESSAGES[session_id][existing_idx] = msg_obj
    else:
        _MESSAGES[session_id].append(msg_obj)
        
    _SESSIONS[session_id]["updated_at"] = now
    _save_data()
    
    asyncio.create_task(broadcast_session_event(session_id, "message.created", msg_obj))
    return msg_obj

@router.delete("/v1/sessions/{session_id}/messages/{message_id}")
@router.delete("/sessions/{session_id}/messages/{message_id}")
@router.delete("/api/v1/sessions/{session_id}/messages/{message_id}")
@router.delete("/hermes/v1/sessions/{session_id}/messages/{message_id}")
async def delete_session_message(session_id: str, message_id: str):
    if session_id not in _SESSIONS and session_id in _CONV_TO_SESSION:
        session_id = _CONV_TO_SESSION[session_id]
        
    msgs = _MESSAGES.get(session_id, [])
    _MESSAGES[session_id] = [m for m in msgs if m["id"] != message_id]
    _save_data()
    asyncio.create_task(broadcast_session_event(session_id, "message.deleted", {"message_id": message_id}))
    return {"status": "deleted", "message_id": message_id}

# -------------------------------------------------------------
# C. Real-Time Sync (WebSocket Watch Protocol)
# -------------------------------------------------------------
@router.websocket("/v1/sessions/{session_id}/watch")
@router.websocket("/sessions/{session_id}/watch")
@router.websocket("/hermes/v1/sessions/{session_id}/watch")
async def watch_session(websocket: WebSocket, session_id: str):
    await websocket.accept()
    if session_id not in _ACTIVE_WATCHERS:
        _ACTIVE_WATCHERS[session_id] = set()
    _ACTIVE_WATCHERS[session_id].add(websocket)
    
    # Send initial connection handshake
    try:
        sess = _SESSIONS.get(session_id) or {
            "session_id": session_id,
            "status": "connected",
            "time": _now_iso()
        }
        await websocket.send_json({
            "type": "session.connected",
            "payload": sess
        })
        
        while True:
            # Keepalive receiver loop
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_text("pong")
    except WebSocketDisconnect:
        pass
    finally:
        if session_id in _ACTIVE_WATCHERS:
            _ACTIVE_WATCHERS[session_id].discard(websocket)
            if not _ACTIVE_WATCHERS[session_id]:
                del _ACTIVE_WATCHERS[session_id]
