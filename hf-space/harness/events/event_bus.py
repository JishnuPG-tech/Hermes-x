"""
Event Bus & Event Streamer
Provides durable logging and real-time SSE streaming for task lifecycle events.
"""
from __future__ import annotations

import asyncio
import json
import time
import uuid
from typing import Dict, Any, AsyncGenerator, Set
from harness.kernel.task_db import TaskDB


class EventBus:
    def __init__(self, db: TaskDB):
        self.db = db
        self._subscribers: Set[asyncio.Queue] = set()

    async def emit(self, task_id: str, event_type: str, actor: str, status: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Emit an event, persist it to SQLite, and dispatch to all active SSE subscribers."""
        event_id = f"evt_{uuid.uuid4().hex[:10]}"
        event_data = {
            "event_id": event_id,
            "task_id": task_id,
            "event_type": event_type,
            "actor": actor,
            "status": status,
            "payload": payload,
            "timestamp": time.time(),
        }

        # 1. Durable persistence
        try:
            self.db.append_event(
                event_id=event_id,
                task_id=task_id,
                event_type=event_type,
                actor=actor,
                status=status,
                payload=payload,
            )
        except Exception as e:
            print(f"[EventBus] Error persisting event: {e}")

        # 2. Real-time broadcast to subscribers
        for queue in list(self._subscribers):
            try:
                queue.put_nowait(event_data)
            except Exception:
                pass

        return event_data

    async def subscribe(self, task_id: Optional[str] = None) -> AsyncGenerator[Dict[str, Any], None]:
        """Async generator that yields events for SSE streaming."""
        queue: asyncio.Queue = asyncio.Queue(maxsize=100)
        self._subscribers.add(queue)
        try:
            while True:
                event = await queue.get()
                if task_id is None or event.get("task_id") == task_id:
                    yield event
        finally:
            self._subscribers.discard(queue)
