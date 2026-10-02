# Hermes Knowledge Integrations

## Notion + Obsidian Architecture for the Hermes/Jarvis Hugging Face Deployment

**Status:** Architecture and implementation specification  
**Target:** Hermes Agent + Jarvis orchestration + OmniRoute on Hugging Face Spaces  
**Integrations:** Notion and Obsidian  
**Version:** 1.0  
**Date:** 2026-09-14

---

## 1. Purpose

This package defines how Hermes becomes a practical knowledge-aware assistant by connecting two complementary knowledge systems:

- **Notion** for structured cloud knowledge, project databases, tasks, documents, meeting notes, and team collaboration.
- **Obsidian** for local-first Markdown knowledge, personal notes, backlinks, graph relationships, daily notes, and offline work.

The integration is designed for the existing Hermes/Jarvis architecture rather than creating a second assistant.

```text
                         ┌──────────────────────┐
                         │   Hermes Mobile UI   │
                         │  Chat / Voice / Jobs  │
                         └──────────┬───────────┘
                                    │ HTTPS / WebSocket
                                    ▼
                         ┌──────────────────────┐
                         │    Jarvis Gateway    │
                         │ auth / policy / API  │
                         └──────────┬───────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                ▼                   ▼                   ▼
         ┌─────────────┐     ┌──────────────┐    ┌──────────────┐
         │ Hermes Core │     │ Memory/Learn │    │   OmniRoute  │
         └──────┬──────┘     └──────────────┘    └──────────────┘
                │
        ┌───────┴────────────────────────────┐
        ▼                                    ▼
┌──────────────────┐                 ┌────────────────────┐
│ Notion Connector │                 │ Obsidian Bridge     │
│ OAuth + REST API │                 │ local vault agent   │
└────────┬─────────┘                 └──────────┬─────────┘
         │                                      │
         ▼                                      ▼
┌──────────────────┐                 ┌────────────────────┐
│ Notion workspace │                 │ Local Markdown      │
└──────────────────┘                 │ Obsidian vault      │
                                      └────────────────────┘
```

## 2. Core design decision

**Do not treat Notion or Obsidian as the Jarvis operational database.**

Jarvis keeps task state, approvals, execution events, audit records, checkpoints, and security policy in its own durable state store. Notion and Obsidian are knowledge systems and synchronization targets.

This prevents a deleted note, API outage, sync conflict, or malformed Markdown file from corrupting autonomous task execution.

## 3. Document index

| Document | Purpose |
|---|---|
| `PRD.md` | Product requirements and user stories |
| `ARCHITECTURE.md` | Complete integration architecture and data flow |
| `IMPLEMENTATION_PLAN.md` | Phased implementation plan for the HF deployment |
| `NOTION.md` | Notion connection, OAuth, data model, sync and permissions |
| `OBSIDIAN.md` | Obsidian local bridge, vault access, mobile/desktop strategy |
| `SYNC_PROTOCOL.md` | Canonical sync protocol, conflict handling and event model |
| `CONFIG_CONTRACT.md` | Environment variables, secrets and configuration contract |
| `SECURITY.md` | Threat model, capabilities, secret handling and privacy |
| `TEST_PLAN.md` | Unit, integration, failure, security and end-to-end tests |

## 4. Definition of done

The integration is production-ready only when Hermes can:

1. Connect to Notion without storing a permanent plaintext OAuth token in source code.
2. Discover authorized Notion pages/data sources according to granted access.
3. Read and write Notion content through a controlled connector.
4. Search and summarize Notion knowledge with citations back to source pages.
5. Connect to an Obsidian vault through an authenticated local bridge.
6. Read Markdown, frontmatter, links, tags and selected attachments.
7. Create, update and safely append Obsidian notes.
8. Open an Obsidian note on a paired device using an Obsidian URI when appropriate.
9. Keep local Obsidian access private and never expose the vault directly to the public Internet.
10. Normalize both sources into a common knowledge representation.
11. Track source IDs, paths, hashes, revisions and sync timestamps.
12. Detect and resolve stale writes instead of silently overwriting user edits.
13. Continue operating when either knowledge provider is temporarily unavailable.
14. Respect source-level permissions and Jarvis capability policy.
15. Record every write operation in the audit trail.
16. Allow the user to ask: `What do I know about this project?` and retrieve relevant knowledge from both sources.
17. Allow the user to say: `Save this to my project notes` and route the content to the selected source.

## 5. Important limitation

A Hugging Face Space is a cloud runtime. It cannot safely assume that the user's private Obsidian vault exists inside the Space. Obsidian integration therefore uses a **local bridge/companion** running beside the vault. The bridge makes narrowly scoped operations available to Hermes while the vault remains local.

Notion is different: its official API is designed for cloud access. Hermes can connect to Notion from the Space after the user authorizes the integration.

## 6. Reference basis

Notion's current API models page content as blocks and provides APIs for creating, reading and appending page content. Current Notion API documentation also exposes data-source retrieval and querying. citeturn1search1turn1search0turn1search6

Obsidian officially supports an `obsidian://` URI protocol for opening, creating, searching and working with notes, which is useful for a companion-driven user experience. citeturn1search2turn1search3

## 7. Recommended rollout

**Phase A:** Notion read-only + Obsidian read-only.  
**Phase B:** Explicit user-approved writes.  
**Phase C:** Source-aware memory retrieval.  
**Phase D:** Background synchronization.  
**Phase E:** Autonomous knowledge maintenance with policy controls.
