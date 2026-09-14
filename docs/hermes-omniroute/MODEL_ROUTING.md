# Model Routing

## Principle

Hermes asks for model power. OmniRoute decides how to satisfy that request within the routing policy.

```text
Hermes task
   |
   | model need
   v
OmniRoute
   |
   +--> choose model/provider
   +--> optimize availability/latency/cost/quality
   +--> stream result
   v
Hermes
```

## Hermes decides the semantic need

Examples:

- simple conversation
- coding
- debugging
- architecture
- long-context analysis
- vision
- tool-heavy task
- autonomous subagent task

Hermes can request an appropriate model or routing profile.

## OmniRoute decides the transport

Within its configured policy, OmniRoute can:

- select providers
- fail over providers
- apply provider constraints
- track routing telemetry
- normalize model access
- optimize routing signals

## Model switching

Changing from one model to another must not reset:

- Hermes identity
- conversation
- memory
- task state
- tool permissions
- project workspace
- voice session

Only the inference engine changes.

## Observability

Record at minimum:

- Hermes request ID
- task ID
- requested model/profile
- selected provider/model when available
- first-token latency
- total model latency
- input/output usage when available
- retry/fallback count
- final outcome

Do not store secrets in telemetry.
