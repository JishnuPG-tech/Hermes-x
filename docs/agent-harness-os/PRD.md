# Product Requirements Document: Hermes-x Agent Harness + Agent OS

## 1. Product vision

Turn Hermes Agent into the execution core of a durable personal engineering and operations environment that can handle short conversations and long-running autonomous work from one interface.

## 2. Problem

A powerful model and terminal agent are not sufficient for reliable autonomous work. Long tasks need durable state, planning, permissions, retries, verification, memory, observability and integration boundaries. Without those layers, a restart can lose work, a failed tool call can terminate progress, and a model can incorrectly report completion.

## 3. Goals

### P0
- Durable task lifecycle
- Durable session and run state
- Plan/execute/verify/recover loop
- Sub-agent orchestration
- Tool permission enforcement
- Approval gates
- Persistent memory and task history
- GitHub end-to-end workflow
- Obsidian and Notion adapters
- Event stream and audit log
- Mobile/web API surface
- Restart/resume support

### P1
- Intelligent model routing through OmniRoute
- Scheduled tasks and watchdogs
- Automatic CI monitoring
- Project knowledge graph
- Artifact registry
- Cost and latency tracking
- Skill promotion from successful experiences
- Server health automation

### P2
- Multi-device control
- Agent marketplace/skill registry
- Advanced policy engine
- Distributed workers
- Cross-project dependency graph
- Simulation/dry-run environment

## 4. Non-goals

- Replacing the Linux kernel
- Training a new foundation model
- Giving unrestricted root access by default
- Claiming infinite execution on finite infrastructure
- Treating model-generated completion text as proof of success

## 5. Personas

### Owner
Controls permissions, credentials, projects, approvals and policies.

### Primary agent
Hermes instance responsible for the user's request.

### Specialist agent
Isolated worker for coding, QA, security, research, architecture, DevOps or documentation.

### Watchdog
Monitors stalled or failed work and initiates safe recovery.

### Reviewer
Validates outputs against acceptance criteria.

## 6. Core user stories

### US-01 Long-running development
As the owner, I can ask Hermes to implement a feature and have it continue after a chat session closes.

### US-02 Autonomous recovery
If tests fail, Hermes diagnoses the failure, changes strategy and retries within policy limits.

### US-03 Knowledge-aware work
Hermes can read approved project notes from Obsidian or Notion before planning work.

### US-04 Knowledge write-back
After completing work, Hermes can update an approved Notion task or Obsidian project note.

### US-05 GitHub delivery
Hermes can branch, edit, test, commit, push, open a PR, monitor CI and verify merge state.

### US-06 Safe autonomy
Dangerous operations pause for explicit approval.

### US-07 Resume
A restart does not create a duplicate job or lose the current state.

### US-08 Mobile control
The owner can inspect, pause, resume, approve and redirect runs from a mobile client.

## 7. Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Create durable tasks with unique IDs | P0 |
| FR-02 | Represent tasks as dependency graphs | P0 |
| FR-03 | Persist every state transition | P0 |
| FR-04 | Execute Hermes runs through a controlled runtime | P0 |
| FR-05 | Spawn isolated sub-agents | P0 |
| FR-06 | Enforce tool and filesystem permissions | P0 |
| FR-07 | Support approval checkpoints | P0 |
| FR-08 | Verify outputs with executable checks | P0 |
| FR-09 | Retry recoverable failures with bounded policies | P0 |
| FR-10 | Resume interrupted tasks | P0 |
| FR-11 | Emit structured run events | P0 |
| FR-12 | Maintain an append-only audit trail | P0 |
| FR-13 | Connect GitHub | P0 |
| FR-14 | Connect Obsidian | P0 |
| FR-15 | Connect Notion | P0 |
| FR-16 | Support schedules and watchdogs | P1 |
| FR-17 | Track model/tool cost and latency | P1 |
| FR-18 | Promote reusable skills from validated lessons | P1 |
| FR-19 | Provide server health actions | P1 |
| FR-20 | Provide project-level knowledge indexes | P1 |

## 8. Task lifecycle

```text
CREATED
  ↓
INTAKE
  ↓
CONTEXT_READY
  ↓
PLANNED
  ↓
WAITING_APPROVAL ──reject──> CANCELLED
  ↓ approve
READY
  ↓
RUNNING
  ├──> WAITING_TOOL
  ├──> WAITING_EXTERNAL
  ├──> FAILED_RECOVERABLE → RECOVERING → RUNNING
  ├──> FAILED_FINAL
  └──> VERIFYING
          ↓
       COMPLETED
```

## 9. Definition of done

A task is complete only when:

1. Required outputs exist.
2. Acceptance criteria are evaluated.
3. Relevant tests/checks pass.
4. Git state is verified when GitHub delivery is requested.
5. External systems report the expected state.
6. No unresolved blocking errors remain.
7. The final run record contains evidence and artifacts.

## 10. Reliability targets

- No silent loss of task state after ordinary restart.
- Duplicate execution prevented using idempotency keys.
- All dangerous actions require policy authorization.
- All completed tasks have verification evidence.
- Recoverable failures are retried with bounded exponential backoff.
- Integration outages do not corrupt the core task record.

## 11. UX requirements

The UI should expose:

- Chat
- Current task status
- Live execution timeline
- Plan and subtasks
- Tool activity
- Approvals
- Artifacts
- GitHub status
- Memory used
- Model/provider used
- Cost/latency summary
- Pause/resume/cancel
- Project and integration settings

## 12. Acceptance criteria

The first production release must successfully survive process restart, worker restart, container recreation and normal Hugging Face Space restart without losing already committed durable task state. A representative GitHub task must be able to plan, implement, test, recover from a failing test, open a PR and produce a verified final report. Obsidian and Notion must support read and controlled write operations without exposing their credentials to the model.
