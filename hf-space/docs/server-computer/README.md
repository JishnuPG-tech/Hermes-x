# Server Computer Documentation

This directory defines the complete plan for turning the Hermes deployment into a persistent, autonomous server-side computer for Jarvis.

## Documents

- [PRD](PRD.md) — product goals, requirements, use cases, and acceptance criteria
- [Architecture](ARCHITECTURE.md) — detailed runtime topology and Agent OS primitives
- [Implementation Plan](IMPLEMENTATION_PLAN.md) — staged engineering work
- [Runtime Contract](RUNTIME_CONTRACT.md) — task, worker, checkpoint, retry, approval, and completion contracts
- [Storage and Durability](STORAGE_AND_DURABILITY.md) — persistent filesystem, database, backup, and dataset strategy
- [Security](SECURITY.md) — trust hierarchy, filesystem, terminal, secret, voice, and self-evolution controls
- [Operations](OPERATIONS.md) — startup, restart, crash recovery, monitoring, backup drills, and upgrades
- [Test Plan](TEST_PLAN.md) — persistence, recovery, security, GitHub, voice, latency, backup, and load tests
- [Deployment](DEPLOYMENT.md) — HF Space topology, environment, startup, deployment, and migration strategy
- [Configuration Contract](CONFIG_CONTRACT.md) — runtime paths, voice defaults, permissions, and persistence settings
- [Roadmap](ROADMAP.md) — milestones from persistent runtime to controlled self-improvement
- [ADR](ADR.md) — architectural decisions and their rationale

## Relationship to existing documentation

This directory complements:

- `docs/ARCHITECTURE.md`
- `docs/PRD.md`
- `docs/IMPLEMENTATION_PLAN.md`
- `docs/agent-harness-os/`
- `docs/voice-autonomy/`
- `docs/omniroute-persistence/`
- `docs/integrations/knowledge/`

The server-computer documents focus specifically on the missing execution-computer layer: persistent filesystem, project workspaces, durable task execution, server-side engineering, recovery, and host operations.

## Design rule

The goal is not to make a fictional unlimited computer. The goal is to build a robust Jarvis-like agent runtime that uses real Linux/container capabilities, durable storage, Hermes execution, OmniRoute intelligence, GitHub engineering workflows, and controlled autonomy.
