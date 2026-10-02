"""
Knowledge Connector Base Interface
==================================
Defines the standard asynchronous contract for all knowledge sources:
- health
- capabilities
- search
- get
- create
- update
- append
- delete
- changes
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import List, Optional, Dict, Any

from harness.knowledge.models import (
    KnowledgeDocument,
    KnowledgeQuery,
    KnowledgeResult,
    WriteIntent,
    WriteResult,
    ConnectorHealth,
    SyncCheckpoint,
)


class KnowledgeConnector(ABC):
    @property
    @abstractmethod
    def name(self) -> str:
        """Name of the connector ('notion', 'obsidian', etc.)."""
        pass

    @abstractmethod
    async def health(self) -> ConnectorHealth:
        """Check connection liveness and return diagnostic status."""
        pass

    @abstractmethod
    async def capabilities(self) -> List[str]:
        """Return list of supported capabilities (e.g., 'read', 'write', 'search', 'databases')."""
        pass

    @abstractmethod
    async def search(self, query: KnowledgeQuery) -> List[KnowledgeResult]:
        """Search knowledge provider and return ranked normalized results."""
        pass

    @abstractmethod
    async def get(self, source_id: str) -> Optional[KnowledgeDocument]:
        """Retrieve a specific document by its provider-specific ID/path."""
        pass

    @abstractmethod
    async def create(self, intent: WriteIntent) -> WriteResult:
        """Create a new document/page in the provider."""
        pass

    @abstractmethod
    async def update(self, source_id: str, intent: WriteIntent) -> WriteResult:
        """Update an existing document/page with conflict checking."""
        pass

    @abstractmethod
    async def append(self, source_id: str, content: str, expected_revision: Optional[str] = None) -> WriteResult:
        """Append content or blocks to an existing document/page."""
        pass

    @abstractmethod
    async def delete(self, source_id: str) -> bool:
        """Soft-delete or archive a document/page."""
        pass

    @abstractmethod
    async def changes(self, checkpoint: Optional[SyncCheckpoint] = None) -> List[Dict[str, Any]]:
        """Fetch incremental changes since the provided checkpoint."""
        pass
