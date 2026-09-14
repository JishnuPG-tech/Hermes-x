# Architecture Decision Record

## ADR-001: Hermes is the system authority

### Status
Accepted

### Decision

Hermes Agent is the king of the Jarvis system. OmniRoute is a subordinate infrastructure layer that supplies model inference and routing.

### Rationale

The user needs one stable assistant identity and one durable agent runtime. Models and providers should be replaceable without changing the assistant's conversation, memory, tools, tasks or behavior contract.

Hermes already provides the agent loop, tools, gateways and autonomous execution model. OmniRoute provides model connectivity and routing. Combining these responsibilities would create an unnecessary second control plane.

### Consequences

Positive:

- One user-facing assistant
- Stable Hermes identity across model changes
- Independent OmniRoute upgrades
- Easier provider replacement
- Clear security boundaries
- Durable Hermes task state independent of model gateway lifetime

Negative:

- Hermes must maintain a provider adapter boundary
- Some routing features remain infrastructure-specific
- Failover logic must be carefully divided between Hermes and OmniRoute

### Explicit rule

> **Hermes is the king. OmniRoute powers the king.**

### ADR-002: User communication terminates at Hermes

The public voice/chat gateway belongs to Hermes. OmniRoute is a private or authenticated internal model service.

### ADR-003: Models are replaceable

A model is an inference dependency, not the assistant identity. Switching models must preserve Hermes session and durable task state.
