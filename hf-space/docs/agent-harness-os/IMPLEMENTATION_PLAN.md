# Hermes-x Agent Harness + Agent OS Implementation Plan

## Phase 0: Baseline and inventory

- Inventory current Hermes CLI, gateway, skills, cron, delegation and memory capabilities.
- Inventory existing Hermes-x deployment and OmniRoute persistence work.
- Define the canonical durable storage root.
- Identify every current process that can mutate task/session state.
- Record current authentication and integration credentials flow.
- Establish baseline health, latency and failure metrics.

**Exit:** architecture map and state ownership matrix approved.

## Phase 1: Durable task kernel

Implement:

- `task_id`
- `project_id`
- idempotency key
- task status machine
- task graph
- checkpoints
- run records
- retry metadata
- cancellation
- pause/resume
- heartbeat
- leases/locks

Example record:

```json
{
  "task_id": "task_123",
  "project_id": "hermes-x",
  "status": "RUNNING",
  "attempt": 2,
  "parent_task_id": null,
  "checkpoint": "verify_tests",
  "retry_budget": 3,
  "created_at": "...",
  "updated_at": "..."
}
```

**Exit:** kill and restart the worker without losing a task.

## Phase 2: Harness execution loop

Build a single orchestrator loop:

1. Load task.
2. Load context.
3. Evaluate policy.
4. Build or refresh plan.
5. Select model via OmniRoute.
6. Execute Hermes.
7. Persist tool results.
8. Check stop/retry conditions.
9. Verify.
10. Commit checkpoint.
11. Continue or finish.

Use structured internal events instead of parsing human-facing text.

**Exit:** one long-running task can survive process restarts.

## Phase 3: Planner and task graph

Add planning contracts:

- objective
- assumptions
- requirements
- dependencies
- subtasks
- acceptance criteria
- risk level
- required integrations
- required approvals
- verification strategy

Planner output must be machine-readable.

**Exit:** a complex feature becomes a reproducible task DAG.

## Phase 4: Multi-agent workforce

Implement isolated worker profiles.

Each worker receives a role-specific system contract and workspace. Start with a safe concurrency limit such as 4, then increase after testing. Hermes itself supports sub-agent delegation, so the harness should orchestrate rather than duplicate the execution primitive.

Add:

- worker leases
- parent/child task links
- result contracts
- artifact handoff
- dependency barriers
- conflict detection
- worktree isolation for code tasks

**Exit:** parallel independent work completes and integrates without file corruption.

## Phase 5: Policy and authority engine

Define permission levels:

```text
L0 observe
L1 read project
L2 edit project
L3 execute terminal
L4 network/API
L5 GitHub write
L6 deploy
L7 system administration
L8 destructive
```

Policies are evaluated before tool execution, not after.

Approval examples:

- production deployment
- deleting data
- changing firewall/SSH/users
- changing secrets
- destructive database operations
- external financial actions

**Exit:** a model cannot bypass policy by changing its wording or tool arguments.

## Phase 6: Verification and recovery

Implement verifier adapters:

- shell checks
- test suites
- build checks
- lint/type checks
- GitHub CI
- health endpoints
- deployment smoke tests

Implement failure taxonomy and recovery strategies. Record the exact failure and recovery attempt.

**Exit:** representative injected failures recover without infinite loops.

## Phase 7: Memory and learning

Implement separate stores for:

- conversation history
- episodic task experiences
- semantic project facts
- procedural skills
- user/project preferences

After successful work, extract lessons only when they are reusable and supported by evidence.

Score lessons by:

```text
utility = success_rate × reuse_frequency × confidence
          × freshness_factor
```

Do not let a single failed run rewrite a trusted skill automatically.

**Exit:** future tasks can retrieve validated lessons without context bloat.

## Phase 8: Integrations

### GitHub

- repository discovery
- issue intake
- branch creation
- implementation
- tests
- commit/push
- PR creation
- review
- CI monitoring
- merge verification
- release verification

### Obsidian

- read approved vault paths
- index markdown notes
- project context retrieval
- backlink-aware context
- controlled note creation/update
- append task decisions and lessons

### Notion

- search pages/databases
- read project specifications
- map tasks to Notion records
- update status/progress
- append decision logs
- create structured completion reports

Credentials must remain in the integration service and never be copied into prompts.

**Exit:** each integration can be disabled without breaking the core agent.

## Phase 9: Event bus and observability

Emit structured events to a durable event log and a live SSE/WebSocket stream.

Track:

- task duration
- model latency
- tool latency
- retries
- failure classes
- approval wait time
- token usage
- estimated cost
- success rate
- recovery rate
- integration errors

**Exit:** the UI can reconstruct a run from events alone.

## Phase 10: Agent OS API

Expose stable APIs:

```text
POST   /v1/tasks
GET    /v1/tasks/{id}
POST   /v1/tasks/{id}/pause
POST   /v1/tasks/{id}/resume
POST   /v1/tasks/{id}/cancel
GET    /v1/tasks/{id}/events
POST   /v1/approvals/{id}/approve
POST   /v1/approvals/{id}/deny
GET    /v1/projects
GET    /v1/memory/search
GET    /v1/integrations
POST   /v1/integrations/{id}/test
GET    /v1/health
```

Use authentication, authorization, rate limits and request IDs.

## Phase 11: Mobile-first control surface

Implement the Hermes mobile UI around these screens:

1. Home/Chat
2. Active Task
3. Task Timeline
4. Approvals
5. Projects
6. Agents
7. GitHub
8. Knowledge
9. Memory
10. Integrations
11. Schedules
12. Settings
13. Security

The UI should show execution evidence rather than dumping raw internal chain-of-thought.

## Phase 12: Watchdog and autonomous operations

Watchdog loop:

```text
HEARTBEAT
  ↓
Detect stale task
  ↓
Load checkpoint
  ↓
Diagnose
  ↓
Resume or recover
  ↓
Verify
  ↓
Record outcome
```

Escalate to the owner when:

- retries are exhausted
- policy requires approval
- credentials are missing
- an external dependency is unavailable beyond the allowed window
- the task becomes ambiguous
- verification cannot establish success

## Phase 13: Production hardening

- persistent storage validation
- backup/restore drills
- encryption key stability
- secret redaction
- audit log integrity
- dependency pinning
- container image pinning
- health/readiness probes
- graceful shutdown
- migration tests
- concurrency tests
- disk pressure tests
- disaster recovery test

## Phase 14: Hugging Face Space release

Release sequence:

1. Build immutable image.
2. Attach persistent storage.
3. Configure runtime secrets.
4. Validate writable durable path.
5. Run database migrations.
6. Start control plane.
7. Start worker.
8. Verify health.
9. Run smoke task.
10. Verify persistence by restart.
11. Enable scheduled watchdog.

## Phase 15: Evolution

Future architecture can split the single process into:

```text
API Gateway
   ↓
Scheduler → Durable Queue → Worker Fleet
                         ↘ Specialist workers
   ↓
Postgres / durable DB
Object Storage
Event Stream
Secrets Manager
Observability
```

Do not introduce distributed infrastructure before the single-instance durability model is correct.
