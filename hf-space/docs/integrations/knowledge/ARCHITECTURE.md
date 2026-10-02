# Hermes Notion + Obsidian Integration Architecture

## 1. Architectural goal

Add knowledge connectors without weakening the existing Jarvis execution architecture.

```text
                    USER
                      │
              Mobile / Web / Voice
                      │
                      ▼
             ┌───────────────────┐
             │   Jarvis Gateway  │
             │ Auth + API + WS   │
             └─────────┬─────────┘
                       │
                       ▼
             ┌───────────────────┐
             │ Hermes Orchestrator│
             └───────┬───────────┘
                     │
       ┌─────────────┼─────────────────┐
       │             │                 │
       ▼             ▼                 ▼
   Planner        Policy          Memory/Learning
       │             │                 │
       └─────────────┼─────────────────┘
                     ▼
             Knowledge Router
                     │
        ┌────────────┴─────────────┐
        ▼                          ▼
 Notion Connector             Obsidian Connector
 OAuth/API                    Local Bridge Protocol
        │                          │
        ▼                          ▼
 Notion Workspace          User's Local Vault
```

## 2. Trust boundaries

### Boundary A: Internet → HF Space

Protect with TLS, authentication, rate limiting and request validation.

### Boundary B: HF Space → Notion

Use an official Notion connection and least-privilege access. Treat all retrieved content as untrusted data, not instructions.

### Boundary C: HF Space → Obsidian Bridge

This is the most important boundary. The bridge must not expose the entire filesystem or a shell. It exposes only explicitly allowed vault operations.

### Boundary D: Knowledge → Agent execution

A note can contain text such as `run this command`. Hermes must treat it as knowledge unless the user explicitly authorizes execution through the policy layer.

## 3. Connector abstraction

Define a common internal interface:

```python
class KnowledgeConnector:
    async def health(self): ...
    async def capabilities(self): ...
    async def search(self, query, scope=None): ...
    async def get(self, source_id): ...
    async def create(self, document, destination): ...
    async def update(self, source_id, document, expected_revision=None): ...
    async def append(self, source_id, content, expected_revision=None): ...
    async def delete(self, source_id): ...
    async def changes(self, checkpoint=None): ...
```

The connector returns normalized records. Provider-specific objects remain inside the adapter.

## 4. Knowledge router

The router chooses sources based on:

- user request
- project context
- source availability
- source permissions
- configured source priority
- freshness requirements
- privacy classification
- query type

Example:

```text
"Find the latest project decision"
        │
        ▼
classify = knowledge_search
        │
        ├── Notion: project databases/pages
        └── Obsidian: project folder + daily notes
        │
        ▼
parallel retrieval
        │
        ▼
normalize
        │
        ▼
deduplicate + rank
        │
        ▼
answer with provenance
```

## 5. Index architecture

Use two levels.

### Source metadata index

Stores:

```text
connector_id
source_id
title
path/url
hash
revision
modified_at
last_seen_at
permissions
sync_state
```

### Semantic retrieval index

Stores chunks derived from normalized documents. Chunk IDs must point back to the source document and exact source location.

Do not make embeddings the only copy of knowledge.

```text
Source → Normalizer → Canonical document → Chunker → Index
                         │
                         └──────────────→ metadata DB
```

## 6. Notion architecture

Notion content is represented through pages and blocks. Data sources can be queried for structured records. Hermes should keep provider-specific IDs and properties in metadata while exposing a normalized document to the rest of Jarvis. citeturn1search1turn1search0turn1search6

Use:

- OAuth/connection flow.
- Token vault.
- Page/data-source discovery.
- Page/block reader.
- Data-source query adapter.
- Page/block writer.
- Rate-limit aware queue.
- Revision-aware write guard.

## 7. Obsidian architecture

Obsidian remains local. The companion/bridge runs next to the vault.

```text
Obsidian Vault
   │
   ├── Markdown
   ├── .obsidian configuration
   ├── attachments
   └── metadata
          │
          ▼
   Hermes Bridge
   │  allowlisted roots
   │  Markdown parser
   │  file watcher
   │  auth
   │  audit
   │
   ▼
   Secure outbound connection
          │
          ▼
   HF Hermes Gateway
```

The bridge should initiate the outbound connection. This avoids exposing an inbound port on the user's home network.

Obsidian itself supports URI actions for opening, creating and searching notes. Hermes can use these URIs as a lightweight handoff to the user's Obsidian application rather than trying to remotely control the Obsidian UI. citeturn1search2turn1search3

## 8. Bridge protocol

Recommended transport:

- HTTPS/WebSocket outbound connection.
- Short-lived access token.
- Device/bridge identity.
- Request ID and nonce.
- Heartbeat.
- Capability negotiation.

Example request:

```json
{
  "id": "req_123",
  "method": "vault.search",
  "params": {
    "query": "OmniRoute persistence",
    "roots": ["Projects/Hermes"],
    "limit": 20
  },
  "auth": {
    "device_id": "dev_01",
    "nonce": "..."
  }
}
```

Example response:

```json
{
  "id": "req_123",
  "ok": true,
  "result": {
    "matches": [
      {
        "path": "Projects/Hermes/OmniRoute.md",
        "title": "OmniRoute",
        "line_start": 42,
        "line_end": 58,
        "hash": "sha256:..."
      }
    ]
  }
}
```

## 9. Event-driven synchronization

```text
Vault change
   ↓
Bridge watcher
   ↓
Debounce
   ↓
Hash + metadata
   ↓
Change event
   ↓
HF queue
   ↓
Normalize/index
   ↓
Memory update
```

For Notion:

```text
Notion change/event/poll
   ↓
Connector
   ↓
Normalize
   ↓
Hash/revision comparison
   ↓
Queue
   ↓
Index
```

Use polling where webhook/event support is not available for the exact operation required. Never assume an event source is perfectly reliable.

## 10. Operational state

Persist:

```text
connector_accounts
connector_capabilities
source_records
sync_checkpoints
sync_events
sync_conflicts
knowledge_documents
knowledge_chunks
write_operations
bridge_devices
```

## 11. Failure isolation

```text
Notion down
  → Obsidian + local index continue

Obsidian bridge offline
  → Notion + cached index continue

Both down
  → existing indexed knowledge remains queryable
  → new writes are queued only when policy allows

Index unavailable
  → direct provider retrieval may still work
```

## 12. Security rule

Knowledge connectors are data sources, not execution channels. Connector content must never directly invoke terminal, browser, GitHub write, deployment, or server administration capabilities.

Every transition from knowledge to action goes through the existing Jarvis planner and policy engine.
