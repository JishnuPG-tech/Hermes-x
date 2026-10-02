"""
Hermes Agent Runtime Structured Errors
Specific, typed exceptions mapped to standardized error codes.
"""
from __future__ import annotations
from typing import Optional, Dict, Any


class HermesRuntimeError(Exception):
    """Base exception for all agent runtime errors."""
    def __init__(self, code: str, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(message)
        self.code = code
        self.message = message
        self.details = details or {}

    def to_dict(self) -> Dict[str, Any]:
        return {
            "code": self.code,
            "message": self.message,
            "details": self.details,
        }


class AuthenticationRequiredError(HermesRuntimeError):
    def __init__(self, message: str = "Authentication required to access this resource.", details: Optional[Dict[str, Any]] = None):
        super().__init__("AUTHENTICATION_REQUIRED", message, details)


class AuthorizationDeniedError(HermesRuntimeError):
    def __init__(self, message: str = "Permission denied for this operation.", details: Optional[Dict[str, Any]] = None):
        super().__init__("AUTHORIZATION_DENIED", message, details)


class ApprovalRequiredError(HermesRuntimeError):
    def __init__(self, message: str = "User approval required for sensitive operation.", details: Optional[Dict[str, Any]] = None):
        super().__init__("APPROVAL_REQUIRED", message, details)


class ToolNotFoundError(HermesRuntimeError):
    def __init__(self, tool_name: str, details: Optional[Dict[str, Any]] = None):
        super().__init__("TOOL_NOT_FOUND", f"Tool '{tool_name}' is not registered or not permitted.", details or {"tool": tool_name})


class ToolExecutionFailedError(HermesRuntimeError):
    def __init__(self, tool_name: str, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__("TOOL_EXECUTION_FAILED", f"Tool '{tool_name}' execution failed: {message}", details or {"tool": tool_name})


class ToolLoopDetectedError(HermesRuntimeError):
    def __init__(self, tool_name: str, iterations: int, details: Optional[Dict[str, Any]] = None):
        super().__init__(
            "TOOL_LOOP_DETECTED",
            f"Pathological execution loop detected: '{tool_name}' failed repeatedly with identical parameters across {iterations} iterations.",
            details or {"tool": tool_name, "iterations": iterations}
        )


class ModelUnavailableError(HermesRuntimeError):
    def __init__(self, model_name: str, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__("MODEL_UNAVAILABLE", f"Model '{model_name}' is unavailable: {message}", details or {"model": model_name})


class ModelTimeoutError(HermesRuntimeError):
    def __init__(self, model_name: str, timeout_seconds: float, details: Optional[Dict[str, Any]] = None):
        super().__init__("MODEL_TIMEOUT", f"Model '{model_name}' timed out after {timeout_seconds}s.", details or {"model": model_name, "timeout": timeout_seconds})


class VerificationFailedError(HermesRuntimeError):
    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__("VERIFICATION_FAILED", f"Empirical verification failed: {message}", details)


class UntrustedInputError(HermesRuntimeError):
    def __init__(self, message: str = "Input violated security boundary.", details: Optional[Dict[str, Any]] = None):
        super().__init__("UNTRUSTED_INPUT", message, details)
