# Android Clients Implementation Plan

## Phase 0: Backend contract

Before building either APK:

- freeze Hermes client API versioning
- define authentication/session contract
- define realtime WebSocket events
- define voice stream protocol
- define task lifecycle events
- define approval events
- define reconnect/resume semantics
- ensure OmniRoute remains internal to Hermes

Deliverable: versioned Hermes Gateway API.

## Phase 1: Shared Android foundation

Build:

- Kotlin project structure
- shared networking module
- authentication
- secure token storage
- WebSocket client
- REST client
- serialization models
- connection manager
- error model
- session manager

Target: both APKs can authenticate and connect to Hermes.

## Phase 2: Agent Chat MVP

Implement:

1. Login/device registration
2. Conversation screen
3. Streaming responses
4. Session persistence
5. Task list
6. Task details
7. Reconnect
8. Notifications

Then add:

- project view
- GitHub view
- terminal/log view
- artifacts
- approvals

## Phase 3: Voice MVP

Implement:

1. Microphone permission
2. Audio capture
3. streaming STT connection
4. Hermes WebSocket voice session
5. streamed audio playback
6. basic turn detection
7. interruption
8. reconnect

Do not begin with a complex wake-word engine. First prove end-to-end realtime conversation.

## Phase 4: Assistant integration

Implement:

- `VoiceInteractionService`
- `VoiceInteractionSessionService`
- assistant role request flow
- invocation UI
- keyguard/lock-screen behavior where supported
- system invocation testing

Keep the global voice service lightweight.

## Phase 5: Wake word

After the assistant path is stable:

- evaluate local wake-word engine
- test false activations
- test battery usage
- test noisy environments
- test Bluetooth/headset microphones
- test screen-off behavior

Wake detection must be designed around Android platform rules.

## Phase 6: Voice quality

Add:

- streaming VAD
- better turn detection
- echo cancellation
- barge-in
- audio buffering
- playback interruption
- sentence-level TTS scheduling
- voice session recovery

## Phase 7: Agentic features

Chat APK:

- task timeline
- tool progress
- subagent status
- code viewer
- diff viewer
- GitHub operations
- CI/deployment status
- approvals
- artifacts

Voice APK:

- concise progress announcements
- approval prompts
- task completion announcements
- natural follow-up conversation

## Phase 8: Cross-client continuity

Test:

```text
Voice -> Chat
Chat -> Voice
Voice -> Chat -> Voice
```

The same Hermes task/session must remain authoritative throughout.

## Phase 9: Security hardening

- certificate/TLS validation strategy
- secure token storage
- token rotation
- device revocation
- replay protection
- session expiry
- microphone privacy indicators
- log redaction
- no server secrets in APK

## Phase 10: Release

Generate:

- debug APKs
- internal testing builds
- release APKs
- signed artifacts

The two applications should have distinct package IDs and names, for example:

```text
Voice:  <chosen.package>.voice
Chat:   <chosen.package>.agent
```

Do not finalize package IDs until the Android project is created.

## Definition of Done

Both APKs are production-ready when:

- they use the same Hermes backend
- tasks survive app closure
- voice sessions survive reconnects
- chat and voice share sessions/tasks
- OmniRoute is never directly exposed
- server-side secrets are never embedded
- Android assistant/foreground-service requirements are satisfied
- destructive actions remain under Hermes authorization
- latency and reliability targets are tested on real devices
