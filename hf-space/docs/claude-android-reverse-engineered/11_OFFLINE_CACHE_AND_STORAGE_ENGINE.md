# Claude Android — Offline Cache, Storage Engine & Sync Architecture

---

## 1. Storage Layers & Data Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 STORAGE ENGINE TOPOLOGY                                │
├─────────────────────────┬──────────────────────────┬───────────────────────────────────┤
│ Layer                   │ Technology               │ Stored Entities                   │
├─────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ 1. Key-Value Settings   │ AndroidX DataStore       │ Selected Model, Voice Pace, Theme,│
│                         │ (Preferences Proto)      │ Haptic Toggle, Recent Prompts     │
├─────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ 2. Draft Storage        │ DataStore / Room         │ SessionDraft, NewSessionDraft,    │
│                         │                          │ DraftMessage, Unsent Attachments  │
├─────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ 3. Structured Cache     │ Room / SQLite Database   │ Chat Conversations, Messages,     │
│                         │ (androidx.room)          │ Artifact Versions, Projects       │
├─────────────────────────┼──────────────────────────┼───────────────────────────────────┤
│ 4. Encrypted Vault      │ EncryptedSharedPreferences│ OAuth Tokens, Session Bearer Key, │
│                         │ (AndroidKeyStore AES-GCM)│ Trusted Device Attestation JWT    │
└─────────────────────────┴──────────────────────────┴───────────────────────────────────┘
```

---

## 2. Draft Persistence & Recovery Engine (`SessionDraft`)

To protect users against accidental app closures or incoming phone calls while composing prompts:

```
┌─────────────────────────────────────────────────────────────────┐
│                       COMPOSER DRAFT ENGINE                     │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Text Debounce (300ms)
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DRAFT MESSAGE STRUCTURE                      │
│  • conversation_uuid: "chat_01Abc..."                           │
│  • draft_text: "Refactor the authentication flow..."            │
│  • model_override: "claude-sonnet-5"                            │
│  • pending_attachments: [ "uri://...", "uri://..." ]           │
│  • updated_timestamp_ms: 1774001200000                         │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Persist to DataStore
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      RESTORATION TRIGGERS                       │
│  • App Launch / Session Navigation                              │
│  • CodeEvents$SessionComposerDraftRestored                      │
│  • ChatEvents$DraftRestoreTrigger                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Timeline Synchronization & Pagination Protocol

```
Client (Room Database)                                    Server Edge
   │                                                           │
   ├────── GET /chat_conversations/{id}?limit=30 ─────────────►│ (Initial load)
   │                                                           │
   │◄───── 200 OK (30 messages, cursor: "cur_xyz") ────────────┤
   │ (Inserts into local Room table)                           │
   │                                                           │
(User flings / scrolls up)                                     │
   │                                                           │
   ├────── GET /chat_conversations/{id}?cursor=cur_xyz&limit=30►│ (Backward pagination)
   │                                                           │
   │◄───── 200 OK (older messages, next_cursor: "cur_abc") ────┤
   │                                                           │
```

* **Consistency Levels (`ConsistencyLevel`)**:
  * `LOCAL_FIRST`: Reads from Room cache instantly, refreshes from server in background.
  * `STRONG`: Waits for authoritative server timeline before rendering (used in sensitive billing and device list views).

---

## 4. Polling & Connection Recovery (`PollingRecoveryConfig`)

During intermittent connectivity (e.g. subway tunnels or cellular handoffs):
* **Exponential Backoff**: Base delay `1000ms`, multiplier `1.5`, max jitter `500ms`, capped at `30000ms`.
* **Budget Limits**: Max retry budget of 5 attempts before surfacing `RetryBudgetExceededException`.
* **Fast Re-attach**: When connection is restored, client sends `from_sequence_num` to re-attach to the SSE event stream without re-fetching the entire message history.
