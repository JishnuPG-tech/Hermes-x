# Hermes ↔ OmniRoute Interface Contract

## Purpose

Define the narrow boundary between the Hermes application and the OmniRoute model gateway.

## Hermes sends

A model request containing, as required by the selected transport:

- model identifier or routing preference
- conversation messages
- system/developer instructions required by Hermes
- tool definitions when supported
- generation parameters
- streaming preference
- request/correlation ID
- optional routing hints

Hermes remains responsible for constructing the semantic request.

## OmniRoute returns

- model output
- tool-call intent when supported
- streaming deltas when requested
- usage metadata when available
- provider/model metadata when safe to expose
- normalized errors

## Example logical contract

```http
POST /v1/chat/completions
Authorization: Bearer <runtime-secret>
Content-Type: application/json

{
  "model": "auto-or-configured-model",
  "messages": [...],
  "tools": [...],
  "stream": true
}
```

The exact request schema follows the deployed compatible API. This example is architectural, not a promise that every OmniRoute release exposes exactly these fields.

## Ownership rule

Hermes decides:

- when to call the model
- why the model is being called
- which task the request belongs to
- how to interpret the result
- whether to call a tool
- whether to retry
- whether to ask for another model turn
- whether the task is complete

OmniRoute decides:

- where the model request is sent
- which configured provider satisfies the routing policy
- how provider fallback is performed
- routing-level telemetry

## Security

The OmniRoute endpoint must not be exposed as the public user conversation endpoint unless there is a deliberate future architecture decision. Prefer private network access or an authenticated service-to-service channel from Hermes.

User authorization belongs to Hermes. Service authentication protects the Hermes-to-OmniRoute connection.

## Compatibility

Hermes should use an adapter so that OmniRoute can be replaced without rewriting Hermes task, memory, voice or tool systems.
