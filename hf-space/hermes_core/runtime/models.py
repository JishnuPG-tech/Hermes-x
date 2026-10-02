"""
Hermes Agent Runtime Data Contracts & Models
Authoritative, structured models for agent execution, capabilities, events, and results.
"""
from __future__ import annotations

import time
import uuid
from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional, Set


class AgentLifecycleState(str, Enum):
    IDLE = "idle"
    THINKING = "thinking"
    DISCOVERING = "discovering"
    PLANNING = "planning"
    EXECUTING = "executing"
    WAITING_APPROVAL = "waiting_approval"
    VERIFYING = "verifying"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class ToolCategory(str, Enum):
    FILESYSTEM = "filesystem"
    SYSTEM = "system"
    WEB = "web"
    KNOWLEDGE = "knowledge"
    MEMORY = "memory"
    CODING = "coding"
    BROWSER = "browser"
    TASK = "task"
    SKILL = "skill"


@dataclass
class ToolMetadata:
    """Structured capability metadata for model discovery and security enforcement."""
    name: str
    description: str
    input_schema: Dict[str, Any]
    category: ToolCategory = ToolCategory.SYSTEM
    permissions: List[str] = field(default_factory=list)
    side_effects: bool = False
    reversible: bool = True
    requires_approval: bool = False
    supports_parallel: bool = True
    timeout_seconds: int = 120
    cost_class: str = "standard"
    availability: bool = True
    version: str = "1.0.0"

    def to_openai_schema(self) -> Dict[str, Any]:
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.input_schema,
            }
        }


@dataclass
class ToolResult:
    """Structured, truthful outcome of a tool execution. Never faked."""
    success: bool
    tool: str
    result: Any = None
    error: Optional[Dict[str, Any]] = None
    metadata: Dict[str, Any] = field(default_factory=dict)
    artifacts: List[Dict[str, Any]] = field(default_factory=list)
    verification_evidence: Optional[Dict[str, Any]] = None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "success": self.success,
            "tool": self.tool,
            "result": self.result,
            "error": self.error,
            "metadata": self.metadata,
            "artifacts": self.artifacts,
            "verification_evidence": self.verification_evidence,
        }

    def to_content_string(self) -> str:
        """String representation fed back to the LLM observation window."""
        import json
        if self.success:
            if isinstance(self.result, str):
                return self.result
            try:
                return json.dumps(self.result, ensure_ascii=False, indent=2)
            except Exception:
                return str(self.result)
        else:
            err_msg = self.error.get("message") if isinstance(self.error, dict) else str(self.error)
            err_code = self.error.get("code") if isinstance(self.error, dict) else "TOOL_ERROR"
            return f"[ERROR: {err_code}] {err_msg}"


@dataclass
class ExecutionContext:
    """Security and environmental context bound to every tool and agent run."""
    user_id: str
    session_id: str
    project_id: str = "default"
    workspace_path: str = "/data"
    permissions: Set[str] = field(default_factory=lambda: {"filesystem.read", "filesystem.write", "system.command", "knowledge.read", "web.search"})
    active_skills: List[str] = field(default_factory=list)
    auth_tokens: Dict[str, str] = field(default_factory=dict)
    is_admin: bool = False
    task_id: Optional[str] = None
    voice_session_id: Optional[str] = None


@dataclass
class AgentEvent:
    """Canonical event model for real-time streaming, Activity feeds, and audits."""
    type: str
    status: str
    user_id: str
    session_id: str
    event_id: str = field(default_factory=lambda: f"evt_{uuid.uuid4().hex[:12]}")
    timestamp: float = field(default_factory=time.time)
    project_id: str = "default"
    task_id: Optional[str] = None
    agent_id: str = "hermes-agent"
    tool_call_id: Optional[str] = None
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "event_id": self.event_id,
            "timestamp": self.timestamp,
            "user_id": self.user_id,
            "session_id": self.session_id,
            "project_id": self.project_id,
            "task_id": self.task_id,
            "agent_id": self.agent_id,
            "tool_call_id": self.tool_call_id,
            "type": self.type,
            "status": self.status,
            "metadata": self.metadata,
        }


@dataclass
class PlanStep:
    id: str
    objective: str
    tool: Optional[str] = None
    arguments: Optional[Dict[str, Any]] = None
    dependencies: List[str] = field(default_factory=list)
    completed: bool = False
    result: Optional[ToolResult] = None


@dataclass
class TaskPlan:
    objective: str
    steps: List[PlanStep] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    is_durable: bool = False
    harness_task_id: Optional[str] = None


@dataclass
class VerificationResult:
    passed: bool
    evidence: Dict[str, Any]
    reason: str
    timestamp: float = field(default_factory=time.time)

    @property
    def verified(self) -> bool:
        return self.passed

