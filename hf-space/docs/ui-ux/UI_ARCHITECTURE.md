# Hermes UI Architecture

## Layers

```text
Presentation
  ↓
Feature State / View Models
  ↓
Hermes UI Gateway
  ↓
Hermes Session API
  ↓
Hermes Agent
  ↓
Agent OS
  ↓
Tools / Terminal / Files / Integrations
  ↓
OmniRoute
  ↓
Models
```

## Presentation layer

Contains only visual and interaction concerns.

Examples:
- screens
- components
- theme
- responsive layouts
- accessibility semantics
- animations

Presentation must not contain provider-specific model logic.

## Feature state

Feature state translates Hermes events into UI state.

Example:

```text
TaskStarted
TaskProgress
AgentSpawned
ToolStarted
ToolOutput
ApprovalRequested
RecoveryStarted
VerificationStarted
TaskCompleted
```

The UI renders these events and exposes user actions back to Hermes.

## Hermes UI Gateway

Responsibilities:
- session connection
- authentication
- event streaming
- command submission
- reconnect
- optimistic UI where safe
- idempotency keys
- event ordering

## Event envelope

Conceptual contract:

```json
{
  "id": "event-id",
  "session_id": "session-id",
  "task_id": "task-id",
  "timestamp": "ISO-8601",
  "type": "task.progress",
  "sequence": 42,
  "payload": {}
}
```

## Command envelope

```json
{
  "id": "command-id",
  "session_id": "session-id",
  "task_id": "task-id",
  "type": "task.pause",
  "idempotency_key": "unique-key",
  "payload": {}
}
```

## UI command categories

- Send message
- Start voice
- Stop voice
- Create task
- Pause task
- Resume task
- Cancel task
- Approve action
- Deny action
- Retry task
- Open project
- Open artifact
- Connect integration
- Revoke integration
- Search memory
- Edit memory
- Run developer action

## Streaming

Chat, voice and task events should stream incrementally.

The UI must not wait for the entire task to finish before showing progress.

## Reconnection

On reconnect:
1. Reauthenticate if required.
2. Resume the same session.
3. Request events after the last acknowledged sequence.
4. Reconcile task state.
5. Render current state.

## Offline behavior

Offline UI should:
- clearly show unavailable operations
- preserve unsent drafts when safe
- avoid claiming execution happened
- retry safe idempotent operations
- provide reconnect status

## Security boundary

The UI never receives raw long-lived provider credentials. It receives sanitized connection metadata and short-lived authorization/session state where required.

## Model abstraction

The UI talks to Hermes. It does not directly depend on OmniRoute provider APIs.

Changing a model must not reset:
- Hermes identity
- conversation
- memory
- task state
- project state
- voice session
