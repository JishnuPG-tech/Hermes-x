# Knowledge Integration Configuration Contract

## 1. Production configuration

Example:

```env
KNOWLEDGE_ENABLED=true
KNOWLEDGE_NOTION_ENABLED=true
KNOWLEDGE_OBSIDIAN_ENABLED=true
KNOWLEDGE_SYNC_ENABLED=true
KNOWLEDGE_AUTONOMOUS_WRITES=false
KNOWLEDGE_DEFAULT_SOURCE=auto
KNOWLEDGE_INDEX_ENABLED=true
KNOWLEDGE_MAX_RESULTS=20
KNOWLEDGE_MAX_DOCUMENT_BYTES=2097152
KNOWLEDGE_SYNC_BATCH_SIZE=50
KNOWLEDGE_RETRY_MAX=5
```

## 2. Notion secrets

Use secret-manager references in production:

```env
NOTION_CLIENT_ID=<secret>
NOTION_CLIENT_SECRET=<secret>
NOTION_REDIRECT_URI=<https endpoint>
NOTION_TOKEN_ENCRYPTION_KEY=<secret>
```

Do not commit real values.

## 3. Obsidian bridge

```env
OBSIDIAN_BRIDGE_ENABLED=true
OBSIDIAN_BRIDGE_URL=wss://<gateway>/v1/obsidian
OBSIDIAN_PAIRING_ENABLED=true
OBSIDIAN_SESSION_TTL_SECONDS=900
OBSIDIAN_MAX_MESSAGE_BYTES=1048576
OBSIDIAN_ALLOWED_METHODS=vault.info,vault.search,note.read,note.create,note.append,note.update,note.open
```

The bridge URL is a logical service endpoint. The bridge itself should establish the outbound connection.

## 4. Policy defaults

```yaml
knowledge_policy:
  read:
    notion: allow
    obsidian: allow
  create:
    notion: approval
    obsidian: approval
  update:
    notion: approval
    obsidian: approval
  delete:
    notion: deny
    obsidian: deny
  autonomous_write:
    default: deny
```

## 5. Project configuration

```yaml
project: Hermes
sources:
  notion:
    enabled: true
    scopes:
      - "Hermes Project"
      - "Engineering Decisions"
  obsidian:
    enabled: true
    roots:
      - "Projects/Hermes"
      - "Projects/OmniRoute"
      - "Daily"
priority:
  - notion
  - obsidian
```

## 6. Secret handling

Secrets must be:

- injected at runtime.
- encrypted at rest.
- excluded from logs.
- excluded from model context unless strictly required.
- rotated without changing source code.

## 7. Startup validation

Hermes must fail closed for an enabled connector when mandatory configuration is invalid.

Example:

```text
KNOWLEDGE_NOTION_ENABLED=true
but NOTION_TOKEN_ENCRYPTION_KEY missing
        ↓
Notion connector = unavailable
        ↓
Hermes remains healthy if policy allows degraded mode
        ↓
Admin alert
```

Do not silently disable security features because a secret is missing.

## 8. Environment separation

Development, staging and production must use different:

- Notion connections.
- encryption keys.
- bridge identities.
- databases.
- callback URLs.

Never reuse production credentials in development.
