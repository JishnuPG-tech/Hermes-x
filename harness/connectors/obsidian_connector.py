"""
Obsidian Knowledge Connector
Allows Agent OS to query notes and persist verified task decisions and knowledge graphs.
"""
from __future__ import annotations

import os
import re
import time
from pathlib import Path
from typing import List, Dict, Any, Optional


def get_default_vault_path() -> Path:
    env_vault = os.getenv("OBSIDIAN_VAULT_DIR")
    if env_vault:
        return Path(env_vault)
    if Path("/data/obsidian/vault").is_dir() or (Path("/data").exists() and os.access("/data", os.W_OK)):
        return Path("/data/obsidian/vault")
    return Path("./data/obsidian/vault")


class ObsidianConnector:
    def __init__(self, vault_dir: Optional[Path] = None):
        self.vault_dir = vault_dir or get_default_vault_path()
        self.vault_dir.mkdir(parents=True, exist_ok=True)

    def list_notes(self) -> List[Dict[str, Any]]:
        """List all markdown notes in the vault."""
        notes = []
        for p in self.vault_dir.rglob("*.md"):
            notes.append({
                "title": p.stem,
                "relative_path": str(p.relative_to(self.vault_dir)).replace("\\", "/"),
                "size": p.stat().st_size,
                "modified": p.stat().st_mtime,
            })
        return notes

    def read_note(self, note_name: str) -> Optional[str]:
        """Read a note by title or relative path."""
        target = self.vault_dir / note_name
        if not target.suffix:
            target = target.with_suffix(".md")
        if target.exists() and target.is_file():
            return target.read_text(encoding="utf-8", errors="replace")

        # Fuzzy search by stem
        for p in self.vault_dir.rglob("*.md"):
            if p.stem.lower() == note_name.lower():
                return p.read_text(encoding="utf-8", errors="replace")
        return None

    def record_task_summary(self, task_id: str, title: str, objective: str, outcome: str, artifacts: List[str]) -> str:
        """Write a verified task summary note with wikilinks into the vault."""
        note_name = f"Task-{task_id}.md"
        target = self.vault_dir / "Tasks" / note_name
        target.parent.mkdir(parents=True, exist_ok=True)

        content = f"""# 📝 Task Record: {title}

**Task ID:** `{task_id}`  
**Date:** {time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime())}  
**Status:** [[Tasks]] &bull; [[Hermes-Agent]]

---

### 🎯 Objective
{objective}

### 🏁 Outcome & Verification
{outcome}

### 📦 Artifacts
"""
        for art in artifacts:
            content += f"- `{art}`\n"

        content += "\n---\n*Recorded automatically by Hermes Agent OS Harness.*"
        target.write_text(content, encoding="utf-8")
        return str(target.relative_to(self.vault_dir)).replace("\\", "/")
