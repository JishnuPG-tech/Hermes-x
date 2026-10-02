"""
Unit Tests for Skills V2 Persistent Store and Manager
"""
import shutil
import tempfile
import unittest
from pathlib import Path

from hermes_core.skills.store import PersistentSkillStore, SkillRecord
from hermes_core.skills.manager import SkillManager, SkillValidator, GitHubSkillImporter


class TestSkillsStoreAndManager(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.db_path = Path(self.temp_dir) / "test_skills.db"
        self.store = PersistentSkillStore(self.db_path)
        self.manager = SkillManager(self.store)

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_save_and_retrieve_skill(self):
        rec = SkillRecord(
            id="test-skill",
            name="Test Skill",
            category="Testing",
            description="A test skill for verification",
            instructions="# Test Skill Instructions\n\nRun tests rigorously.",
            source_type="custom",
        )
        saved = self.store.save_skill(rec)
        self.assertEqual(saved.id, "test-skill")

        fetched = self.store.get_skill("test-skill")
        self.assertIsNotNone(fetched)
        self.assertEqual(fetched.name, "Test Skill")
        self.assertEqual(fetched.category, "Testing")
        self.assertIn("Run tests rigorously", fetched.instructions)

    def test_persistent_activation_state(self):
        rec = SkillRecord(
            id="persist-skill",
            name="Persist Skill",
            category="Engineering",
            description="Testing persistent activation",
            instructions="Execute persistent actions",
        )
        self.store.save_skill(rec)

        # Initially inactive
        self.assertFalse(self.store.is_active("persist-skill"))

        # Activate globally
        ok = self.store.set_activation("persist-skill", True, session_id="global")
        self.assertTrue(ok)
        self.assertTrue(self.store.is_active("persist-skill"))

        # Reopen same SQLite database file to verify persistence
        new_store = PersistentSkillStore(self.db_path)
        self.assertTrue(new_store.is_active("persist-skill"))

        # Deactivate
        new_store.set_activation("persist-skill", False, session_id="global")
        self.assertFalse(new_store.is_active("persist-skill"))

    def test_create_and_delete_custom_skill(self):
        created = self.manager.create_skill(
            name="API Optimizer",
            category="Performance",
            description="Optimizes REST and GraphQL payloads",
            instructions="# API Optimizer\n\nBenchmark endpoint latencies.",
            activate=True,
        )
        self.assertEqual(created["id"], "api-optimizer")
        self.assertTrue(created["is_active"])
        self.assertTrue(created["can_delete"])

        # Listing includes it
        skills = self.manager.list_skills()
        self.assertTrue(any(s["id"] == "api-optimizer" for s in skills))

        # Delete it
        del_ok = self.manager.delete_skill("api-optimizer")
        self.assertTrue(del_ok)
        self.assertIsNone(self.store.get_skill("api-optimizer"))

    def test_skill_validator_frontmatter(self):
        sample_md = """---
name: security-scanner
category: Security
description: Automated vulnerability triage and scanner
version: 2.1.0
---

# Security Scanner Body
Analyze HTTP headers and auth tokens.
"""
        ok, msg, extracted = SkillValidator.validate_and_extract(sample_md)
        self.assertTrue(ok)
        self.assertEqual(extracted["id"], "security-scanner")
        self.assertEqual(extracted["name"], "Security Scanner")
        self.assertEqual(extracted["category"], "Security")
        self.assertEqual(extracted["version"], "2.1.0")

    def test_github_url_parser(self):
        url = "https://github.com/anthropics/skills/tree/main/skills/python-pro"
        parsed = GitHubSkillImporter.parse_github_url(url)
        self.assertEqual(parsed["owner"], "anthropics")
        self.assertEqual(parsed["repo"], "skills")
        self.assertEqual(parsed["branch"], "main")
        self.assertEqual(parsed["path"], "skills/python-pro")


if __name__ == "__main__":
    unittest.main()
