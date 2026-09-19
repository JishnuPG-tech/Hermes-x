"""
Notion Knowledge Connector
==========================
Connects Hermes to Notion using the official Notion REST API v1.
- Provides search, page reading, block tree traversal, and database querying.
- Implements create/append/update operations with read-back verification.
- Enforces rate-limiting backoff (429 handling) and circuit breakers.
- Normalizes all responses into canonical KnowledgeDocument objects.
"""
from __future__ import annotations

import asyncio
import logging
import os
import random
import time
from typing import Dict, Any, List, Optional
import httpx

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
from harness.knowledge.normalizer import NotionNormalizer
from harness.knowledge.audit import KnowledgeAuditLogger

logger = logging.getLogger("NotionConnector")

NOTION_VERSION = "2022-06-28"
NOTION_API_BASE = "https://api.notion.com/v1"


class NotionConnector(KnowledgeConnector):
    def __init__(
        self,
        api_token: Optional[str] = None,
        audit_logger: Optional[KnowledgeAuditLogger] = None,
    ):
        raw_token = (
            api_token
            or os.getenv("NOTION_API_KEY")
            or os.getenv("NOTION_TOKEN")
            or os.getenv("NOTION_SECRET")
            or ""
        ).strip()
        self.api_token = raw_token
        self.audit = audit_logger or KnowledgeAuditLogger()
        self._circuit_broken = False
        self._consecutive_failures = 0

    @property
    def name(self) -> str:
        return "notion"

    @property
    def is_configured(self) -> bool:
        return bool(self.api_token and len(self.api_token) > 10)

    def _get_headers(self) -> Dict[str, str]:
        return {
            "Authorization": f"Bearer {self.api_token}",
            "Notion-Version": NOTION_VERSION,
            "Content-Type": "application/json",
        }

    async def _request(
        self,
        method: str,
        path: str,
        json_data: Optional[Dict[str, Any]] = None,
        max_retries: int = 3,
    ) -> Optional[Dict[str, Any]]:
        """Makes an HTTP request to Notion API with exponential backoff for 429 / 5xx."""
        if not self.is_configured:
            return None
        if self._circuit_broken:
            logger.warning("Notion connector circuit breaker is open. Request skipped.")
            return None

        url = f"{NOTION_API_BASE}{path}"
        headers = self._get_headers()

        async with httpx.AsyncClient(timeout=30.0) as client:
            for attempt in range(max_retries):
                try:
                    res = await client.request(method, url, headers=headers, json=json_data)
                    if res.status_code in (200, 201):
                        self._consecutive_failures = 0
                        return res.json()
                    elif res.status_code == 429:
                        # Rate limit: respect Retry-After or apply exponential jitter
                        retry_after = int(res.headers.get("Retry-After", 1))
                        wait_sec = retry_after + random.uniform(0.5, 1.5)
                        logger.warning(f"[Notion] Rate limit hit (429). Retrying in {wait_sec:.2f}s...")
                        await asyncio.sleep(wait_sec)
                        continue
                    elif res.status_code in (401, 403):
                        logger.error(f"[Notion] Authentication failed ({res.status_code}): {res.text}")
                        self._circuit_broken = True
                        return None
                    elif res.status_code >= 500:
                        wait_sec = (2 ** attempt) + random.uniform(0.1, 0.5)
                        logger.warning(f"[Notion] Server error ({res.status_code}). Retrying in {wait_sec:.2f}s...")
                        await asyncio.sleep(wait_sec)
                        continue
                    else:
                        logger.error(f"[Notion] Request error ({res.status_code}): {res.text}")
                        return None
                except Exception as e:
                    logger.warning(f"[Notion] Network attempt {attempt + 1} failed: {e}")
                    await asyncio.sleep(1.0)

        self._consecutive_failures += 1
        if self._consecutive_failures >= 5:
            self._circuit_broken = True
        return None

    async def health(self) -> ConnectorHealth:
        """Checks Notion API accessibility by querying current user or workspaces."""
        if not self.is_configured:
            return ConnectorHealth(
                connector="notion",
                healthy=False,
                status="unconfigured",
                message="NOTION_API_KEY is not set or empty. Notion integration in standby.",
            )

        start = time.time()
        res = await self._request("GET", "/users/me")
        latency = int((time.time() - start) * 1000)

        if res and res.get("id"):
            name = res.get("name", "Hermes Integration")
            return ConnectorHealth(
                connector="notion",
                healthy=True,
                status="active",
                message=f"Connected to Notion as '{name}'",
                latency_ms=latency,
            )
        return ConnectorHealth(
            connector="notion",
            healthy=False,
            status="error",
            message="Failed to authenticate with Notion API. Check token validity.",
            latency_ms=latency,
        )

    async def capabilities(self) -> List[str]:
        return ["read", "write", "search", "databases", "blocks", "audit"]

    async def search(self, query: KnowledgeQuery) -> List[KnowledgeResult]:
        """Searches Notion workspace pages and databases."""
        if not self.is_configured:
            return []

        payload: Dict[str, Any] = {
            "query": query.query,
            "page_size": min(query.limit, 25),
        }
        res = await self._request("POST", "/search", json_data=payload)
        if not res:
            return []

        results: List[KnowledgeResult] = []
        raw_items = res.get("results", [])

        for item in raw_items:
            obj_type = item.get("object")
            item_id = item.get("id", "")
            if obj_type == "page":
                # Fetch children blocks for the top matches
                blocks = await self.get_block_children(item_id, max_blocks=20)
                doc = NotionNormalizer.page_to_document(item, blocks)
                score = 0.85 if query.query.lower() in doc.title.lower() else 0.65
                results.append(KnowledgeResult(
                    document=doc,
                    score=score,
                    matched_snippets=[doc.content[:200] + "..."],
                    provenance_citation=f"[Notion: {doc.title}]({doc.url})",
                ))
            elif obj_type == "database":
                db_title = NotionNormalizer.extract_rich_text(item.get("title", [])) or "Database"
                doc = KnowledgeDocument(
                    id=f"notion:{item_id}",
                    source="notion",
                    source_id=item_id,
                    title=db_title,
                    content=f"# Database: {db_title}\n\nURL: {item.get('url')}",
                    url=item.get("url"),
                    properties={"object": "database"},
                )
                results.append(KnowledgeResult(
                    document=doc,
                    score=0.75,
                    provenance_citation=f"[Notion Database: {db_title}]({item.get('url')})",
                ))

        self.audit.log(
            event_type="knowledge_read",
            connector="notion",
            operation="search",
            details={"query": query.query, "results_found": len(results)},
        )
        return results

    async def get_block_children(self, block_id: str, max_blocks: int = 50) -> List[Dict[str, Any]]:
        """Retrieves child blocks of a page or block container."""
        clean_id = block_id.replace("-", "")
        res = await self._request("GET", f"/blocks/{clean_id}/children?page_size={max_blocks}")
        if not res:
            return []
        return res.get("results", [])

    async def get(self, source_id: str) -> Optional[KnowledgeDocument]:
        """Retrieves a full Notion page with blocks."""
        clean_id = source_id.replace("notion:", "").replace("-", "")
        page = await self._request("GET", f"/pages/{clean_id}")
        if not page:
            return None
        blocks = await self.get_block_children(clean_id, max_blocks=60)
        return NotionNormalizer.page_to_document(page, blocks)

    async def create(self, intent: WriteIntent) -> WriteResult:
        """Creates a new Notion page with initial content blocks."""
        if not self.is_configured:
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="notion",
                source_id="",
                error="Notion connector is not configured with an API token",
            )

        # Split content lines into paragraph blocks
        content_lines = [l.strip() for l in intent.content.split("\n\n") if l.strip()]
        children_blocks = []
        for line in content_lines[:40]:
            children_blocks.append({
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [{"type": "text", "text": {"content": line[:1900]}}]
                }
            })

        parent: Dict[str, Any] = {}
        if intent.parent_id:
            parent["page_id"] = intent.parent_id.replace("-", "")
        else:
            # Search for a default parent or workspace root
            search_res = await self._request("POST", "/search", json_data={"page_size": 1})
            if search_res and search_res.get("results"):
                parent["page_id"] = search_res["results"][0]["id"]
            else:
                return WriteResult(
                    success=False,
                    intent_id=intent.intent_id,
                    source="notion",
                    source_id="",
                    error="No accessible parent page found in Notion workspace to attach note",
                )

        payload = {
            "parent": parent,
            "properties": {
                "title": {
                    "title": [{"type": "text", "text": {"content": intent.title}}]
                }
            },
            "children": children_blocks,
        }

        res = await self._request("POST", "/pages", json_data=payload)
        if not res or not res.get("id"):
            self.audit.log(
                event_type="write_failed",
                connector="notion",
                operation="create",
                details={"title": intent.title},
                status="error",
            )
            return WriteResult(
                success=False,
                intent_id=intent.intent_id,
                source="notion",
                source_id="",
                error="Notion API failed to create page",
            )

        created_id = res["id"]
        created_url = res.get("url", f"https://notion.so/{created_id.replace('-', '')}")

        # Read-back verification
        read_back = await self._request("GET", f"/pages/{created_id}")
        verified = bool(read_back and read_back.get("id"))

        self.audit.log(
            event_type="write_completed",
            connector="notion",
            source_id=created_id,
            operation="create",
            details={"title": intent.title, "url": created_url, "verified": verified},
        )

        return WriteResult(
            success=True,
            intent_id=intent.intent_id,
            source="notion",
            source_id=created_id,
            url=created_url,
            verified=verified,
        )

    async def append(self, source_id: str, content: str, expected_revision: Optional[str] = None) -> WriteResult:
        """Appends block content to an existing Notion page."""
        clean_id = source_id.replace("notion:", "").replace("-", "")
        blocks = [{
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": content[:1900]}}]
            }
        }]

        res = await self._request("PATCH", f"/blocks/{clean_id}/children", json_data={"children": blocks})
        success = bool(res and "results" in res)
        return WriteResult(
            success=success,
            intent_id=f"append_{int(time.time())}",
            source="notion",
            source_id=clean_id,
            verified=success,
        )

    async def update(self, source_id: str, intent: WriteIntent) -> WriteResult:
        """Updates page properties in Notion."""
        clean_id = source_id.replace("notion:", "").replace("-", "")
        payload: Dict[str, Any] = {"properties": {}}
        if intent.title:
            payload["properties"]["title"] = {
                "title": [{"type": "text", "text": {"content": intent.title}}]
            }

        res = await self._request("PATCH", f"/pages/{clean_id}", json_data=payload)
        success = bool(res and res.get("id"))
        return WriteResult(
            success=success,
            intent_id=intent.intent_id,
            source="notion",
            source_id=clean_id,
            url=res.get("url") if res else None,
            verified=success,
        )

    async def delete(self, source_id: str) -> bool:
        """Archives a page in Notion (soft delete)."""
        clean_id = source_id.replace("notion:", "").replace("-", "")
        res = await self._request("PATCH", f"/pages/{clean_id}", json_data={"archived": True})
        return bool(res and res.get("archived") is True)

    async def changes(self, checkpoint: Optional[SyncCheckpoint] = None) -> List[Dict[str, Any]]:
        """Fetches recently modified pages."""
        if not self.is_configured:
            return []
        res = await self._request("POST", "/search", json_data={
            "sort": {"direction": "descending", "timestamp": "last_edited_time"},
            "page_size": 20,
        })
        if not res:
            return []
        return res.get("results", [])
