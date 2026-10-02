# PRD: Hermes Knowledge Layer with Notion and Obsidian

**Version:** 1.0  
**Status:** Implementation specification  
**Target deployment:** Hermes/Jarvis on Hugging Face Spaces

## 1. Product vision

Turn Hermes into a knowledge-aware technical assistant that can understand the user's personal/project knowledge without forcing all knowledge into the agent's internal memory.

The system should answer from the user's authorized Notion workspace and local Obsidian vault, preserve source provenance, and safely write useful outputs back to the correct knowledge system.

## 2. User stories

### US-01: Ask across knowledge

> "Hermes, what decisions did we make about the OmniRoute persistence design?"

Hermes searches authorized Notion and Obsidian sources, ranks evidence, summarizes it, and shows source references.

### US-02: Save to knowledge

> "Save this architecture decision to my Hermes project notes."

Hermes determines the configured destination, previews the write if policy requires it, and writes an idempotent note/page.

### US-03: Daily knowledge assistant

> "Review today's notes and create a concise project update in Notion."

Hermes reads configured daily-note locations, extracts meaningful changes, creates the requested Notion page, and records provenance.

### US-04: Obsidian local-first

> "Open the Hermes persistence note in Obsidian."

Hermes returns an Obsidian URI or invokes the paired companion. The vault itself stays local.

### US-05: Autonomous maintenance

> "Every evening, summarize important engineering notes and update the project knowledge base."

The scheduler executes a bounded workflow, uses source policies, writes only to authorized destinations, verifies the result, and reports failures.

## 3. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Connect/disconnect Notion account | P0 |
| FR-02 | List authorized Notion pages/data sources | P0 |
| FR-03 | Read Notion pages and nested blocks | P0 |
| FR-04 | Query Notion data sources | P0 |
| FR-05 | Create/append/update Notion content | P1 |
| FR-06 | Pair an Obsidian local bridge | P0 |
| FR-07 | Search/read Markdown notes | P0 |
| FR-08 | Parse frontmatter, tags and wikilinks | P0 |
| FR-09 | Create/append/update Markdown notes | P1 |
| FR-10 | Produce source-aware unified search | P0 |
| FR-11 | Detect stale/conflicting writes | P0 |
| FR-12 | Audit all write operations | P0 |
| FR-13 | Apply capability policy to every connector action | P0 |
| FR-14 | Work in degraded mode when a provider is unavailable | P0 |
| FR-15 | Support scheduled synchronization | P1 |
| FR-16 | Support user-triggered sync | P1 |
| FR-17 | Support source-specific retention and indexing policies | P1 |
| FR-18 | Support attachment metadata without blindly importing binaries | P2 |

## 4. Non-functional requirements

### Security

- No connector credential in Git.
- Encryption at rest for connector tokens.
- TLS for remote traffic.
- Local Obsidian bridge must require authentication.
- No public raw-vault endpoint.
- Least-privilege connector scopes.
- Audit all writes.

### Reliability

- Idempotent operations.
- Retry only transient failures.
- Exponential backoff with jitter.
- Persistent sync checkpoints.
- No silent overwrite on revision mismatch.
- Circuit breaker for repeated provider failures.

### Performance

- Cache metadata and normalized documents.
- Incrementally synchronize using source revisions/hashes where available.
- Limit page depth and attachment expansion during interactive queries.
- Use background jobs for large imports.

### Privacy

- User chooses which Notion pages/data sources are accessible.
- User chooses which Obsidian directories are exposed.
- Sensitive directories can be deny-listed.
- Retrieval should preserve source boundaries.

## 5. Common knowledge model

```yaml
KnowledgeDocument:
  id: stable internal ID
  source: notion | obsidian
  source_id: provider-specific ID/path
  title: string
  content: normalized markdown/text
  url: optional source URL
  path: optional local path
  tags: []
  links: []
  properties: {}
  hash: content hash
  revision: provider revision if available
  modified_at: timestamp
  indexed_at: timestamp
  permissions: source permission metadata
  provenance: source metadata
```

## 6. Acceptance criteria

The feature is accepted when:

- A fresh HF deployment can authorize Notion and persist its connection securely.
- A paired Obsidian bridge can be restarted without losing pairing configuration.
- A user can search both systems through one Hermes request.
- Every result identifies its source.
- A write cannot accidentally replace a newer user edit.
- A provider outage does not destroy local normalized knowledge or task state.
- A revoked connector immediately stops new access after the token is invalidated or the local pairing is revoked.
- Background sync can resume after a crash from a durable checkpoint.
