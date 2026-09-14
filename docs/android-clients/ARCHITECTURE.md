# Dual Android Client Architecture

## 1. System boundary

The Android applications are client surfaces. Hermes is the backend authority.

```text
                 ANDROID DEVICES

     +-------------------+   +----------------------+
     | Hermes Voice APK  |   | Hermes Agent Chat APK |
     |                   |   |                      |
     | Mic               |   | Chat                 |
     | VAD               |   | Tasks                |
     | Wake/invocation   |   | Projects             |
     | Speaker           |   | GitHub               |
     +---------+---------+   | Files/artifacts      |
               |             | Approvals             |
               +------+------+
                      |
                 TLS / WebSocket
                      |
                      v
             +---------------------+
             |    HERMES GATEWAY   |
             +----------+----------+
                        |
                        v
             +---------------------+
             |     HERMES AGENT     |
             |        KING          |
             +----------+----------+
                        |
       +----------------+----------------+
       |                |                |
       v                v                v
    Memory           Agent OS         Tools
                                      |
                    +-----------------+------------------+
                    | terminal / files / GitHub / web   |
                    +-----------------+------------------+
                                      |
                                      v
                                 OMNIROUTE
                                      |
                                      v
                                    MODELS
```

## 2. One identity, two clients

Both APKs identify the same user and can reference the same Hermes sessions and tasks.

Example:

```text
Voice APK
  -> start TASK-123

Chat APK
  -> open TASK-123
  -> inspect progress
  -> approve action

Voice APK
  -> receives approval/result event
```

## 3. Voice session architecture

```text
Android Voice APK
  |
  +-- AudioRecord / platform voice APIs
  +-- VAD
  +-- wake/invocation handling
  +-- WebSocket client
  |
  v
Hermes Voice Gateway
  |
  +-- STT streaming
  +-- turn detection
  +-- session state
  +-- TTS streaming
  |
  v
Hermes Agent
```

The client may perform local VAD/wake detection where appropriate to reduce latency, but Hermes remains authoritative for the resulting interaction.

## 4. Chat session architecture

```text
Agent Chat APK
  |
  +-- REST
  |    +-- sessions
  |    +-- tasks
  |    +-- projects
  |    +-- artifacts
  |
  +-- WebSocket
       +-- message stream
       +-- task events
       +-- approval events
       +-- execution events
       |
       v
   Hermes Gateway
```

## 5. API separation

### Client-facing API

Owned by Hermes.

Examples:

```text
POST /api/v1/sessions
POST /api/v1/messages
GET  /api/v1/tasks/{task_id}
POST /api/v1/tasks/{task_id}/cancel
POST /api/v1/tasks/{task_id}/approve
GET  /api/v1/projects
GET  /api/v1/projects/{project_id}
WS   /api/v1/realtime
WS   /api/v1/voice/{voice_session_id}
```

These are conceptual contracts. Exact paths are implementation details.

### Internal model API

Only Hermes uses OmniRoute.

```text
Hermes -> OmniRoute -> Model
Hermes <- OmniRoute <- Model
```

The Android clients must never call the internal model API.

## 6. Session state

Authoritative state lives on the server.

```json
{
  "user_id": "...",
  "session_id": "...",
  "client_id": "voice_android",
  "active_task_id": "...",
  "connection_id": "..."
}
```

The exact schema will be versioned during implementation.

## 7. Voice and chat handoff

A handoff must not copy the conversation manually between apps.

Both clients reference the same Hermes session or task.

```text
VOICE_SESSION_42
       |
       +-- messages
       +-- task references
       +-- task events
       +-- preferences
       |
       +--> Voice APK
       +--> Chat APK
```

## 8. Android assistant behavior

The Voice APK should support Android's `VoiceInteractionService` architecture for assistant use cases.

Android documentation states that the currently selected `VoiceInteractionService` may be kept running by the system to listen for hotwords and initiate voice interactions. This makes it the correct platform integration point for the assistant-style APK.

The app must remain lightweight in this always-running component.

## 9. Background microphone policy

Do not implement an unrestricted hidden microphone service.

For Android versions with foreground-service restrictions, the implementation must declare the correct microphone foreground-service type and permissions and respect background-start rules.

Where Android's selected assistant service provides an allowed path, use that platform mechanism.

## 10. Data ownership

| Data | Authority |
|---|---|
| Authentication/session | Hermes Gateway |
| Conversation | Hermes |
| Task state | Hermes / Agent OS |
| Voice state | Hermes Voice Gateway |
| UI cache | Android client |
| Model routing | OmniRoute |
| Model credentials | Server only |
| GitHub credentials | Server only |
| Project files | Hermes server runtime |
| Long-term memory | Hermes / Agent OS |

## 11. Failure handling

If Android disconnects:

```text
Client disconnect
      |
      v
Hermes continues task
      |
      v
State checkpointed
      |
      v
Client reconnects
      |
      v
Fetch authoritative state
```

If Hermes disconnects from OmniRoute:

```text
OmniRoute unavailable
       |
       v
Hermes retries/falls back/pauses
       |
       v
Task state preserved
       |
       v
Client receives truthful status
```

## 12. Build strategy

Recommended implementation structure:

```text
android/
├── hermes-voice/
├── hermes-agent-chat/
└── shared/
    ├── api/
    ├── auth/
    ├── models/
    ├── realtime/
    ├── storage/
    └── design/
```

The exact repository layout can change, but the two APKs should share protocol models and authentication logic while keeping voice and chat UI concerns separate.
