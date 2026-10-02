# Obsidian Connector and Local Bridge Specification

## 1. Why a bridge is required

The Hermes instance runs in a Hugging Face Space, while an Obsidian vault normally lives on the user's device. The cloud agent must therefore not pretend that `/home/user/Vault` exists inside the Space.

The recommended topology is:

```text
Android / Laptop
 ├── Obsidian
 ├── Local vault
 └── Hermes Obsidian Bridge
          │
          │ outbound TLS/WebSocket
          ▼
Hugging Face Space
 └── Hermes/Jarvis
```

## 2. Bridge responsibilities

The bridge:

- knows the vault root.
- enforces allowlisted folders.
- parses Markdown.
- watches changes.
- answers authenticated Hermes requests.
- performs safe file mutations.
- generates Obsidian URIs.
- reports health and sync state.

The bridge is **not** a general-purpose remote shell.

## 3. Allowed operations

### Read

```text
vault.info
vault.list
vault.search
note.read
note.metadata
note.links
```

### Write

```text
note.create
note.append
note.update
note.rename
note.move
```

### User handoff

```text
note.open
search.open
vault.open
```

Obsidian officially supports URI actions such as `open`, `new`, `daily`, `unique`, and `search`. citeturn1search2turn1search3

## 4. Vault scoping

Example configuration:

```yaml
vault:
  root: /Users/me/Obsidian/HermesVault
  allow:
    - Projects/Hermes
    - Projects/OmniRoute
    - Daily
    - Research
  deny:
    - .obsidian/plugins
    - Secrets
    - Private
  extensions:
    read: [md]
    write: [md]
```

The bridge must resolve real paths and prevent traversal such as:

```text
../../Secrets
```

## 5. File safety

Before writing:

1. Normalize path.
2. Resolve real path.
3. Verify it is inside an allowlisted root.
4. Read current file.
5. Calculate current hash.
6. Compare expected revision if supplied.
7. Write atomically.
8. Re-read and verify.
9. Emit audit event.

## 6. Atomic writes

Recommended sequence:

```text
write temp file
   ↓
fsync
   ↓
atomic rename
   ↓
verify hash
```

Never truncate the original file before the replacement content has been safely written.

## 7. Change watcher

The bridge should debounce filesystem events because editors can emit several changes for one user action.

```text
filesystem event
 ↓
50–1000 ms debounce window
 ↓
coalesce by path
 ↓
read stable file
 ↓
hash
 ↓
compare metadata
 ↓
send change event
```

Exact timing should be configurable.

## 8. Markdown parsing

Parse and preserve:

- YAML frontmatter.
- headings.
- tags.
- wikilinks.
- aliases.
- Markdown links.
- code blocks.
- callouts where possible.
- line ranges.

Do not rewrite formatting merely because Hermes read the file.

## 9. Wikilink graph

Build relationships:

```text
Note A ──links──> Note B
   │                 │
   └────tag──────────┘
```

Store graph metadata separately from file content so the retrieval system can use relationships without modifying notes.

## 10. Obsidian URI handoff

For a configured vault, Hermes can generate links such as:

```text
obsidian://open?vault=HermesVault&file=Projects%2FHermes%2FArchitecture
```

Values must be URI encoded. Obsidian's official documentation explicitly requires encoding reserved characters in URI parameters. citeturn1search2turn1search3

## 11. Mobile strategy

The Android Hermes app should not need direct filesystem access to the user's laptop vault.

Recommended flow:

```text
Hermes Android
  ↓
Hermes Cloud
  ↓
Paired Obsidian Bridge
  ↓
Laptop vault
```

If the user has a locally accessible Android Obsidian vault, an Android-side companion can be added later, but it should use the same protocol and capability model.

## 12. Bridge authentication

Preferred approach:

- device-generated asymmetric key pair.
- server stores public key.
- short-lived access token/session.
- signed request or mutually authenticated session.
- revocation list.

Never use a single permanent shared password for all bridge installations.

## 13. Offline behavior

If the bridge loses connection:

- Obsidian remains fully usable.
- Local change journal records changes.
- Hermes marks bridge `offline`.
- On reconnection, bridge sends checkpointed changes.
- Server reconciles by hash/revision.

## 14. Compromise response

If a bridge device is lost or compromised:

```text
Revoke device
 ↓
Reject future sessions
 ↓
Invalidate token
 ↓
Rotate sensitive server-side material if required
 ↓
Review audit log
 ↓
Pair new bridge
```

## 15. Important restriction

Never expose the vault through:

```text
public file server
anonymous HTTP
public WebDAV
SSH with unrestricted agent credentials
remote shell endpoint
```

The bridge should expose an intentionally tiny capability surface.
