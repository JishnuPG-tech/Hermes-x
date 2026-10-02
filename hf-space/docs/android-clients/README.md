# Hermes Android App

## Purpose

Hermes Android is **one APK** with two integrated interfaces:

1. **Agent Chat** — the full visual agent workspace.
2. **Voice Mode** — the Google-Assistant-style voice interface.

They are not two assistants. Both are views of the same Hermes identity, session, memory and task state.

```text
                         USER
                           |
                 +---------+---------+
                 |   HERMES ANDROID |
                 |      ONE APK     |
                 +---------+---------+
                           |
             +-------------+-------------+
             |                           |
       AGENT CHAT                    VOICE MODE
       WORKSPACE                    ASSISTANT UI
             |                           |
             +-------------+-------------+
                           |
                     Hermes Gateway
                           |
                     HERMES AGENT
                         KING
                           |
              +------------+------------+
              |                         |
           Agent OS                 OmniRoute
                                      |
                                    Models
```

## UI/UX specification

The complete frontend specification is now in:

`docs/android-clients/ANDROID_UI_UX.md`

It defines the visual system, information architecture, screens, navigation, composer, voice states, task cards, plan timeline, permission UX, device-action overlay, memory, skills, settings, responsive layouts, accessibility, motion, testing and implementation order.

The visual direction is Claude-inspired but original: warm editorial typography, low chrome, rounded surfaces and generous whitespace. Hermes adds visible autonomous-agent state so device actions are never silent. fileciteturn117file0L4-L10 fileciteturn117file4L8-L16

## Shared backend

```text
Android APK
  ├── Chat UI
  └── Voice UI
        |
        v
  Hermes Gateway
        |
        v
    Hermes Agent
        |
   +----+----+
   |         |
 Agent OS  OmniRoute
              |
            Models
```

The Android client calls Hermes only. OmniRoute is internal model infrastructure and is never exposed directly to the APK. The existing architecture defines Hermes as the backend authority. fileciteturn119file0

## Session continuity

```text
Voice
  -> start TASK-123
  -> open Chat
  -> inspect TASK-123
  -> approve action
  -> return to Voice
  -> receive result
```

There is no manual conversation copying. The server owns authoritative session and task state.

## Main UX

### Home

Sparse greeting, Quick Actions and floating composer.

Quick Actions:

- Read screen
- Automate task
- Check memory
- New skill

The Hermes feature specification defines these actions for Home. fileciteturn117file5L12-L16

### Chat

- Streaming responses
- Markdown/code
- Attachments
- Collapsible tool activity
- Screen-context previews
- Task cards
- Plan Timeline
- Inline permission cards

### Voice

Full-screen states:

`connecting → listening → thinking → speaking → acting → ended`

Voice uses the same Hermes session as Chat.

### Agent workspace

Drawer sections:

- Chats
- Projects
- Tasks
- Skills
- Memory
- Pinned
- Recents

Settings sections:

- Account
- Appearance
- Voice
- Skills
- Device Permissions
- Memory
- Backend
- Notifications
- Privacy
- Sharing

## Android implementation

Recommended frontend stack:

- Kotlin
- Jetpack Compose
- Material 3
- Material 3 Adaptive for large windows
- Navigation Compose
- Kotlin platform services for AccessibilityService and voice integration

Android's current guidance recommends adaptive layouts for different window sizes, and Material 3 provides the current Compose design foundation. citeturn0search2turn0search10

## Security

The APK must never contain:

- OmniRoute provider secrets
- Model provider API keys
- GitHub tokens
- Server shell credentials
- Database credentials
- Storage encryption keys

Secrets remain server-side.

## Canonical rule

> **One APK, one Hermes Agent. Chat and Voice are interfaces to the same Hermes session. Hermes remains the king. OmniRoute only powers Hermes with models.**
