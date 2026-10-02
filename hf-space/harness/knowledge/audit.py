"""
Knowledge Audit Logger
======================
Persistent SQLite audit trail for all knowledge actions, reads, writes,
approvals, syncs, and security denials.
"""
from __future__ import annotations

import json
import logging
import os
import sqlite3
import time
from pathlib import Path
from typing import Dict, Any, List, Optional

logger = logging.getLogger("KnowledgeAudit")


def get_default_audit_db_path() -> Path:
    env_path = os.getenv("KNOWLEDGE_AUDIT_DB")
    if env_path:
        return Path(env_path)
    base = Path("/data/hermes")
    if not base.exists() and not (Path("/data").exists() and os.access("/data", os.W_OK)):
        base = Path("./data/hermes")
    base.mkdir(parents=True, exist_ok=True)
    return base / "knowledge_audit.sqlite"


class KnowledgeAuditLogger:
    def __init__(self, db_path: Optional[Path] = None):
        self.db_path = db_path or get_default_audit_db_path()
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    def _get_conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(str(self.db_path), timeout=10.0)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA synchronous=NORMAL;")
        return conn

    def _init_db(self):
        with self._get_conn() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS audit_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_type TEXT NOT NULL,
                    connector TEXT NOT NULL,
                    source_id TEXT,
                    operation TEXT,
                    details_json TEXT,
                    status TEXT NOT NULL,
                    timestamp REAL NOT NULL
                );
            """)
            conn.execute("CREATE INDEX IF NOT EXISTS idx_audit_connector ON audit_events(connector);")
            conn.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(timestamp);")
            conn.commit()

    def log(
        self,
        event_type: str,
        connector: str,
        source_id: Optional[str] = None,
        operation: Optional[str] = None,
        details: Optional[Dict[str, Any]] = None,
        status: str = "ok",
    ):
        """Records an audit event safely without logging sensitive secrets."""
        clean_details = dict(details or {})
        # Scrub secret keys if accidentally present
        for key in list(clean_details.keys()):
            if any(s in key.lower() for s in ["token", "secret", "password", "api_key"]):
                clean_details[key] = "[REDACTED]"

        ts = time.time()
        try:
            with self._get_conn() as conn:
                conn.execute("""
                    INSERT INTO audit_events (event_type, connector, source_id, operation, details_json, status, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """, (
                    event_type,
                    connector,
                    source_id,
                    operation,
                    json.dumps(clean_details, ensure_ascii=False),
                    status,
                    ts,
                ))
                conn.commit()
        except Exception as e:
            logger.error(f"Failed to record knowledge audit event: {e}")

    def get_recent_events(self, limit: int = 50, connector: Optional[str] = None) -> List[Dict[str, Any]]:
        """Retrieves recent audit events."""
        query = "SELECT * FROM audit_events"
        params = []
        if connector:
            query += " WHERE connector = ?"
            params.append(connector)
        query += " ORDER BY timestamp DESC LIMIT ?"
        params.append(limit)

        with self._get_conn() as conn:
            cursor = conn.execute(query, params)
            rows = cursor.fetchall()

        events = []
        for r in rows:
            events.append({
                "id": r["id"],
                "event_type": r["event_type"],
                "connector": r["connector"],
                "source_id": r["source_id"],
                "operation": r["operation"],
                "details": json.loads(r["details_json"]) if r["details_json"] else {},
                "status": r["status"],
                "timestamp": r["timestamp"],
            })
        return events
