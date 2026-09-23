# Sovereign Agent Runtime Test Plan

## 1. Test Scope & Matrix

This test suite rigorously validates the 91-section specification for the Hermes sovereign agent runtime across 6 key pillars:

| Pillar | Test Area | Verification Method |
|---|---|---|
| **1. Intent Sovereignty** | Natural language queries without tool names trigger autonomous tool selection | `tests/test_agent_runtime.py::test_natural_language_tool_selection` |
| **2. Dynamic Tool Discovery** | Adding tools without modifying agent code; `search_tools` discovery | `tests/test_agent_runtime.py::test_dynamic_tool_addition` & `test_search_tools` |
| **3. Grounded Verification** | VerificationGate rejects claims without empirical disk or process evidence | `tests/test_agent_runtime.py::test_verification_gate_rejection` |
| **4. Loop Protection** | Infinite tool call repetition triggers breaker and forces re-planning | `tests/test_agent_runtime.py::test_loop_detector` |
| **5. Policy & Permissions** | RBAC permission denial and approval interception | `tests/test_agent_runtime.py::test_policy_and_permissions` |
| **6. Secret Redaction** | Event bus and logging redact tokens, keys, and passwords | `tests/test_agent_runtime.py::test_credential_sanitization` |

## 2. Test Execution Command

Run the unified runtime test suite:
```bash
python -m unittest tests/test_agent_runtime.py -v
```
