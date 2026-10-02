"""Immutable Audit Logger for Hermes Operations.

Logs all actions, policy decisions, credentials used, and outcomes to append-only SQLite store.
"""

from __future__ import annotations

import json
import logging
import sqlite3
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)

DEFAULT_AUDIT_DB = Path("/data/jarvis/logs/audit.db") if Path("/data").exists() else Path("/tmp/jarvis/logs/audit.db")


class AuditLogger:
    """Records security-critical and operational events."""

    def __init__(self, db_path: Optional[Path] = None) -> None:
        self.db_path = db_path or DEFAULT_AUDIT_DB
        self._init_db()

    def _init_db(self) -> None:
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS audit_trail (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp REAL NOT NULL,
                    task_id TEXT,
                    chat_id TEXT,
                    tool_name TEXT NOT NULL,
                    decision TEXT NOT NULL,
                    credential_id TEXT,
                    model_id TEXT,
                    arguments_summary TEXT,
                    status TEXT NOT NULL,
                    details TEXT
                )
            """)
            conn.commit()

    def record(
        self,
        tool_name: str,
        decision: str,
        task_id: Optional[str] = None,
        chat_id: Optional[str] = None,
        credential_id: Optional[str] = None,
        model_id: Optional[str] = None,
        arguments_summary: Optional[Dict[str, Any]] = None,
        status: str = "success",
        details: Optional[str] = None,
    ) -> None:
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute(
                    """
                    INSERT INTO audit_trail 
                    (timestamp, task_id, chat_id, tool_name, decision, credential_id, model_id, arguments_summary, status, details)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        time.time(),
                        task_id or "",
                        chat_id or "",
                        tool_name,
                        decision,
                        credential_id or "",
                        model_id or "",
                        json.dumps(arguments_summary or {}),
                        status,
                        details or "",
                    ),
                )
                conn.commit()
        except Exception as e:
            logger.error("Failed to write audit entry: %s", e)

    def get_recent(self, limit: int = 50) -> List[Dict[str, Any]]:
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cur = conn.cursor()
            cur.execute("SELECT * FROM audit_trail ORDER BY id DESC LIMIT ?", (limit,))
            return [dict(r) for r in cur.fetchall()]


_AUDIT_INSTANCE: Optional[AuditLogger] = None


def get_audit_logger() -> AuditLogger:
    global _AUDIT_INSTANCE
    if _AUDIT_INSTANCE is None:
        _AUDIT_INSTANCE = AuditLogger()
    return _AUDIT_INSTANCE
