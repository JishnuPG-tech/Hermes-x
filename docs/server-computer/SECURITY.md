# Security Model

## Security objectives

The agent must be powerful enough to operate a development computer while remaining unable to silently escalate authority or destroy protected state.

## Authority layers

```text
Owner
 ↓
Global policy
 ↓
Project policy
 ↓
Task policy
 ↓
Worker policy
 ↓
Tool permission
 ↓
Action
```

## Protected resources

- secrets
- authentication configuration
- SSH keys
- GitHub credentials
- production databases
- deployment credentials
- `/data/jarvis` root policy files
- other projects not assigned to the task

## Filesystem controls

Every task gets an allowed root. File tools must reject:

- path traversal
- symlink escape outside the allowed root
- writes to protected policy files
- access to unrelated projects
- accidental writes to secret directories

## Terminal controls

Commands should be classified as:

1. read-only
2. reversible development
3. network mutation
4. destructive
5. production/high-risk

Policies can allow classes 1–2 automatically while requiring approval for configured classes 3–5.

## Network controls

Prefer explicit egress permissions for agents. Record destination and purpose for sensitive network operations. Do not expose the internal terminal or filesystem directly to the public Internet.

## Secrets

Secrets must live in the platform secret store or a dedicated secret manager. Never write them into:

- Git repositories
- MEMORY.md
- USER.md
- public datasets
- task logs
- voice transcripts

Redact credentials from logs and error reports.

## GitHub security

Use the minimum GitHub permissions required. Separate read-only inspection from write operations. PR creation, merge, release, and destructive repository operations should be policy-controlled.

## Voice security

Voice commands are authenticated through the same user session as text. Wake-word detection alone is not authorization. Sensitive actions require the authenticated session and, where configured, explicit confirmation.

## Multi-device sessions

Each device session gets an ID and expiry. Reconnect must resume the same authorized task/session only after authentication. Revoked sessions cannot regain access through an old WebSocket.

## Prompt/tool injection

External content is untrusted. Repository files, web pages, issue text, documents, and tool outputs must not be treated as policy. System and project policy remain higher authority.

## Self-evolution safety

The agent may propose changes to its own skills or workflows but activation follows:

```text
propose
 → sandbox
 → test
 → evaluate
 → security review
 → approval/policy
 → activate
 → monitor
 → rollback if needed
```

An agent cannot grant itself new authority by editing its own configuration.

## Audit

Record who/what initiated an action, task ID, worker ID, tool, target, policy decision, result, and timestamp. Avoid recording sensitive payloads unnecessarily.
