# Jarvis Architecture
## Hermes-Centric System Design Reference

## 1. Non-negotiable hierarchy

**Hermes Agent is the king of the system.**

Hermes is the user-facing autonomous agent, conversation owner, tool executor, task manager, subagent coordinator, and operational authority within the configured policy boundary.

**OmniRoute is not the boss, planner, agent, or user interface. OmniRoute is the model-power layer.**

Its job is to provide Hermes with access to models through a stable model-routing interface. Hermes decides what to do. OmniRoute helps Hermes think by supplying the appropriate model/provider.

```text
                         USER
                           |
                 voice / chat / gateway
                           |
                           v
                 +-------------------+
                 |   HERMES AGENT    |
                 |      KING         |
                 |                   |
                 | conversation      |
                 | intent             |
                 | memory             |
                 | planning           |
                 | tools              |
                 | terminal           |
                 | files              |
                 | GitHub             |
                 | subagents          |
                 | verification       |
                 | recovery           |
                 | scheduling         |
                 | policy/approval    |
                 +---------+---------+
                           |
                    model inference
                           |
                           v
                 +-------------------+
                 |     OMNIROUTE     |
                 |  POWER LAYER ONLY |
                 |                   |
                 | model catalog     |
                 | provider routing  |
                 | fallback          |
                 | latency/cost      |
                 | availability      |
                 +---------+---------+
                           |
                  models / providers
                           |
                           v
             Claude / GPT / Qwen / Gemini /
             DeepSeek / other configured models
```

## 2. Authority boundaries

### Hermes owns

- User communication
- Conversation state
- Voice session state
- Intent understanding
- Planning and decomposition
- Tool selection
- Terminal and filesystem operations
- Git and GitHub operations
- Subagent creation and supervision
- Task state and checkpoints
- Verification
- Recovery and retries
- Memory usage and learning workflows
- Scheduling/background work
- Permission and approval decisions
- Final user-facing answer

### OmniRoute owns

- Model/provider connectivity
- Model discovery/catalog where configured
- Routing requests to the selected model/provider
- Provider fallback where configured
- Model availability handling
- Routing telemetry
- Cost/latency/quality routing signals

### OmniRoute MUST NOT own

- The conversation with the user
- The task lifecycle
- Hermes memory
- Hermes tool permissions
- Project ownership
- GitHub authority
- Final task decisions
- Autonomous planning outside a Hermes request
- User-facing personality or voice session control

## 3. Request flow

```text
User speaks/types
       |
       v
Hermes receives request
       |
       +--> recall context/memory
       |
       +--> reason / plan
       |
       +--> choose tools and agents
       |
       +--> request model inference
                  |
                  v
              OmniRoute
                  |
                  +--> select/forward model
                  |
                  v
                Model
                  |
                  v
              OmniRoute
                  |
                  v
              Hermes
       |
       +--> interpret result
       +--> execute tools
       +--> verify
       +--> recover if needed
       |
       v
Hermes responds to user
```

OmniRoute is therefore a dependency of Hermes inference, not a peer authority.

## 4. Autonomous execution loop

```text
USER REQUEST
     |
     v
HERMES
     |
     +--> RECALL
     +--> PLAN
     +--> AUTHORIZE
     +--> DISPATCH
     +--> EXECUTE
     +--> OBSERVE
     +--> VERIFY
     |
     +---- PASS ----> COMPLETE
     |
     +---- FAIL ----> DIAGNOSE
                         |
                         v
                    CHANGE STRATEGY
                         |
                         v
                       RETRY
                         |
                         v
                      VERIFY
```

Hermes remains in control throughout this loop. A model returned by OmniRoute never becomes the system controller.

## 5. Model-power contract

Hermes should communicate with OmniRoute through an OpenAI-compatible model interface or the supported Hermes provider abstraction.

Conceptually:

```text
Hermes
  POST /v1/chat/completions
        |
        v
OmniRoute
        |
        +--> route to model/provider
        +--> stream model output
        +--> return model response
        |
        v
Hermes
```

The exact provider protocol may change, but the ownership rule does not.

## 6. Voice architecture

Voice is another interface to Hermes, not a separate assistant.

```text
Microphone
   |
   v
Voice Gateway / STT
   |
   v
HERMES
   |
   +--> OmniRoute --> Model
   |
   +--> tools / agents / memory
   |
   v
Streaming response
   |
   v
TTS
   |
   v
User speaker
```

Wake word, continuous listening, interruption, turn detection, TTS and session management belong to the Hermes-facing voice layer. OmniRoute only supplies model inference when Hermes needs it.

## 7. Agent OS placement

The Agent OS layer is Hermes infrastructure. It extends Hermes with durable state, trust, task management, scheduling, traces, memory and coordination primitives.

```text
User
 |
Voice / Chat
 |
v
HERMES AGENT
 |
 +-- Agent OS primitives
 |    +-- Identity
 |    +-- Memory
 |    +-- Trust
 |    +-- Spec
 |    +-- Trace
 |    +-- Scheduler
 |    +-- Agent Registry
 |    +-- Task Manager
 |    +-- Event Bus
 |
 +-- Tools / terminal / files / GitHub / browser
 |
v
OMNIROUTE
 |
v
Models
```

The Agent OS must never be implemented as a second competing assistant above Hermes.

## 8. Server computer

The server is Hermes' execution environment.

```text
Phone / Laptop
      |
      | user control
      v
HERMES
      |
      +--> tools
      +--> terminal
      +--> filesystem
      +--> GitHub
      +--> databases
      +--> background jobs
      |
      v
/data/jarvis persistent runtime
```

The client is a control surface. Hermes is the agent operating the server computer.

## 9. Data ownership

| Domain | Owner |
|---|---|
| User conversation | Hermes |
| Task state | Hermes / Agent OS |
| Memory | Hermes / Agent OS |
| Tool permissions | Hermes policy layer |
| GitHub workflow | Hermes |
| Project workspace | Hermes execution runtime |
| Voice session | Hermes voice layer |
| Model selection request | Hermes |
| Model/provider routing | OmniRoute |
| Provider telemetry | OmniRoute |
| Model response | OmniRoute returns it to Hermes |

## 10. Failure semantics

If OmniRoute is unavailable:

```text
OmniRoute failure
      |
      v
Hermes detects inference failure
      |
      +--> retry
      +--> use configured fallback provider/path
      +--> pause task if inference is unavailable
      +--> preserve durable task state
      |
      v
Hermes reports status to user
```

OmniRoute failure must not destroy Hermes state, task state, memory, files, Git state or voice session state.

## 11. Architectural rule

The system must always preserve this sentence:

> **Hermes is the king. OmniRoute powers the king. Models power OmniRoute's routing targets. The user communicates with Hermes, never with OmniRoute directly.**

Optimize in this order:

```text
Hermes correctness
 -> verification
 -> recovery
 -> persistence
 -> security
 -> autonomy
 -> model routing optimization
 -> latency/cost optimization
```
