# Architecture Decision Records

## ADR-001 — Build an Agent OS layer, not a new kernel

**Decision:** Build Jarvis Agent OS above Hermes.

**Reason:** Hermes already provides the agent harness, tools, memory, skills, sessions, subagents, and automation primitives. The missing layer is policy, durable task state, orchestration, identity, verification, and system-level lifecycle.

## ADR-002 — Phone/laptop are control surfaces

**Decision:** Execute work on the server runtime.

**Reason:** The user should not need a local development environment for server-managed projects. Clients provide voice, text, approvals, and status.

## ADR-003 — Storage Bucket is the primary mutable Space filesystem

**Decision:** Mount a read-write HF Storage Bucket at `/data` and keep application state under `/data/jarvis`.

**Reason:** Space container storage is ephemeral while attached buckets provide persistent filesystem access.

## ADR-004 — GitHub is code source of truth

**Decision:** GitHub owns source history, branches, pull requests, and CI state. Server workspaces are working copies.

**Reason:** This preserves reviewability, rollback, collaboration, and disaster recovery.

## ADR-005 — Dataset repositories are not the live OS disk

**Decision:** Use HF Datasets for large/versioned AI data, not as the mutable runtime filesystem.

**Reason:** Dataset repositories are Git/Xet-backed and versioned. Jarvis needs frequent mutable writes and filesystem-like access.

## ADR-006 — Durable tasks instead of infinite interactive loops

**Decision:** Long jobs use durable task state plus background scheduling/workers.

**Reason:** Browser sessions and interactive loops can disconnect. Durable jobs can checkpoint, recover, and continue.

## ADR-007 — Verification before completion

**Decision:** The runtime must verify artifacts and system state before marking a task complete.

**Reason:** Model self-report is insufficient evidence of successful execution.

## ADR-008 — Voice is an interface, not a second brain

**Decision:** Voice sessions call the same Jarvis task and memory system as text.

**Reason:** Separate voice state would create inconsistent context and duplicated task execution.

## ADR-009 — Kokoro primary, EdgeTTS fallback

**Decision:** Prefer self-hosted Kokoro for low-cost TTS, with EdgeTTS as a configured fallback for latency/availability.

**Reason:** The target is low first-audio latency without making a cloud TTS dependency mandatory.

## ADR-010 — Start as one integrated runtime

**Decision:** Keep Jarvis Core, Hermes, voice gateway, and Kokoro together where practical, with OmniRoute separate. Extract services only when measured resource or reliability needs justify it.

**Reason:** Fewer network hops and simpler operations during the first production stages.

## ADR-011 — Security policy cannot be self-modified by the agent

**Decision:** Agents may propose skill/runtime improvements but cannot grant themselves authority.

**Reason:** Self-improvement must not become privilege escalation.

## ADR-012 — Backups are mandatory for critical state

**Decision:** Persistent storage alone is not considered sufficient disaster recovery.

**Reason:** A persistent volume protects against ordinary container restarts but does not eliminate deletion, corruption, account, or storage failure risk.
