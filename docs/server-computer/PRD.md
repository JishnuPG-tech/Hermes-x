# Server Computer PRD

## Product goal

Make Hermes the execution runtime of a persistent Jarvis computer. A user should be able to say a task from phone or laptop and have the server independently inspect files, plan work, edit code, run commands, test, use GitHub, maintain state, and report completion.

## Users

### Owner
Full authority subject to hard safety boundaries.

### Project agent
Can work inside an approved project and its declared tools.

### Specialist worker
Short-lived or persistent subagent with a narrow contract.

## Primary use cases

1. Create a project from a GitHub repository.
2. Continue an existing project from its server working copy.
3. Fix a bug end-to-end.
4. Implement a feature from a natural-language requirement.
5. Run tests and repair failures autonomously.
6. Review and improve code.
7. Operate development services and databases.
8. Create branches, commits, and pull requests.
9. Monitor CI and recover failed runs.
10. Execute scheduled maintenance through Hermes cron.
11. Continue long jobs after the user disconnects.
12. Control all of the above through realtime voice or text.

## Functional requirements

### FR-1 Persistent filesystem
All important runtime data must be stored on durable storage. The runtime must validate `/data/jarvis` at startup.

### FR-2 Project registry
Every project must have a stable project ID and mapping to local path, GitHub repository, branch policy, environment policy, and ownership.

### FR-3 Task isolation
Independent tasks should use isolated workspaces or Git worktrees. A task must not silently modify another task's workspace.

### FR-4 Agent lifecycle
Tasks support CREATE, INITIALIZE, AUTHORIZE, RUN, PAUSE, RESUME, SPAWN, CHECKPOINT, COMPLETE, FAILED, ARCHIVE.

### FR-5 Durable execution
Long-running jobs must use durable task state and a background scheduler/worker model, not only an interactive session loop.

### FR-6 Verification
No task may be marked complete merely because the model says it is complete. Completion requires the task's verification contract to pass.

### FR-7 GitHub integration
The agent can inspect repositories, branches, issues, pull requests, Actions status, and commits. Writes must obey project policy.

### FR-8 Database operations
The agent can use project databases through declared adapters. Destructive schema/data operations require elevated approval unless explicitly covered by policy.

### FR-9 Memory
Durable facts and reusable lessons are separated from temporary task state and full session history.

### FR-10 Voice
Voice is a control interface to the same runtime. Voice interruption must not destroy task state.

### FR-11 Observability
Every task has an auditable trace with timestamps, tool calls, model decisions where available, permissions, failures, retries, verification, and final artifacts.

### FR-12 Recovery
Recoverable failures trigger bounded retry or strategy change. Unknown failures are diagnosed and escalated instead of looping forever.

## Non-functional requirements

- Secure by default
- Restart-safe
- Observable
- Idempotent where possible
- Explicit permissions
- Reproducible builds
- Low-latency voice response
- No silent data loss
- Provider/model agnostic through OmniRoute

## Acceptance criteria

A representative project can be cloned, modified, tested, committed, pushed, and verified entirely from the server. Restarting the Space during a task must not lose the durable task record or project source. A user can reconnect from another device and recover the task state.
