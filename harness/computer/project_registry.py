"""
Server Computer Project Registry
================================
Maintains persistent records of all registered software projects:
- Maps project_id to local working path and GitHub repository.
- Persists to SQLite (/data/jarvis/databases/projects.sqlite) in WAL mode.
- Enforces branch policies and access boundaries.
"""
from __future__ import annotations

import os
import sqlite3
import time
from pathlib import Path
from typing import Dict, Any, List, Optional

from harness.computer.models import ProjectRecord


def get_projects_db_path() -> Path:
    env_path = os.getenv("JARVIS_PROJECTS_DB")
    if env_path:
        return Path(env_path)
    base = Path("/data/jarvis/databases")
    if not base.exists() and not (Path("/data").exists() and os.access("/data", os.W_OK)):
        base = Path("./data/jarvis/databases")
    base.mkdir(parents=True, exist_ok=True)
    return base / "projects.sqlite"


class ProjectRegistry:
    def __init__(self, db_path: Optional[Path] = None):
        self.db_path = db_path or get_projects_db_path()
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    def _get_conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(str(self.db_path), timeout=10.0)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL;")
        return conn

    def _init_db(self):
        with self._get_conn() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    project_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    path TEXT NOT NULL,
                    github_repo TEXT,
                    default_branch TEXT DEFAULT 'main',
                    policy TEXT DEFAULT 'standard-development',
                    description TEXT,
                    created_at REAL NOT NULL,
                    last_active_at REAL NOT NULL
                );
            """)
            conn.commit()

    def register_project(
        self,
        project_id: str,
        name: Optional[str] = None,
        path: Optional[str] = None,
        github_repo: Optional[str] = None,
        default_branch: str = "main",
        policy: str = "standard-development",
        description: Optional[str] = None,
    ) -> ProjectRecord:
        """Registers or updates a project in the durable registry."""
        proj_name = name or project_id
        clean_id = project_id.lower().replace(" ", "-").strip()
        default_path = path or f"/data/jarvis/projects/{clean_id}"

        now = time.time()
        record = ProjectRecord(
            project_id=clean_id,
            name=proj_name,
            path=default_path,
            github_repo=github_repo,
            default_branch=default_branch,
            policy=policy,
            description=description,
            created_at=now,
            last_active_at=now,
        )

        with self._get_conn() as conn:
            conn.execute("""
                INSERT INTO projects (project_id, name, path, github_repo, default_branch, policy, description, created_at, last_active_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(project_id) DO UPDATE SET
                    name = excluded.name,
                    path = excluded.path,
                    github_repo = excluded.github_repo,
                    default_branch = excluded.default_branch,
                    policy = excluded.policy,
                    description = excluded.description,
                    last_active_at = excluded.last_active_at
            """, (
                record.project_id,
                record.name,
                record.path,
                record.github_repo,
                record.default_branch,
                record.policy,
                record.description,
                record.created_at,
                record.last_active_at,
            ))
            conn.commit()

        return record

    def get_project(self, project_id: str) -> Optional[ProjectRecord]:
        """Retrieves a project record by ID."""
        clean_id = project_id.lower().strip()
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM projects WHERE project_id = ?", (clean_id,)).fetchone()
            if row:
                return ProjectRecord(
                    project_id=row["project_id"],
                    name=row["name"],
                    path=row["path"],
                    github_repo=row["github_repo"],
                    default_branch=row["default_branch"],
                    policy=row["policy"],
                    description=row["description"],
                    created_at=row["created_at"],
                    last_active_at=row["last_active_at"],
                )
        return None

    def list_projects(self) -> List[ProjectRecord]:
        """Returns all registered projects."""
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM projects ORDER BY last_active_at DESC").fetchall()
            return [
                ProjectRecord(
                    project_id=r["project_id"],
                    name=r["name"],
                    path=r["path"],
                    github_repo=r["github_repo"],
                    default_branch=r["default_branch"],
                    policy=r["policy"],
                    description=r["description"],
                    created_at=r["created_at"],
                    last_active_at=r["last_active_at"],
                )
                for r in rows
            ]

    def touch_project(self, project_id: str):
        """Updates last_active_at timestamp."""
        with self._get_conn() as conn:
            conn.execute("UPDATE projects SET last_active_at = ? WHERE project_id = ?", (time.time(), project_id.lower()))
            conn.commit()

    def delete_project(self, project_id: str) -> bool:
        """Removes a project from the registry."""
        with self._get_conn() as conn:
            cursor = conn.execute("DELETE FROM projects WHERE project_id = ?", (project_id.lower(),))
            conn.commit()
            return cursor.rowcount > 0
