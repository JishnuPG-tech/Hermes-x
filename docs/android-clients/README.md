# Android Client Architecture

## Purpose

The project has **two Android APKs** that are different user interfaces for the same Hermes Agent backend.

They are not two assistants.

```text
                         USER
                           |
              +------------+------------+
              |                         |
              v                         v
     +----------------+        +-------------------+
     | HERMES VOICE   |        | HERMES AGENT CHAT |
     | APK            |        | APK               |
     | Google-Assistant|       | Agentic workspace |
     | style           |       |                   |
     +--------+-------+        +---------+---------+
              |                          |
              +------------+-------------+
                           |
                     HTTPS / WebSocket
                           |
                           v
                 +-------------------+
                 |   HERMES AGENT    |
                 |       KING        |
                 +---------+---------+
                           |
                           v
                      OMNIROUTE
                    model power layer
                           |
                           v
                         MODELS
```

## APK 1: Hermes Voice

A voice-first assistant intended to feel like a phone assistant.

Primary behavior:

- Wake word / assistant invocation where Android permits it
- Continuous conversational voice sessions
- Streaming STT
- Streaming Hermes responses
- Streaming TTS
- Barge-in and interruption
- Very small visual UI
- Lock-screen / system-assistant integration where supported
- Quick spoken commands
- Spoken progress updates for long-running tasks
- Text fallback when voice is unavailable

The app should request the Android **assistant role** where appropriate. Android exposes `ROLE_ASSISTANT` and `VoiceInteractionService` specifically for system voice-interaction use cases.

The selected `VoiceInteractionService` can be kept running by Android for hotwording and voice interactions, so the always-running component must remain lightweight. Heavy UI and active sessions belong in the associated voice session service.

## APK 2: Hermes Agent Chat

A full agentic workspace for users who want to see and control work.

Primary behavior:

- Chat with Hermes
- Streaming text responses
- Markdown/code rendering
- Tool-call visibility
- Task progress
- Background job status
- Agent/subagent status
- Terminal output viewer
- File browser/editor
- Git/GitHub activity
- Pull request and CI status
- Approvals for sensitive actions
- Artifacts and generated files
- Memory and project context controls
- Voice input/output as an optional secondary interface

This APK is the **control center** for Hermes rather than a simple chatbot.

## Shared backend

Both APKs use the same Hermes identity, conversation/task state and backend.

They must not create separate AI brains.

```text
Voice APK ----+
              |
Chat APK -----+----> Hermes Gateway
                         |
                         v
                     Hermes Agent
                         |
              +----------+----------+
              |                     |
            Tools              OmniRoute
                                  |
                                Models
```

A conversation started in the voice APK can be continued in the chat APK. A task started in chat can be monitored through voice.

## Shared identity

The client authenticates as a user device/session. Hermes owns the authoritative identity and task state.

Recommended identifiers:

- `USER_ID`
- `DEVICE_ID`
- `CLIENT_ID`: `voice_android` or `agent_android`
- `SESSION_ID`
- `VOICE_SESSION_ID` for active voice sessions
- `TASK_ID` for durable tasks

## Shared API

Both APKs should use one versioned Hermes API.

Suggested channels:

```text
HTTPS
  - authentication
  - conversations
  - task state
  - files/artifacts
  - approvals
  - project metadata

WebSocket
  - realtime text streaming
  - voice session events
  - task events
  - tool progress
  - connection state
```

Do not expose OmniRoute directly to either APK.

The Android clients call Hermes. Hermes calls OmniRoute when model inference is required.

## Voice APK latency target

For a normal short request:

```text
wake/invoke
   -> VAD
   -> streaming STT
   -> Hermes
   -> OmniRoute
   -> model
   -> Hermes
   -> streaming TTS
   -> speaker
```

Target first playable spoken audio: <= 5 seconds.
Hard maximum target: <= 10 seconds under normal backend availability.

The client must not wait for the complete model response before playback begins.

## Android platform constraints

Android places restrictions on background microphone and foreground-service startup. The implementation must follow the current Android permission and foreground-service rules instead of assuming an ordinary background service can always capture audio.

For Android 14+ microphone foreground services, the app needs the appropriate microphone foreground-service declaration/permission and `RECORD_AUDIO`, and background-start restrictions apply. The selected `VoiceInteractionService` has special platform treatment for assistant scenarios.

Therefore:

1. Prefer `VoiceInteractionService` for the true assistant integration.
2. Keep the global voice-interaction component lightweight.
3. Move active interaction work to the voice session/service.
4. Use foreground-service mechanisms only where the platform policy permits them.
5. Never attempt to bypass Android privacy restrictions.

## Security

The APKs must never contain:

- OmniRoute provider secrets
- Model-provider API keys
- GitHub personal access tokens
- Server shell credentials
- Database credentials
- Storage encryption keys

The Android apps authenticate to Hermes using short-lived credentials/session tokens. Secrets stay server-side.

## Offline behavior

Voice APK:

- Detect offline state
- Stop pretending a request is executing
- Preserve unsent input locally when safe
- Reconnect automatically
- Resume the Hermes session when possible

Chat APK:

- Cache recent conversation/task metadata
- Queue safe UI operations only when explicitly designed for offline use
- Never fabricate task completion while disconnected

## Accessibility and UX

Voice APK should be usable with minimal visual interaction.

Chat APK should provide:

- large readable text
- accessible controls
- clear approval prompts
- visible running/stopped/error states
- explicit recording indicator
- explicit microphone permission state
- clear connection state

## Non-goals

These APKs are not separate model clients.

They do not:

- call OmniRoute directly
- choose provider secrets
- own Hermes memory
- execute arbitrary server commands locally
- become independent autonomous agents

## Canonical rule

> **Two APKs, one Hermes Agent. The Voice APK is the voice interface. The Agent Chat APK is the visual agent workspace. Hermes remains the king. OmniRoute only powers Hermes with models.**
