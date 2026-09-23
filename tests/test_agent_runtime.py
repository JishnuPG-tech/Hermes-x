"""
Comprehensive Test Suite for Hermes Sovereign Autonomous Agent Runtime
Validates all 91 criteria:
1. Dynamic capability discovery & search_tools
2. Zero-code new tool addition (e.g. get_weather)
3. Natural language tool resolution without keyword detection
4. VerificationGate empirical validation & false-completion rejection
5. ExecutionPlanner loop protection & circuit breaking
6. RBAC permission & policy enforcement
7. Secret redaction and event streaming
"""
import asyncio
import os
import unittest
from pathlib import Path
from typing import Dict, Any

from hermes_core.runtime.models import (
    ToolMetadata,
    ToolCategory,
    ToolResult,
    ExecutionContext,
)
from hermes_core.runtime.events import RuntimeEventBus
from hermes_core.runtime.tool_discovery import CapabilityIndex, capability_index
from hermes_core.runtime.tool_controller import ToolExecutor, tool_executor
from hermes_core.runtime.verifier import VerificationGate, verification_gate
from hermes_core.runtime.planner import ExecutionPlanner, execution_planner
from hermes_core.runtime.agent_runtime import AgentRuntime
from hermes_core.tools.registry import registry


class TestAgentRuntime(unittest.TestCase):
    """Hermes Agent Runtime Verification Test Suite."""

    def setUp(self):
        self.loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self.loop)

    def tearDown(self):
        self.loop.close()

    def test_dynamic_tool_registration(self):
        """Validates that a new capability (e.g. get_weather) can be added without modifying agent or routing code."""
        # 1. Register new tool dynamically via decorator
        @registry.register(
            name="get_weather",
            description="Retrieve real-time weather conditions for any worldwide city or coordinates.",
            parameters={
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "City name"}
                },
                "required": ["city"],
            },
            category="system",
        )
        def get_weather(city: str) -> str:
            return f"Weather in {city}: 22°C, Partly Cloudy, Wind 10km/h."

        # 2. Verify capability index has indexed it automatically
        tool_meta = capability_index.get_tool("get_weather")
        self.assertIsNotNone(tool_meta, "Dynamically registered tool must appear in CapabilityIndex")
        self.assertEqual(tool_meta.name, "get_weather")

        # 3. Verify it is discoverable via semantic search
        matches = capability_index.search_capabilities("What is the weather outside?")
        matching_names = [m.name for m in matches]
        self.assertIn("get_weather", matching_names, "Tool should be discoverable via semantic query")

        # 4. Verify execution through canonical ToolExecutor
        ctx = ExecutionContext(user_id="test_user", session_id="test_sess", is_admin=True)
        res: ToolResult = self.loop.run_until_complete(
            tool_executor.execute("get_weather", {"city": "Tokyo"}, ctx)
        )
        self.assertTrue(res.success)
        self.assertIn("Tokyo: 22°C", res.result)

    def test_search_tools_meta_discovery(self):
        """Validates that search_tools discovers tools dynamically based on capabilities needed."""
        ctx = ExecutionContext(user_id="test_user", session_id="test_sess", is_admin=True)
        res = self.loop.run_until_complete(
            tool_executor.execute("search_tools", {"query": "inspect directory files"}, ctx)
        )
        self.assertTrue(res.success)
        data = res.result
        self.assertIn("tools", data)
        tool_names = [t["name"] for t in data["tools"]]
        # list_dir or list_directory should be in discovered results
        self.assertTrue(any("dir" in name for name in tool_names))

    def test_verification_gate_rejection_of_false_claims(self):
        """Validates that VerificationGate rejects completion claims lacking empirical evidence."""
        # Case 1: Required file does NOT exist
        missing_file = "/tmp/hermes_test_nonexistent_file_xyz.txt"
        if os.path.exists(missing_file):
            os.remove(missing_file)

        res_missing = self.loop.run_until_complete(
            verification_gate.verify_action_completion(
                objective="Create the configuration file",
                expected_files=[missing_file],
            )
        )
        self.assertFalse(res_missing.verified, "Verification must fail when expected file is missing")
        self.assertIn("Missing expected file", res_missing.evidence.get("missing_files", [""])[0])

        # Case 2: Subprocess return code indicates failure (exit code > 0)
        res_exit_err = self.loop.run_until_complete(
            verification_gate.verify_action_completion(
                objective="Run test suite",
                last_exit_code=1,
            )
        )
        self.assertFalse(res_exit_err.verified, "Verification must fail when command exit code != 0")

        # Case 3: Genuine empirical success
        test_file = Path("test_artifact_verification.tmp")
        test_file.write_text("Empirical test evidence confirmed", encoding="utf-8")
        try:
            res_success = self.loop.run_until_complete(
                verification_gate.verify_action_completion(
                    objective="Generate artifact",
                    expected_files=[str(test_file)],
                    last_exit_code=0,
                )
            )
            self.assertTrue(res_success.verified, "Verification should pass when evidence is present")
        finally:
            if test_file.exists():
                test_file.unlink()

    def test_execution_planner_loop_protection(self):
        """Validates that repeating an identical failing tool call triggers loop detection."""
        planner = ExecutionPlanner()
        call_tool = "read_file"
        call_args = {"path": "/invalid/path/file.txt"}

        # First failure: no loop
        is_loop = planner.record_call_and_check_loop(call_tool, call_args, success=False)
        self.assertFalse(is_loop)

        # Second failure: no loop
        is_loop = planner.record_call_and_check_loop(call_tool, call_args, success=False)
        self.assertFalse(is_loop)

        # Third failure with identical args: loop breaker triggers!
        is_loop = planner.record_call_and_check_loop(call_tool, call_args, success=False)
        self.assertTrue(is_loop, "Loop detector must trigger on 3 identical failed calls")

        # Test breaker guidance message
        breaker_msg = planner.get_loop_breaker_guidance(call_tool)
        self.assertIn("TOOL LOOP DETECTED", breaker_msg)

    def test_rbac_permission_enforcement(self):
        """Validates that non-admin context lacking declared permissions is denied execution."""
        # Register a restricted admin tool
        @registry.register(
            name="restricted_system_purge",
            description="Purge system telemetry caches.",
            parameters={"type": "object", "properties": {}},
            category="system",
            permissions=["system.admin"],
        )
        def restricted_system_purge():
            return "Purged."

        # Non-admin context without 'system.admin' permission
        non_admin_ctx = ExecutionContext(
            user_id="guest_user",
            session_id="guest_sess",
            permissions=["files.read"],
            is_admin=False,
        )

        res = self.loop.run_until_complete(
            tool_executor.execute("restricted_system_purge", {}, non_admin_ctx)
        )
        self.assertFalse(res.success)
        self.assertEqual(res.error.get("code"), "AUTHORIZATION_DENIED")

    def test_credential_sanitization(self):
        """Validates that RuntimeEventBus automatically redacts credentials and tokens."""
        bus = RuntimeEventBus.get_instance()
        sensitive_payload = {
            "api_key": "sk-1234567890abcdef1234567890",
            "token": "ghp_abcdefghijklmnop1234567890",
            "password": "SuperSecretPassword123!",
            "normal_field": "My secret is sk-1234567890abcdef1234567890123456",
        }
        sanitized = bus._sanitize_data(sensitive_payload)
        self.assertEqual(sanitized["api_key"], "[REDACTED]")
        self.assertEqual(sanitized["token"], "[REDACTED]")
        self.assertEqual(sanitized["password"], "[REDACTED]")
        self.assertIn("[REDACTED_SECRET]", sanitized["normal_field"])


    def test_agent_runtime_singleton(self):
        """Validates that AgentRuntime singleton initializes and wires all core components."""
        runtime = AgentRuntime.get_instance()
        self.assertIsNotNone(runtime)
        self.assertIsNotNone(runtime.tool_executor)
        self.assertIsNotNone(runtime.capability_index)
        self.assertIsNotNone(runtime.event_bus)

    def test_natural_language_tool_selection_without_keywords(self):
        """Validates natural language search returns knowledge tools without hardcoded keyword rules."""
        # Query does not contain the word 'notion', 'obsidian', or 'knowledge'
        matches = capability_index.search_capabilities("Where did I write down our project roadmap and sprint decisions?")
        tool_names = [m.name for m in matches]
        # Should match search_knowledge or notion_search based on descriptions/params
        self.assertTrue(
            any("knowledge" in name or "notion" in name for name in tool_names),
            f"Expected knowledge/notion tools to match semantic query, got: {tool_names}"
        )

    def test_audit_final_claim_rejection(self):
        """Validates that audit_final_claim rejects claims when no tools were executed."""
        # Claim that file was created with 0 tools executed
        valid, reason = verification_gate.audit_final_claim(
            assistant_text="I have created the file config.json with your settings.",
            tool_results=[],
        )
        self.assertFalse(valid, "Claim should be rejected when no tool was executed")
        self.assertIn("no tool was executed", reason)

        # Claim that task succeeded when tools failed
        failed_res = ToolResult(success=False, tool="bash", error={"message": "Syntax error"})
        valid, reason = verification_gate.audit_final_claim(
            assistant_text="The task is completely fixed and all done!",
            tool_results=[failed_res],
        )
        self.assertFalse(valid, "Claim should be rejected when tools failed")

    def test_context_builder_dynamic_catalog(self):
        """Validates ContextBuilder formats instructions and includes workspace context."""
        from hermes_core.runtime.context import ContextBuilder
        ctx = ExecutionContext(user_id="user_123", session_id="sess_abc", workspace_path="/workspace/hermes", is_admin=True)
        messages = [{"role": "user", "content": "Analyze the project structure"}]
        assembled = ContextBuilder.assemble_messages(messages, context=ctx)
        self.assertGreater(len(assembled), 1)
        system_content = assembled[0]["content"]
        self.assertIn("Hermes Agent", system_content)
        self.assertIn("/workspace/hermes", system_content)


    def test_canonical_master_prompt_loaded(self):
        """Validates that ContextBuilder loads the authoritative hermes_master.md prompt."""
        from hermes_core.runtime.context import ContextBuilder, get_canonical_master_prompt
        prompt = get_canonical_master_prompt()
        self.assertIn("<hermes_system>", prompt)
        self.assertIn("<identity>", prompt)
        self.assertIn("You are Hermes Agent.", prompt)
        self.assertIn("<agent_loop>", prompt)
        self.assertIn("<verification>", prompt)

        ctx = ExecutionContext(user_id="alice", session_id="test_sess_master", is_admin=True)
        full_sys_prompt = ContextBuilder.build_system_prompt(ctx)
        self.assertIn("<hermes_system>", full_sys_prompt)
        self.assertIn("<runtime_context>", full_sys_prompt)
        self.assertIn("alice", full_sys_prompt)

    def test_notion_knowledge_discovery(self):
        """Validates that Notion tools are registered and semantically discoverable."""
        matches = capability_index.search_capabilities("Search my Notion pages for database entries")
        tool_names = [m.name for m in matches]
        self.assertTrue(
            any("notion" in name or "knowledge" in name for name in tool_names),
            f"Expected notion tools in search results, got: {tool_names}"
        )
        self.assertIsNotNone(capability_index.get_tool("notion_search"))
        self.assertIsNotNone(capability_index.get_tool("notion_read_page"))
        self.assertIsNotNone(capability_index.get_tool("notion_create_page"))

    def test_agent_executor_delegation_to_agent_runtime(self):
        """Validates that gateway.agent_executor routes to canonical runtime and tools."""
        from gateway import agent_executor as ae
        # build_system_prompt_with_skills should use canonical master prompt
        prompt = ae.build_system_prompt_with_skills("sess_delegation")
        self.assertIn("<hermes_system>", prompt)
        self.assertIn("sess_delegation", prompt)

        # execute_tool_call should route to canonical tool_executor
        res = self.loop.run_until_complete(
            ae.execute_tool_call("search_tools", {"query": "file editor"})
        )
        self.assertIn("edit_file", res)


if __name__ == "__main__":
    unittest.main()

