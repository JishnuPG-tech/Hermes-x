# Architecture Decision Records

## ADR-001: Hermes remains the execution engine

**Decision:** Do not replace Hermes with a second agent loop. Hermes provides model/tool execution, skills, memory and sub-agent primitives. The harness adds durable orchestration and policy.

**Reason:** Avoid duplicated runtime logic and preserve upstream compatibility.

## ADR-002: Agent OS is an application layer

**Decision:** Agent OS is the control environment around Hermes, not a kernel or operating-system replacement.

**Reason:** It provides projects, tasks, permissions, scheduling, memory and integrations while Linux remains the host OS.

## ADR-003: Durable task state is authoritative

**Decision:** Task state is persisted independently of model conversations.

**Reason:** Models can restart, change providers or lose context. Durable state must not depend on a model's memory.

## ADR-004: Verification is a separate boundary

**Decision:** Completion requires external evidence.

**Reason:** A language model can produce an incorrect success statement.

## ADR-005: Policy is enforced outside the model

**Decision:** Permissions are checked by the harness before tool execution.

**Reason:** Prompt instructions are not a security boundary.

## ADR-006: External knowledge is untrusted

**Decision:** GitHub, Obsidian, Notion and web content are treated as data, not authority.

**Reason:** Prevent prompt injection and privilege escalation.

## ADR-007: Start single-instance, design for distribution

**Decision:** First release uses a single control-plane deployment with durable storage. Interfaces are designed so workers can later be distributed.

**Reason:** Distributed infrastructure before durable semantics are correct adds failure modes without solving the fundamental problem.

## ADR-008: Integrations use capability adapters

**Decision:** Integrations expose narrow actions rather than raw SDKs or credentials.

**Reason:** Stable contracts make permissions, auditing and testing possible.

## ADR-009: Events are first-class

**Decision:** Run events are durable and drive UI, monitoring and audit.

**Reason:** The UI and watchdog must be able to reconstruct what happened without reading model internals.

## ADR-010: Bounded autonomy

**Decision:** Autonomous loops continue until completion, policy stop, budget exhaustion, unresolved ambiguity or infrastructure failure.

**Reason:** "Never stop" is not safe or technically meaningful on finite infrastructure.
