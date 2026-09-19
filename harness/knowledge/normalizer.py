"""
Knowledge Normalizer
====================
Normalizes Markdown (Obsidian) and block structures (Notion) into canonical
KnowledgeDocument and KnowledgeChunk structures with source provenance.
"""
from __future__ import annotations

import re
import yaml
from typing import Dict, Any, List, Tuple, Optional
from harness.knowledge.models import KnowledgeDocument, KnowledgeChunk


class MarkdownNormalizer:
    @staticmethod
    def parse_markdown(
        content: str,
        source: str = "obsidian",
        source_id: str = "",
        title: Optional[str] = None,
        url: Optional[str] = None,
        path: Optional[str] = None,
        extra_properties: Optional[Dict[str, Any]] = None,
    ) -> KnowledgeDocument:
        """Parses a markdown string extracting frontmatter, tags, wikilinks, and clean content."""
        frontmatter: Dict[str, Any] = {}
        body = content
        extracted_tags: List[str] = []

        # 1. Parse YAML frontmatter if present
        fm_match = re.match(r"^---\r?\n(.*?)\r?\n---\r?\n(.*)$", content, re.DOTALL)
        if fm_match:
            try:
                raw_fm = fm_match.group(1)
                parsed = yaml.safe_load(raw_fm)
                if isinstance(parsed, dict):
                    frontmatter = parsed
                    body = fm_match.group(2)
            except Exception:
                pass

        # 2. Extract title: from frontmatter, first # heading, or filename
        doc_title = title
        if not doc_title and "title" in frontmatter:
            doc_title = str(frontmatter["title"])
        if not doc_title:
            h1_match = re.search(r"^#\s+(.+)$", body, re.MULTILINE)
            if h1_match:
                doc_title = h1_match.group(1).strip()
        if not doc_title and path:
            doc_title = path.split("/")[-1].replace(".md", "")
        if not doc_title:
            doc_title = "Untitled Document"

        # 3. Extract tags from frontmatter and inline #tags
        if "tags" in frontmatter:
            val = frontmatter["tags"]
            if isinstance(val, list):
                extracted_tags.extend([str(t).lstrip("#") for t in val])
            elif isinstance(val, str):
                extracted_tags.extend([t.strip().lstrip("#") for t in val.split(",")])

        inline_tags = re.findall(r"(?:^|\s)#([a-zA-Z0-9_\-/]+)", body)
        for t in inline_tags:
            if t not in extracted_tags:
                extracted_tags.append(t)

        # 4. Extract wikilinks [[Target Note]]
        wikilinks = re.findall(r"\[\[(.*?)\]\]", body)
        clean_links: List[str] = []
        for wl in wikilinks:
            # Handle aliases like [[Target|Alias]]
            target = wl.split("|")[0].strip()
            if target and target not in clean_links:
                clean_links.append(target)

        # 5. Combine properties
        merged_props = dict(frontmatter)
        if extra_properties:
            merged_props.update(extra_properties)

        doc_id = f"{source}:{source_id or doc_title}"

        return KnowledgeDocument(
            id=doc_id,
            source=source,
            source_id=source_id or path or doc_title,
            title=doc_title,
            content=body.strip(),
            url=url,
            path=path,
            tags=extracted_tags,
            links=clean_links,
            properties=merged_props,
            provenance={
                "source": source,
                "path": path,
                "url": url,
                "links_count": len(clean_links),
                "tags_count": len(extracted_tags),
            },
        )

    @staticmethod
    def chunk_document(doc: KnowledgeDocument, max_chunk_lines: int = 40) -> List[KnowledgeChunk]:
        """Breaks a document into chunks while preserving heading context."""
        lines = doc.content.splitlines()
        chunks: List[KnowledgeChunk] = []
        current_heading = doc.title
        current_lines: List[str] = []
        start_line = 1

        for idx, line in enumerate(lines, start=1):
            if re.match(r"^#{1,3}\s+", line):
                if current_lines:
                    chunk_text = "\n".join(current_lines).strip()
                    if chunk_text:
                        chunks.append(KnowledgeChunk(
                            id=f"{doc.id}:chunk_{len(chunks)}",
                            document_id=doc.id,
                            source=doc.source,
                            content=chunk_text,
                            line_start=start_line,
                            line_end=idx - 1,
                            heading_context=current_heading,
                        ))
                    current_lines = []
                    start_line = idx
                current_heading = re.sub(r"^#{1,3}\s+", "", line).strip()

            current_lines.append(line)
            if len(current_lines) >= max_chunk_lines:
                chunk_text = "\n".join(current_lines).strip()
                if chunk_text:
                    chunks.append(KnowledgeChunk(
                        id=f"{doc.id}:chunk_{len(chunks)}",
                        document_id=doc.id,
                        source=doc.source,
                        content=chunk_text,
                        line_start=start_line,
                        line_end=idx,
                        heading_context=current_heading,
                    ))
                current_lines = []
                start_line = idx + 1

        if current_lines:
            chunk_text = "\n".join(current_lines).strip()
            if chunk_text:
                chunks.append(KnowledgeChunk(
                    id=f"{doc.id}:chunk_{len(chunks)}",
                    document_id=doc.id,
                    source=doc.source,
                    content=chunk_text,
                    line_start=start_line,
                    line_end=len(lines),
                    heading_context=current_heading,
                ))

        return chunks


class NotionNormalizer:
    @staticmethod
    def extract_rich_text(rich_text_list: List[Dict[str, Any]]) -> str:
        """Extracts plain text and markdown links from Notion rich_text blocks."""
        out = []
        for item in rich_text_list or []:
            plain = item.get("plain_text", "")
            href = item.get("href")
            annotations = item.get("annotations", {})
            if annotations.get("code"):
                plain = f"`{plain}`"
            elif annotations.get("bold"):
                plain = f"**{plain}**"
            elif annotations.get("italic"):
                plain = f"*{plain}*"

            if href:
                plain = f"[{plain}]({href})"
            out.append(plain)
        return "".join(out)

    @staticmethod
    def block_to_markdown(block: Dict[str, Any]) -> str:
        """Converts a single Notion block object into Markdown."""
        b_type = block.get("type", "")
        data = block.get(b_type, {})
        rich = data.get("rich_text", [])
        text = NotionNormalizer.extract_rich_text(rich)

        if b_type == "paragraph":
            return f"{text}\n"
        elif b_type == "heading_1":
            return f"# {text}\n"
        elif b_type == "heading_2":
            return f"## {text}\n"
        elif b_type == "heading_3":
            return f"### {text}\n"
        elif b_type == "bulleted_list_item":
            return f"- {text}"
        elif b_type == "numbered_list_item":
            return f"1. {text}"
        elif b_type == "to_do":
            checked = "[x]" if data.get("checked") else "[ ]"
            return f"- {checked} {text}"
        elif b_type == "toggle":
            return f"<details><summary>{text}</summary>\n"
        elif b_type == "code":
            lang = data.get("language", "text")
            return f"```{lang}\n{text}\n```"
        elif b_type == "quote":
            return f"> {text}"
        elif b_type == "callout":
            icon = data.get("icon", {}).get("emoji", "💡")
            return f"> {icon} {text}"
        elif b_type == "divider":
            return "---"
        elif b_type == "bookmark":
            url = data.get("url", "")
            return f"[{url}]({url})"
        return text

    @staticmethod
    def page_to_document(
        page: Dict[str, Any],
        blocks: List[Dict[str, Any]],
        database_title: Optional[str] = None
    ) -> KnowledgeDocument:
        """Converts a Notion page + block list into a canonical KnowledgeDocument."""
        page_id = page.get("id", "")
        url = page.get("url", f"https://notion.so/{page_id.replace('-', '')}")
        properties = page.get("properties", {})

        # Extract title
        title = "Untitled Page"
        tags: List[str] = []
        clean_props: Dict[str, Any] = {}

        for prop_name, prop_data in properties.items():
            p_type = prop_data.get("type")
            if p_type == "title":
                title_text = NotionNormalizer.extract_rich_text(prop_data.get("title", []))
                if title_text:
                    title = title_text
            elif p_type == "multi_select":
                for opt in prop_data.get("multi_select", []):
                    tags.append(opt.get("name", ""))
            elif p_type == "select":
                sel = prop_data.get("select")
                if sel and sel.get("name"):
                    clean_props[prop_name] = sel.get("name")
            elif p_type in ("rich_text", "text"):
                val = NotionNormalizer.extract_rich_text(prop_data.get(p_type, []))
                if val:
                    clean_props[prop_name] = val
            elif p_type in ("status", "email", "phone_number", "url", "number"):
                val = prop_data.get(p_type)
                if val:
                    clean_props[prop_name] = val

        # Convert blocks to markdown body
        md_lines = []
        for b in blocks:
            converted = NotionNormalizer.block_to_markdown(b)
            if converted:
                md_lines.append(converted)

        body_content = "\n\n".join(md_lines).strip()
        if not body_content:
            body_content = f"*(Empty page or structured properties only)*\n\n{clean_props}"

        return KnowledgeDocument(
            id=f"notion:{page_id}",
            source="notion",
            source_id=page_id,
            title=title,
            content=body_content,
            url=url,
            tags=tags,
            properties=clean_props,
            provenance={
                "source": "notion",
                "database": database_title,
                "page_id": page_id,
                "url": url,
                "blocks_count": len(blocks),
            },
        )
