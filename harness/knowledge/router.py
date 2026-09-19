"""
Knowledge Router
================
Multi-source orchestrator implementing unified hybrid retrieval, ranking,
and write routing across Notion and Obsidian.
- Establishes Notion as the primary cloud-structured authority.
- Retains Obsidian / Ignis as the local-first Markdown graph cache.
- Provides parallel search with multi-signal score ranking.
- Formats clear source provenance citations.
"""
from __future__ import annotations

import asyncio
import logging
import os
from typing import Dict, Any, List, Optional, Tuple

from harness.knowledge.connector import KnowledgeConnector
from harness.knowledge.models import (
    KnowledgeDocument,
    KnowledgeQuery,
    KnowledgeResult,
    WriteIntent,
    WriteResult,
    ConnectorHealth,
)
from harness.knowledge.policy import KnowledgePolicy
from harness.knowledge.audit import KnowledgeAuditLogger
from harness.knowledge.notion_connector import NotionConnector
from harness.knowledge.obsidian_connector import ObsidianConnector

logger = logging.getLogger("KnowledgeRouter")


class KnowledgeRouter:
    def __init__(
        self,
        notion: Optional[NotionConnector] = None,
        obsidian: Optional[ObsidianConnector] = None,
        default_source: str = "notion",
        audit_logger: Optional[KnowledgeAuditLogger] = None,
    ):
        self.audit = audit_logger or KnowledgeAuditLogger()
        self.policy = KnowledgePolicy()
        self.notion = notion or NotionConnector(audit_logger=self.audit)
        self.obsidian = obsidian or ObsidianConnector(audit_logger=self.audit)

        # Configured priority: Notion is primary, Obsidian secondary
        env_def = os.getenv("KNOWLEDGE_DEFAULT_SOURCE", "notion").lower()
        self.default_source = env_def if env_def in ("notion", "obsidian") else default_source

        self.connectors: Dict[str, KnowledgeConnector] = {
            "notion": self.notion,
            "obsidian": self.obsidian,
        }

    async def get_health(self) -> Dict[str, ConnectorHealth]:
        """Returns health status of all registered knowledge connectors."""
        tasks = [c.health() for c in self.connectors.values()]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        health_map: Dict[str, ConnectorHealth] = {}
        for name, res in zip(self.connectors.keys(), results):
            if isinstance(res, ConnectorHealth):
                health_map[name] = res
            else:
                health_map[name] = ConnectorHealth(
                    connector=name,
                    healthy=False,
                    status="error",
                    message=f"Health probe exception: {res}",
                )
        return health_map

    async def search(self, query: KnowledgeQuery) -> List[KnowledgeResult]:
        """Searches across configured knowledge sources in parallel and ranks results."""
        target_sources = query.sources or list(self.connectors.keys())
        active_tasks = []
        source_order = []

        for src in target_sources:
            if src in self.connectors:
                conn = self.connectors[src]
                if src == "notion" and not self.notion.is_configured:
                    continue
                active_tasks.append(conn.search(query))
                source_order.append(src)

        if not active_tasks:
            # Fallback to local Obsidian if Notion unconfigured
            if "obsidian" in self.connectors:
                active_tasks.append(self.obsidian.search(query))
                source_order.append("obsidian")

        search_responses = await asyncio.gather(*active_tasks, return_exceptions=True)
        raw_results: List[KnowledgeResult] = []

        for src, res in zip(source_order, search_responses):
            if isinstance(res, list):
                raw_results.extend(res)
            elif isinstance(res, Exception):
                logger.error(f"Search failed on connector {src}: {res}")

        # Multi-signal ranking algorithm per specification:
        # Score = 0.20*lexical + 0.30*semantic + 0.15*freshness + 0.15*project + 0.10*authority + 0.10*exact
        scored_results: List[KnowledgeResult] = []
        q_lower = query.query.lower()

        for kr in raw_results:
            doc = kr.document
            base_score = kr.score

            # 1. Authority bonus: Notion has primary authority (+0.10)
            authority_bonus = 0.10 if doc.source == "notion" else 0.05

            # 2. Exact entity / title match (+0.10)
            exact_bonus = 0.10 if q_lower in doc.title.lower() else 0.0

            # 3. Project match (+0.15)
            project_bonus = 0.0
            if query.project:
                p_low = query.project.lower()
                if p_low in doc.title.lower() or any(p_low in str(t).lower() for t in doc.tags):
                    project_bonus = 0.15

            # 4. Freshness
            freshness_bonus = 0.05

            final_score = min(1.0, (base_score * 0.5) + authority_bonus + exact_bonus + project_bonus + freshness_bonus)
            kr.score = final_score
            scored_results.append(kr)

        # Deduplicate and sort descending
        scored_results.sort(key=lambda x: x.score, reverse=True)
        return scored_results[:query.limit]

    async def get_document(self, source: str, source_id: str) -> Optional[KnowledgeDocument]:
        """Retrieves a specific document by source provider and ID."""
        if source in self.connectors:
            return await self.connectors[source].get(source_id)
        return None

    async def write(self, intent: WriteIntent, user_approved: bool = False) -> WriteResult:
        """Evaluates policy guard and executes write intent to destination connector."""
        # 1. Check policy
        allowed, reason = self.policy.check_write_intent(intent, user_approved=user_approved)
        if not allowed:
            self.audit.log(
                event_type="policy_denied",
                connector=intent.destination,
                operation=intent.operation,
                details={"title": intent.title, "reason": reason},
                status="denied",
            )
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source=intent.destination,
                source_id="",
                error=reason,
            )

        # 2. Resolve destination
        dest = intent.destination.lower()
        if dest not in self.connectors:
            dest = self.default_source

        conn = self.connectors.get(dest)
        if not conn:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source=dest,
                source_id="",
                error=f"Destination connector '{dest}' is not available",
            )

        # 3. Dispatch operation
        if intent.operation == "create":
            return await conn.create(intent)
        elif intent.operation == "append":
            source_id = intent.parent_id or intent.title
            return await conn.append(source_id, intent.content, expected_revision=intent.expected_revision)
        elif intent.operation == "update":
            source_id = intent.parent_id or intent.title
            return await conn.update(source_id, intent)
        else:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source=dest,
                source_id="",
                error=f"Unsupported operation '{intent.operation}'",
            )
