# Hermes Master Blueprint

## 1. Product identity

Hermes is the user-facing autonomous agent and system authority.

Canonical hierarchy:

```text
User
  ↓
Hermes Agent
  ├─ Conversation
  ├─ Memory
  ├─ Planning
  ├─ Agent OS primitives
  ├─ Tools / Terminal / Files
  ├─ GitHub / Notion / Integrations
  ├─ Permissions / Security
  ├─ Voice session
  ├─ Background jobs
  └─ Verification / Recovery / Learning
          ↓
      OmniRoute
          ↓
       Models
```

**Hermes is the king. OmniRoute powers the king. Models power OmniRoute's routing targets. The user communicates with Hermes, never with OmniRoute directly.**

## 2. Responsibility boundaries

### Hermes owns
- User conversation and identity
- Task lifecycle
- Planning and decomposition
- Tool selection and execution
- Memory
- Workspace and project ownership
- GitHub authority
- Notion authority
- Permissions and approvals
- Agent delegation and supervision
- Verification and recovery
- Voice session state
- Background jobs
- Final decisions and user-facing responses
- Learning and self-improvement policy

### OmniRoute owns
- Model/provider connectivity
- Model routing
- Provider fallback
- Model availability
- Routing telemetry
- Cost, latency and quality signals

OmniRoute is not a second agent, planner, memory owner, user-facing assistant or authority layer.

## 3. Core runtime

Every objective follows:

```text
INTAKE
 → SPECIFY
 → PLAN
 → AUTHORIZE
 → EXECUTE
 → OBSERVE
 → VERIFY
 → RECOVER if needed
 → COMPLETE
 → RECORD
 → LEARN
```

Long-running objectives persist checkpoints so restart or reconnect does not lose task state.

## 4. Agent OS

Agent OS is the infrastructure layer inside Hermes, not a competing operating system.

Primitives:
- Spec: task contract
- Trace: execution trace and audit
- Trust: authority and permission
- Memory: durable knowledge
- Pipe: agent-to-agent communication
- Checkpoint: resumable execution state
- Workspace: isolated project/task filesystem
- Artifact: produced output

## 5. Trust hierarchy

```text
Owner Policy
  ↓
Hermes Policy
  ↓
Project Policy
  ↓
Task Policy
  ↓
Worker Policy
  ↓
Tool Permission
  ↓
Actual Action
```

Destructive, financial, security-sensitive, credential, external-publication or scope-expanding actions require approval unless an explicit policy already authorizes them.

## 6. Autonomous execution

Hermes should continue until the objective is complete, not merely until the first tool call succeeds.

Required behavior:
- Retry transient failures
- Change strategy after repeated failure
- Wait for dependencies when appropriate
- Request approval only when authority is insufficient
- Persist state before long waits
- Verify local and remote outcomes
- Report completion only after verification
- Record useful lessons

Autonomy remains bounded by provider availability, host resources, permissions, safety policies and explicit approval requirements.

## 7. Multi-agent execution

Hermes may delegate specialized work to worker agents.

Worker contract:
- Objective
- Inputs
- Allowed tools
- Workspace
- Expected output
- Verification criteria
- Deadline or resource boundary
- Reporting format

Workers do not become system authorities. Hermes supervises, synthesizes and verifies their results.

## 8. Voice architecture

```text
Wake phrase
 → VAD
 → Streaming STT
 → Hermes session
 → OmniRoute / model inference
 → Hermes action loop
 → Streaming TTS
 → Speaker
 ↘ barge-in → STT
```

Voice is an interface to the same Hermes session. It does not create a second assistant.

The session retains:
- conversation context
- active task
- current plan
- agent state
- interruption state
- listening/speaking state
- permissions

## 9. UI architecture

The UI is mobile-first and preserves the calm Claude-style visual language.

```text
Hermes UI
├─ Conversation
├─ Voice
├─ Tasks
├─ Agents
├─ Projects
├─ GitHub
├─ Notion
├─ Memory
├─ Integrations
├─ Terminal
├─ Artifacts
├─ Notifications
└─ Settings
       ↓
Hermes Agent
       ↓
Agent OS
       ↓
OmniRoute
       ↓
Models
```

The UI must make Hermes feel like one coherent autonomous assistant rather than a collection of unrelated tools.

## 10. Persistence

Persistent application state belongs on durable storage, with code/version truth in GitHub.

Recommended live state:

```text
/data/jarvis/
├─ config/
├─ secrets/
├─ memory/
├─ agent-state/
├─ projects/
├─ workspaces/
├─ artifacts/
└─ logs/
```

GitHub remains source-code and release truth. Notion remains structured human-visible project knowledge. Agent OS remains execution-trace truth.

## 11. Integrations

Integrations are capability adapters selected and authorized by Hermes.

Initial integration families:
- GitHub
- Notion
- Gmail
- Telegram
- WhatsApp
- Hugging Face
- OAuth providers
- API/MCP providers

Credentials are stored as encrypted references. Secrets are never intentionally placed in model prompts, traces or Notion pages.

## 12. Engineering lifecycle

```text
Idea
 → PRD
 → UX contract
 → Architecture
 → UI specification
 → Implementation
 → Unit tests
 → Functional QA
 → Visual QA
 → Security QA
 → CI
 → Remote verification
 → Release
 → Observability
 → Learning
```

## 13. Definition of done

A Hermes feature is done only when:
- Product behavior is specified
- UX states are specified
- Permissions are defined
- Mobile and desktop behavior are defined
- Loading, empty, error, offline and success states exist
- Accessibility requirements are met
- Functional tests pass
- Visual regression passes
- Remote state is verified where applicable
- Documentation is updated
- Notion is synchronized

## 14. Source-of-truth model

| Domain | Source of truth |
|---|---|
| Product planning | Notion |
| Human-visible knowledge | Notion |
| Source code | GitHub |
| CI/CD | GitHub Actions |
| Runtime execution | Hermes / Agent OS |
| Memory | Hermes durable memory |
| Model/provider routing | OmniRoute |
| Live filesystem | Persistent Hermes storage |
| UI design contract | `docs/ui-ux/` + Notion Hermes UI Kit |
| Audit trace | Agent OS |

## 15. Non-goals

- OmniRoute becoming a second autonomous assistant
- UI exposing model providers as separate assistants
- Rebuilding Linux as an Agent OS
- Treating the frontend as the authority
- Treating screenshots as the only source of UI behavior
- Storing secrets in source code
