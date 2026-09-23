# Runtime Event Bus & Observability

## 1. RuntimeEventBus Architecture

`RuntimeEventBus` (`hermes_core/runtime/events.py`) provides an asynchronous, non-blocking pub/sub event backbone for all agent activities.

## 2. Canonical Event Taxonomy

| Event Type | Stage | Payload Details |
|---|---|---|
| `agent.started` | Turn Start | `session_id`, `objective`, `model` |
| `agent.thinking` | Reasoning | Reasoning thought chunk (separated from user text) |
| `tool.started` | Tool Execution | `tool_name`, `tool_call_id`, `arguments` (sanitized) |
| `tool.completed` | Tool Success | `tool_name`, `duration_ms`, `result` |
| `tool.failed` | Tool Error | `tool_name`, `error_code`, `error_message` |
| `approval.required` | Safety Gate | `approval_id`, `action_name`, `risk_level` |
| `plan.updated` | Re-planning | Current plan steps and status |
| `verification.completed` | Completion Gate | `verified`, `evidence`, `status` |
| `agent.completed` | Turn Complete | Final response text, total token latency |

## 3. Security & Isolation

- **Subscriber Isolation**: Slow or failing event subscribers (e.g. mobile SSE drops) are isolated and cannot block or slow down agent reasoning.
- **Credential Masking**: All payload objects are sanitized through `_sanitize_data` to ensure tokens, keys, and passwords never appear in event streams or persistent logs.
