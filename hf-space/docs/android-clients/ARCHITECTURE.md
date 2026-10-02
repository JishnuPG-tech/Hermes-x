# Hermes Android Architecture

## 1. System boundary

Hermes Android is **one APK** containing two presentation surfaces: Agent Chat and Voice Mode. Hermes is the backend authority.

```text
                    ANDROID DEVICE

             +-------------------------+
             |     HERMES ANDROID      |
             |         ONE APK         |
             |                         |
             |  +-------------------+  |
             |  | Agent Chat        |  |
             |  | workspace         |  |
             |  +-------------------+  |
             |  +-------------------+  |
             |  | Voice Mode        |  |
             |  | assistant UI      |  |
             |  +-------------------+  |
             +-----------+-------------+
                         |
                    HTTPS / WS
                         |
                         v
                 +---------------+
                 | Hermes Gateway |
                 +-------+-------+
                         |
                         v
                 +---------------+
                 | Hermes Agent  |
                 |     KING      |
                 +-------+-------+
                         |
              +----------+----------+
              |          |          |
            Memory     Agent OS    Tools
                                   |
                           terminal/files/web/
                           GitHub/device skills
                                   |
                                   v
                              OmniRoute
                                   |
                                   v
                                 Models
```

## 2. One APK, one identity

Chat and Voice are not separate clients and do not own separate conversations.

```text
Hermes Android
   |
   +-- SESSION_ID
   +-- VOICE_SESSION_ID
   +-- TASK_ID
   +-- authoritative Hermes state
```

A task started by voice is immediately visible in Chat. A task started in Chat can be queried and controlled through Voice.

## 3. Client responsibilities

The APK owns presentation and local interaction only:

- Render Hermes state.
- Capture text, voice and attachments.
- Maintain temporary UI cache.
- Provide realtime transport.
- Provide Android voice interaction integration.
- Provide AccessibilityService UI when explicitly enabled.
- Show permissions and action state.

The APK does **not** own:

- Hermes memory authority
- Task authority
- GitHub credentials
- Server shell credentials
- OmniRoute credentials
- Model provider secrets
- Autonomous server execution

## 4. Chat architecture

```text
Chat UI
  |
  +-- REST: sessions/tasks/projects/artifacts
  |
  +-- WebSocket: streams/task events/approvals
  |
  v
Hermes Gateway
  |
  v
Hermes Agent
```

## 5. Voice architecture

```text
Voice Mode
  |
  +-- Audio input
  +-- VAD
  +-- VoiceInteractionService where appropriate
  +-- WebSocket
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

Voice remains another Hermes interface. It does not create a second assistant brain.

## 6. Client API boundary

Client-facing APIs are owned by Hermes:

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

These are conceptual contracts and will be versioned during implementation.

The Android client must never call OmniRoute directly.

```text
Android -> Hermes -> OmniRoute -> Models
```

## 7. Authoritative state

```json
{
  "user_id": "...",
  "device_id": "...",
  "session_id": "...",
  "voice_session_id": "...",
  "active_task_id": "...",
  "connection_id": "..."
}
```

Exact schemas are implementation contracts, not UI-owned state.

## 8. Device-control UX boundary

Device actions are locally visible but Hermes-authorized.

```text
Hermes decides action
        |
        v
Android receives action event
        |
        v
Amber target overlay
        |
        v
Permission/guardrail check
        |
        v
AccessibilityService action
        |
        v
Observation returned to Hermes
```

The UI must show what Hermes is about to do. The action overlay and permission-card patterns are mandatory parts of the frontend specification. fileciteturn117file4L44-L63

## 9. Frontend module structure

```text
android/hermes/
├── app/
├── ui/
│   ├── theme/
│   ├── components/
│   ├── navigation/
│   ├── home/
│   ├── chat/
│   ├── voice/
│   ├── tasks/
│   ├── projects/
│   ├── skills/
│   ├── memory/
│   ├── permissions/
│   └── settings/
├── core/
│   ├── api/
│   ├── realtime/
│   ├── auth/
│   ├── storage/
│   └── models/
├── voice/
│   ├── VoiceInteractionService
│   ├── VAD
│   ├── STT
│   └── TTS
└── device/
    ├── AccessibilityService
    └── skills/
```

The complete UI specification is `docs/android-clients/ANDROID_UI_UX.md`.

## 10. Responsive UI

Compact phones use a single pane. Larger windows use adaptive list-detail layouts. Material 3 Adaptive is the preferred implementation foundation for this behavior. citeturn0search2turn0search5

## 11. Failure handling

If the Android device disconnects:

```text
Client disconnect
      |
      v
Hermes continues durable task
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

If OmniRoute is unavailable:

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

## 12. Data ownership

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

## 13. Security

The APK must never contain provider secrets, GitHub personal access tokens, server shell credentials, database credentials or storage encryption keys.

## 14. Build direction

Use Kotlin + Jetpack Compose for the frontend, with Kotlin platform services for AccessibilityService and voice integration. Use Material 3 and Material 3 Adaptive for the design system and large-window behavior. Android's current documentation recommends adaptive layouts and provides the required Compose libraries. citeturn0search10turn0search4
