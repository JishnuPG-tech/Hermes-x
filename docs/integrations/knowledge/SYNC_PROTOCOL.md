# Knowledge Sync Protocol

## 1. Goal

Synchronize Notion and Obsidian into Hermes without creating silent data loss, duplicate documents, or unsafe writes.

## 2. Canonical states

Every source record has:

```text
new
indexed
dirty
syncing
synced
conflict
error
removed
```

## 3. Change event

```json
{
  "event_id": "evt_01",
  "connector": "obsidian",
  "source_id": "Projects/Hermes/Architecture.md",
  "operation": "updated",
  "hash": "sha256:...",
  "revision": "mtime:...",
  "occurred_at": "...",
  "device_id": "dev_01"
}
```

For Notion, provider IDs and provider revisions/timestamps are retained when available.

## 4. Checkpoint

```yaml
connector: obsidian
scope: Projects/Hermes
cursor: ...
last_event_id: evt_01
last_success_at: ...
```

Checkpoints must be durable so a process restart does not require a full rescan unless necessary.

## 5. Idempotency

Every mutation receives:

```text
idempotency_key
```

Repeated delivery of the same event must not create duplicate pages or notes.

## 6. Conflict model

### Case A: unchanged source

Safe update.

### Case B: source changed after Hermes read

Conflict.

### Case C: Hermes changed normalized copy but source changed independently

Three-way merge candidate.

### Case D: ambiguous semantic merge

Require user approval.

## 7. Three-way merge

```text
BASE
 ├── LOCAL/HERMES CHANGE
 └── REMOTE/USER CHANGE
        ↓
      MERGE
        ↓
   ┌────┴────┐
   │         │
clean      conflict
   │         │
write      review
```

Never let an LLM silently choose between conflicting user edits when the conflict changes meaning.

## 8. Sync queue

Recommended job fields:

```text
job_id
connector
source_id
operation
priority
attempt
next_attempt_at
idempotency_key
payload_ref
status
last_error
created_at
updated_at
```

Use a dead-letter queue for repeated permanent failures.

## 9. Retry policy

```text
network timeout → retry
429 → retry after provider guidance
5xx → retry with backoff
401/403 → stop + reauthorization/revocation workflow
invalid document → quarantine
conflict → reconciliation
```

## 10. Delete semantics

Deletion is more dangerous than update.

Default behavior:

```text
provider says deleted
 ↓
mark source record removed
 ↓
retain tombstone
 ↓
do not immediately erase derived memory
 ↓
apply retention policy
```

A deleted note should not automatically erase a historical task record.

## 11. Knowledge freshness

Each retrieval result should include:

```text
source_modified_at
indexed_at
age
stale=true/false
```

For high-value project decisions, prefer fresh source content over old embeddings.

## 12. Sync direction

Support explicit modes:

```text
pull-only
push-only
bidirectional
```

Start production with `pull-only` for both providers. Enable bidirectional synchronization only after conflict and audit tests pass.

## 13. Source authority

Suggested hierarchy:

```text
Active project decision in designated Notion decision record
        >
recent explicit Obsidian project note
        >
older project documentation
        >
derived summary
        >
LLM-generated memory
```

This is configurable per project.

## 14. Reconciliation report

Each sync run should produce:

```yaml
run_id: sync_01
started_at: ...
finished_at: ...
read: 120
created: 8
updated: 12
unchanged: 94
conflicts: 2
errors: 4
verified: 16
```

## 15. Recovery

If a sync worker crashes:

```text
restart
 ↓
load checkpoint
 ↓
load in-flight jobs
 ↓
check idempotency state
 ↓
resume
 ↓
verify
```

Never assume an interrupted HTTP request means the provider did not mutate data.
