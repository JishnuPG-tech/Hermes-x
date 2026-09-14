# Observability

## Goals

The owner should be able to answer:

- What is Hermes doing now?
- Why did it choose this action?
- Which worker is running?
- What is blocked?
- What failed?
- What has been verified?
- What remains?
- How much time and model/tool usage was consumed?

Do not expose private chain-of-thought. Expose concise execution events, decisions, tool names, results, evidence and status.

## Event pipeline

```text
Hermes / workers
      ↓
Event emitter
      ↓
Durable event log
      ├── live SSE/WebSocket
      ├── dashboard
      ├── audit store
      └── metrics exporter
```

## Core metrics

### Reliability

- task_success_total
- task_failure_total
- task_recovery_total
- task_resume_total
- verification_failure_total
- approval_timeout_total

### Performance

- task_duration_seconds
- model_first_token_seconds
- model_total_latency_seconds
- tool_duration_seconds
- queue_wait_seconds

### Resource usage

- model_tokens_total
- estimated_model_cost
- tool_calls_total
- worker_cpu_seconds
- storage_bytes

### Integrations

- github_api_errors
- notion_api_errors
- obsidian_index_errors
- integration_latency

## Run timeline

A UI timeline should render:

```text
11:20 Task created
11:20 Context loaded
11:21 Plan created: 8 subtasks
11:21 Architect started
11:24 Architecture verified
11:24 Backend worker started
11:29 Tests failed
11:30 Recovery attempt 1
11:34 Tests passed
11:35 Security review passed
11:36 PR opened
11:37 CI running
11:41 CI passed
11:41 Final verification passed
```

## Health model

Expose:

- liveness
- readiness
- durable storage health
- model provider health
- GitHub health
- Obsidian health
- Notion health
- worker health
- scheduler health

Readiness should fail if the system cannot safely persist canonical state.

## Alerts

Alert on:

- stale tasks
- repeated identical failures
- storage near capacity
- backup failure
- missing encryption key
- integration authentication failure
- worker crash loops
- queue backlog
- excessive retry rate

## Audit retention

Audit records should be retained longer than ephemeral logs. Sensitive fields must be redacted before persistence.
