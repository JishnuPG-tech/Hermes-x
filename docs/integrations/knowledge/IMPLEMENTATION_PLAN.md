# Implementation Plan: Notion + Obsidian for Hermes on Hugging Face

## Phase 0: Repository and deployment baseline

### Tasks

1. Confirm current Hermes/Jarvis API boundary.
2. Confirm persistent database location.
3. Confirm secrets storage for the HF Space.
4. Add a connector registry.
5. Add structured audit events.
6. Add feature flags so connectors can be disabled without redeploying code.

### Acceptance

The deployment starts with both connectors disabled and no connector secret required.

---

## Phase 1: Common knowledge contract

Create:

```text
src/jarvis/knowledge/
  models.py
  connector.py
  registry.py
  router.py
  normalizer.py
  provenance.py
  policy.py
  cache.py
  sync.py
  audit.py
```

Recommended models:

```text
KnowledgeDocument
KnowledgeChunk
KnowledgeSource
KnowledgeQuery
KnowledgeResult
WriteIntent
WriteResult
SyncCheckpoint
SyncConflict
ConnectorHealth
```

### Important invariant

A provider-specific object must never leak through the core interface. Convert it to the common model first.

---

## Phase 2: Notion read-only connector

Implement:

```text
NotionClient
NotionAuth
NotionPageReader
NotionBlockReader
NotionDataSourceQuery
NotionNormalizer
```

### Flow

```text
User authorizes
    ↓
Callback
    ↓
Validate state/nonce
    ↓
Encrypt token
    ↓
Store connection metadata
    ↓
Discover allowed sources
    ↓
Index metadata
```

### Read flow

```text
Query
 ↓
Knowledge Router
 ↓
Notion adapter
 ↓
Pages/data sources
 ↓
Blocks/properties
 ↓
Normalize
 ↓
Rank
 ↓
Cite source
```

Notion's current documentation supports creating and retrieving page content through block children and querying data sources. citeturn1search1turn1search6

---

## Phase 3: Notion write path

Never allow an arbitrary model string to become an unrestricted API mutation.

```text
Model proposes WriteIntent
        ↓
Policy check
        ↓
Destination validation
        ↓
Permission check
        ↓
Revision check
        ↓
Preview if required
        ↓
Execute
        ↓
Verify
        ↓
Audit
```

Start with:

- create page
- append blocks
- update selected properties

Add destructive deletion only after the safety model is proven.

---

## Phase 4: Obsidian bridge MVP

Build a small companion service/plugin.

### Recommended capabilities

```text
vault.info
vault.list
vault.search
note.read
note.create
note.append
note.update
note.move
note.open_uri
sync.status
bridge.health
```

Do **not** implement:

```text
shell.exec
filesystem.read_anywhere
filesystem.delete_anywhere
```

The bridge should expose only configured vault roots.

### Local filesystem policy

```yaml
allowed_roots:
  - /path/to/Vault/Projects/Hermes
  - /path/to/Vault/Daily
read:
  - md
  - canvas
write:
  - md
```

Attachments should be metadata-only by default.

---

## Phase 5: Secure bridge pairing

### Pairing sequence

```text
User opens Hermes Settings
        ↓
Add Obsidian
        ↓
Hermes creates one-time pairing challenge
        ↓
Local bridge displays challenge
        ↓
User confirms pairing
        ↓
Bridge creates device key
        ↓
Gateway registers device public key
        ↓
Short-lived access token issued
        ↓
Connection established
```

Store the private key only on the bridge device.

Support:

- revoke device
- rename device
- rotate key
- last seen
- capability list

---

## Phase 6: Markdown normalization

Parse:

- YAML frontmatter
- headings
- paragraphs
- lists
- code blocks
- tags
- wikilinks
- Markdown links
- embeds
- aliases

Preserve the original file path and line ranges.

Example normalized metadata:

```json
{
  "source": "obsidian",
  "source_id": "Projects/Hermes/Architecture.md",
  "title": "Architecture",
  "tags": ["hermes", "architecture"],
  "links": ["OmniRoute", "Memory"],
  "hash": "sha256:..."
}
```

---

## Phase 7: Unified retrieval

Implement hybrid retrieval:

```text
lexical search
     +
metadata filters
     +
semantic search
     +
recency
     +
project relevance
     +
source authority
     ↓
final ranking
```

Do not automatically prefer a semantically similar but stale note over a recent explicit project decision.

Suggested score:

```text
score =
  lexical * 0.20
+ semantic * 0.30
+ freshness * 0.15
+ project_match * 0.15
+ source_authority * 0.10
+ exact_entity_match * 0.10
```

Tune weights from evaluation data later.

---

## Phase 8: Sync engine

Create durable jobs:

```text
initial_import
incremental_sync
reindex
push_changes
pull_changes
reconcile_conflicts
```

Use:

- debounce
- batching
- exponential backoff
- idempotency keys
- checkpoints
- dead-letter queue
- circuit breaker

---

## Phase 9: Conflict handling

Never silently overwrite.

```text
Read source revision/hash
       ↓
User edits source
       ↓
Hermes attempts write
       ↓
Expected revision != current revision
       ↓
CONFLICT
       ↓
Fetch latest
       ↓
Three-way merge where safe
       ↓
If ambiguous → user approval
```

For Markdown, use a three-way diff. For structured Notion properties, compare field-level changes where possible.

---

## Phase 10: Memory integration

Knowledge retrieval should feed the existing memory/learning pipeline as **evidence**, not blindly as permanent memory.

```text
Knowledge result
  ↓
Evidence evaluation
  ↓
Confidence + provenance
  ↓
Temporary context
  │
  └── optional durable memory fact
```

Every durable fact should retain:

- source
- source location
- timestamp
- confidence
- last verified time

---

## Phase 11: Autonomous workflows

Examples:

### Daily project brief

```text
cron
 ↓
read today's Obsidian notes
 ↓
read configured Notion project pages
 ↓
compare with yesterday
 ↓
summarize changes
 ↓
create/update Notion report
 ↓
verify
 ↓
notify
```

### Research capture

```text
web research
 ↓
source validation
 ↓
summarize
 ↓
create Obsidian research note
 ↓
link related notes
 ↓
index
```

### Engineering memory

```text
GitHub issue/PR
 ↓
implementation
 ↓
verification
 ↓
extract durable lessons
 ↓
append to project knowledge
```

---

## Phase 12: Hugging Face deployment

### Cloud services

The Space hosts:

```text
Hermes
Jarvis API
Knowledge Router
Notion connector
Sync worker
Index
Task scheduler
```

### Local service

The user's machine hosts:

```text
Obsidian
Obsidian bridge
vault
```

### Required network rule

Prefer:

```text
Bridge → outbound TLS → HF
```

over:

```text
HF → inbound home network port
```

This reduces router exposure and avoids requiring port forwarding.

---

## Phase 13: Production hardening

Before enabling autonomous writes:

- rotate all secrets from development.
- enable audit logging.
- enforce source scopes.
- configure rate limits.
- configure backups.
- configure connector health checks.
- test revocation.
- test stale-write conflicts.
- test bridge compromise response.
- test provider outage behavior.

---

## Phase 14: Rollout gates

### Gate 1

Notion read-only passes.

### Gate 2

Obsidian read-only passes.

### Gate 3

Unified retrieval passes.

### Gate 4

Explicit writes pass with audit and conflict protection.

### Gate 5

Background sync passes restart/rollback tests.

### Gate 6

Autonomous workflows pass policy, security and recovery tests.

### Gate 7

Production enablement.
