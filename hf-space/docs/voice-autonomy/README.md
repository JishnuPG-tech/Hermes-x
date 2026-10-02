# Voice-First Jarvis Autonomy

This document set defines the production design for a voice-first Jarvis layer built around Hermes Agent and OmniRoute.

## Scope

The design covers real-time voice interaction, wake-word operation, continuous listening, streaming STT, streaming LLM responses, low-latency TTS, Kokoro primary TTS, EdgeTTS fallback, barge-in, durable task execution, background work, mobile and laptop access, observability, security, testing, and operations.

## Existing system relationship

- Hermes is the execution and agent harness layer.
- OmniRoute is the model/provider routing layer.
- Jarvis Core is the orchestration and control layer.
- Agent OS provides durable task, policy, memory, scheduler, trace, and agent lifecycle primitives.
- GitHub is the engineering system of record for software delivery.
- Notion is the structured workspace layer.
- Obsidian is the long-form knowledge and review layer.

## Voice principle

Voice is an interface to the same durable Jarvis runtime, not a separate assistant. A voice request creates or resumes a normal Jarvis session/task. Text, voice, mobile, and laptop clients therefore share task state, memory, permissions, and execution history.

## Latency principle

The voice runtime has a formal first-audio SLA:

- Target: first playable spoken audio within 5 seconds after the user finishes speaking.
- Hard maximum target: 10 seconds.
- Kokoro is the primary local TTS path.
- If Kokoro has not produced playable audio by the fallback deadline, EdgeTTS is activated automatically.
- Long tasks must acknowledge immediately and continue in the background.
- TTS must consume streamed LLM text incrementally instead of waiting for the full response.

## Document index

| Document | Purpose |
|---|---|
| `PRD.md` | Product requirements and acceptance criteria |
| `ARCHITECTURE.md` | Detailed runtime and component architecture |
| `IMPLEMENTATION_PLAN.md` | Phased implementation plan and milestones |
| `RUNTIME_CONTRACT.md` | Runtime contracts, states, events, and interfaces |
| `LATENCY_SLA.md` | Voice latency budget, measurement, and SLOs |
| `TTS_FAILOVER.md` | Kokoro to EdgeTTS failover design |
| `SECURITY.md` | Voice, tool, agent, secret, and approval security |
| `OPERATIONS.md` | Deployment, health, recovery, and incident procedures |
| `TEST_PLAN.md` | Unit, integration, performance, chaos, and voice tests |
| `ADR.md` | Architectural decisions and rejected alternatives |

## Deployment intent

Initial deployment keeps voice gateway, Jarvis Core, Hermes, and Kokoro together in the Hermes Space where practical. OmniRoute remains a separate routing service. A third compute surface remains available for a later bottleneck such as streaming STT, browser automation, or worker execution.

Hugging Face CPU Basic currently provides 2 vCPU and 16 GB RAM at no hardware hourly charge, but creating new compute Spaces such as Docker or Gradio Spaces requires a paid plan. Existing deployment entitlements must therefore be treated as an infrastructure constraint, not assumed to be unlimited. 

## Non-goals

This design does not claim perfect or unlimited autonomy. The runtime is bounded by provider availability, execution permissions, infrastructure limits, model capability, and safety policies. The objective is dependable autonomous execution with recovery and evidence-based completion.
