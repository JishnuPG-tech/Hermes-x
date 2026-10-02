# Test Plan: Notion + Obsidian Knowledge Layer

## 1. Unit tests

### Common layer

- document normalization.
- chunking.
- provenance generation.
- path normalization.
- hash calculation.
- idempotency key generation.
- ranking.
- policy decisions.

### Obsidian bridge

- allowlisted path accepted.
- non-allowlisted path rejected.
- traversal rejected.
- atomic write.
- malformed Markdown tolerated.
- watcher debounce.
- URI encoding.

### Notion

- page normalization.
- nested block traversal.
- data-source property normalization.
- rate-limit classification.
- auth error classification.

## 2. Integration tests

### Notion

1. Authorize test connection.
2. Discover allowed pages/data sources.
3. Read nested page content.
4. Query structured data source.
5. Create test page.
6. Append content.
7. Read back and verify.
8. Revoke connection.
9. Confirm access stops.

### Obsidian

1. Pair bridge.
2. Read vault metadata.
3. Search allowlisted directory.
4. Read note.
5. Create note.
6. Append note.
7. Update note with expected hash.
8. Modify note externally.
9. Attempt stale update.
10. Confirm conflict.
11. Revoke bridge.
12. Confirm requests fail.

## 3. Recovery tests

Simulate:

- network timeout.
- provider 429.
- provider 5xx.
- bridge offline.
- process crash.
- duplicate event.
- duplicate write.
- interrupted write.
- index unavailable.
- database unavailable.

Expected behavior:

```text
retry transient
pause permanent
resume from checkpoint
never silently overwrite
never lose audit event
```

## 4. Security tests

- prompt injection in Notion page.
- prompt injection in Obsidian note.
- path traversal.
- symlink escape attempt.
- oversized bridge request.
- malformed JSON.
- replayed request.
- invalid device key.
- revoked device.
- leaked token in logs.
- connector scope violation.

## 5. Concurrency tests

Test:

- two simultaneous reads.
- two simultaneous writes to different notes.
- two writes to same note.
- sync plus manual user edit.
- duplicate event delivery.

## 6. End-to-end test

User says:

> "Read my Hermes project notes, find the current OmniRoute persistence decision, summarize it, and save a concise decision record to Notion."

Expected:

```text
intent classification
 ↓
source policy
 ↓
Obsidian search
 ↓
Notion search if configured
 ↓
provenance-preserving synthesis
 ↓
WriteIntent
 ↓
policy approval
 ↓
Notion write
 ↓
read-back verification
 ↓
audit
```

## 7. Autonomous test

Scheduled job:

> "Every evening, summarize important Hermes engineering changes from today's Obsidian notes and update the project report in Notion."

Verify:

- scheduler persistence.
- source availability handling.
- deduplication.
- write idempotency.
- failure recovery.
- notification.
- audit.

## 8. Disaster/restart tests

Restart the HF Space during:

- retrieval.
- sync.
- Notion write.
- conflict resolution.

Restart the Obsidian bridge during:

- file upload.
- change event.
- write operation.

The system must resume without duplicate writes or corrupted notes.

## 9. Performance tests

Measure:

```text
interactive search latency
Notion API latency
bridge round-trip latency
index throughput
initial vault import time
incremental sync rate
memory usage
queue depth
```

## 10. Production gate

Do not enable autonomous bidirectional writes until all P0 tests pass and the following are demonstrated:

- no silent overwrite.
- reliable revocation.
- durable audit trail.
- restart-safe sync.
- prompt injection isolation.
- bridge path sandboxing.
- provider outage recovery.
