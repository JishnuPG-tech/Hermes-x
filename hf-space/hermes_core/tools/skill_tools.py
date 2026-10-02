"""
Skill Agent Tools
=================
Exposes dynamic skill activation and discovery to Hermes Agent.
"""
from __future__ import annotations

import os
from pathlib import Path
from typing import Optional, Dict, Any, List
from hermes_core.tools.registry import registry

BUILTIN_SKILLS = {
    "python-pro": {
        "name": "python-pro",
        "description": "Master Python 3.12+ with modern features, async programming, performance optimization, and clean architecture.",
    },
    "fastapi-pro": {
        "name": "fastapi-pro",
        "description": "Expert in building high-performance async APIs with FastAPI, Pydantic V2, and SQLAlchemy 2.0.",
    },
    "code-reviewer": {
        "name": "code-reviewer",
        "description": "Elite code review specialist analyzing security, performance, correctness, and clean code practices.",
    },
    "docker-expert": {
        "name": "docker-expert",
        "description": "Containerization expert specializing in multi-stage Docker builds, orchestration, security, and minimal images.",
    },
    "database-architect": {
        "name": "database-architect",
        "description": "Database design and optimization expert for PostgreSQL, SQLite, Redis, and schema modeling.",
    },
    "security-auditor": {
        "name": "security-auditor",
        "description": "Security auditing specialist analyzing OWASP Top 10 vulnerabilities, API security, and privilege escalation.",
    },
    "systematic-debugging": {
        "name": "systematic-debugging",
        "description": "Root-cause diagnosis and debugging specialist tracing stack traces, network failures, and race conditions.",
    },
}

SKILLS_DIR = Path("/data/hermes/skills") if Path("/data/hermes").exists() else Path("/tmp/hermes/skills")
SKILLS_DIR.mkdir(parents=True, exist_ok=True)

ACTIVE_CONVERSATION_SKILLS: Dict[str, List[str]] = {}


@registry.register(
    name="activate_skill",
    description="Activate a specialized domain skill to enhance domain expertise and capabilities.",
    parameters={
        "type": "object",
        "properties": {
            "skill_name": {"type": "string", "description": "The name of the skill to activate"}
        },
        "required": ["skill_name"],
    },
    category="skill",
)
def activate_skill(skill_name: str, context: Optional[Any] = None) -> str:
    sname = skill_name.strip().lower()
    chat_id = getattr(context, "session_id", "default_chat") if context else "default_chat"

    if sname in BUILTIN_SKILLS:
        skill_info = BUILTIN_SKILLS[sname]
        if chat_id not in ACTIVE_CONVERSATION_SKILLS:
            ACTIVE_CONVERSATION_SKILLS[chat_id] = []
        if sname not in ACTIVE_CONVERSATION_SKILLS[chat_id]:
            ACTIVE_CONVERSATION_SKILLS[chat_id].append(sname)
        return f"Skill '{sname}' is now active. Description: {skill_info['description']}"

    custom_skill_file = SKILLS_DIR / sname / "SKILL.md"
    if custom_skill_file.exists():
        content = custom_skill_file.read_text(encoding="utf-8", errors="replace")
        if chat_id not in ACTIVE_CONVERSATION_SKILLS:
            ACTIVE_CONVERSATION_SKILLS[chat_id] = []
        if sname not in ACTIVE_CONVERSATION_SKILLS[chat_id]:
            ACTIVE_CONVERSATION_SKILLS[chat_id].append(sname)
        return f"Custom skill '{sname}' loaded and activated from disk.\n{content[:400]}..."

    return f"Error: Skill '{sname}' not found. Use list_skills to view available skills."


@registry.register(
    name="list_skills",
    description="List all available skills that can be activated on the server.",
    parameters={"type": "object", "properties": {}},
    category="skill",
)
def list_skills(context: Optional[Any] = None) -> str:
    chat_id = getattr(context, "session_id", "default_chat") if context else "default_chat"
    lines = ["Available Built-in Skills:"]
    for k, v in BUILTIN_SKILLS.items():
        lines.append(f"- **{k}**: {v['description']}")

    if SKILLS_DIR.exists():
        disk_skills = [d for d in os.listdir(SKILLS_DIR) if (SKILLS_DIR / d).is_dir()]
        if disk_skills:
            lines.append("\nAvailable Custom Skills on Disk:")
            for ds in disk_skills:
                lines.append(f"- **{ds}**")

    active = ACTIVE_CONVERSATION_SKILLS.get(chat_id, [])
    if active:
        lines.append(f"\nCurrently Active: {', '.join(active)}")
    return "\n".join(lines)
