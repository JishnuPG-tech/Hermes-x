# Hermes ↔ OmniRoute Test Plan

## Architecture invariants

- User messages enter Hermes, not OmniRoute.
- Hermes owns the conversation.
- Hermes owns task lifecycle.
- Hermes owns tool execution.
- Hermes owns final responses.
- OmniRoute only supplies model inference and routing services.
- Model changes do not reset Hermes state.

## Integration tests

### Basic request

1. Send a user request to Hermes.
2. Hermes creates a model request.
3. OmniRoute routes it.
4. Model output returns to Hermes.
5. Hermes produces the final response.

### Tool request

1. Hermes asks the model for reasoning/tool intent.
2. OmniRoute returns model output.
3. Hermes validates the tool call.
4. Hermes executes the tool.
5. Hermes sends the tool result through the next model turn if needed.
6. Hermes verifies completion.

### Model switch

Switch the configured model during an active conversation and verify that the same Hermes session continues with the same memory and task state.

### OmniRoute outage

Simulate an OmniRoute outage and verify that:

- Hermes remains alive
- task state remains durable
- conversation remains available
- retry/fallback policy runs
- no data is lost
- user receives a clear status

### OmniRoute recovery

Restore OmniRoute and verify Hermes resumes a checkpointed task without duplicating completed tool actions.

### Security boundary

Verify that OmniRoute cannot:

- execute Hermes tools directly
- mutate Hermes memory
- change user permissions
- publish to GitHub
- become the public conversation endpoint

## Acceptance criterion

A production test passes only when the user can interact entirely with Hermes while OmniRoute can be restarted, upgraded, replaced, or temporarily disabled without corrupting Hermes-owned state.
