"""
Durable Task Database Repository
Provides SQLite WAL-backed persistence for tasks, subtasks, steps, checkpoints, and approvals.
"""
from __future__ import annotations

import json
import os
import sqlite3
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from harness.kernel.models import (
    Task,
    Subtask,
    RunStep,
    Checkpoint,
    ApprovalRequest,
    TaskStatus,
    RiskLevel,
    PermissionLevel,
    ApprovalStatus,
)


def get_default_db_path() -> str:
    env_path = os.getenv("HERMES_TASKS_DB")
    if env_path:
        return env_path
    if Path("/data/hermes").is_dir() or (Path("/data").exists() and os.access("/data", os.W_OK)):
        return "/data/hermes/tasks.sqlite"
    # Local fallback for development/testing
    local_dir = Path("./data")
    local_dir.mkdir(parents=True, exist_ok=True)
    return str(local_dir / "tasks.sqlite")


class TaskDB:
    def __init__(self, db_path: Optional[str] = None):
        self.db_path = db_path or get_default_db_path()
        Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)
        self._init_schema()

    def _get_conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path, timeout=10.0)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode = WAL;")
        conn.execute("PRAGMA synchronous = NORMAL;")
        conn.execute("PRAGMA foreign_keys = ON;")
        conn.execute("PRAGMA busy_timeout = 5000;")
        return conn

    def _init_schema(self):
        with self._get_conn() as conn:
            conn.executescript("""
            CREATE TABLE IF NOT EXISTS tasks (
                task_id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                objective TEXT NOT NULL,
                status TEXT NOT NULL,
                risk_level TEXT NOT NULL,
                idempotency_key TEXT UNIQUE,
                parent_task_id TEXT,
                assigned_worker TEXT,
                workspace_path TEXT,
                allowed_tools TEXT NOT NULL,
                allowed_integrations TEXT NOT NULL,
                retry_budget INTEGER NOT NULL,
                retry_count INTEGER NOT NULL,
                checkpoint TEXT,
                created_at REAL NOT NULL,
                updated_at REAL NOT NULL,
                metadata TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS subtasks (
                subtask_id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                role TEXT NOT NULL,
                dependencies TEXT NOT NULL,
                status TEXT NOT NULL,
                acceptance_criteria TEXT NOT NULL,
                verification_command TEXT,
                result TEXT,
                created_at REAL NOT NULL,
                updated_at REAL NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks (task_id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS run_steps (
                step_id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                subtask_id TEXT,
                step_index INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                action_name TEXT NOT NULL,
                input_payload TEXT NOT NULL,
                output_payload TEXT,
                status TEXT NOT NULL,
                error TEXT,
                duration_ms INTEGER,
                timestamp REAL NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks (task_id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS checkpoints (
                checkpoint_id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                completed_subtasks TEXT NOT NULL,
                active_subtask_id TEXT,
                workspace_revision TEXT,
                known_failures TEXT NOT NULL,
                retry_count INTEGER NOT NULL,
                state_payload TEXT NOT NULL,
                created_at REAL NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks (task_id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS approvals (
                approval_id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                action TEXT NOT NULL,
                risk_level TEXT NOT NULL,
                permission_level TEXT NOT NULL,
                reason TEXT NOT NULL,
                parameters TEXT NOT NULL,
                requested_by TEXT NOT NULL,
                requested_at REAL NOT NULL,
                expires_at REAL NOT NULL,
                status TEXT NOT NULL,
                decision_by TEXT,
                decision_at REAL,
                decision_reason TEXT,
                FOREIGN KEY (task_id) REFERENCES tasks (task_id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS task_events (
                event_id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                event_type TEXT NOT NULL,
                actor TEXT NOT NULL,
                status TEXT NOT NULL,
                payload TEXT NOT NULL,
                timestamp REAL NOT NULL,
                FOREIGN KEY (task_id) REFERENCES tasks (task_id) ON DELETE CASCADE
            );

            CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
            CREATE INDEX IF NOT EXISTS idx_tasks_idempotency ON tasks(idempotency_key);
            CREATE INDEX IF NOT EXISTS idx_subtasks_task ON subtasks(task_id);
            CREATE INDEX IF NOT EXISTS idx_steps_task ON run_steps(task_id);
            CREATE INDEX IF NOT EXISTS idx_approvals_status ON approvals(status);
            CREATE INDEX IF NOT EXISTS idx_events_task ON task_events(task_id, timestamp);
            """)

    # ── Task CRUD ───────────────────────────────────────────────
    def save_task(self, task: Task) -> None:
        task.updated_at = time.time()
        with self._get_conn() as conn:
            conn.execute("""
            INSERT INTO tasks (
                task_id, project_id, objective, status, risk_level, idempotency_key,
                parent_task_id, assigned_worker, workspace_path, allowed_tools,
                allowed_integrations, retry_budget, retry_count, checkpoint,
                created_at, updated_at, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(task_id) DO UPDATE SET
                status = excluded.status,
                risk_level = excluded.risk_level,
                parent_task_id = excluded.parent_task_id,
                assigned_worker = excluded.assigned_worker,
                workspace_path = excluded.workspace_path,
                allowed_tools = excluded.allowed_tools,
                allowed_integrations = excluded.allowed_integrations,
                retry_budget = excluded.retry_budget,
                retry_count = excluded.retry_count,
                checkpoint = excluded.checkpoint,
                updated_at = excluded.updated_at,
                metadata = excluded.metadata;
            """, (
                task.task_id,
                task.project_id,
                task.objective,
                task.status.value,
                task.risk_level.value,
                task.idempotency_key,
                task.parent_task_id,
                task.assigned_worker,
                task.workspace_path,
                json.dumps(task.allowed_tools),
                json.dumps(task.allowed_integrations),
                task.retry_budget,
                task.retry_count,
                task.checkpoint,
                task.created_at,
                task.updated_at,
                json.dumps(task.metadata),
            ))

    def get_task(self, task_id: str) -> Optional[Task]:
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM tasks WHERE task_id = ?", (task_id,)).fetchone()
            if not row:
                return None
            return self._row_to_task(row)

    def get_task_by_idempotency_key(self, key: str) -> Optional[Task]:
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM tasks WHERE idempotency_key = ?", (key,)).fetchone()
            if not row:
                return None
            return self._row_to_task(row)

    def list_tasks(self, project_id: Optional[str] = None, status: Optional[TaskStatus] = None, limit: int = 50) -> List[Task]:
        query = "SELECT * FROM tasks WHERE 1=1"
        params: List[Any] = []
        if project_id:
            query += " AND project_id = ?"
            params.append(project_id)
        if status:
            query += " AND status = ?"
            params.append(status.value)
        query += " ORDER BY created_at DESC LIMIT ?"
        params.append(limit)

        with self._get_conn() as conn:
            rows = conn.execute(query, params).fetchall()
            return [self._row_to_task(r) for r in rows]

    def _row_to_task(self, row: sqlite3.Row) -> Task:
        return Task(
            task_id=row["task_id"],
            project_id=row["project_id"],
            objective=row["objective"],
            status=TaskStatus(row["status"]),
            risk_level=RiskLevel(row["risk_level"]),
            idempotency_key=row["idempotency_key"],
            parent_task_id=row["parent_task_id"],
            assigned_worker=row["assigned_worker"],
            workspace_path=row["workspace_path"],
            allowed_tools=json.loads(row["allowed_tools"]),
            allowed_integrations=json.loads(row["allowed_integrations"]),
            retry_budget=row["retry_budget"],
            retry_count=row["retry_count"],
            checkpoint=row["checkpoint"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
            metadata=json.loads(row["metadata"]),
        )

    # ── Subtasks CRUD ───────────────────────────────────────────
    def save_subtasks(self, subtasks: List[Subtask]) -> None:
        if not subtasks:
            return
        now = time.time()
        with self._get_conn() as conn:
            for s in subtasks:
                s.updated_at = now
                conn.execute("""
                INSERT INTO subtasks (
                    subtask_id, task_id, title, description, role, dependencies,
                    status, acceptance_criteria, verification_command, result,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(subtask_id) DO UPDATE SET
                    title = excluded.title,
                    description = excluded.description,
                    role = excluded.role,
                    dependencies = excluded.dependencies,
                    status = excluded.status,
                    acceptance_criteria = excluded.acceptance_criteria,
                    verification_command = excluded.verification_command,
                    result = excluded.result,
                    updated_at = excluded.updated_at;
                """, (
                    s.subtask_id,
                    s.task_id,
                    s.title,
                    s.description,
                    s.role,
                    json.dumps(s.dependencies),
                    s.status.value,
                    json.dumps(s.acceptance_criteria),
                    s.verification_command,
                    s.result,
                    s.created_at,
                    s.updated_at,
                ))

    def get_subtasks(self, task_id: str) -> List[Subtask]:
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM subtasks WHERE task_id = ? ORDER BY created_at ASC", (task_id,)).fetchall()
            return [
                Subtask(
                    subtask_id=r["subtask_id"],
                    task_id=r["task_id"],
                    title=r["title"],
                    description=r["description"],
                    role=r["role"],
                    dependencies=json.loads(r["dependencies"]),
                    status=TaskStatus(r["status"]),
                    acceptance_criteria=json.loads(r["acceptance_criteria"]),
                    verification_command=r["verification_command"],
                    result=r["result"],
                    created_at=r["created_at"],
                    updated_at=r["updated_at"],
                )
                for r in rows
            ]

    # ── Steps & Checkpoints ─────────────────────────────────────
    def record_step(self, step: RunStep) -> None:
        with self._get_conn() as conn:
            conn.execute("""
            INSERT INTO run_steps (
                step_id, task_id, subtask_id, step_index, action_type, action_name,
                input_payload, output_payload, status, error, duration_ms, timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """, (
                step.step_id,
                step.task_id,
                step.subtask_id,
                step.step_index,
                step.action_type,
                step.action_name,
                json.dumps(step.input_payload),
                json.dumps(step.output_payload) if step.output_payload is not None else None,
                step.status,
                step.error,
                step.duration_ms,
                step.timestamp,
            ))

    def get_steps(self, task_id: str) -> List[RunStep]:
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM run_steps WHERE task_id = ? ORDER BY step_index ASC", (task_id,)).fetchall()
            return [
                RunStep(
                    step_id=r["step_id"],
                    task_id=r["task_id"],
                    subtask_id=r["subtask_id"],
                    step_index=r["step_index"],
                    action_type=r["action_type"],
                    action_name=r["action_name"],
                    input_payload=json.loads(r["input_payload"]),
                    output_payload=json.loads(r["output_payload"]) if r["output_payload"] else None,
                    status=r["status"],
                    error=r["error"],
                    duration_ms=r["duration_ms"],
                    timestamp=r["timestamp"],
                )
                for r in rows
            ]

    def save_checkpoint(self, checkpoint: Checkpoint) -> None:
        with self._get_conn() as conn:
            conn.execute("""
            INSERT INTO checkpoints (
                checkpoint_id, task_id, completed_subtasks, active_subtask_id,
                workspace_revision, known_failures, retry_count, state_payload, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """, (
                checkpoint.checkpoint_id,
                checkpoint.task_id,
                json.dumps(checkpoint.completed_subtasks),
                checkpoint.active_subtask_id,
                checkpoint.workspace_revision,
                json.dumps(checkpoint.known_failures),
                checkpoint.retry_count,
                json.dumps(checkpoint.state_payload),
                checkpoint.created_at,
            ))
            conn.execute("UPDATE tasks SET checkpoint = ?, updated_at = ? WHERE task_id = ?", (checkpoint.checkpoint_id, time.time(), checkpoint.task_id))

    def get_latest_checkpoint(self, task_id: str) -> Optional[Checkpoint]:
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM checkpoints WHERE task_id = ? ORDER BY created_at DESC LIMIT 1", (task_id,)).fetchone()
            if not row:
                return None
            return Checkpoint(
                checkpoint_id=row["checkpoint_id"],
                task_id=row["task_id"],
                completed_subtasks=json.loads(row["completed_subtasks"]),
                active_subtask_id=row["active_subtask_id"],
                workspace_revision=row["workspace_revision"],
                known_failures=json.loads(row["known_failures"]),
                retry_count=row["retry_count"],
                state_payload=json.loads(row["state_payload"]),
                created_at=row["created_at"],
            )

    # ── Approvals CRUD ──────────────────────────────────────────
    def create_approval(self, request: ApprovalRequest) -> None:
        with self._get_conn() as conn:
            conn.execute("""
            INSERT INTO approvals (
                approval_id, task_id, action, risk_level, permission_level,
                reason, parameters, requested_by, requested_at, expires_at,
                status, decision_by, decision_at, decision_reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """, (
                request.approval_id,
                request.task_id,
                request.action,
                request.risk_level.value,
                request.permission_level.value,
                request.reason,
                json.dumps(request.parameters),
                request.requested_by,
                request.requested_at,
                request.expires_at,
                request.status.value,
                request.decision_by,
                request.decision_at,
                request.decision_reason,
            ))

    def get_approval(self, approval_id: str) -> Optional[ApprovalRequest]:
        with self._get_conn() as conn:
            row = conn.execute("SELECT * FROM approvals WHERE approval_id = ?", (approval_id,)).fetchone()
            if not row:
                return None
            return ApprovalRequest(
                approval_id=row["approval_id"],
                task_id=row["task_id"],
                action=row["action"],
                risk_level=RiskLevel(row["risk_level"]),
                permission_level=PermissionLevel(row["permission_level"]),
                reason=row["reason"],
                parameters=json.loads(row["parameters"]),
                requested_by=row["requested_by"],
                requested_at=row["requested_at"],
                expires_at=row["expires_at"],
                status=ApprovalStatus(row["status"]),
                decision_by=row["decision_by"],
                decision_at=row["decision_at"],
                decision_reason=row["decision_reason"],
            )

    def list_pending_approvals(self) -> List[ApprovalRequest]:
        now = time.time()
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM approvals WHERE status = ? AND expires_at > ? ORDER BY requested_at ASC", (ApprovalStatus.PENDING.value, now)).fetchall()
            return [
                ApprovalRequest(
                    approval_id=r["approval_id"],
                    task_id=r["task_id"],
                    action=r["action"],
                    risk_level=RiskLevel(r["risk_level"]),
                    permission_level=PermissionLevel(r["permission_level"]),
                    reason=r["reason"],
                    parameters=json.loads(r["parameters"]),
                    requested_by=r["requested_by"],
                    requested_at=r["requested_at"],
                    expires_at=r["expires_at"],
                    status=ApprovalStatus(r["status"]),
                    decision_by=r["decision_by"],
                    decision_at=r["decision_at"],
                    decision_reason=r["decision_reason"],
                )
                for r in rows
            ]

    def update_approval(self, approval_id: str, status: ApprovalStatus, decision_by: str, reason: Optional[str] = None) -> bool:
        with self._get_conn() as conn:
            res = conn.execute("""
            UPDATE approvals SET
                status = ?,
                decision_by = ?,
                decision_at = ?,
                decision_reason = ?
            WHERE approval_id = ? AND status = ?;
            """, (status.value, decision_by, time.time(), reason, approval_id, ApprovalStatus.PENDING.value))
            return res.rowcount > 0

    # ── Events Log ──────────────────────────────────────────────
    def append_event(self, event_id: str, task_id: str, event_type: str, actor: str, status: str, payload: Dict[str, Any]) -> None:
        with self._get_conn() as conn:
            conn.execute("""
            INSERT INTO task_events (event_id, task_id, event_type, actor, status, payload, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """, (event_id, task_id, event_type, actor, status, json.dumps(payload), time.time()))

    def get_events(self, task_id: str, limit: int = 200) -> List[Dict[str, Any]]:
        with self._get_conn() as conn:
            rows = conn.execute("SELECT * FROM task_events WHERE task_id = ? ORDER BY timestamp ASC LIMIT ?", (task_id, limit)).fetchall()
            return [
                {
                    "event_id": r["event_id"],
                    "task_id": r["task_id"],
                    "event_type": r["event_type"],
                    "actor": r["actor"],
                    "status": r["status"],
                    "payload": json.loads(r["payload"]),
                    "timestamp": r["timestamp"],
                }
                for r in rows
            ]
