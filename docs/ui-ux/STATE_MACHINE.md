# Hermes UI State Machine

## Global states

```text
OFFLINE
  ↓
RECONNECTING
  ↓
READY
  ↓
THINKING
  ↓
RUNNING
  ├─ DELEGATING
  ├─ WAITING
  ├─ NEEDS_APPROVAL
  └─ RECOVERING
        ↓
     RUNNING
        ↓
   VERIFYING
    ├─ FAILED
    └─ COMPLETED
```

## State meanings

| State | Meaning | Primary UI treatment |
|---|---|---|
| Ready | Hermes can accept work | Quiet status |
| Listening | Voice input active | Voice activity |
| Thinking | Model inference active | Subtle thinking indicator |
| Running | Hermes executing | Progress/task activity |
| Delegating | Workers active | Agent activity |
| Waiting | Dependency or timer | Waiting explanation |
| Needs approval | User authority required | Approval card |
| Recovering | Hermes repairing/retrying | Recovery explanation |
| Paused | Work intentionally stopped | Resume action |
| Failed | Objective cannot continue | Cause + recovery actions |
| Completed | Verified objective finished | Result + artifacts |
| Offline | Backend unavailable | Offline banner |
| Reconnecting | Connection recovery | Reconnect indicator |

## Chat state

```text
EMPTY
 → COMPOSING
 → SUBMITTING
 → THINKING
 → STREAMING
 → TOOL_ACTIVITY
 → COMPLETED
```

Errors return to a recoverable state where possible.

## Task state

```text
CREATED
 → AUTHORIZED
 → PLANNING
 → RUNNING
 → VERIFYING
 → COMPLETED
```

Alternate paths:
- RUNNING → WAITING
- RUNNING → NEEDS_APPROVAL
- RUNNING → RECOVERING
- RECOVERING → RUNNING
- any active state → PAUSED
- active state → FAILED

## Voice state

```text
IDLE
 → WAKE_DETECTED
 → LISTENING
 → TRANSCRIBING
 → HERMES_PROCESSING
 → SPEAKING
 → LISTENING
```

Barge-in:

```text
SPEAKING → INTERRUPTED → LISTENING
```

Connection failure:

```text
any voice state → RECONNECTING → READY/IDLE
```

## Approval state

```text
ACTION_PROPOSED
 → APPROVAL_REQUIRED
 ├─ APPROVED → EXECUTING
 └─ DENIED → CANCELLED
```

Approval must show the exact action and scope before the user commits.

## Agent state

```text
CREATED
 → INITIALIZING
 → AUTHORIZED
 → RUNNING
 → CHECKPOINTING
 → COMPLETED
```

Failure may enter recovery before returning to RUNNING.

## Integration state

```text
DISCONNECTED
 → AUTHENTICATING
 → CONNECTED
 → VERIFYING
 → HEALTHY
```

Alternate states:
- AUTH_REQUIRED
- EXPIRED
- REVOKED
- ERROR

## UI rule

Never communicate an internal state only through color. Use text, iconography, structure or motion as secondary support.
