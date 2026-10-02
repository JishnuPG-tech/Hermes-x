# Hermes Android — Complete UI/UX Frontend Specification

## Product direction

Hermes Android is **one APK** with two integrated surfaces:

- **Agent Chat**: the primary visual workspace.
- **Voice Mode**: a full-screen assistant experience using the same Hermes session.

The visual language is Claude-inspired in its calm, editorial, low-chrome character, but this is an original Hermes design. Do not copy Claude branding, logo, proprietary artwork or pixel-perfect layouts.

Reference principles: near-black surfaces, restrained accent color, serif personality moments, sans-serif functional UI, rounded controls, generous whitespace and minimal borders. fileciteturn117file0L4-L10

Hermes adds **visible agency**. When Hermes reads, taps, types, swipes, opens apps or requests permission, the user must understand what is happening. fileciteturn117file4L8-L16

> One APK. One Hermes identity. Chat and Voice are interfaces to the same Hermes session. Hermes remains the authority. OmniRoute remains internal model infrastructure.

## UX principles

1. Conversation first.
2. Agency second.
3. Complexity on demand.
4. Calm authority, not flashy AI.
5. Never silently manipulate the device.
6. Never fabricate task progress or completion.
7. Server-authoritative state always wins over local UI guesses.

## Information architecture

```text
Hermes Android
├── Home / Chat
│   ├── New conversation
│   ├── Conversation
│   ├── Task activity
│   └── Composer
├── Voice Mode
│   ├── Connecting
│   ├── Listening
│   ├── Thinking
│   ├── Speaking
│   ├── Acting
│   └── Ended summary
├── Drawer
│   ├── Chats
│   ├── Projects
│   ├── Tasks
│   ├── Skills
│   ├── Memory
│   ├── Pinned
│   └── Recents
└── Settings
    ├── Account
    ├── Appearance
    ├── Voice
    ├── Skills
    ├── Device Permissions
    ├── Memory
    ├── Backend
    ├── Notifications
    ├── Privacy
    └── Sharing / Reset / Log out
```

The Hermes feature specification explicitly makes Skills, Memory, Device Permissions and Backend first-class user-visible areas. fileciteturn117file5L31-L68

## Navigation

### Phone

Use a clean single-pane conversation. Open the main IA with a left navigation drawer. Voice Mode is a full-screen state. Settings use hierarchical push navigation.

Android supports modal navigation drawers for complex multi-section apps. citeturn0search11

### Tablet / foldable / large window

Use adaptive list-detail layouts instead of stretching the phone UI:

```text
┌──────────────┬───────────────────────────────┐
│ Chats        │ Conversation                  │
│ Projects     │                               │
│ Tasks        │ messages / task activity      │
│ Skills       │                               │
│ Memory       │                               │
│ Settings     │                               │
└──────────────┴───────────────────────────────┘
```

Android recommends adapting layout and navigation to window size, and Material 3 Adaptive provides list-detail scaffolds. citeturn0search2turn0search5

## Home

```text
┌──────────────────────────────────┐
│ ☰                          ◌     │
│                                  │
│          Good evening            │
│          What can I do?          │
│                                  │
│ [ Read screen ] [ Automate ]     │
│ [ Check memory ] [ New skill ]   │
│                                  │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ Ask Hermes...                │ │
│ │                              │ │
│ │ +   📎                 🎙  ➤ │ │
│ └──────────────────────────────┘ │
│          Hermes · connected      │
└──────────────────────────────────┘
```

Keep the home screen sparse. Quick Actions are Read screen, Automate task, Check memory and New skill. fileciteturn117file5L12-L16

Use serif display typography for the greeting. Do not turn Home into a statistics dashboard.

## Composer

The composer is the primary interaction surface.

```text
┌──────────────────────────────────┐
│ Ask Hermes...                    │
│                                  │
│ +    📎                       🎙 │
└──────────────────────────────────┘
          Hermes · self-hosted
```

Behavior:

- Expands vertically with text.
- Remains responsive during streaming.
- Supports text, files, images and voice.
- Shows project/context chips only when relevant.
- Voice button launches full Voice Mode.

### Add-to-chat sheet

Use one consistent rounded-top sheet, about 85–90% height, with a centered drag handle.

```text
Add to chat

📷 Camera
🖼 Photos
📄 Files
🌐 Web lookup              ON
🧠 Memory                  ON
📁 Project                  >
```

The reference interaction uses Camera, Photos, Files, Web search, Memory and project selection in this sheet. fileciteturn117file1L12-L20

## Chat

### Header

```text
←  Expense Tracker             ⋮
```

Use a tiny Hermes state indicator when active rather than a large status banner.

### Messages

- Hermes messages are editorial and spacious.
- User messages stay visually quiet.
- Markdown and code are first-class.
- Attachments use compact rounded previews.
- Streaming begins immediately as content arrives.

### Thinking

Do not use a generic spinner. Use the Hermes mark with a subtle glow and a short serif state line:

```text
        ✦
   Hermes is thinking…
```

The reference design uses its brand mark and glow for thinking/connecting states. fileciteturn117file0L127-L133

## Agent activity

Tool activity stays collapsed by default:

```text
⚙ Working · 4 actions                       >
```

Expanded:

```text
✓ Read screen
✓ Opened Settings
✓ Found Accessibility
▶ Tapping Accessibility
```

Tool-call visibility, screen-context previews and inline permission cards are required parts of Hermes chat UX. fileciteturn117file5L18-L23

## Task card

Long-running work gets an inline card:

```text
┌─────────────────────────────────┐
│ Building expense tracker         │
│                                 │
│ ✓ Planning                       │
│ ✓ Repository                     │
│ ● Backend                        │
│ ○ Frontend                       │
│ ○ Tests                          │
│ ○ Deployment                     │
│                                 │
│ View plan          Activity >    │
└─────────────────────────────────┘
```

State language:

- Green = complete
- Amber = current
- Gray = pending
- Red = blocked/error

Never invent a percentage without meaningful progress data.

## Plan Timeline

Open from `View plan` in a bottom sheet:

```text
Expense Tracker Plan

✓ Understand requirements
│
✓ Create repository
│
● Implement backend
│
○ Implement frontend
│
○ Run tests
│
○ Deploy
```

The Hermes design system defines this checklist-style timeline for multi-step execution. fileciteturn117file4L50-L56

## Permission card

Do not overload the user with modal dialogs. Use an inline card:

```text
┌─────────────────────────────────┐
│ Hermes wants to                 │
│ send this message               │
│                                 │
│ Allow once   Allow always   Deny│
└─────────────────────────────────┘
```

Explain the action, target and relevant skill. Record the decision. The dedicated permission-card design uses Allow once, Allow always and Deny. fileciteturn117file4L58-L63

## Device-action overlay

Before a meaningful UI action, highlight the exact target:

```text
        [ Send ]
          ◯
       amber ring
```

Rules:

- About 400ms.
- Maximum two pulses.
- Never obscure the target.
- Stop if permission changes.

The amber action ring is a core Hermes trust mechanic. fileciteturn117file4L44-L48

## Screen context

When Hermes reads the current screen:

```text
┌──────────────────────────────┐
│ [ redacted preview ]         │
│ Screen context          >    │
└──────────────────────────────┘
```

Captured screens remain in memory unless explicitly saved. fileciteturn117file5L71-L80

# Voice Mode

Voice is a full-screen mode inside the same APK.

### Connecting

```text
┌──────────────────────────────────┐
│                           ⚙      │
│                                  │
│                ✦                 │
│                                  │
│       Hold tight, connecting…    │
│                                  │
│          +  Hermes        ✕      │
└──────────────────────────────────┘
```

### Listening

```text
┌──────────────────────────────────┐
│ ● Live                     ⚙     │
│                                  │
│                ✦                 │
│            Listening              │
│                                  │
│       "Build my website"        │
│                                  │
│          +  Hermes        ✕      │
└──────────────────────────────────┘
```

### Thinking

```text
                ✦

          Hermes is thinking
```

### Speaking

```text
                ✦

          Hermes is speaking

        ▂ ▄ ▆ ▄ ▂
```

### Acting

```text
                ✦

          Hermes is acting

             Step 2 / 5
       Opening the application…
```

The reference voice design uses full-bleed dark UI, a centered glowing mark, serif status copy and a visible live microphone indicator. fileciteturn117file0L114-L119

Voice controls: live mic state, settings, add content, optional transcript, interrupt and end. Voice-triggered device work ends with an action summary. fileciteturn117file5L25-L29

### Voice ended

```text
Voice chat ended

Hermes completed 3 actions.

👍   👎                     ×
```

## Drawer

```text
Hermes                         +

🔍 Search

Chats
Projects
Tasks
Skills
Memory

Pinned
  Expense tracker
  Server deployment

Recents
  Android APK
  Fix GitHub CI
  Jellyfin

● Profile                 + New chat
```

The drawer is content-focused, not a dashboard.

## Projects

```text
Projects

+ New project

Expense Tracker
Building...

Hermes Android
12 tasks

Server
Healthy
```

Project detail exposes Overview, Tasks, Files, GitHub, Deployments and Memory. Projects are context containers, not separate agents.

## Tasks

```text
Tasks

RUNNING
● Build expense tracker
  Step 3 / 7

WAITING
○ Deploy server
  Awaiting approval

COMPLETED
✓ Fix Android build
```

Task detail exposes objective, current state, plan, activity, agents/subtasks, artifacts, Git changes, approvals, recovery and cancel/pause/resume.

## Skills

Use dependency-aware capability rows:

```text
Skills

Screen Reader                 ON
Reads the current screen

Tap / Type / Swipe            ON
Required by device actions

App Launcher                  ON

Messaging                     OFF
Requires Screen Reader + Actuator

Alarms & Reminders            ON
Clipboard                     ON
Web Lookup                    ON
Automation Recorder           OFF
```

Dependencies dim rather than silently failing. fileciteturn117file4L65-L69 The initial skill set is defined in the feature specification. fileciteturn117file5L31-L44

## Memory

Memory must be transparent:

```text
Memory

Semantic
Jishnu prefers concise explanations
Hermes Android uses self-hosted backend

Recent activity
Today
  Learned project preference
  Updated server context

[ Search memory ]
[ Export ] [ Wipe ]
```

Semantic and episodic memory must be visually distinct. Viewing/editing, search, export and wipe are required. fileciteturn117file5L46-L52

## Device Permissions

```text
Device Permissions

Accessibility
Connected ✓

Microphone
Allowed ✓

Skills
Screen Reader       Allowed
Actuator            Allowed
Messaging           Ask every time
Alarms              Allowed

Permission history >
```

Accessibility gets a dedicated explanation/onboarding screen.

## Backend

The Android UI must **not expose OmniRoute configuration** to normal users.

```text
Backend

Hermes endpoint
https://your-hermes-space/...

Connection
● Connected

[ Test connection ]

Latency
420 ms
```

Endpoint, connection test and local latency/token statistics are user-facing backend settings. fileciteturn117file5L60-L64

## Settings IA

```text
Settings
├── Account
├── Profile
├── Appearance
│   ├── Color mode
│   ├── Font style
│   └── Haptics
├── Voice
│   ├── Voice settings
│   ├── Voice selection
│   ├── Language
│   ├── Pace
│   └── Live transcript
├── Skills
├── Device Permissions
├── Memory
├── Backend
├── Notifications
├── Privacy
├── Sharing
└── Reset pairing / Log out
```

Settings should be hierarchical full-page navigation, not a pile of dialogs. fileciteturn117file1L58-L88

# Visual system

## Colors

```text
Background             #141414
Elevated surface       #202020
Composer               #2E2E2E
Primary text           #F5F2ED
Secondary text         #8E8E93
Muted text             #5C5C5E
Hermes brand           #5B8CFF
System toggle          #4A7FE8
Agent action           #FFB454
Success                #3FC97D
Danger                 #E5484D
Divider                #2A2A2A
```

Blue is Hermes identity. Amber means an imminent device action. Red means danger. Green means confirmed success. This separation is deliberate. fileciteturn117file4L18-L30

## Typography

```text
Display XL      34sp  Serif
Display L       28sp  Serif
Title           20sp  Serif/Sans
Body            16sp  Sans
Secondary       14sp  Sans
Caption         13sp  Sans
```

Serif is for greetings, sheet titles and personality moments. Permissions, logs and safety text stay sans-serif. fileciteturn117file4L33-L40

## Shapes

```text
Pill             999dp
Card              20dp
Sheet top         20dp
Icon button       999dp
Small card        14dp
```

Spacing: `4 / 8 / 12 / 16 / 20 / 24 / 32dp`.

## Iconography

- 24dp grid
- 1.5–2dp rounded stroke
- Outline-first
- Minimal filled icons
- Original Hermes mark
- Amber target reticle
- Plan checklist
- Memory brain
- Automation gear
- Screen reader icon

The reference uses a restrained line-icon grammar and radiating mark. fileciteturn117file0L55-L63

## Hermes mark

Create an original thin-radiating Hermes mark. Do not copy Claude's asterisk exactly.

States:

```text
Idle       static
Thinking   soft glow
Listening  slow breathing glow
Speaking   gentle pulse
Acting     amber target nearby
Error      dim + red status
```

## Agent state system

```text
IDLE       no persistent noise
LISTENING  live/pulsing indicator
THINKING   glowing Hermes mark
ACTING     amber dot + Step N/M
SPEAKING   subtle waveform
WAITING    neutral explanation
APPROVAL   permission card
ERROR      red + retry
COMPLETE   green check
```

Every state must remain understandable without animation. fileciteturn117file4L71-L74

## Motion

```text
Sheet enter          280ms
Sheet exit           220ms
Screen transition    220ms
Action ring          400ms × max 2
Step completion      150ms
Error shake          100ms
Thinking glow        subtle continuous
```

Reduce Motion disables nonessential animation.

# Accessibility

- 44dp+ practical touch targets
- TalkBack labels
- Dynamic type
- High contrast
- Recording state visible and announced
- Permission state never communicated by color alone
- Reduce-motion mode
- Configurable haptics
- Keyboard support on large screens

The reference design emphasizes high contrast and non-color-only state communication. fileciteturn117file0L135-L139

# Offline / reconnect UX

Never fake completion.

```text
● Offline
Messages will send when connection returns.
```

For an existing server task:

```text
Connection lost

Hermes is still working.
Last confirmed state: Step 3 of 6

[ Reconnect ]
```

On reconnect, authoritative Hermes state replaces local guesses.

# Error UX

Bad:

```text
Something went wrong.
```

Good:

```text
I couldn't open Accessibility settings.

Android did not return the expected screen.

[ Try again ]   [ View details ]
```

For autonomous work:

```text
Task paused

Hermes could not complete Step 4:
"Deploy backend"

Reason: deployment credentials unavailable.

[ Add credentials ] [ Retry ] [ Stop task ]
```

# Voice / Chat continuity

Both surfaces reference the same session and task:

```text
Voice
  ↓
Start TASK-123
  ↓
Open Chat
  ↓
TASK-123 already visible
  ↓
Approve deployment
  ↓
Return to Voice
  ↓
Hermes reports result
```

No manual conversation copying.

# Frontend architecture

Use native Kotlin + Jetpack Compose for the UI and Kotlin platform components for device control.

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

Use Compose Material 3 and Material 3 Adaptive for large-window behavior. Current Android documentation lists Material 3 as stable and provides adaptive layout/navigation libraries. citeturn0search10turn0search4

Use Navigation Compose with predictive back support. citeturn0search6

## Component library

```text
HermesScaffold
HermesTopBar
HermesDrawer
HermesComposer
HermesIconButton
HermesPill
HermesSheet
HermesCard
HermesMessage
HermesCodeBlock
HermesAttachment
HermesQuickAction
HermesStateIndicator
HermesThinkingMark
HermesTaskCard
HermesPlanTimeline
HermesToolFooter
HermesScreenContext
HermesPermissionCard
HermesActionOverlay
HermesSettingRow
HermesSkillRow
HermesMemoryRow
HermesVoiceControls
HermesVoiceWaveform
HermesErrorCard
HermesConnectionBanner
```

No screen should invent its own variant when a shared component exists.

# Implementation order

### Phase 1: Visual foundation

1. Theme
2. Typography
3. Spacing/shapes
4. Icon set
5. Hermes mark
6. Scaffold
7. Drawer
8. Composer

### Phase 2: Core chat

9. Home
10. Chat
11. Streaming
12. Attachments
13. Add-to-chat sheet
14. Offline/error states

### Phase 3: Agent UX

15. Task card
16. Tool footer
17. Plan Timeline
18. Permission card
19. Screen context
20. Action overlay

### Phase 4: Voice

21. Voice Mode
22. Listening/thinking/speaking/acting states
23. Live transcript
24. Voice ended summary

### Phase 5: Agent workspace

25. Projects
26. Tasks
27. Skills
28. Memory
29. Device Permissions
30. Backend

### Phase 6: Polish

31. Settings
32. Tablet/foldable adaptation
33. Accessibility
34. Reduce Motion
35. Visual/performance polish

# Testing matrix

Every component must be tested for:

- Normal
- Pressed
- Focused
- Disabled
- Loading
- Error
- Offline
- Permission-required
- Long text
- Large font
- TalkBack
- Dark/light mode
- Landscape
- Folded/unfolded

Agent components additionally require:

- Idle
- Running
- Paused
- Waiting for approval
- Recovering
- Failed
- Completed
- Cancelled

# Performance targets

- Stream content as soon as it arrives.
- Composer remains responsive during streaming.
- UI never blocks on agent execution.
- Voice transitions immediately on session events.
- Action overlay appears within the device-action latency budget.
- Accessibility listeners are event-driven rather than polling.

The product specification targets first-token chat rendering below 1.5 seconds under suitable conditions and action-overlay appearance below 300ms from decision. fileciteturn117file5L71-L78

# Security UX

The APK must never contain OmniRoute provider secrets, model-provider API keys, GitHub personal access tokens, server shell credentials, database credentials or storage encryption keys.

The Android app authenticates to Hermes. Hermes owns privileged credentials. fileciteturn119file0

# Definition of done

The frontend is complete only when:

- One APK provides Chat + Voice.
- Chat has calm editorial polish.
- Voice feels like a premium assistant.
- Long-running tasks are visibly autonomous.
- Device actions are inspectable.
- Permissions are understandable.
- Memory is transparent.
- Skills are manageable.
- Errors are actionable.
- Offline state is truthful.
- Phone/tablet/foldable layouts adapt.
- TalkBack and dynamic type work.
- No secrets are embedded in the APK.
- OmniRoute is never exposed directly to the client.
- UI always reflects authoritative Hermes state.

The existing release criteria already require Claude-equivalent chat/voice polish and explicit action traceability. fileciteturn117file6L59-L65
