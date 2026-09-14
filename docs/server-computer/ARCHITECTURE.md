# Detailed Architecture

## 1. System topology

**Hermes Agent is the king. The server computer exists to give Hermes the execution environment it needs. OmniRoute only powers Hermes with model inference.**

```text
┌─────────────────────────────────────────────────────────┐
│ Client surfaces                                         │
│ Android PWA / Hermes-compatible UI / Laptop browser    │
│ Mic • speaker • text • approvals • status               │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTPS / WebSocket
┌──────────────────────────▼──────────────────────────────┐
│ Hermes Voice / Gateway                                  │
│ auth • sessions • VAD/STT/TTS • reconnect • streaming  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│ HERMES AGENT                                            │
│                    SYSTEM KING                          │
│ conversation • planning • tools • memory • tasks       │
│ GitHub • terminal • browser • subagents • verification │
│ recovery • scheduling • approvals • final responses     │
└──────────────────────────┬──────────────────────────────┘
                           │ model requests only
┌──────────────────────────▼──────────────────────────────┐
│ OmniRoute                                               │
│               MODEL POWER LAYER                         │
│ routing • provider abstraction • model selection       │
└──────────────────────────┬──────────────────────────────┘
                           │
                     Models / APIs

Hermes tools and state connect to:
/data/jarvis + GitHub + databases + approved external services
```

## 2. Agent OS primitives

Agent OS is Hermes infrastructure, not a competing agent above Hermes.

### Spec
Machine-readable task contract: objective, constraints, inputs, expected outputs, verification, deadline, approval requirements.

### Trace
Append-only execution record of important state transitions and actions.

### Trust
Policy engine determining whether an action is allowed.

### Memory
Durable facts, lessons, project knowledge, and task checkpoints.

### Pipe
Typed communication between Hermes components, workers, schedulers, and integrations.

## 3. Trust hierarchy

```text
Owner Policy
    ↓
Hermes Policy
    ↓
Project Policy
    ↓
Task Policy
    ↓
Worker Policy
    ↓
Tool Permission
    ↓
Actual Action
```

OmniRoute is outside this authority chain as an inference dependency. A model gateway cannot grant itself tool or project permissions.

## 4. Project workspace

```text
/data/jarvis/projects/<project-id>/
```

A managed project contains a normal Git working tree. Concurrent tasks use:

```text
/data/jarvis/workspaces/<task-id>/
```

Prefer `git worktree` for code tasks when the repository supports it. The task record stores the exact workspace path and Git commit/ref.

## 5. Databases

Start with SQLite for single-runtime metadata where its concurrency characteristics are sufficient. Use PostgreSQL when multiple services, higher concurrency, remote access, or operational requirements justify it.

SQLite files that matter must live under durable storage and must be closed cleanly. Never keep the only copy in the container layer.

## 6. State separation

```text
Session history      → Hermes conversation continuity
Hermes memory        → compact reusable facts/preferences
Hermes task state    → checkpoints and execution state
Project files        → Git working tree
Artifacts            → generated outputs
Secrets              → secret manager / Space Secrets
Logs and traces      → durable logs with retention
OmniRoute state      → OmniRoute persistence boundary
```

Do not merge these categories into one giant memory file.

## 7. Event model

Recommended events:

- task.created
- task.authorized
- task.started
- task.checkpointed
- task.worker.spawned
- tool.started
- tool.completed
- tool.failed
- approval.requested
- approval.granted
- approval.denied
- task.retrying
- model.requested
- model.completed
- model.failed
- model.fallback
- task.verification.started
- task.verification.passed
- task.verification.failed
- task.completed
- task.failed
- deployment.started
- deployment.verified

## 8. Completion pipeline

```text
Plan
 ↓
Implement
 ↓
Unit tests
 ↓
Integration tests
 ↓
Build
 ↓
Security checks
 ↓
Git diff review
 ↓
CI
 ↓
Deploy if authorized
 ↓
Remote health check
 ↓
Artifact/state verification
 ↓
Mark complete
```

Hermes owns this pipeline. OmniRoute only supplies model inference when Hermes needs reasoning.

## 9. Recovery pipeline

```text
Failure
 ↓
Classify
 ├─ transient → bounded retry
 ├─ dependency → wait/recheck
 ├─ code → diagnose/change/test
 ├─ environment → repair/rebuild
 ├─ model gateway → retry/fallback/checkpoint
 ├─ permission → request approval
 └─ unknown → capture evidence + escalate
```

Repeated failure must trigger a strategy change, not infinite identical retries.

## 10. Voice integration

Voice is an interface to Hermes, never a separate task runtime.

```text
Wake phrase
 → continuous capture
 → VAD / turn detection
 → streaming STT
 → HERMES session/task
 → OmniRoute → model
 → HERMES interprets/acts
 → sentence buffer
 → Kokoro TTS
 → audio playback
 → barge-in
```

Kokoro is the primary self-hosted TTS target. EdgeTTS is a latency/failure fallback. The voice session carries a durable task ID when a request becomes long-running.
