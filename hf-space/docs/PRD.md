# Jarvis Autonomous AI System
## Product Requirements Document (PRD)

**Status:** Draft v1.0  
**Date:** 2026-09-13  
**Primary foundation:** Hermes Agent  
**Model routing:** OmniRoute  
**Product concept:** A persistent, voice-first, autonomous AI assistant that can plan, delegate, execute, verify, recover, learn, operate a hosted server, and manage software projects end-to-end.

---

## 1. Executive Summary

Jarvis is a higher-level autonomous assistant built around Hermes Agent rather than a replacement for Hermes.

The system should feel like a persistent technical assistant that can be reached from phone or laptop, wake on a configured voice phrase, maintain long-term context, execute terminal and browser tasks, delegate large work to specialized agents, manage GitHub projects, operate the server on which it is hosted, recover from failures, verify work independently, and learn from successful and failed experiences.

The design deliberately avoids claiming perfect autonomy or AGI. The objective is reliable autonomy through planning, persistence, delegation, verification, recovery, permissions, and learning.

### Core separation of responsibilities

- **Hermes:** execution engine, tools, sessions, skills, voice, terminal, browser, delegation, scheduling, messaging.
- **OmniRoute:** model/provider routing and selection.
- **Jarvis Core:** orchestration, planning, task state, policy, verification, recovery, learning coordination.
- **Memory layer:** durable factual, project, preference, experience, and knowledge state.
- **Learning Engine:** converts outcomes and corrections into reusable strategies and skills.
- **Server Operations:** controlled administration and self-healing of the hosted machine.
- **GitHub Engineering:** repository, issue, branch, PR, CI, review, release, and deployment workflows.
- **Mobile/Web Interface:** remote control, voice, approvals, status, logs, and notifications.

---

## 2. Product Vision

### Vision

Create a persistent personal AI engineering and operations assistant that can receive a goal in natural language and independently coordinate the work required to achieve it.

### Example

User says:

> "Build the authentication service, test it, create the GitHub PR, monitor CI, fix failures, and tell me when it is ready."

Jarvis should:

1. Understand the objective.
2. Inspect project context.
3. Build a plan.
4. Identify dependencies.
5. Create specialized sub-agents.
6. Execute independent work in parallel where safe.
7. Integrate artifacts.
8. Run tests and security checks.
9. Create a branch and PR.
10. Monitor CI.
11. Diagnose failures.
12. Change strategy and retry.
13. Verify the final result.
14. Update project state.
15. Record useful lessons.
16. Notify the user.

---

## 3. Goals

### Primary goals

1. Persistent autonomous task execution.
2. Large-task decomposition into specialized agents.
3. Reliable recovery from failures.
4. Independent verification before completion.
5. Long-term learning from experience.
6. Intelligent model routing through OmniRoute.
7. Full software development lifecycle support.
8. GitHub-first engineering workflows.
9. Controlled server administration.
10. Real-time voice interaction.
11. Mobile and laptop access.
12. Proactive monitoring and notifications.
13. Strong security and permission controls.
14. Persistent project state and checkpoints.

### Non-goals

- Claiming human-level AGI.
- Blind unlimited execution with no safeguards.
- Giving every agent unrestricted root access by default.
- Treating an LLM response as proof that work succeeded.
- Replacing every existing Hermes capability unnecessarily.

---

## 4. User Experience

### 4.1 Voice

Desired flow:

`Wake phrase -> listen -> understand -> reason -> execute -> speak -> listen again`

Requirements:

- Configurable wake phrase.
- Streaming speech-to-text.
- Streaming text-to-speech.
- Silence detection.
- Barge-in.
- User interruption.
- Cancel/stop commands.
- Multilingual speech support.
- Automatic language detection.
- Voice session continuity.
- Spoken progress updates for long jobs.

### 4.2 Mobile and laptop

The device should primarily be a control surface.

Capabilities:

- Chat.
- Voice.
- Active task list.
- Agent status.
- Approval requests.
- Logs.
- GitHub status.
- Notifications.
- Server health.
- Project dashboard.
- Start/resume/cancel tasks.

Actual execution occurs on the persistent Jarvis server.

---

## 5. Functional Requirements

### FR-01 Intent and Query Engine

Jarvis must classify requests before execution.

It should determine:

- Intent.
- Complexity.
- Required context.
- Required tools.
- Whether web research is needed.
- Whether coding is needed.
- Whether delegation is needed.
- Whether the task should be synchronous or background.
- Required permission level.
- Appropriate model/provider.

Pipeline:

`Request -> Intent -> Memory -> Complexity -> Plan -> Tools -> OmniRoute -> Execution`

---

### FR-02 Persistent Task Manager

Every autonomous task must have durable state.

Minimum state:

```yaml
task_id:
objective:
status:
priority:
created_at:
updated_at:
project:
parent_task:
dependencies:
plan:
current_step:
assigned_agents:
artifacts:
attempts:
failures:
verification:
permissions:
checkpoint:
next_action:
result:
lessons:
```

States:

- queued
- planning
- running
- waiting
- blocked
- verifying
- recovering
- completed
- failed
- cancelled
- awaiting_approval

The task must survive process restart.

---

### FR-03 Planner

The planner creates executable plans rather than only conversational answers.

A plan contains:

- Objective.
- Constraints.
- Success criteria.
- Dependencies.
- Risks.
- Required roles.
- Parallelizable tasks.
- Sequential tasks.
- Verification gates.
- Recovery strategy.
- Approval requirements.

---

### FR-04 Multi-Agent Orchestrator

Large tasks must be decomposed automatically.

Example roles:

- Product Manager.
- Business Analyst.
- Requirements Engineer.
- Architect.
- UI/UX.
- Frontend Developer.
- Backend Developer.
- Database Engineer.
- QA Engineer.
- Security Engineer.
- Code Reviewer.
- DevOps Engineer.
- SRE.
- Researcher.
- Documentation Engineer.

Each task must have a contract:

```yaml
TASK:
ROLE:
INPUT:
RESPONSIBILITIES:
CONSTRAINTS:
SUCCESS_CRITERIA:
OUTPUT_ARTIFACTS:
DEPENDENCIES:
VERIFICATION:
PERMISSIONS:
```

Agents should not edit the same files concurrently unless using worktree isolation or explicit coordination.

---

### FR-05 Shared Project Workspace

Each project receives persistent artifacts:

```text
project/
  requirements.md
  user-stories.md
  acceptance-criteria.md
  architecture.md
  decisions.md
  task-board.json
  progress.json
  test-results.json
  security-report.md
  review.md
  deployment.md
  lessons.md
```

Agents communicate through artifacts and structured results, not only natural-language summaries.

---

### FR-06 Verification Engine

Jarvis must independently verify completion.

For software:

1. Inspect diff.
2. Lint.
3. Type check.
4. Unit tests.
5. Integration tests.
6. Build.
7. Security checks.
8. Runtime test.
9. Behavior verification.
10. Review.
11. GitHub state verification.

Completion must require objective evidence.

---

### FR-07 Self-Healing and Recovery

Recovery loop:

`Attempt -> Observe -> Diagnose -> Search memory -> Change strategy -> Retry -> Verify`

Suggested policy:

```yaml
max_attempts: 5
strategy_change_after: 2
research_after: 3
specialist_after: 3
human_escalation_after: 5
```

Recovery must avoid blindly repeating identical failed actions.

---

### FR-08 Learning Engine

Learning must operate at the agent layer and must not be confused with retraining the underlying model.

Store:

- Task.
- Approach.
- Tool usage.
- Model/provider.
- Result.
- Error.
- Correction.
- Successful fix.
- Duration.
- Cost.
- Confidence.
- User acceptance.
- Reusability.

Learning pipeline:

`Task -> Execution -> Outcome -> Evaluation -> Lesson -> Memory/Skill -> Future retrieval`

The system should learn which strategies and models work best for specific task types.

---

### FR-09 Memory

Memory categories:

1. User preferences.
2. Environment facts.
3. Project facts.
4. Technical knowledge.
5. Experience memory.
6. Error/fix memory.
7. Successful workflows.
8. Model performance.
9. Tool performance.
10. Agent performance.

Metadata:

```yaml
importance:
confidence:
recency:
frequency:
source:
last_used:
project:
scope:
```

Required features:

- Semantic retrieval.
- Keyword/session retrieval.
- Contradiction detection.
- Deduplication.
- Importance scoring.
- Archiving.
- Compression.
- Project scoping.
- User scoping.
- Experience retrieval.
- Skill conversion.

---

### FR-10 Strategy Library

Repeated successful workflows should become reusable strategies.

Example:

`"Fix Docker service failing after deployment"`

could evolve into:

`docker-service-recovery-v2`

The strategy records:

- Preconditions.
- Diagnosis steps.
- Known failure modes.
- Fixes.
- Verification.
- Rollback.
- Last successful version.

---

### FR-11 Model Routing

OmniRoute should be treated as a learned routing layer.

Score candidates using:

- Task fit.
- Historical success.
- Quality.
- Latency.
- Cost.
- Context capacity.
- Reliability.
- Tool-use ability.

Example routing:

- Simple tasks -> fast/cheap model.
- Complex coding -> strong coding model.
- Architecture -> reasoning model.
- Research -> search/reasoning model.
- Vision -> vision-capable model.
- Long context -> long-context model.

The router should continuously update model performance statistics.

---

### FR-12 GitHub Engineering

Jarvis must support end-to-end engineering workflows:

- Authentication checks.
- Repository discovery.
- Clone/read/write.
- Branch creation.
- Commits.
- Push.
- Issues.
- Labels.
- Assignments.
- PR creation.
- PR review.
- CI monitoring.
- CI failure diagnosis.
- Fix and push.
- Merge when authorized.
- Releases.
- Deployment verification.

Canonical workflow:

`Issue -> Requirements -> Plan -> Branch -> Implementation -> Tests -> Review -> PR -> CI -> Fix -> Merge -> Verify`

Never claim:

- CI is green without checking.
- PR is merged without checking remote state.
- Deployment succeeded without verification.

---

### FR-13 Full SDLC and Agile

Supported lifecycle:

`Idea -> Requirements -> User Stories -> Acceptance Criteria -> Architecture -> Design -> Development -> Testing -> Security -> Review -> CI/CD -> Deployment -> Monitoring -> Maintenance`

Agile support:

- Product backlog.
- Prioritization.
- Sprint planning.
- Sprint execution.
- Daily progress.
- Review.
- Retrospective.
- Technical debt.
- Kanban.
- Release planning.

---

### FR-14 Server Operations

When Hermes is hosted on a server using local execution, the agent can operate within the OS permissions of its running account.

Jarvis should provide controlled server operations:

- Service management.
- Process management.
- Package management.
- Docker.
- Logs.
- Files.
- Networking.
- Ports.
- Storage.
- Backups.
- Scheduled jobs.
- Application deployment.
- Health checks.
- SSL/certificate checks.
- Database operations.
- Resource monitoring.

The system must distinguish read, normal-write, privileged, and destructive actions.

---

### FR-15 Server Watchdog

Monitor:

- CPU.
- RAM.
- Disk.
- Processes.
- Services.
- Containers.
- Network.
- HTTP endpoints.
- Database health.
- Application logs.
- Certificate expiration.

On anomaly:

`Detect -> Diagnose -> Repair -> Verify -> Learn -> Notify`

---

### FR-16 Permission and Safety System

Permission tiers:

```text
0 Read-only
1 Normal project changes
2 Terminal execution
3 Network/API access
4 GitHub write
5 Deployment
6 Destructive/system administration
```

Default policies:

- Reading -> automatic.
- Testing -> automatic.
- Branch creation -> automatic.
- PR creation -> automatic.
- Production deployment -> approval.
- Firewall changes -> approval.
- SSH configuration -> approval.
- User/sudo changes -> approval.
- Data deletion -> approval.
- Disk destruction -> approval.

High-risk actions must have explicit approval and audit logs.

---

### FR-17 Browser and Computer Automation

Support:

- Navigation.
- Search.
- Forms.
- Authenticated sessions.
- Downloads.
- Uploads.
- Web applications.
- Visual verification.

Browser actions must respect domain and permission policies.

---

### FR-18 Scheduling and Proactive Behavior

Jarvis should support:

- One-time tasks.
- Recurring tasks.
- Cron-style schedules.
- Health checks.
- CI monitoring.
- GitHub webhook events.
- Notifications.
- Nightly project reviews.
- Daily summaries.
- Security monitoring.

Example:

`Every morning -> inspect projects -> summarize blockers -> notify only if action is needed.`

---

### FR-19 Notifications

Channels may include:

- Web UI.
- Mobile.
- Telegram.
- Discord.
- Slack.
- Email where configured.

Notifications should be severity-aware.

---

### FR-20 Control Center

Dashboard should show:

- Active tasks.
- Agents.
- Queued tasks.
- Failed tasks.
- Blocked tasks.
- Server health.
- GitHub PRs.
- CI state.
- Memory statistics.
- Skills.
- Learning statistics.
- Model routing statistics.
- Approvals.
- Audit log.

---

## 6. Non-Functional Requirements

### Reliability

- Persistent task state.
- Checkpoints.
- Retry.
- Recovery.
- Verification.
- Idempotent operations where possible.

### Security

- Least privilege.
- Sandboxing where appropriate.
- Approval gates.
- Secret isolation.
- Audit logging.
- Prompt-injection defenses.
- Skill validation.
- Repository safety checks.
- No credentials in prompts or logs.

### Observability

Every task should record:

- Start/end.
- Model.
- Agent.
- Tools.
- Commands.
- Outputs.
- Errors.
- Retries.
- Cost if available.
- Verification.
- Final status.

### Performance

- Parallelize independent tasks.
- Progressive skill loading.
- Cache reusable context.
- Avoid unnecessary model calls.
- Route simple requests to inexpensive models.

### Extensibility

New capabilities should preferably be added as skills when instructions plus existing tools are sufficient, and as tools when precise programmatic integration is required.

---

## 7. Reference Architecture

```text
                         USER
                  Voice / Text / Web
                          |
                          v
                 +----------------+
                 |  JARVIS API    |
                 | Auth + Session |
                 +-------+--------+
                         |
                         v
                 +----------------+
                 | JARVIS CORE    |
                 | Identity       |
                 | Intent         |
                 | Planner        |
                 | Policy         |
                 +-------+--------+
                         |
              +----------+----------+
              |          |          |
              v          v          v
           Memory    Task Manager  OmniRoute
              |          |          |
              |          v          v
              |     Orchestrator  Best Model
              |          |
              |    +-----+-----+
              |    |     |     |
              v    v     v     v
          Learning Coder QA Research
              |          |
              +----+-----+
                   |
                   v
             Verification
                   |
              +----+-----+
              |          |
              v          v
          Recovery    Watchdog
              |          |
              +----+-----+
                   |
       +-----------+-----------+
       |           |           |
       v           v           v
    GitHub      Browser      Server
       |           |           |
       +-----------+-----------+
                   |
                   v
              Artifacts
                   |
                   v
             Long-term Memory
```

---

## 8. Success Metrics

### Reliability

- >= 95% successful completion for supported repeatable workflows.
- 100% of high-risk actions gated by policy.
- 100% of completed engineering tasks independently verified.

### Learning

- Increasing success rate on repeated task classes.
- Reduction in repeated failure patterns.
- Increased reuse of successful strategies.

### Engineering

- PR creation success.
- CI recovery success.
- Test pass rate.
- Deployment verification success.

### User experience

- Low time from request to execution.
- Low unnecessary clarification rate.
- Accurate progress reporting.
- Clear approval requests.
- Useful proactive notifications.

---

## 9. Risks

### Model errors

Mitigation: verification, retries, specialist delegation.

### Prompt injection

Mitigation: trust boundaries, tool permissions, domain controls, repository inspection, approval gates.

### Malicious repositories or skills

Mitigation: sandboxing, skill review, restricted credentials, isolated worktrees.

### Destructive commands

Mitigation: approval policy, command classification, checkpoints, backups.

### Memory corruption

Mitigation: scoped memory, confidence, contradiction detection, write approval.

### Infinite loops

Mitigation: attempt budgets, strategy-change thresholds, escalation.

### Server compromise

Mitigation: least privilege, separate execution identities, firewall, secret isolation, audit logs.

---

## 10. Acceptance Criteria

The first production milestone is accepted only when:

1. Jarvis can receive a task remotely.
2. Task state survives restart.
3. Planner generates a structured plan.
4. Large tasks can spawn specialized agents.
5. Independent work is safely parallelized.
6. Artifacts are collected.
7. Verification runs automatically.
8. Failures trigger diagnosis and strategy changes.
9. Useful lessons are stored.
10. GitHub issue-to-PR workflow works.
11. CI status is independently checked.
12. Server health can be monitored.
13. High-risk actions require approval.
14. Voice wake and conversational voice work.
15. Mobile/laptop clients can inspect and control tasks.
16. Every task has an audit trail.
