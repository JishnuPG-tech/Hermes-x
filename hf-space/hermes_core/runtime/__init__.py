"""
Hermes Agent Runtime Package
The sovereign, canonical autonomous agent runtime.
"""
from hermes_core.runtime.models import (
    AgentEvent,
    AgentLifecycleState,
    ExecutionContext,
    ToolMetadata,
    ToolResult,
    ToolCategory,
    TaskPlan,
    VerificationResult,
)
from hermes_core.runtime.errors import (
    HermesRuntimeError,
    AuthenticationRequiredError,
    AuthorizationDeniedError,
    ApprovalRequiredError,
    ToolNotFoundError,
    ToolExecutionFailedError,
    ToolLoopDetectedError,
    ModelUnavailableError,
    ModelTimeoutError,
    VerificationFailedError,
)
from hermes_core.runtime.events import RuntimeEventBus
from hermes_core.runtime.tool_discovery import capability_index, CapabilityIndex
from hermes_core.runtime.tool_controller import tool_executor, ToolExecutor
from hermes_core.runtime.verifier import verification_gate, VerificationGate
from hermes_core.runtime.planner import execution_planner, ExecutionPlanner
from hermes_core.runtime.context import ContextBuilder
from hermes_core.runtime.agent_loop import AutonomousAgentLoop
from hermes_core.runtime.agent_runtime import AgentRuntime

__all__ = [
    "AgentRuntime",
    "AutonomousAgentLoop",
    "ContextBuilder",
    "ExecutionPlanner",
    "execution_planner",
    "VerificationGate",
    "verification_gate",
    "ToolExecutor",
    "tool_executor",
    "CapabilityIndex",
    "capability_index",
    "RuntimeEventBus",
    "AgentEvent",
    "AgentLifecycleState",
    "ExecutionContext",
    "ToolMetadata",
    "ToolResult",
    "ToolCategory",
    "TaskPlan",
    "VerificationResult",
    "HermesRuntimeError",
    "AuthenticationRequiredError",
    "AuthorizationDeniedError",
    "ApprovalRequiredError",
    "ToolNotFoundError",
    "ToolExecutionFailedError",
    "ToolLoopDetectedError",
    "ModelUnavailableError",
    "ModelTimeoutError",
    "VerificationFailedError",
]
