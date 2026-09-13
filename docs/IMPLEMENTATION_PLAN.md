# Jarvis Implementation Plan
## From Hermes + OmniRoute to a Persistent Autonomous AI System

**Version:** 1.0  
**Status:** Implementation roadmap  
**Date:** 2026-09-13

---

# 1. Implementation Principles

1. Extend Hermes rather than duplicate its existing capabilities.
2. Keep Jarvis Core as the orchestration/control layer.
3. Keep model selection separate in OmniRoute.
4. Make task state durable before adding high autonomy.
5. Verification is mandatory.
6. Recovery must change strategy, not blindly retry.
7. Permissions must be explicit.
8. Every important operation must be observable.
9. Start with one reliable execution environment.
10. Add complexity only after the previous layer is stable.

Hermes already provides skills, persistent memory, terminal/file tools, browser automation, delegation, scheduling, voice, and messaging capabilities. Its skill system is designed for progressive loading, and its delegation system provides isolated child-agent contexts. The Jarvis layer should orchestrate these capabilities instead of rebuilding them.

---

# 2. Target Repository Structure

```text
jarvis/
├── README.md
├── LICENSE
├── SECURITY.md
├── CONTRIBUTING.md
├── docs/
│   ├── PRD.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── ARCHITECTURE.md
│   ├── SECURITY_ARCHITECTURE.md
│   ├── OPERATIONS.md
│   ├── MEMORY_AND_LEARNING.md
│   ├── MULTI_AGENT.md
│   ├── SDLC.md
│   └── ADR/
├── src/
│   └── jarvis/
│       ├── core/
│       ├── planner/
│       ├── tasks/
│       ├── orchestration/
│       ├── verification/
│       ├── recovery/
│       ├── learning/
│       ├── memory/
│       ├── routing/
│       ├── policy/
│       ├── github/
│       ├── server/
│       ├── voice/
│       ├── notifications/
│       └── api/
├── skills/
│   └── jarvis/
│       ├── sdlc/
│       ├── agile/
│       ├── engineering/
│       ├── server/
│       ├── github/
│       ├── security/
│       ├── autonomous/
│       └── research/
├── tests/
│   ├── unit/
│   ├── integration/
│   ├── security/
│   └── e2e/
├── deployments/
│   ├── docker/
│   ├── systemd/
│   └── reverse-proxy/
├── config/
│   ├── default.yaml
│   ├── policy.yaml
│   └── routing.yaml
└── scripts/
```

---

# 3. Phase 0: Foundation and Baseline

## Objectives

- Freeze a known-good Hermes version.
- Document environment.
- Confirm OmniRoute connectivity.
- Confirm GitHub access.
- Confirm local terminal execution.
- Confirm persistent storage.
- Establish backups.

## Tasks

### Infrastructure

- Choose persistent server.
- Create dedicated OS user for Jarvis.
- Configure filesystem layout.
- Configure service manager.
- Configure firewall.
- Configure TLS/reverse proxy if remotely exposed.
- Configure backups.

### Hermes baseline

Verify:

- CLI.
- API server if used.
- Terminal.
- File tools.
- Browser.
- Skills.
- Memory.
- Delegation.
- Cron.
- Voice.
- Messaging.
- GitHub.

### Acceptance

A baseline task can:

`receive -> execute -> verify -> persist -> restart -> continue`

---

# 4. Phase 1: Jarvis Core

Build the central orchestrator.

## Components

### Identity

Stores:

- System identity.
- Personality.
- Operating rules.
- User preferences.
- Project context.

### Intent engine

Produces:

```json
{
  "intent": "software_delivery",
  "complexity": "large",
  "needs_delegation": true,
  "needs_web": false,
  "needs_github": true,
  "needs_background_execution": true,
  "risk_level": "medium"
}
```

### Planner

Produces a structured execution plan.

### Policy engine

Determines whether an operation is:

- automatic
- approval-required
- forbidden

### Orchestrator

Coordinates the full lifecycle.

---

# 5. Phase 2: Persistent Task Manager

Implement first-class task persistence.

## Storage

Start with SQLite for a single-server deployment.

Potential later migration:

- PostgreSQL for multi-user or multi-node operation.

## Tables

```text
tasks
task_steps
task_dependencies
task_attempts
task_events
task_artifacts
task_agents
task_checkpoints
task_approvals
task_verifications
task_lessons
```

## Event model

Every state change creates an event.

Example:

```json
{
  "task_id": "T-1042",
  "event": "step_failed",
  "step": "integration_tests",
  "attempt": 2,
  "error_class": "dependency_failure"
}
```

This becomes the foundation for recovery and learning.

---

# 6. Phase 3: Multi-Agent Orchestration

Integrate Hermes delegation.

## Role registry

```yaml
architect:
  objective: architecture
  tools: [file, web, terminal]
  outputs:
    - architecture.md

backend:
  objective: backend implementation
  outputs:
    - source
    - tests

qa:
  objective: testing
  outputs:
    - test-results.json

security:
  objective: security review
  outputs:
    - security-report.md

reviewer:
  objective: independent code review
  outputs:
    - review.md
```

## Orchestration algorithm

```text
Understand task
    |
Create dependency graph
    |
Identify parallel work
    |
Create isolated agents
    |
Dispatch
    |
Collect artifacts
    |
Resolve conflicts
    |
Run integration
    |
Verify
```

## Work isolation

Use isolated worktrees for agents modifying overlapping repositories.

Never allow multiple agents to modify the same working tree concurrently without explicit coordination.

---

# 7. Phase 4: Verification Engine

Create verification adapters.

```text
VerificationProvider
├── Python
├── Node
├── Go
├── Rust
├── Docker
├── GitHub Actions
├── HTTP
├── CLI
└── Generic
```

Each provider returns:

```json
{
  "passed": true,
  "checks": [
    {
      "name": "unit_tests",
      "status": "passed"
    }
  ]
}
```

## Completion rule

A task cannot transition to `completed` merely because the agent says it completed.

It must have verification evidence.

---

# 8. Phase 5: Recovery Engine

Implement error classification.

Classes:

- transient
- dependency
- environment
- code
- configuration
- permission
- authentication
- network
- unknown
- destructive-risk

Recovery policies:

```text
transient -> retry
dependency -> inspect dependency
code -> debugger
configuration -> config specialist
network -> connectivity diagnosis
permission -> request approval
unknown -> research + specialist
repeated failure -> strategy change
```

## Strategy memory

Before retrying:

1. Search previous failures.
2. Search successful fixes.
3. Search project lessons.
4. Select a different strategy.

---

# 9. Phase 6: Learning Engine

## Experience record

```yaml
task_type:
objective:
context:
strategy:
models:
tools:
attempts:
result:
errors:
fixes:
verification:
duration:
cost:
user_feedback:
confidence:
reusable:
```

## Evaluation

Score:

- correctness
- completeness
- user satisfaction
- tool success
- efficiency
- reliability

## Learning outputs

A completed experience may produce:

- memory fact
- lesson
- strategy
- skill
- routing update
- warning
- regression test

---

# 10. Phase 7: Advanced Memory

Use layered memory.

```text
Layer 1: Immediate session
Layer 2: Project memory
Layer 3: User memory
Layer 4: Experience memory
Layer 5: Knowledge
Layer 6: Strategy library
Layer 7: Skill library
```

Use an external memory provider only when the built-in memory limits become a real constraint.

Keep structured task/experience data in the Jarvis database even if semantic memory is external.

---

# 11. Phase 8: OmniRoute Learning

Create a routing telemetry table.

```text
model
provider
task_type
success
quality_score
latency_ms
cost
context_size
tool_success
user_feedback
timestamp
```

Routing score:

```text
score =
  task_fit
+ historical_success
+ quality
+ reliability
- latency_penalty
- cost_penalty
```

Do not optimize for cost alone.

---

# 12. Phase 9: GitHub Engineering

Implement a GitHub workflow controller.

## Workflow

```text
Issue
 |
Read complete context
 |
Duplicate check
 |
Plan
 |
Branch
 |
Delegate
 |
Implement
 |
Test
 |
Review
 |
Commit
 |
Push
 |
PR
 |
CI
 |
Failure?
 ├── yes -> diagnose -> fix -> push
 └── no
 |
Review
 |
Merge if policy allows
 |
Verify remote state
```

## Guardrails

Never infer:

- merge status
- CI status
- deployment status

Always inspect current remote state.

---

# 13. Phase 10: SDLC and Agile

Build Jarvis skills rather than hard-coding every process.

Skill tree:

```text
sdlc/
  requirements/
  architecture/
  planning/
  development/
  testing/
  security/
  deployment/
  maintenance/

agile/
  scrum/
  sprint-planning/
  backlog/
  kanban/
  retrospectives/

engineering/
  coding/
  debugging/
  code-review/
  refactoring/

autonomous/
  task-manager/
  watchdog/
  recovery/
  self-learning/
```

Each skill should have:

- Trigger conditions.
- Inputs.
- Procedure.
- Outputs.
- Verification.
- Failure handling.
- Security notes.

---

# 14. Phase 11: Server Operations

Create a server operations service around Hermes local terminal capabilities.

## Read-only operations

- system status
- CPU/RAM
- disk
- processes
- services
- Docker
- ports
- logs
- network

## Safe operations

- restart configured service
- rotate logs
- pull approved deployment
- rebuild approved container
- restart unhealthy container

## Privileged operations

Require approval:

- firewall changes
- SSH changes
- users
- sudo
- system-wide packages
- storage changes

## Destructive

Always require explicit approval:

- deleting databases
- deleting volumes
- deleting repositories
- formatting disks
- mass file deletion

---

# 15. Phase 12: Server Watchdog

Create health checks.

```text
HealthMonitor
├── Host
├── Services
├── Containers
├── Applications
├── HTTP endpoints
├── Databases
├── Disk
└── Network
```

Example incident:

```text
Service unhealthy
  |
Collect logs
  |
Check resource pressure
  |
Check dependencies
  |
Search known fixes
  |
Apply safe repair
  |
Restart
  |
HTTP health check
  |
Observe for stability period
  |
Record result
  |
Notify
```

---

# 16. Phase 13: Security Architecture

Security should be implemented before unrestricted autonomy.

## Identity

- Dedicated service account.
- Separate credentials.
- No secrets in model context unless required.
- Environment-specific credentials.

## Permissions

Use capability-based policies.

Example:

```yaml
agent: qa
permissions:
  terminal: true
  repository_write: false
  production_deploy: false
  server_admin: false
```

## Approval system

Approvals must include:

- action
- reason
- target
- risk
- proposed command
- rollback
- expiration

## Audit

Record every privileged action.

---

# 17. Phase 14: Voice

Build on Hermes voice capabilities.

Required experience:

```text
Wake
 |
Listen
 |
Transcribe
 |
Plan
 |
Execute
 |
Speak
 |
Listen
```

Enhancements:

- configurable wake phrase
- language detection
- multilingual TTS
- barge-in
- interruption
- streaming
- spoken progress
- cancellation
- voice confirmations for risky actions

---

# 18. Phase 15: Remote API and Mobile UI

Expose a secure control API.

Endpoints conceptually:

```text
POST /tasks
GET /tasks
GET /tasks/{id}
POST /tasks/{id}/cancel
POST /tasks/{id}/resume
GET /tasks/{id}/events
GET /agents
GET /projects
GET /server/health
GET /github/status
GET /approvals
POST /approvals/{id}/approve
POST /approvals/{id}/reject
```

Use:

- strong authentication
- TLS
- rate limiting
- audit logging
- session isolation
- role-based access

---

# 19. Phase 16: Notifications and Proactive Operations

Implement event-driven notifications.

Events:

- task completed
- task failed
- task blocked
- approval required
- CI failed
- deployment failed
- server unhealthy
- security issue
- scheduled report ready

Notification policy:

```text
critical -> immediate
high -> immediate
normal -> batched
low -> digest
```

---

# 20. Phase 17: Control Center

Dashboard sections:

1. Overview.
2. Tasks.
3. Agents.
4. Projects.
5. GitHub.
6. Server.
7. Memory.
8. Learning.
9. Skills.
10. Approvals.
11. Audit.
12. Model routing.

---

# 21. Phase 18: Testing

## Unit

Test:

- planner
- policy engine
- state machine
- routing
- recovery
- learning
- memory
- verification

## Integration

Test:

- Hermes integration.
- OmniRoute.
- GitHub.
- database.
- terminal.
- browser.
- notifications.

## End-to-end

Scenario:

```text
"Fix GitHub issue #X"
```

Expected:

- issue read
- plan
- agents
- branch
- code
- tests
- PR
- CI
- recovery
- verification
- final status

## Chaos tests

Simulate:

- process crash
- network loss
- model timeout
- GitHub failure
- disk pressure
- Docker failure
- agent failure
- conflicting edits

---

# 22. Deployment Strategy

Recommended initial topology:

```text
Internet
   |
Reverse Proxy
   |
Jarvis API
   |
Hermes
   |
+--+-------------+
|                |
OmniRoute       Database
|
Models/providers

Background:
Task Worker
Agent Workers
Watchdog
Scheduler
```

Use systemd or an equivalent supervisor.

Keep persistent state outside ephemeral containers.

---

# 23. Rollout Strategy

### Milestone 1

Foundation + Task Manager.

### Milestone 2

Planner + Delegation.

### Milestone 3

Verification + Recovery.

### Milestone 4

Memory + Learning.

### Milestone 5

GitHub + SDLC.

### Milestone 6

Server Operations + Watchdog.

### Milestone 7

Security hardening.

### Milestone 8

Voice + Remote API.

### Milestone 9

Mobile dashboard.

### Milestone 10

Proactive autonomous operation.

---

# 24. Recommended First Production Workflow

The first serious autonomous workflow should be:

> "Take this GitHub issue and deliver it."

Pipeline:

```text
User
 |
Jarvis
 |
Understand issue
 |
Check project memory
 |
Create plan
 |
Architect
 |
Developer
 |
QA
 |
Security
 |
Reviewer
 |
GitHub PR
 |
CI
 |
Recovery if needed
 |
Verification
 |
Merge if authorized
 |
Deployment
 |
Health check
 |
Learn
 |
Notify user
```

If this workflow becomes reliable, the same architecture can be generalized to most software engineering tasks.

---

# 25. Definition of Done for Jarvis v1

Jarvis v1 is complete when it can:

- Receive remote text/voice tasks.
- Persist tasks.
- Resume tasks after restart.
- Plan complex work.
- Delegate specialized work.
- Store artifacts.
- Verify results.
- Recover from common failures.
- Learn from outcomes.
- Route models intelligently.
- Manage GitHub issue-to-PR workflows.
- Support SDLC/Agile skills.
- Monitor and operate the hosted server under permissions.
- Require approval for high-risk actions.
- Notify the user.
- Maintain a complete audit trail.

---

# 26. Future Evolution

After v1:

- Multi-server orchestration.
- Specialized persistent worker pools.
- Distributed task queues.
- Advanced knowledge graph.
- Predictive maintenance.
- Proactive project management.
- Automatic dependency upgrade campaigns.
- Automated security remediation.
- Cross-repository change propagation.
- Cost-aware fleet routing.
- Rich multimodal computer use.
- More advanced personal knowledge modeling.

The long-term design should remain modular so these capabilities can be added without replacing the core Hermes foundation.
