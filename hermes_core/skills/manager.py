"""
Skill Manager & Importers
==========================
Handles Skill validation, GitHub import, custom authoring, and lifecycle operations.
"""
from __future__ import annotations

import json
import re
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from hermes_core.skills.store import PersistentSkillStore, SkillRecord


class SkillValidator:
    @staticmethod
    def parse_frontmatter(content: str) -> Tuple[Dict[str, str], str]:
        """Extracts YAML frontmatter if present and returns (metadata, body)."""
        metadata: Dict[str, str] = {}
        lines = content.splitlines()
        if not lines or lines[0].strip() != "---":
            return metadata, content

        end_idx = -1
        for i in range(1, len(lines)):
            if lines[i].strip() == "---":
                end_idx = i
                break

        if end_idx == -1:
            return metadata, content

        for line in lines[1:end_idx]:
            if ":" in line:
                k, v = line.split(":", 1)
                metadata[k.strip().lower()] = v.strip().strip("'\"")

        body = "\n".join(lines[end_idx + 1:]).strip()
        return metadata, body

    @classmethod
    def validate_and_extract(
        cls,
        content: str,
        fallback_name: Optional[str] = None
    ) -> Tuple[bool, str, Dict[str, Any]]:
        """Validates SKILL.md content and extracts metadata."""
        if not content or not content.strip():
            return False, "SKILL.md content cannot be empty", {}

        frontmatter, body = cls.parse_frontmatter(content)
        name = frontmatter.get("name") or fallback_name or ""
        if not name:
            # Try to grab first H1 header
            for line in content.splitlines():
                if line.startswith("# "):
                    name = line[2:].strip()
                    break

        if not name:
            name = "custom-skill"

        clean_id = re.sub(r"[^a-zA-Z0-9_\-]+", "-", name.lower()).strip("-")
        category = frontmatter.get("category") or "Domain Specialist"
        description = frontmatter.get("description") or ""

        if not description:
            # Use first paragraph of text
            for line in body.splitlines():
                s = line.strip()
                if s and not s.startswith("#"):
                    description = s[:200]
                    break

        if not description:
            description = f"Specialized instructions for {name}."

        instructions = content.strip()

        return True, "Valid", {
            "id": clean_id,
            "name": name.replace("-", " ").title(),
            "category": category,
            "description": description,
            "instructions": instructions,
            "version": frontmatter.get("version", "1.0.0"),
            "author": frontmatter.get("author", "Community"),
        }


class GitHubSkillImporter:
    @staticmethod
    def parse_github_url(url: str) -> Dict[str, str]:
        """Parses a GitHub repo or tree URL into owner, repo, branch, path."""
        clean_url = url.strip()
        if clean_url.endswith(".git"):
            clean_url = clean_url[:-4]

        # e.g. https://github.com/owner/repo/tree/main/skills/python-pro
        # e.g. https://github.com/owner/repo/blob/main/SKILL.md
        # e.g. https://github.com/owner/repo
        pattern = r"https?://github\.com/([^/]+)/([^/]+)(?:/(?:tree|blob)/([^/]+)/(.*))?"
        match = re.match(pattern, clean_url)
        if not match:
            raise ValueError(f"Invalid GitHub repository URL: {url}")

        owner = match.group(1)
        repo = match.group(2)
        branch = match.group(3) or "main"
        path = (match.group(4) or "").strip("/")

        return {
            "owner": owner,
            "repo": repo,
            "branch": branch,
            "path": path,
            "original_url": url,
        }

    @classmethod
    def fetch_url_content(cls, url: str) -> str:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "Hermes-Agent-Skill-Importer/1.0",
                "Accept": "text/plain,application/json,*/*",
            }
        )
        with urllib.request.urlopen(req, timeout=12.0) as resp:
            if resp.status != 200:
                raise RuntimeError(f"HTTP {resp.status} fetching {url}")
            return resp.read().decode("utf-8", errors="replace")

    @classmethod
    def import_skill(cls, github_url: str) -> SkillRecord:
        parsed = cls.parse_github_url(github_url)
        owner = parsed["owner"]
        repo = parsed["repo"]
        branch = parsed["branch"]
        subpath = parsed["path"]

        # Strategy 1: Attempt to fetch SKILL.md from raw.githubusercontent.com
        candidate_paths = []
        if subpath:
            if subpath.endswith("SKILL.md"):
                candidate_paths.append(subpath)
            else:
                candidate_paths.append(f"{subpath}/SKILL.md")
                candidate_paths.append(f"{subpath}/skill.md")
        else:
            candidate_paths.extend(["SKILL.md", "skill.md", ".claude/SKILL.md", "skills/SKILL.md"])

        skill_content: Optional[str] = None
        used_path = ""

        # Try main branch and master branch
        branches_to_try = [branch]
        if branch == "main":
            branches_to_try.append("master")

        for b in branches_to_try:
            for p in candidate_paths:
                raw_url = f"https://raw.githubusercontent.com/{owner}/{repo}/{b}/{p}"
                try:
                    content = cls.fetch_url_content(raw_url)
                    if content and len(content.strip()) > 10:
                        skill_content = content
                        used_path = p
                        break
                except Exception:
                    continue
            if skill_content:
                break

        if not skill_content:
            raise FileNotFoundError(
                f"Could not locate SKILL.md in {github_url} across tried paths: {candidate_paths}"
            )

        fallback_name = Path(subpath).name if subpath else repo
        ok, msg, extracted = SkillValidator.validate_and_extract(skill_content, fallback_name=fallback_name)
        if not ok:
            raise ValueError(f"SKILL.md validation failed: {msg}")

        record = SkillRecord(
            id=extracted["id"],
            name=extracted["name"],
            category=extracted["category"],
            description=extracted["description"],
            instructions=extracted["instructions"],
            source_type="github",
            source_url=github_url,
            author=f"{owner}/{repo}",
            version=extracted.get("version", "1.0.0"),
        )
        return record


class SkillManager:
    _instance: Optional[SkillManager] = None

    def __init__(self, store: Optional[PersistentSkillStore] = None):
        self.store = store or PersistentSkillStore.get_instance()

    @classmethod
    def get_instance(cls) -> SkillManager:
        if cls._instance is None:
            cls._instance = SkillManager()
        return cls._instance

    def list_skills(self, session_id: str = "global") -> List[Dict[str, Any]]:
        records = self.store.list_skills()
        active_ids = set(self.store.get_active_skills(session_id))

        results = []
        for r in records:
            d = r.to_dict()
            d["is_active"] = r.id in active_ids
            d["can_delete"] = r.source_type != "builtin"
            results.append(d)
        return results

    def get_skill(self, skill_id: str, session_id: str = "global") -> Optional[Dict[str, Any]]:
        r = self.store.get_skill(skill_id)
        if not r:
            return None
        d = r.to_dict()
        d["is_active"] = self.store.is_active(r.id, session_id)
        d["can_delete"] = r.source_type != "builtin"
        return d

    def create_skill(
        self,
        name: str,
        category: str,
        description: str,
        instructions: str,
        author: str = "User",
        version: str = "1.0.0",
        activate: bool = True,
        session_id: str = "global"
    ) -> Dict[str, Any]:
        clean_name = name.strip()
        if not clean_name:
            raise ValueError("Skill name cannot be blank")

        clean_id = re.sub(r"[^a-zA-Z0-9_\-]+", "-", clean_name.lower()).strip("-")
        if not clean_id:
            raise ValueError(f"Invalid skill name '{name}'")

        clean_cat = category.strip() or "Custom"
        clean_desc = description.strip() or f"User-created custom skill for {clean_name}."
        clean_inst = instructions.strip()
        if not clean_inst:
            clean_inst = f"# {clean_name}\n\n{clean_desc}"

        rec = SkillRecord(
            id=clean_id,
            name=clean_name,
            category=clean_cat,
            description=clean_desc,
            instructions=clean_inst,
            source_type="custom",
            source_url="",
            author=author,
            version=version,
        )
        saved = self.store.save_skill(rec)
        if activate:
            self.store.set_activation(saved.id, True, session_id=session_id)

        d = saved.to_dict()
        d["is_active"] = activate
        d["can_delete"] = True
        return d

    def import_from_github(
        self,
        github_url: str,
        activate: bool = True,
        session_id: str = "global"
    ) -> Dict[str, Any]:
        record = GitHubSkillImporter.import_skill(github_url)
        saved = self.store.save_skill(record)
        if activate:
            self.store.set_activation(saved.id, True, session_id=session_id)

        d = saved.to_dict()
        d["is_active"] = activate
        d["can_delete"] = True
        return d

    def delete_skill(self, skill_id: str) -> bool:
        return self.store.delete_skill(skill_id)

    def set_activation(self, skill_id: str, is_active: bool, session_id: str = "global") -> bool:
        return self.store.set_activation(skill_id, is_active, session_id=session_id)

    def sync_local_skills_directory(self, skills_dir: Path) -> int:
        """Imports any SKILL.md directories found on disk into persistent store."""
        count = 0
        if not skills_dir.exists() or not skills_dir.is_dir():
            return count

        for child in skills_dir.iterdir():
            if child.is_dir():
                skill_md = child / "SKILL.md"
                if not skill_md.exists():
                    skill_md = child / "skill.md"
                if skill_md.exists():
                    try:
                        content = skill_md.read_text(encoding="utf-8", errors="replace")
                        ok, _, extracted = SkillValidator.validate_and_extract(content, fallback_name=child.name)
                        if ok:
                            existing = self.store.get_skill(extracted["id"])
                            if not existing:
                                rec = SkillRecord(
                                    id=extracted["id"],
                                    name=extracted["name"],
                                    category="Domain Specialist",
                                    description=extracted["description"],
                                    instructions=extracted["instructions"],
                                    source_type="file",
                                    source_url=str(child),
                                    author="Workspace",
                                    version=extracted.get("version", "1.0.0"),
                                )
                                self.store.save_skill(rec)
                                count += 1
                    except Exception:
                        pass
        return count
