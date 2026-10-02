# Roadmap

## Milestone 1 — Persistent runtime

- [ ] durable `/data/jarvis`
- [ ] Hermes state persistence
- [ ] startup persistence checks
- [ ] project registry
- [ ] backup foundation

## Milestone 2 — Engineering computer

- [ ] project/workspace manager
- [ ] Git worktrees
- [ ] durable task manager
- [ ] verification engine
- [ ] GitHub Actions integration
- [ ] CI recovery loop

## Milestone 3 — Agent OS

- [ ] Spec
- [ ] Trace
- [ ] Trust
- [ ] Memory integration
- [ ] Pipe/event bus
- [ ] policy engine
- [ ] worker registry

## Milestone 4 — Realtime Jarvis

- [ ] WebSocket voice gateway
- [ ] wake phrase
- [ ] streaming STT
- [ ] streaming model output
- [ ] Kokoro streaming TTS
- [ ] EdgeTTS fallback
- [ ] barge-in
- [ ] reconnect/resume

## Milestone 5 — Knowledge fabric

- [ ] Notion structured workspace integration
- [ ] Obsidian local bridge
- [ ] GitHub code/issue/PR state
- [ ] Hermes durable memory
- [ ] Jarvis project knowledge index

## Milestone 6 — Autonomous operations

- [ ] scheduled maintenance
- [ ] dependency update agent
- [ ] CI watchdog
- [ ] backup watchdog
- [ ] health watchdog
- [ ] deployment verification

## Milestone 7 — Self-improvement

- [ ] skill proposal engine
- [ ] sandboxed evaluation
- [ ] regression suite
- [ ] approval/policy gate
- [ ] automatic rollback

## Milestone 8 — Scale beyond one Space

Move specific components to dedicated services only when measured constraints justify it:

- voice gateway
- browser workers
- GPU inference
- PostgreSQL
- queue/event infrastructure
- dedicated VM for Docker Compose/systemd-heavy workloads

Do not distribute the system prematurely.

## Success state

Jarvis behaves as a durable software-engineering computer: it receives a goal from any authorized client, plans and delegates work, operates the server filesystem and tools, persists progress, verifies results, uses GitHub as the engineering source of truth, survives restart, and improves through controlled learning.
