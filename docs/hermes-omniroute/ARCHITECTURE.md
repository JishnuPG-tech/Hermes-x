# Hermes ↔ OmniRoute Architecture

## 1. Roles

### Hermes Agent

Hermes is the sovereign runtime and user-facing autonomous agent.

It owns:

- user communication
- conversation context
- identity and personality
- voice sessions
- task lifecycle
- planning
- tool execution
- terminal and filesystem access
- Git and GitHub operations
- subagents
- memory
- verification
- retries and recovery
- background jobs
- permissions and approvals
- final responses

### OmniRoute

OmniRoute is a model-power service.

It owns:

- model/provider connectivity
- model routing
- provider selection
- provider fallback
- model availability
- routing telemetry
- latency/cost/quality signals

It does not own user-facing behavior or autonomous task control.

## 2. Layered architecture

```text
+------------------------------------------------------+
|                    USER INTERFACE                    |
|             Voice / Chat / Web / Mobile             |
+----------------------------+-------------------------+
                             |
                             v
+------------------------------------------------------+
|                    HERMES AGENT                     |
|                    SYSTEM KING                      |
|                                                      |
| Conversation | Memory | Planner | Tools | Agents    |
| Tasks | Terminal | GitHub | Browser | Verification  |
| Recovery | Scheduler | Policy | Voice | Responses   |
+----------------------------+-------------------------+
                             |
                    Model inference only
                             |
                             v
+------------------------------------------------------+
|                     OMNIROUTE                       |
|                 MODEL POWER LAYER                   |
|                                                      |
| Catalog | Routing | Provider fallback | Telemetry   |
+----------------------------+-------------------------+
                             |
                             v
+------------------------------------------------------+
|                 MODEL PROVIDERS                     |
| Claude | GPT | Qwen | Gemini | DeepSeek | ...     |
+------------------------------------------------------+
```

## 3. Control flow

The control plane belongs to Hermes.

```text
User request
   |
   v
Hermes interprets request
   |
   +--> decide whether model reasoning is needed
   |
   +--> create model request
   |
   v
OmniRoute
   |
   +--> select configured model/provider
   +--> stream request/response
   |
   v
Hermes receives model output
   |
   +--> decide next action
   +--> call tools
   +--> inspect results
   +--> ask model again if needed
   +--> verify result
   |
   v
Hermes responds to user
```

The model response is advice/input to Hermes. Hermes remains the decision-maker under its configured policies.

## 4. Multi-turn agent loop

```text
while task_not_complete:
    user_or_task_event
    hermes_updates_context()
    hermes_requests_model_power()
    omniroute_routes_to_model()
    model_returns_reasoning_or_tool_intent()
    hermes_interprets_model_output()
    hermes_executes_authorized_tools()
    hermes_observes_results()
    hermes_verifies()
    hermes_recovers_or_continues()
```

OmniRoute participates only in the model-power step.

## 5. Model abstraction

Hermes should depend on an abstract inference provider contract rather than OmniRoute-specific application logic.

```text
Hermes ModelProvider
        |
        +--> OmniRoute adapter
        +--> direct provider adapter if needed
        +--> future router adapter
```

OmniRoute is the preferred production model gateway in this project, but the Hermes application must not be architecturally coupled to OmniRoute's internal database, routing algorithms or implementation details.

## 6. Voice

Voice terminates at Hermes.

```text
Mic
 |
v
STT / Voice transport
 |
v
Hermes
 |
v
OmniRoute -> model
 |
v
Hermes
 |
v
TTS
 |
v
Speaker
```

The user never needs to connect to OmniRoute for a conversation.

## 7. Failure isolation

```text
              OmniRoute unavailable
                       |
                       v
                    Hermes
                       |
             +---------+---------+
             |                   |
          retry             fallback
             |                   |
             +---------+---------+
                       |
                still unavailable
                       |
                       v
              checkpoint task
                       |
                       v
             tell user / resume later
```

No OmniRoute failure may erase Hermes memory, task state, project files, Git state or user conversation.

## 8. Scaling

Hermes can remain one logical agent while OmniRoute scales independently.

```text
                    Hermes
                       |
              +--------+--------+
              |        |        |
          OmniRoute OmniRoute OmniRoute
              |        |        |
           models    models    models
```

If OmniRoute is horizontally scaled, each instance must use the documented persistent-state strategy and must not corrupt shared SQLite state. Stateless routing is preferred where possible.

## 9. Golden invariant

**Changing the model must not change who the user is talking to. The user is always talking to Hermes.**
