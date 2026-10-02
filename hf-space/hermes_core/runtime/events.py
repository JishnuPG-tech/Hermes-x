"""
Hermes Canonical Event Bus
Publish/subscribe event propagation with strict credential sanitization.
"""
from __future__ import annotations

import asyncio
import logging
import re
from typing import Callable, Coroutine, Dict, List, Optional, Set
from hermes_core.runtime.models import AgentEvent

logger = logging.getLogger("hermes.runtime.events")

SECRET_PATTERNS = [
    re.compile(r'(?:api[_-]?key|token|secret|password|bearer|auth)["\']?\s*[:=]\s*["\']?([^"\'\s]{8,})', re.IGNORECASE),
    re.compile(r'ghp_[a-zA-Z0-9]{36}'),
    re.compile(r'github_pat_[a-zA-Z0-9_]{80}'),
    re.compile(r'hf_[a-zA-Z0-9]{34}'),
    re.compile(r'sk-[a-zA-Z0-9]{32,}'),
]


def sanitize_event_data(data: Dict) -> Dict:
    """Recursively scrub sensitive keys and tokens from event payloads."""
    cleaned = {}
    for k, v in data.items():
        if any(sec in k.lower() for sec in ("token", "secret", "password", "api_key", "authorization", "private_key")):
            cleaned[k] = "[REDACTED]"
        elif isinstance(v, dict):
            cleaned[k] = sanitize_event_data(v)
        elif isinstance(v, list):
            cleaned[k] = [sanitize_event_data(item) if isinstance(item, dict) else item for item in v]
        elif isinstance(v, str):
            s = v
            for p in SECRET_PATTERNS:
                s = p.sub("[REDACTED_SECRET]", s)
            cleaned[k] = s
        else:
            cleaned[k] = v
    return cleaned


class RuntimeEventBus:
    """Thread-safe, async event bus for agent lifecycle events."""
    _instance: Optional[RuntimeEventBus] = None

    def __init__(self):
        self._subscribers: Set[asyncio.Queue] = set()
        self._listeners: Dict[str, List[Callable[[AgentEvent], Coroutine[None, None, None]]]] = {}

    @classmethod
    def get_instance(cls) -> RuntimeEventBus:
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def _sanitize_data(self, data: Dict) -> Dict:
        """Sanitizes data dictionaries scrubbing tokens and credentials."""
        return sanitize_event_data(data)


    def subscribe(self) -> asyncio.Queue:
        q = asyncio.Queue()
        self._subscribers.add(q)
        return q

    def unsubscribe(self, q: asyncio.Queue):
        self._subscribers.discard(q)

    def on(self, event_type: str, handler: Callable[[AgentEvent], Coroutine[None, None, None]]):
        if event_type not in self._listeners:
            self._listeners[event_type] = []
        self._listeners[event_type].append(handler)

    async def emit(self, event: AgentEvent):
        """Emit a canonical event to all subscribers and registered listeners."""
        # Sanitize metadata before emission
        event.metadata = sanitize_event_data(event.metadata)
        event_dict = event.to_dict()

        # Broadcast to stream queues
        dead_queues = set()
        for q in list(self._subscribers):
            try:
                q.put_nowait(event_dict)
            except Exception:
                dead_queues.add(q)
        self._subscribers.difference_update(dead_queues)

        # Notify type-specific handlers
        handlers = self._listeners.get(event.type, []) + self._listeners.get("*", [])
        for handler in handlers:
            try:
                asyncio.create_task(handler(event))
            except Exception as e:
                logger.warning(f"Error dispatching event handler for {event.type}: {e}")

        # Persist event to TaskDB if related to a durable task
        if event.task_id:
            try:
                from harness.kernel.task_db import TaskDB
                db = TaskDB()
                db.save_event(
                    task_id=event.task_id,
                    event_type=event.type,
                    source=event.agent_id,
                    status=event.status,
                    payload=event.metadata,
                )
            except Exception:
                pass
