"""
Canonical Knowledge Layer Models
================================
Defines common representations for multi-provider knowledge systems:
- Notion (Cloud structured pages, databases, blocks)
- Obsidian (Local-first Markdown notes, wikilinks, frontmatter)
"""
from __future__ import annotations

import hashlib
import time
from dataclasses import dataclass, field, asdict
from typing import Dict, Any, List, Optional


@dataclass
class KnowledgeDocument:
    id: str
    source: str  # "notion" | "obsidian"
    source_id: str  # Notion page ID or Obsidian relative path
    title: str
    content: str  # Normalized markdown text
    url: Optional[str] = None
    path: Optional[str] = None
    tags: List[str] = field(default_factory=list)
    links: List[str] = field(default_factory=list)  # Wikilinks or external URLs
    properties: Dict[str, Any] = field(default_factory=dict)  # Notion properties or YAML frontmatter
    hash: str = ""
    revision: Optional[str] = None
    modified_at: float = field(default_factory=time.time)
    indexed_at: float = field(default_factory=time.time)
    provenance: Dict[str, Any] = field(default_factory=dict)

    def __post_init__(self):
        if not self.hash:
            self.hash = hashlib.sha256(self.content.encode("utf-8")).hexdigest()

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class KnowledgeChunk:
    id: str
    document_id: str
    source: str
    content: str
    line_start: int
    line_end: int
    heading_context: Optional[str] = None
    score: float = 0.0

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class KnowledgeSource:
    id: str
    name: str
    source_type: str  # "notion" | "obsidian"
    enabled: bool = True
    priority: int = 1  # Lower number = higher priority (1 for Notion, 2 for Obsidian)
    config: Dict[str, Any] = field(default_factory=dict)
    status: str = "active"  # "active" | "degraded" | "offline"
    last_sync_at: Optional[float] = None
    document_count: int = 0

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class KnowledgeQuery:
    query: str
    sources: Optional[List[str]] = None  # None = query all active
    project: Optional[str] = None
    tags: Optional[List[str]] = None
    limit: int = 10
    min_score: float = 0.15
    freshness_weight: float = 0.15

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class KnowledgeResult:
    document: KnowledgeDocument
    score: float
    matched_snippets: List[str] = field(default_factory=list)
    provenance_citation: str = ""

    def to_dict(self) -> Dict[str, Any]:
        return {
            "document": self.document.to_dict(),
            "score": round(self.score, 4),
            "matched_snippets": self.matched_snippets,
            "provenance_citation": self.provenance_citation,
        }


@dataclass
class WriteIntent:
    intent_id: str
    destination: str  # "notion" | "obsidian"
    title: str
    content: str
    parent_id: Optional[str] = None  # Notion parent page/db or Obsidian directory
    tags: List[str] = field(default_factory=list)
    properties: Dict[str, Any] = field(default_factory=dict)
    operation: str = "create"  # "create" | "append" | "update"
    expected_revision: Optional[str] = None
    idempotency_key: str = ""

    def __post_init__(self):
        if not self.idempotency_key:
            raw = f"{self.destination}:{self.title}:{self.operation}:{self.parent_id}"
            self.idempotency_key = hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class WriteResult:
    success: bool
    intent_id: str
    source: str
    source_id: str
    url: Optional[str] = None
    path: Optional[str] = None
    revision: Optional[str] = None
    hash: str = ""
    error: Optional[str] = None
    verified: bool = False

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class SyncCheckpoint:
    connector: str
    scope: str
    cursor: Optional[str] = None
    last_event_id: Optional[str] = None
    last_success_at: float = field(default_factory=time.time)
    items_synced: int = 0

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class SyncConflict:
    source: str
    source_id: str
    base_hash: str
    local_hash: str
    remote_hash: str
    local_content: str
    remote_content: str
    detected_at: float = field(default_factory=time.time)
    resolved: bool = False
    resolution: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class ConnectorHealth:
    connector: str
    healthy: bool
    status: str
    message: str
    latency_ms: Optional[int] = None
    last_checked: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
