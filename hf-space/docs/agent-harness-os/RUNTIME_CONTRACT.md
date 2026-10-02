# Runtime Contract

## 1. Task contract

Every task must define:

```yaml
id: unique task ID
project: project ID
objective: desired outcome
context: approved context sources
constraints: hard constraints
responsibilities: work scope
success_criteria: verifiable conditions
risk_level: low|medium|high|critical
allowed_tools: tool IDs
allowed_integrations: integration IDs
approval_policy: policy ID
retry_policy: policy ID
verification: verifier definitions
outputs: expected artifacts
```

## 2. Worker contract

A worker must:

- operate only inside its assigned workspace
- use only allowed tools
- persist checkpoints
- emit events
- report structured results
- never claim verification it did not perform
- stop when its scope is complete
- escalate when blocked by authority or missing credentials

## 3. Tool contract

Each tool declares:

```yaml
name:
input_schema:
output_schema:
risk_level:
required_permissions:
network_access:
filesystem_scope:
secret_refs:
idempotency_support:
timeout:
retryable_errors:
audit_fields:
```

## 4. Event contract

Events contain:

```json
{
  "event_id": "evt_123",
  "event_type": "tool.finished",
  "task_id": "task_123",
  "run_id": "run_456",
  "timestamp": "...",
  "actor": "worker:backend",
  "status": "success",
  "payload": {},
  "correlation_id": "corr_789"
}
```

Do not place raw credentials, access tokens or sensitive prompt content into events.

## 5. Checkpoint contract

A checkpoint must be sufficient to resume without guessing:

```yaml
checkpoint_id:
task_id:
completed_nodes:
active_node:
workspace_revision:
known_failures:
retry_count:
required_external_state:
next_action:
created_at:
```

## 6. Approval contract

```yaml
approval_id:
task_id:
action:
risk:
reason:
requested_by:
requested_at:
expires_at:
status: pending|approved|denied|expired
approved_by:
```

Approval must be bound to a specific action and arguments or a tightly scoped action class. A general "yes" must not authorize unrelated future operations.

## 7. Verification contract

```yaml
verifier_id:
criteria:
checks:
status: passed|failed|inconclusive
artifacts:
evidence:
executed_at:
```

## 8. Integration contract

Integrations expose capabilities rather than raw credentials.

```text
search
read
create
update
append
move
archive
health
```

Every mutating capability must declare scope and authorization requirements.

## 9. Idempotency

External writes must include an idempotency key derived from task ID + operation ID. Retries must first determine whether the previous operation already succeeded.

## 10. Cancellation

Cancellation is cooperative. The harness sends a cancel signal, stops scheduling new work, waits for safe tool termination, checkpoints the task and marks it cancelled. Destructive rollback is not automatically implied.
