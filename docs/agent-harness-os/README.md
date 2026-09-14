# Hermes-x Agent Harness + Agent OS

## Purpose

This document set defines the next architectural layer around Hermes Agent: a production-grade **Agent Harness** and a user-facing **Agent OS** for the Hermes-x Hugging Face deployment.

The goal is not to replace Hermes. Hermes remains the execution engine and model-facing agent. Hermes-x adds the durable control plane around it so the system can plan, delegate, execute, verify, recover, remember, govern, observe, and operate long-running work.

## Core idea

```text
                    HERMES-X AGENT OS
┌──────────────────────────────────────────────────────────────┐
│ Mobile / Web / Voice / Telegram / Desktop                   │
├──────────────────────────────────────────────────────────────┤
│ Identity • Sessions • Projects • Tasks • Approvals           │
├──────────────────────────────────────────────────────────────┤
│                    AGENT HARNESS                             │
│ Intent → Context → Plan → Act → Observe → Verify → Recover  │
│              ↘ Delegate → Review → Integrate                │
├──────────────────────────────────────────────────────────────┤
│ Hermes Agent runtime • Skills • MCP • Tools • Terminal       │
├──────────────────────────────────────────────────────────────┤
│ OmniRoute • Models • Provider routing                         │
├──────────────────────────────────────────────────────────────┤
│ Memory • Event log • Task state • Artifacts • Audit          │
├──────────────────────────────────────────────────────────────┤
│ GitHub • Obsidian • Notion • HF • Servers • External APIs    │
└──────────────────────────────────────────────────────────────┘
```

## What is an Agent Harness?

An agent harness is the runtime scaffolding around a model that determines what the agent sees, what it can do, how state is persisted, how tools are called, how approvals work, how failures are recovered, and how completion is verified. This matches current agent-framework architecture where a harness coordinates model calls, tools, context, state, approvals and bounded progress loops. See the references at the end.

For Hermes-x, the harness owns **control and reliability**, while Hermes owns **agent execution**.

## What is Agent OS?

Agent OS is the higher-level operating environment built on top of the harness. It is not a literal replacement for Linux or Android. It is an application-level operating system for autonomous digital work.

It provides:

- Projects and workspaces
- Long-running tasks
- Agent and sub-agent lifecycle
- Durable queues
- Scheduling
- Memory and knowledge
- Permissions and approvals
- Secrets boundaries
- Files and artifacts
- GitHub lifecycle
- Obsidian and Notion knowledge integration
- Server operations
- Monitoring and alerts
- Audit trails
- Recovery and resumability
- Mobile/web control surfaces

## Design principles

1. **Model-agnostic**: OmniRoute selects the best available model.
2. **Runtime-first**: Hermes remains the actual execution engine.
3. **Durable by default**: important state survives process and Space restarts.
4. **Verify before declaring done**: success is evidence, not model text.
5. **Least authority**: permissions are explicit and scoped.
6. **Human approval for dangerous actions**: destructive and production operations can pause for approval.
7. **Idempotent operations**: retries must not corrupt state.
8. **Observable execution**: every run has events, status and audit metadata.
9. **Composable integrations**: GitHub, Obsidian, Notion and future connectors use stable adapters.
10. **Graceful degradation**: loss of one integration must not destroy the core agent.
11. **No fake autonomy**: the system may continue only while infrastructure, credentials and policy permit it.
12. **Learning is explicit**: lessons are captured, evaluated and promoted into reusable skills only when useful.

## Document map

| Document | Purpose |
|---|---|
| `PRD.md` | Product requirements and acceptance criteria |
| `ARCHITECTURE.md` | Detailed system architecture |
| `IMPLEMENTATION_PLAN.md` | Phased implementation plan |
| `RUNTIME_CONTRACT.md` | Contracts for runs, tasks, tools and events |
| `SECURITY.md` | Threat model and authority model |
| `OBSERVABILITY.md` | Events, traces, metrics and operational visibility |
| `INTEGRATIONS.md` | GitHub, Obsidian, Notion and external systems |
| `TEST_PLAN.md` | Reliability and acceptance testing |
| `ADRs.md` | Architecture decisions |
| `ROADMAP.md` | Delivery sequence and future extensions |

## Target outcome

A user can say:

> "Hermes, build this feature, use the project notes in Obsidian, update the Notion task, implement it in GitHub, run tests, fix failures, open the PR, monitor CI, and tell me only when it is actually ready."

The system should turn that request into a durable task graph, execute independent work in parallel where safe, preserve state across restarts, verify every important boundary, request approval only when policy requires it, and leave an auditable record of what happened.

## References

- Hermes Agent: https://github.com/hermes-agent-org/hermes
- Microsoft Agent Framework Harness concept: https://learn.microsoft.com/en-us/agent-framework/concepts/harness
- Harness architecture reference: https://www.harnessarch.com/harness
- Example Agent OS architecture: https://github.com/earthwalker17/agent-os
- Hermes-x repository: https://github.com/JishnuPG-tech/Hermes-x
