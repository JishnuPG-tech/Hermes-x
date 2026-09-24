"""
Persistent Skill Store
======================
Durable SQLite persistence for Hermes Skills and activation state across gateway reboots.
"""
from __future__ import annotations

import os
import sqlite3
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional


@dataclass
class SkillRecord:
    id: str
    name: str
    category: str
    description: str
    instructions: str
    source_type: str = "builtin"  # builtin, github, custom, file
    source_url: str = ""
    author: str = "Hermes Core"
    version: str = "1.0.0"
    created_at: float = 0.0
    updated_at: float = 0.0

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class PersistentSkillStore:
    _instance: Optional[PersistentSkillStore] = None

    def __init__(self, db_path: Optional[Path | str] = None):
        if db_path is None:
            data_dir = Path(os.getenv("HERMES_DATA_DIR", "data/hermes"))
            data_dir.mkdir(parents=True, exist_ok=True)
            self.db_path = data_dir / "skills.db"
        else:
            self.db_path = Path(db_path)
            self.db_path.parent.mkdir(parents=True, exist_ok=True)

        self._init_db()

    @classmethod
    def get_instance(cls, db_path: Optional[Path | str] = None) -> PersistentSkillStore:
        if cls._instance is None:
            cls._instance = PersistentSkillStore(db_path)
        return cls._instance

    def _get_conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(str(self.db_path), timeout=15.0)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self) -> None:
        with self._get_conn() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS skills (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    instructions TEXT NOT NULL,
                    source_type TEXT NOT NULL,
                    source_url TEXT DEFAULT '',
                    author TEXT DEFAULT 'Hermes Core',
                    version TEXT DEFAULT '1.0.0',
                    created_at REAL NOT NULL,
                    updated_at REAL NOT NULL
                )
            """)
            conn.execute("""
                CREATE TABLE IF NOT EXISTS skill_activations (
                    session_id TEXT NOT NULL,
                    skill_id TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    updated_at REAL NOT NULL,
                    PRIMARY KEY (session_id, skill_id)
                )
            """)
            conn.commit()

    def save_skill(self, record: SkillRecord) -> SkillRecord:
        now = time.time()
        if record.created_at <= 0:
            record.created_at = now
        record.updated_at = now

        with self._get_conn() as conn:
            conn.execute("""
                INSERT INTO skills (
                    id, name, category, description, instructions,
                    source_type, source_url, author, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name=excluded.name,
                    category=excluded.category,
                    description=excluded.description,
                    instructions=excluded.instructions,
                    source_type=excluded.source_type,
                    source_url=excluded.source_url,
                    author=excluded.author,
                    version=excluded.version,
                    updated_at=excluded.updated_at
            """, (
                record.id,
                record.name,
                record.category,
                record.description,
                record.instructions,
                record.source_type,
                record.source_url,
                record.author,
                record.version,
                record.created_at,
                record.updated_at,
            ))
            conn.commit()
        return record

    def get_skill(self, skill_id: str) -> Optional[SkillRecord]:
        clean_id = skill_id.strip().lower()
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM skills WHERE id = ?", (clean_id,)).fetchone()
            if not row:
                return None
            return SkillRecord(
                id=row["id"],
                name=row["name"],
                category=row["category"],
                description=row["description"],
                instructions=row["instructions"],
                source_type=row["source_type"],
                source_url=row["source_url"] or "",
                author=row["author"] or "Hermes Core",
                version=row["version"] or "1.0.0",
                created_at=row["created_at"],
                updated_at=row["updated_at"],
            )

    def list_skills(self) -> List[SkillRecord]:
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM skills ORDER BY name ASC").fetchall()
            results = []
            for row in rows:
                results.append(SkillRecord(
                    id=row["id"],
                    name=row["name"],
                    category=row["category"],
                    description=row["description"],
                    instructions=row["instructions"],
                    source_type=row["source_type"],
                    source_url=row["source_url"] or "",
                    author=row["author"] or "Hermes Core",
                    version=row["version"] or "1.0.0",
                    created_at=row["created_at"],
                    updated_at=row["updated_at"],
                ))
            return results

    def delete_skill(self, skill_id: str) -> bool:
        clean_id = skill_id.strip().lower()
        with self._get_conn() as conn:
            cursor = conn.execute("DELETE FROM skills WHERE id = ? AND source_type != 'builtin'", (clean_id,))
            conn.execute("DELETE FROM skill_activations WHERE skill_id = ?", (clean_id,))
            conn.commit()
            return cursor.rowcount > 0

    def set_activation(self, skill_id: str, is_active: bool, session_id: str = "global") -> bool:
        clean_id = skill_id.strip().lower()
        sid = (session_id or "global").strip()

        # Check if skill exists
        if not self.get_skill(clean_id):
            return False

        now = time.time()
        with self._get_conn() as conn:
            conn.execute("""
                INSERT INTO skill_activations (session_id, skill_id, is_active, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(session_id, skill_id) DO UPDATE SET
                    is_active=excluded.is_active,
                    updated_at=excluded.updated_at
            """, (sid, clean_id, 1 if is_active else 0, now))
            conn.commit()
        return True

    def is_active(self, skill_id: str, session_id: str = "global") -> bool:
        clean_id = skill_id.strip().lower()
        sid = (session_id or "global").strip()

        with self._get_conn() as conn:
            # Check session level first
            if sid != "global":
                row = conn.execute(
                    "SELECT is_active FROM skill_activations WHERE session_id = ? AND skill_id = ?",
                    (sid, clean_id)
                ).fetchone()
                if row is not None:
                    return bool(row["is_active"])

            # Fall back to global activation
            row = conn.execute(
                "SELECT is_active FROM skill_activations WHERE session_id = 'global' AND skill_id = ?",
                (clean_id,)
            ).fetchone()
            if row is not None:
                return bool(row["is_active"])

        return False

    def get_active_skills(self, session_id: str = "global") -> List[str]:
        sid = (session_id or "global").strip()
        active = set()

        with self._get_conn() as conn:
            # Global actives
            for row in conn.execute(
                "SELECT skill_id FROM skill_activations WHERE session_id = 'global' AND is_active = 1"
            ).fetchall():
                active.add(row["skill_id"])

            # Session overrides
            if sid != "global":
                for row in conn.execute(
                    "SELECT skill_id, is_active FROM skill_activations WHERE session_id = ?",
                    (sid,)
                ).fetchall():
                    if row["is_active"]:
                        active.add(row["skill_id"])
                    else:
                        active.discard(row["skill_id"])

        return list(active)

    def seed_builtins(self, builtins_dict: Dict[str, Dict[str, Any]]) -> None:
        now = time.time()
        for s_id, s_data in builtins_dict.items():
            clean_id = s_id.strip().lower()
            existing = self.get_skill(clean_id)
            if not existing:
                rec = SkillRecord(
                    id=clean_id,
                    name=s_data.get("name", clean_id.replace("-", " ").title()),
                    category=s_data.get("category", "Built-in"),
                    description=s_data.get("description", ""),
                    instructions=s_data.get("instructions", s_data.get("description", "")),
                    source_type="builtin",
                    source_url="",
                    author="Hermes Core",
                    version="1.0.0",
                    created_at=now,
                    updated_at=now,
                )
                self.save_skill(rec)
