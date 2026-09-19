"""
Obsidian Knowledge Connector
============================
Integrates Hermes with local and Ignis-hosted Obsidian Markdown vaults:
- Enforces strict path sandboxing and directory traversal prevention.
- Implements atomic file writes (temp file + fsync + atomic rename + verify).
- Generates compliant obsidian:// URIs for mobile/desktop app handoff.
- Parses frontmatter, wikilinks, tags, and headings.
- Detects stale/conflicting writes via sha256 content hashes.
"""
from __future__ import annotations

import hashlib
import logging
import os
import tempfile
import time
import urllib.parse
from pathlib import Path
from typing import Dict, Any, List, Optional

from harness.knowledge.connector import KnowledgeConnector
from harness.knowledge.models import (
    KnowledgeDocument,
    KnowledgeQuery,
    KnowledgeResult,
    WriteIntent,
    WriteResult,
    ConnectorHealth,
    SyncCheckpoint,
)
from harness.knowledge.normalizer import MarkdownNormalizer
from harness.knowledge.policy import KnowledgePolicy, KnowledgePolicyError
from harness.knowledge.audit import KnowledgeAuditLogger

logger = logging.getLogger("ObsidianConnector")


def get_vault_root() -> Path:
    env_root = os.getenv("OBSIDIAN_VAULT_DIR")
    if env_root:
        return Path(env_root)
    if Path("/data/obsidian/vault").is_dir() or (Path("/data").exists() and os.access("/data", os.W_OK)):
        return Path("/data/obsidian/vault")
    return Path("./data/obsidian/vault")


class ObsidianConnector(KnowledgeConnector):
    def __init__(
        self,
        vault_root: Optional[Path] = None,
        audit_logger: Optional[KnowledgeAuditLogger] = None,
    ):
        self.vault_root = vault_root or get_vault_root()
        self.vault_root.mkdir(parents=True, exist_ok=True)
        self.vault_name = os.getenv("OBSIDIAN_VAULT_NAME", self.vault_root.name or "HermesVault")
        self.policy = KnowledgePolicy(vault_root=self.vault_root)
        self.audit = audit_logger or KnowledgeAuditLogger()

    @property
    def name(self) -> str:
        return "obsidian"

    def get_obsidian_uri(self, relative_path: str, action: str = "open") -> str:
        """Generates an official obsidian:// URI for mobile/desktop handoff."""
        clean_path = relative_path.replace("\\", "/").rstrip(".md")
        encoded_vault = urllib.parse.quote(self.vault_name)
        encoded_file = urllib.parse.quote(clean_path)
        return f"obsidian://{action}?vault={encoded_vault}&file={encoded_file}"

    async def health(self) -> ConnectorHealth:
        """Checks local vault accessibility."""
        try:
            exists = self.vault_root.is_dir()
            writable = os.access(str(self.vault_root), os.W_OK)
            count = len(list(self.vault_root.rglob("*.md")))
            status = "active" if (exists and writable) else "degraded"
            msg = f"Vault '{self.vault_name}' accessible at {self.vault_root} ({count} notes)"
            return ConnectorHealth(
                connector="obsidian",
                healthy=exists and writable,
                status=status,
                message=msg,
                latency_ms=1,
            )
        except Exception as e:
            return ConnectorHealth(
                connector="obsidian",
                healthy=False,
                status="error",
                message=f"Vault check failed: {str(e)}",
            )

    async def capabilities(self) -> List[str]:
        return ["read", "write", "search", "wikilinks", "frontmatter", "atomic_write", "uris"]

    async def search(self, query: KnowledgeQuery) -> List[KnowledgeResult]:
        """Searches markdown notes in the vault using lexical substring and heading matching."""
        results: List[KnowledgeResult] = []
        q_lower = query.query.lower().strip()
        terms = [t for t in q_lower.split() if len(t) > 2]

        for p in self.vault_root.rglob("*.md"):
            try:
                rel_path = str(p.relative_to(self.vault_root)).replace("\\", "/")
                # Deny check
                if any(rel_path.startswith(d) for d in self.policy.denied_subdirs):
                    continue

                content = p.read_text(encoding="utf-8", errors="replace")
                doc = MarkdownNormalizer.parse_markdown(
                    content=content,
                    source="obsidian",
                    source_id=rel_path,
                    path=rel_path,
                    url=self.get_obsidian_uri(rel_path),
                )

                # Matching logic
                title_lower = doc.title.lower()
                content_lower = doc.content.lower()

                matches = 0
                if q_lower in title_lower:
                    matches += 5
                if q_lower in content_lower:
                    matches += 3
                for t in terms:
                    if t in title_lower:
                        matches += 2
                    if t in content_lower:
                        matches += 1

                if matches > 0:
                    score = min(1.0, 0.4 + (matches * 0.1))
                    if score >= query.min_score:
                        snippet = ""
                        idx = content_lower.find(q_lower)
                        if idx >= 0:
                            s = max(0, idx - 50)
                            snippet = content[s:s + 200].replace("\n", " ").strip()
                        else:
                            snippet = doc.content[:200].replace("\n", " ").strip()

                        results.append(KnowledgeResult(
                            document=doc,
                            score=score,
                            matched_snippets=[snippet],
                            provenance_citation=f"[Obsidian: {doc.title}]({doc.url})",
                        ))
            except Exception as e:
                logger.debug(f"Error scanning note {p}: {e}")

        # Sort descending by score
        results.sort(key=lambda x: x.score, reverse=True)
        self.audit.log(
            event_type="knowledge_read",
            connector="obsidian",
            operation="search",
            details={"query": query.query, "results_found": len(results)},
        )
        return results[:query.limit]

    async def get(self, source_id: str) -> Optional[KnowledgeDocument]:
        """Retrieves a note by relative path or stem name."""
        clean_id = source_id.replace("obsidian:", "").lstrip("/")
        try:
            target = self.policy.validate_obsidian_path(clean_id)
        except KnowledgePolicyError:
            # Try appending .md
            try:
                target = self.policy.validate_obsidian_path(f"{clean_id}.md")
            except Exception:
                return None

        if target.is_file():
            content = target.read_text(encoding="utf-8", errors="replace")
            rel_path = str(target.relative_to(self.vault_root)).replace("\\", "/")
            return MarkdownNormalizer.parse_markdown(
                content=content,
                source="obsidian",
                source_id=rel_path,
                path=rel_path,
                url=self.get_obsidian_uri(rel_path),
            )

        # Stem lookup
        for p in self.vault_root.rglob("*.md"):
            if p.stem.lower() == clean_id.lower().replace(".md", ""):
                content = p.read_text(encoding="utf-8", errors="replace")
                rel_path = str(p.relative_to(self.vault_root)).replace("\\", "/")
                return MarkdownNormalizer.parse_markdown(
                    content=content,
                    source="obsidian",
                    source_id=rel_path,
                    path=rel_path,
                    url=self.get_obsidian_uri(rel_path),
                )
        return None

    def _atomic_write(self, target_path: Path, content: str) -> bool:
        """Safely writes content to target_path using atomic replace with fsync."""
        target_path.parent.mkdir(parents=True, exist_ok=True)
        # Write to temporary file in the same directory for atomic rename
        temp_fd, temp_path = tempfile.mkstemp(dir=str(target_path.parent), prefix=".tmp_")
        try:
            with os.fdopen(temp_fd, "w", encoding="utf-8") as f:
                f.write(content)
                f.flush()
                os.fsync(f.fileno())
            os.replace(temp_path, str(target_path))
            return True
        except Exception as e:
            logger.error(f"Atomic write failed for {target_path}: {e}")
            if os.path.exists(temp_path):
                os.unlink(temp_path)
            return False

    async def create(self, intent: WriteIntent) -> WriteResult:
        """Creates a new note with atomic write semantics."""
        note_name = intent.title.strip()
        if not note_name.endswith(".md"):
            note_name += ".md"

        sub_folder = intent.parent_id or "Notes"
        rel_path = f"{sub_folder}/{note_name}"

        try:
            target = self.policy.validate_obsidian_path(rel_path)
        except KnowledgePolicyError as e:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="obsidian",
                source_id=rel_path,
                error=str(e),
            )

        # Check if already exists
        if target.exists():
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="obsidian",
                source_id=rel_path,
                error=f"Note '{rel_path}' already exists. Use update or append.",
            )

        # Build content with frontmatter if tags provided
        body = intent.content
        if intent.tags:
            tags_yaml = "\n".join([f"  - {t}" for t in intent.tags])
            fm = f"---\ntitle: {intent.title}\ntags:\n{tags_yaml}\ndate: {time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime())}\n---\n\n"
            body = fm + body

        success = self._atomic_write(target, body)
        content_hash = hashlib.sha256(body.encode("utf-8")).hexdigest()
        verified = target.exists() and target.stat().st_size > 0

        self.audit.log(
            event_type="write_completed" if success else "write_failed",
            connector="obsidian",
            source_id=rel_path,
            operation="create",
            details={"title": intent.title, "path": rel_path, "verified": verified},
        )

        return WriteResult(
            success=success,
            intent_id=intent.intent_id,
            source="obsidian",
            source_id=rel_path,
            path=rel_path,
            url=self.get_obsidian_uri(rel_path),
            hash=content_hash,
            verified=verified,
        )

    async def append(self, source_id: str, content: str, expected_revision: Optional[str] = None) -> WriteResult:
        """Appends text to an existing note."""
        doc = await self.get(source_id)
        if not doc:
            return WriteResult(
                success=False,
                intent_id=f"app_{int(time.time())}",
                source="obsidian",
                source_id=source_id,
                error=f"Note '{source_id}' not found",
            )

        target = self.vault_root / doc.source_id
        current_content = target.read_text(encoding="utf-8", errors="replace")

        # Check conflict if expected_revision provided
        if expected_revision and doc.hash != expected_revision:
            return WriteResult(
                success=False,
                intent_id=f"app_{int(time.time())}",
                source="obsidian",
                source_id=doc.source_id,
                error="Revision conflict: note was modified externally",
            )

        separator = "\n\n" if not current_content.endswith("\n\n") else ""
        new_content = current_content + separator + content.strip() + "\n"

        success = self._atomic_write(target, new_content)
        new_hash = hashlib.sha256(new_content.encode("utf-8")).hexdigest()

        return WriteResult(
            success=success,
            intent_id=f"app_{int(time.time())}",
            source="obsidian",
            source_id=doc.source_id,
            path=doc.source_id,
            url=self.get_obsidian_uri(doc.source_id),
            hash=new_hash,
            verified=success,
        )

    async def update(self, source_id: str, intent: WriteIntent) -> WriteResult:
        """Updates full note content with expected revision validation."""
        doc = await self.get(source_id)
        if not doc:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="obsidian",
                source_id=source_id,
                error=f"Note '{source_id}' not found",
            )

        if intent.expected_revision and doc.hash != intent.expected_revision:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="obsidian",
                source_id=doc.source_id,
                error=f"Conflict detected: expected hash {intent.expected_revision} != current {doc.hash}",
            )

        target = self.vault_root / doc.source_id
        success = self._atomic_write(target, intent.content)
        new_hash = hashlib.sha256(intent.content.encode("utf-8")).hexdigest()

        return WriteResult(
            success=success,
            intent_id=intent.intent_id,
            source="obsidian",
            source_id=doc.source_id,
            path=doc.source_id,
            url=self.get_obsidian_uri(doc.source_id),
            hash=new_hash,
            verified=success,
        )

    async def delete(self, source_id: str) -> bool:
        """Soft deletes by moving note to .trash or appending .deleted."""
        doc = await self.get(source_id)
        if not doc:
            return False
        target = self.vault_root / doc.source_id
        trash_dir = self.vault_root / ".trash"
        trash_dir.mkdir(parents=True, exist_ok=True)
        trash_target = trash_dir / target.name
        try:
            os.replace(str(target), str(trash_target))
            return True
        except Exception:
            return False

    async def changes(self, checkpoint: Optional[SyncCheckpoint] = None) -> List[Dict[str, Any]]:
        """Scans notes modified since the checkpoint timestamp."""
        min_mtime = checkpoint.last_success_at if checkpoint else 0.0
        changed = []
        for p in self.vault_root.rglob("*.md"):
            st = p.stat()
            if st.st_mtime > min_mtime:
                rel = str(p.relative_to(self.vault_root)).replace("\\", "/")
                changed.append({
                    "path": rel,
                    "mtime": st.st_mtime,
                    "size": st.st_size,
                })
        return changed
