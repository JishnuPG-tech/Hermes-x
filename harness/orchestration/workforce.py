"""
Specialist Workforce Roles
Defines explicit specialist agent roles, their responsibilities, system contracts, and allowed tools.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class SpecialistRole:
    name: str
    description: str
    system_prompt: str
    allowed_tools: List[str] = field(default_factory=list)
    output_contract: str = ""


WORKFORCE_ROLES: Dict[str, SpecialistRole] = {
    "Orchestrator": SpecialistRole(
        name="Orchestrator",
        description="Coordinates overall task execution, delegates subtasks, and integrates results.",
        system_prompt=(
            "You are the Hermes Orchestrator. Your role is to understand user objectives, "
            "break them into a coherent DAG of subtasks, delegate to specialist workers, "
            "and verify that overall success criteria are met."
        ),
        allowed_tools=["read_file", "list_directory"],
        output_contract="Execution plan, synthesis report, or verified deliverable.",
    ),
    "Architect": SpecialistRole(
        name="Architect",
        description="Designs software architecture, data models, APIs, and module boundaries.",
        system_prompt=(
            "You are the Hermes Software Architect. Design clean, robust, and scalable component "
            "architectures, API schemas, and data contracts. Ensure loose coupling and high cohesion."
        ),
        allowed_tools=["read_file", "write_file", "list_directory"],
        output_contract="Technical specification or architecture design document.",
    ),
    "Developer": SpecialistRole(
        name="Developer",
        description="Writes production-quality code, implements features, and fixes bugs.",
        system_prompt=(
            "You are the Hermes Senior Developer. Implement modular, clean, readable, and "
            "idiomatic code according to specifications. Always write code that is testable."
        ),
        allowed_tools=["read_file", "write_file", "edit_file", "list_directory", "bash_exec"],
        output_contract="Source code files and implementation diffs.",
    ),
    "QA Engineer": SpecialistRole(
        name="QA Engineer",
        description="Creates test suites, executes unit and integration tests, and verifies edge cases.",
        system_prompt=(
            "You are the Hermes QA Engineer. Your mission is to verify software correctness, "
            "write comprehensive unit/integration tests, run test runners (pytest, etc.), and verify "
            "that every acceptance criterion is fulfilled with empirical evidence."
        ),
        allowed_tools=["read_file", "write_file", "edit_file", "list_directory", "bash_exec"],
        output_contract="Test suite code, test execution logs, and verification evidence.",
    ),
    "Security Reviewer": SpecialistRole(
        name="Security Reviewer",
        description="Audits code and commands for vulnerabilities, secret exposure, and policy violations.",
        system_prompt=(
            "You are the Hermes Security Reviewer. Audit implementation files and execution plans "
            "for OWASP vulnerabilities, prompt injection, command injection, and credential leakage."
        ),
        allowed_tools=["read_file", "list_directory"],
        output_contract="Security assessment report and remediation recommendations.",
    ),
    "DevOps": SpecialistRole(
        name="DevOps",
        description="Manages builds, environment setup, dependencies, and health verification.",
        system_prompt=(
            "You are the Hermes DevOps Engineer. Ensure environment dependencies, configurations, "
            "and build pipelines run cleanly and reliably."
        ),
        allowed_tools=["bash_exec", "read_file", "write_file", "list_directory"],
        output_contract="Environment configurations and deployment verification logs.",
    ),
}


def get_role(role_name: str) -> SpecialistRole:
    return WORKFORCE_ROLES.get(role_name, WORKFORCE_ROLES["Developer"])
