# Hermes-x Agent Harness Architecture

## 1. Layering

```text
Client Layer
  Mobile / Web / Voice / Telegram / Desktop
        ↓
API + Session Gateway
        ↓
Agent OS Control Plane
  Identity | Projects | Tasks | Approvals | Schedules
        ↓
Agent Harness
  Intent | Context | Planning | Policy | Execution | Recovery | Verification
        ↓
Hermes Runtime
  LLM loop | tools | terminal | skills | MCP | sub-agents
        ↓
OmniRoute
  model selection | provider routing | fallbacks | credentials
        ↓
External systems
  GitHub | Obsidian | Notion | HF | servers | APIs
```

## 2. Control plane vs data plane

### Control plane
Owns task definitions, policies, identities, schedules, approvals, project configuration, run metadata and audit records.

### Data plane
Performs actual work: model inference, terminal execution, file editing, Git operations, web access, API calls and integration actions.

The model never becomes the authority layer. The harness is the policy enforcement point.

## 3. Agent loop

```text
User intent
  ↓
Normalize request
  ↓
Load project + memory context
  ↓
Risk classification
  ↓
Build plan/task graph
  ↓
Check approval requirements
  ↓
Select model/provider via OmniRoute
  ↓
Run Hermes
  ↓
Observe tool results
  ↓
Update durable state
  ↓
Verify
  ├── pass → integrate/report
  └── fail → diagnose → recover → verify
```

The loop is bounded by policy. A task can continue through many iterations, but every retry has a reason, budget and stopping rule.

## 4. Task graph

A task is a DAG of nodes rather than one giant prompt.

```text
                 REQUIREMENTS
                      ↓
                 ARCHITECTURE
                 ↙          ↘
          BACKEND            UI
             ↓                ↓
            TESTS ←────── SECURITY
                 \          /
                   REVIEW
                     ↓
                    PR
                     ↓
                    CI
                     ↓
                  RELEASE
```

Independent nodes can run in parallel only when their workspace and resource contracts do not conflict.

## 5. Agent roles

Recommended built-in roles:

- Orchestrator
- Product manager
- Requirements analyst
- Architect
- Developer
- Frontend developer
- Backend developer
- Database engineer
- QA engineer
- Security engineer
- Code reviewer
- DevOps engineer
- SRE
- Researcher
- Technical writer

Each role has explicit responsibilities and output contracts.

## 6. Execution isolation

Each worker receives:

- Task ID
- Project ID
- Workspace path
- Allowed tools
- Allowed integrations
- Environment variables by reference
- Time budget
- Token/model budget
- Retry budget
- Acceptance criteria

Sub-agents must not inherit unrestricted credentials simply because the parent has them.

## 7. Durable state

Recommended durable entities:

```text
projects
sessions
conversations
tasks
runs
run_steps
subtasks
approvals
artifacts
memories
lessons
skills
integrations
credentials_refs
schedules
locks
checkpoints
audit_events
```

SQLite is acceptable for the first single-instance deployment if WAL, backups, permissions and recovery are designed correctly. The persistence design must follow the same principles already documented for OmniRoute.

## 8. Event model

Every meaningful operation emits events:

```text
run.created
run.started
plan.created
subtask.created
agent.started
tool.started
tool.finished
approval.requested
approval.granted
approval.denied
checkpoint.saved
verification.started
verification.passed
verification.failed
recovery.started
recovery.completed
run.paused
run.resumed
run.completed
run.failed
```

The event stream drives the mobile UI, dashboard, audit log and monitoring system.

## 9. Memory architecture

```text
Working memory
  current prompt + tool results

Episodic memory
  previous runs + failures + decisions

Semantic memory
  facts + project knowledge + user preferences

Procedural memory
  validated skills + reusable workflows

External knowledge
  Obsidian + Notion + GitHub + project files
```

Context assembly chooses only relevant information. The full memory database must never be dumped into the model context.

## 10. Verification architecture

Verification is a separate phase and may use another model or deterministic tools.

Examples:

- `pytest`
- `npm test`
- `ruff`
- type checker
- build
- security scanner
- Git diff validation
- GitHub CI checks
- API health check
- deployment smoke test

The verifier returns structured evidence, not just natural language.

## 11. Recovery architecture

```text
Failure
  ↓
Classify
  ├─ transient → retry
  ├─ dependency → wait/recheck
  ├─ code defect → debug/fix
  ├─ permission → approval/escalation
  ├─ environment → repair environment
  └─ unknown → bounded diagnostic attempt
```

Repeated identical failures must trigger strategy change rather than blind repetition.

## 12. Agent OS services

1. API Gateway
2. Session Manager
3. Task Scheduler
4. Orchestrator
5. Worker Manager
6. Policy Engine
7. Approval Service
8. Memory Service
9. Knowledge Connector Service
10. Artifact Service
11. GitHub Service
12. Event Bus
13. Audit Service
14. Watchdog
15. Notification Service
16. Health Service

For the first deployment these may run in one process. Interfaces should remain separable so they can later be split into workers.

## 13. Failure domains

- UI failure must not lose task state.
- Model provider failure must not lose task state.
- GitHub outage must not corrupt local task state.
- Obsidian/Notion outage must not block unrelated work.
- Worker crash must permit resume from checkpoint.
- Redis outage must not destroy canonical state.
- Storage outage must fail closed for durable writes.

## 14. Hugging Face deployment

The Agent OS control plane should use the persistent storage architecture defined for this repository. Ephemeral container paths are caches only. Canonical durable state must be placed on attached persistent storage, with explicit startup validation and backups.
