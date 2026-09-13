# Jarvis Architecture
## System Design Reference

## 1. Core Principle

Jarvis is an orchestration layer above Hermes Agent.

```text
User
 |
Interface
 |
Jarvis Core
 |
+-- Intent
+-- Memory
+-- Planner
+-- Policy
+-- Task Manager
+-- Agent Orchestrator
+-- Verification
+-- Recovery
+-- Learning
 |
+-- Hermes execution/tool layer
 |
+-- OmniRoute model routing
 |
+-- GitHub
+-- Browser
+-- Server
+-- Notifications
```

## 2. Execution Loop

```text
REQUEST
  |
CLASSIFY
  |
RECALL
  |
PLAN
  |
AUTHORIZE
  |
DISPATCH
  |
EXECUTE
  |
OBSERVE
  |
VERIFY
  |
+-- PASS -> COMPLETE
|
+-- FAIL -> DIAGNOSE
             |
             CHANGE STRATEGY
             |
             RETRY
             |
             VERIFY
```

## 3. Autonomous Task State Machine

```text
queued
  |
planning
  |
authorized
  |
running
  |
verifying
  |
completed

running -> blocked
running -> failed
failed -> recovering
recovering -> running
blocked -> awaiting_approval
awaiting_approval -> authorized
any active state -> cancelled
```

## 4. Agent Contracts

Agents should return structured artifacts.

Example:

```yaml
status: completed
summary: ...
artifacts:
  - path: architecture.md
verification:
  passed: true
  evidence:
    - architecture reviewed
risks:
  - ...
lessons:
  - ...
```

## 5. Data Domains

### Operational database

Task state, agents, events, approvals, verification, audit.

### Memory database

Facts, preferences, project knowledge, experiences.

### Artifact store

Documents, reports, patches, logs, test results.

### Skill store

Procedural knowledge.

### Routing telemetry

Model/provider performance.

## 6. Security Boundary

Treat the following as trust boundaries:

- User input.
- Web content.
- Git repositories.
- External skills.
- Agent-generated commands.
- Model outputs.
- Third-party APIs.

No external content should automatically gain permission to execute privileged operations.

## 7. Reliability

Every autonomous action should be:

- observable
- bounded
- recoverable where possible
- verifiable
- auditable

## 8. Server Administration Model

The agent controls only what the execution identity permits.

Use:

```text
Jarvis
  |
Policy
  |
Capability
  |
Execution identity
  |
Operating system
```

Never bypass the policy layer for convenience.

## 9. Knowledge Lifecycle

```text
Experience
  |
Evaluate
  |
Lesson
  |
Score
  |
Store
  |
Retrieve
  |
Reuse
  |
Evaluate again
```

Successful lessons can become skills.

## 10. Model Routing Lifecycle

```text
Task
 |
Classify
 |
Candidate models
 |
Historical telemetry
 |
Score
 |
Select
 |
Execute
 |
Evaluate
 |
Update telemetry
```

## 11. Observability

Use correlation IDs:

```text
request_id
task_id
step_id
agent_id
attempt_id
event_id
```

A user should be able to trace:

`"Why did Jarvis say this task failed?"`

to the exact agent, tool, command, error, retry, and verification result.

## 12. Architectural Rule

Do not optimize for maximum autonomy first.

Optimize in this order:

```text
Correctness
 ->
Verification
 ->
Recovery
 ->
Persistence
 ->
Security
 ->
Autonomy
 ->
Optimization
```

That ordering is essential for a trustworthy Jarvis.
