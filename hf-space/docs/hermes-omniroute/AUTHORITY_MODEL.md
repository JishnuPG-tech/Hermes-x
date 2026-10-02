# Authority Model

## Sovereignty

Hermes is the sole user-facing autonomous agent in this architecture.

```text
OWNER POLICY
     |
     v
HERMES AUTHORITY
     |
     +--> task policy
     +--> tool permissions
     +--> subagent permissions
     +--> approval gates
     |
     v
EXECUTION ENVIRONMENT
```

OmniRoute sits below Hermes as an inference dependency.

```text
HERMES
  |
  | needs reasoning
  v
OMNIROUTE
  |
  v
MODEL
```

## Prohibited inversion

The following architecture is explicitly forbidden:

```text
User
  |
OmniRoute
  |
Model
  |
Hermes tools
```

That would make the model gateway a competing agent authority.

The required architecture is:

```text
User
  |
Hermes
  |
OmniRoute
  |
Model
  |
OmniRoute
  |
Hermes
  |
User
```

## Subagents

Subagents are created and supervised by Hermes. OmniRoute may provide the models used by those subagents, but does not own their lifecycle.

## Tool execution

A model response can suggest a tool call, but Hermes validates permissions and executes the tool. OmniRoute never directly executes Hermes tools.

## Final answer

Only Hermes produces the final user-facing response. Model output is an internal inference result until Hermes interprets it.
