# Runtime Contract

## Task contract

Every autonomous task MUST have:

```yaml
id: task-...
project: hermes-x
objective: "..."
workspace: /data/jarvis/workspaces/task-...
git:
  remote: JishnuPG-tech/Hermes-x
  base_ref: main
permissions:
  terminal: true
  filesystem: project-only
  network: declared
  github_write: approval-required
verification:
  required: true
completion:
  evidence_required: true
```

## State machine

```text
CREATE
  ↓
INITIALIZE
  ↓
AUTHORIZE
  ↓
RUN ↔ PAUSE
  ↓
CHECKPOINT
  ↓
VERIFY
  ├── fail → RECOVER → RUN
  └── pass → COMPLETE

Any terminal failure → FAILED → ARCHIVE
```

## Idempotency

Operations should be idempotent where possible. Every external mutation must have an operation ID so retries can detect already-completed actions.

Examples:

- Git push: verify remote ref before retry.
- Database migration: record migration version.
- Deployment: record release ID.
- Artifact upload: use content hash.

## Checkpoint contract

Before long or risky operations, persist:

- current phase
- last successful action
- workspace path
- Git SHA
- pending tool call if safely resumable
- child worker IDs
- retry count
- verification results
- user approval state

## Worker contract

Subagents receive:

- narrow objective
- explicit inputs
- workspace
- allowed tools
- expected artifact
- verification command
- timeout
- reporting format

Workers may not broaden their own permissions.

## Completion contract

A task is complete only when:

1. objective-specific implementation exists
2. required tests pass
3. required build/security checks pass
4. relevant Git state is verified
5. expected artifact exists
6. remote state is verified when applicable
7. durable task state is updated

## Retry contract

Retries are bounded per failure class. After repeated failure, the system must change strategy, gather new evidence, or request intervention.

## Approval contract

Approval is required for configured high-risk operations such as:

- production deletion
- destructive database operations
- secret exposure
- changing authentication/authorization
- publishing private data
- destructive infrastructure changes
- irreversible external side effects

Approval is attached to a specific operation and expires when its scope changes.
