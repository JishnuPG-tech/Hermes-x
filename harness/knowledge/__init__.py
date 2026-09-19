"""
Hermes Knowledge Layer
======================
Connectors, models, routing, policy, and synchronization for Notion and Obsidian.
"""
from harness.knowledge.models import (
    KnowledgeDocument,
    KnowledgeChunk,
    KnowledgeSource,
    KnowledgeQuery,
    KnowledgeResult,
    WriteIntent,
    WriteResult,
    SyncCheckpoint,
    SyncConflict,
    ConnectorHealth,
)
from harness.knowledge.connector import KnowledgeConnector
from harness.knowledge.normalizer import MarkdownNormalizer, NotionNormalizer
from harness.knowledge.policy import KnowledgePolicy, KnowledgePolicyError
from harness.knowledge.audit import KnowledgeAuditLogger
from harness.knowledge.notion_connector import NotionConnector
from harness.knowledge.obsidian_connector import ObsidianConnector
from harness.knowledge.router import KnowledgeRouter
from harness.knowledge.sync import KnowledgeSyncEngine

__all__ = [
    "KnowledgeDocument",
    "KnowledgeChunk",
    "KnowledgeSource",
    "KnowledgeQuery",
    "KnowledgeResult",
    "WriteIntent",
    "WriteResult",
    "SyncCheckpoint",
    "SyncConflict",
    "ConnectorHealth",
    "KnowledgeConnector",
    "MarkdownNormalizer",
    "NotionNormalizer",
    "KnowledgePolicy",
    "KnowledgePolicyError",
    "KnowledgeAuditLogger",
    "NotionConnector",
    "ObsidianConnector",
    "KnowledgeRouter",
    "KnowledgeSyncEngine",
]
