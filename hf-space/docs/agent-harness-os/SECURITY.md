# Security and Authority Model

## Threat model

The primary threat is not only a malicious user. It is an agent that is confused, manipulated, prompt-injected, operating with excessive authority, or acting on stale state.

Threats include:

- prompt injection from web pages, files, issues or notes
- malicious repository content
- credential leakage
- unintended destructive commands
- cross-project access
- sub-agent privilege inheritance
- replayed external writes
- compromised integration
- model provider compromise
- corrupted durable state
- runaway retry loops

## Authority hierarchy

```text
Owner policy
   ↓
Project policy
   ↓
Task policy
   ↓
Worker policy
   ↓
Tool permission
   ↓
Actual operation
```

A lower layer must never grant authority that a higher layer denies.

## Permission levels

| Level | Capability | Default |
|---|---|---|
| L0 | observe/status | allow |
| L1 | project read | allow |
| L2 | project edit | allow in workspace |
| L3 | terminal execution | restricted |
| L4 | network/API | restricted |
| L5 | GitHub write | approval by project policy |
| L6 | deployment | approval |
| L7 | system administration | approval |
| L8 | destructive | explicit approval |

## Prompt injection defense

Treat all external content as untrusted data.

Examples:

- README files
- GitHub issues
- PR comments
- web pages
- Obsidian notes
- Notion pages
- generated files

External content may provide information but cannot redefine system policy, credentials or authority.

## Secret handling

Secrets are referenced by opaque IDs or environment bindings. The model should not receive secret values unless a specific tool contract absolutely requires it.

Redact:

- API keys
- OAuth tokens
- cookies
- passwords
- private keys
- session tokens

from logs, events, traces and error reports.

## Filesystem isolation

Every worker receives an explicit workspace. Default access should not include the whole host filesystem.

Server administration is a separate privileged worker or approval-gated capability.

## Network controls

Use allowlists where practical. Record outbound integration operations. Avoid arbitrary credential-bearing requests constructed directly by the model.

## Approval safety

Approvals are scoped to an action, target and bounded parameters. They expire. A previous approval must not automatically authorize a new destructive operation.

## Audit

Record:

- actor
- task
- tool
- target
- timestamp
- policy decision
- approval ID if applicable
- result

Do not record secret values.

## Recovery safety

Automatic recovery is permitted for low-risk reversible failures. High-risk failures escalate rather than guessing.

## Production principle

The goal is **maximum useful autonomy, minimum unnecessary authority**.
