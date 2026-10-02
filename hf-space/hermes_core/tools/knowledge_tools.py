"""
Knowledge Layer Agent Tools
===========================
Exposes high-level knowledge actions to Hermes Agent:
- search_knowledge (cross-system hybrid search across Notion and Obsidian)
- read_knowledge_note (fetch full note with provenance)
- save_knowledge_note (atomic save to Notion [primary] or Obsidian)
- sync_knowledge_sources (trigger sync between providers)
- get_obsidian_uri (generate obsidian:// URI for mobile/desktop app)
"""
from __future__ import annotations

import json
from typing import Optional, List, Dict, Any

from hermes_core.tools.registry import registry
from harness.knowledge.router import KnowledgeRouter
from harness.knowledge.models import KnowledgeQuery, WriteIntent

# Global router singleton
_router = KnowledgeRouter()


@registry.register(
    name="search_knowledge",
    description="Search across authorized knowledge systems (Notion and Obsidian). Notion is primary for project decisions, databases, and structured docs; Obsidian for local Markdown notes. Returns ranked notes with provenance citations.",
    parameters={
        "type": "object",
        "properties": {
            "query": {"type": "string", "description": "Search keyword, question, or topic"},
            "sources": {
                "type": "array",
                "items": {"type": "string", "enum": ["notion", "obsidian"]},
                "description": "Optional list of sources to search (defaults to both, prioritizing Notion)"
            },
            "project": {"type": "string", "description": "Optional project name filter (e.g. 'Hermes', 'OmniRoute')"},
            "limit": {"type": "integer", "description": "Maximum number of results to return (default 5)"}
        },
        "required": ["query"]
    },
    category="vault"
)
async def search_knowledge(
    query: str,
    sources: Optional[List[str]] = None,
    project: Optional[str] = None,
    limit: int = 5,
) -> str:
    kq = KnowledgeQuery(query=query, sources=sources, project=project, limit=limit)
    results = await _router.search(kq)
    if not results:
        return f"No knowledge records found matching '{query}' in Notion or Obsidian."

    formatted = []
    for r in results:
        doc = r.document
        source_label = "🔷 Notion" if doc.source == "notion" else "🟣 Obsidian"
        cite = r.provenance_citation or f"[{doc.title}]({doc.url or doc.path})"
        snippet = r.matched_snippets[0] if r.matched_snippets else doc.content[:180]
        formatted.append(f"{source_label} **{doc.title}** (Score: {r.score:.2f})\nCitation: {cite}\nSnippet: {snippet}\n")

    return "\n---\n".join(formatted)


@registry.register(
    name="read_knowledge_note",
    description="Read the complete text, properties, and metadata of a specific Notion page or Obsidian note.",
    parameters={
        "type": "object",
        "properties": {
            "source": {"type": "string", "enum": ["notion", "obsidian"], "description": "Knowledge provider"},
            "source_id": {"type": "string", "description": "Notion page ID or Obsidian relative path"}
        },
        "required": ["source", "source_id"]
    },
    category="vault"
)
async def read_knowledge_note(source: str, source_id: str) -> str:
    doc = await _router.get_document(source, source_id)
    if not doc:
        return f"Note '{source_id}' not found in {source}."

    props_str = json.dumps(doc.properties, ensure_ascii=False) if doc.properties else "None"
    return f"""# {doc.title}
**Source:** {doc.source.upper()} | **ID/Path:** `{doc.source_id}`  
**URL:** {doc.url or 'N/A'}  
**Tags:** {', '.join(doc.tags) if doc.tags else 'None'}  
**Properties:** {props_str}  

---

{doc.content}
"""


@registry.register(
    name="save_knowledge_note",
    description="Save a decision record, research report, or architecture note to Notion (primary) or Obsidian vault. Enforces policy validation and atomic writes.",
    parameters={
        "type": "object",
        "properties": {
            "title": {"type": "string", "description": "Note or page title"},
            "content": {"type": "string", "description": "Full markdown content to save"},
            "destination": {
                "type": "string",
                "enum": ["notion", "obsidian"],
                "description": "Destination knowledge system (defaults to 'notion' as primary)"
            },
            "parent_id": {"type": "string", "description": "Notion parent page/db ID or Obsidian folder (e.g. 'Projects/Hermes')"},
            "tags": {
                "type": "array",
                "items": {"type": "string"},
                "description": "Optional list of topic tags"
            }
        },
        "required": ["title", "content"]
    },
    category="vault"
)
async def save_knowledge_note(
    title: str,
    content: str,
    destination: str = "notion",
    parent_id: Optional[str] = None,
    tags: Optional[List[str]] = None,
) -> str:
    intent = WriteIntent(
        intent_id=f"write_{title.replace(' ', '_')[:20]}",
        destination=destination,
        title=title,
        content=content,
        parent_id=parent_id,
        tags=tags or [],
        operation="create",
    )
    res = await _router.write(intent, user_approved=True)
    if res.success:
        url_text = f"URL: {res.url}" if res.url else f"Path: {res.path}"
        return f"Successfully saved knowledge note '{title}' to {res.source.upper()}.\n{url_text} (Verified: {res.verified})"
    else:
        return f"Failed to save knowledge note: {res.error}"


@registry.register(
    name="get_obsidian_uri",
    description="Generate an official obsidian:// URI for opening a note on desktop or mobile Obsidian application.",
    parameters={
        "type": "object",
        "properties": {
            "file_path": {"type": "string", "description": "Relative path of note in vault (e.g. 'Projects/Hermes.md')"},
            "vault_name": {"type": "string", "description": "Optional vault name"}
        },
        "required": ["file_path"]
    },
    category="vault"
)
def get_obsidian_uri(file_path: str, vault_name: Optional[str] = None) -> str:
    return _router.obsidian.get_obsidian_uri(file_path)


@registry.register(
    name="notion_search",
    description="Search Notion workspace for pages, databases, and structured project notes.",
    parameters={
        "type": "object",
        "properties": {
            "query": {"type": "string", "description": "The search term or topic to find in Notion"}
        },
        "required": ["query"],
    },
    category="vault",
)
async def notion_search(query: str) -> str:
    return await search_knowledge(query=query, sources=["notion"], limit=8)


@registry.register(
    name="notion_read_page",
    description="Read full content of a Notion page by its ID.",
    parameters={
        "type": "object",
        "properties": {
            "page_id": {"type": "string", "description": "The ID of the Notion page to read"}
        },
        "required": ["page_id"],
    },
    category="vault",
)
async def notion_read_page(page_id: str) -> str:
    return await read_knowledge_note(source="notion", source_id=page_id)


@registry.register(
    name="notion_create_page",
    description="Create a new note or document in Notion.",
    parameters={
        "type": "object",
        "properties": {
            "title": {"type": "string", "description": "Title of the note"},
            "content": {"type": "string", "description": "Markdown body content"},
            "parent_id": {"type": "string", "description": "Optional parent page or database ID"},
        },
        "required": ["title", "content"],
    },
    category="vault",
    side_effects=True,
)
async def notion_create_page(title: str, content: str, parent_id: Optional[str] = None) -> str:
    return await save_knowledge_note(title=title, content=content, destination="notion", parent_id=parent_id)


@registry.register(
    name="knowledge_search",
    description="Unified cross-system knowledge search across all connected sources.",
    parameters={
        "type": "object",
        "properties": {
            "query": {"type": "string", "description": "Search term"},
            "sources": {"type": "string", "description": "Comma separated sources e.g. 'notion,obsidian'"},
        },
        "required": ["query"],
    },
    category="vault",
)
async def knowledge_search(query: str, sources: str = "notion,obsidian") -> str:
    src_list = [s.strip() for s in sources.split(",") if s.strip()]
    return await search_knowledge(query=query, sources=src_list, limit=6)

