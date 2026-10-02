# Hermes ↔ OmniRoute Integration

## Core rule

> **Hermes is the king. OmniRoute powers the king.**

Hermes Agent is the application the user communicates with and the autonomous system that owns conversation, planning, tools, memory, execution, subagents, verification, recovery, scheduling and final responses.

OmniRoute is an infrastructure dependency used by Hermes to obtain model inference. It is a model gateway/router, not a second assistant.

## Topology

```text
USER
  |
  | voice / chat / text
  v
HERMES AGENT
  |
  | model inference request
  v
OMNIROUTE
  |
  +--> provider/model A
  +--> provider/model B
  +--> provider/model C
  +--> fallback provider
  |
  v
MODEL RESPONSE
  |
  v
HERMES AGENT
  |
  +--> tools
  +--> terminal
  +--> files
  +--> GitHub
  +--> browser
  +--> memory
  +--> subagents
  +--> verification
  |
  v
USER
```

## Hard boundaries

OmniRoute must never become the user-facing assistant, task owner, memory owner, GitHub owner, permission authority, or autonomous planner.

Hermes must remain functional as the system authority even when OmniRoute is temporarily unavailable. Durable state, project files and task checkpoints must never depend on the lifetime of an OmniRoute process.

## Why this separation matters

The model can change without changing the assistant. OmniRoute can route Hermes to different models based on quality, latency, cost or availability while Hermes preserves the same identity, tools, memory and workflow.

This also lets the project replace OmniRoute later without rebuilding the Hermes application architecture.

## Documentation

- `ARCHITECTURE.md` - detailed integration architecture
- `INTERFACE_CONTRACT.md` - Hermes-to-OmniRoute API contract
- `AUTHORITY_MODEL.md` - ownership and permission boundaries
- `FAILURE_AND_FALLBACK.md` - model gateway failures and recovery
- `MODEL_ROUTING.md` - how Hermes requests model power
- `TEST_PLAN.md` - integration and failure tests
- `ADR.md` - architecture decisions
