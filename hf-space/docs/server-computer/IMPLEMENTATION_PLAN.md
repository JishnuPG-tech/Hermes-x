# Implementation Plan

## Phase 0 — Foundation

- Create `/data/jarvis` at startup.
- Validate that the mount is writable.
- Persist Hermes home/state on durable storage.
- Add structured configuration validation.
- Add health endpoint and startup diagnostics.
- Add a safe shutdown hook.

## Phase 1 — Project registry

Implement a registry such as:

```json
{
  "projects": {
    "hermes-x": {
      "path": "/data/jarvis/projects/hermes-x",
      "github": "JishnuPG-tech/Hermes-x",
      "default_branch": "main",
      "policy": "standard-development"
    }
  }
}
```

The registry should be backed by a durable database rather than only JSON once the runtime becomes multi-project.

## Phase 2 — Workspace manager

Build a service that can:

1. clone or fetch a repository
2. verify remote identity
3. create task workspace
4. create branch/worktree
5. lock workspace ownership
6. clean up safely after completion
7. preserve workspace when debugging is required

## Phase 3 — Task manager

Implement durable task records:

```text
id
project_id
objective
status
workspace
git_ref
created_at
updated_at
checkpoint
retry_count
approval_state
verification_state
parent_task_id
worker_ids
artifact_refs
error_class
```

The task manager is the source of truth for autonomous progress.

## Phase 4 — Agent OS policy

Implement policy evaluation before tool execution.

Required actions:

- allow
- deny
- require approval
- require elevated worker
- audit only

Add command/path/network allowlists and protected path rules.

## Phase 5 — Tool adapters

Standardize adapters for:

- terminal
- filesystem
- Git
- GitHub
- browser
- HTTP APIs
- databases
- Docker where available
- Notion
- Obsidian bridge

Each adapter declares permissions, side effects, timeout, and audit requirements.

## Phase 6 — Verification engine

Create reusable verification contracts:

```yaml
verification:
  - command: pytest -q
  - command: npm test
  - command: npm run build
  - check: git diff --check
  - check: working_tree_clean_after_commit
  - remote: github_ci_green
```

A project chooses only relevant checks. The agent cannot remove required checks during execution.

## Phase 7 — Background execution

Use Hermes cron and a durable task worker for scheduled/background work. Store job definitions and run history durably. Long jobs must not depend on a browser tab remaining open.

## Phase 8 — GitHub engineering loop

Implement:

```text
Issue/requirement
 → task spec
 → workspace
 → implementation
 → tests
 → commit
 → push/PR
 → Actions
 → CI diagnosis
 → repair
 → review
 → merge/deploy when authorized
```

## Phase 9 — Voice gateway

Implement provider interfaces:

```text
STTProvider
TTSProvider
AudioTransport
WakeWordDetector
TurnDetector
VoiceSession
```

Use WebSocket first. Add WebRTC later if required for lower-latency media transport.

## Phase 10 — TTS latency path

Stream model output into phrase buffers instead of waiting for the entire answer.

Target:

- first playable spoken audio: ≤5 seconds
- hard response ceiling: 10 seconds
- fallback trigger: around 4 seconds without playable Kokoro audio

Keep Kokoro warm. Use EdgeTTS as fallback when configured.

## Phase 11 — Backup and restore

Automate backups for:

- task metadata
- memory
- SQLite databases
- project registry
- agent checkpoints
- critical artifacts
- configuration excluding secrets

Test restoration regularly. A backup that has never been restored is not considered verified.

## Phase 12 — Hardening

- authentication
- device/session management
- approval workflow
- command sandboxing
- path traversal protection
- secret isolation
- rate limits
- audit logs
- dependency pinning
- resource quotas
- network egress policy

## Phase 13 — Production acceptance

Run restart, crash, network loss, tool timeout, CI failure, model failure, partial Git push, concurrent-task, and restore drills before enabling unattended high-authority automation.
