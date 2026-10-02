# Claude Android — Client State Machines & Execution Flows

---

## 1. MVI Architecture & State Reducer Pattern

The Android application implements a strict **Model-View-Intent (MVI)** reactive pattern using Kotlin Coroutines, `StateFlow`, and `SharedFlow`:

```
           ┌─────────────────────────────┐
           │      COMPOSE UI SCREEN      │
           └──────────────┬──────────────┘
                          │ Dispatches UiIntent (Events)
                          ▼
           ┌─────────────────────────────┐
           │      VIEWMODEL REDUCER      │ ◄── Pure State Transformations
           └──────────────┬──────────────┘
                          │ Emits Immutable State (StateFlow<UiState>)
                          ▼
           ┌─────────────────────────────┐
           │      STATEFLOW COLLECTOR    │
           │  (Lifecycle-Aware Compose)  │
           └─────────────────────────────┘
```

---

## 2. Chat & Streaming State Machine

```
       ┌────────────────────────┐
       │          IDLE          │ ◄──────────────────────────────┐
       └───────────┬────────────┘                                │
                   │ User sends message (ChatCompletionRequest)  │
                   ▼                                             │
       ┌────────────────────────┐                                │
       │        SENDING         │                                │
       └───────────┬────────────┘                                │
                   │ HTTP 200 / SSE Connection Opened            │
                   ▼                                             │
       ┌────────────────────────┐                                │
 ┌───► │       STREAMING        │                                │
 │     └───────────┬────────────┘                                │
 │                 │                                             │
 │  ┌──────────────┼──────────────┬──────────────┐               │
 │  ▼              ▼              ▼              ▼               │
 │ Thinking     Text Delta     Tool Call     Citations           │
 │ (Adaptive)  (30fps Smooth)  (Execution)   (Sources)           │
 │  │              │              │              │               │
 │  └──────────────┴──────────────┴──────────────┘               │
 │                 │                                             │
 │                 ├───────────────────────┐                     │
 │                 ▼                       ▼                     │
 │      ┌─────────────────────┐ ┌─────────────────────┐          │
 │      │ NEEDS_TOOL_APPROVAL │ │  ERROR / RETRY      │          │
 │      └──────────┬──────────┘ └──────────┬──────────┘          │
 │                 │ User Approves         │ Retry Clicked       │
 │                 └───────────────────────┴─────────────────────┤
 │                                                               │
 └───────────────── (Message Stop Event / Completion) ───────────┘
```

### State Definitions
1. **`IDLE`**: Ready for input. Displays greeting or conversation history.
2. **`SENDING`**: Local optimistic user message appended; network request in flight.
3. **`STREAMING`**: SSE stream active (`/completion2`).
   * **`ThinkingBlock`**: Extended thinking stream rendered in a collapsible disclosure container.
   * **`TextBlock`**: Stream tokens passed through `StreamSmoothingEngine` at 30 FPS.
   * **`ToolUseBlock`**: Displays live tool invocation banner (e.g., `"Running Web Search..."`).
4. **`NEEDS_TOOL_APPROVAL`**: Stream paused waiting for user approval intent.
5. **`ERROR / RETRY`**: Network failure, model rate-limit, or token limit reached.

---

## 3. Realtime Voice State Machine ("Bell Mode")

```
   ┌────────────────────────────────────────────────────────┐
   │                  VOICE_CONNECTING                      │
   │      (WebSocket Handshake & ClockSyncPing/Pong)        │
   └───────────────────────────┬────────────────────────────┘
                               │ SessionServerInitialized
                               ▼
   ┌────────────────────────────────────────────────────────┐
   │                   LISTENING (IDLE)                     │
   │       Microphone stream open · VAD Active on Edge      │
   └─────────────┬────────────────────────────▲─────────────┘
                 │ User speech start          │ User speech stops
                 ▼                            │ (UserInputEnd Event)
   ┌───────────────────────────┐              │
   │    USER_SPEAKING (STT)    │              │
   │ Live Transcript Streamed  │              │
   └─────────────┬─────────────┘              │
                 │ Server VAD trigger         │
                 ▼                            │
   ┌───────────────────────────┐              │
   │      THINKING / LLM       │              │
   │   Model generates tokens  │              │
   └─────────────┬─────────────┘              │
                 │ PlaybackStart Event        │
                 ▼                            │
   ┌───────────────────────────┐              │
   │    ASSISTANT_SPEAKING     │              │
   │  Opus Streaming Playback  │              │
   │  Word-level PTS sync      │ ─────────────┘
   └─────────────┬─────────────┘ (PlaybackComplete Event)
                 │
                 │ User interrupts (Barge-in detected)
                 ▼
   ┌───────────────────────────┐
   │     BARGE_IN_HANDLING     │
   │  Mute Audio · Flush Queue │ ──► Return to USER_SPEAKING
   └───────────────────────────┘
```

---

## 4. Claude Code Remote (CCR) Session Lifecycle Flow

```
1. DISCOVERY & INITIALIZATION
   ├─ Fetch active devices: GET /v1/environment_providers/.../environments
   ├─ Choose runtime target: Local Computer (Remote Control) vs Anthropic Cloud Container
   ├─ Select base git branch: GET /api/github/.../branches
   └─ Select Permission Mode: Default | Plan | Auto | AcceptEdits | BypassPermissions

2. SESSION EXECUTION
   ├─ Connect SSE stream: GET /v1/code/sessions/{sessionId}/events/stream
   ├─ Dispatch prompt: POST /v1/code/sessions/{sessionId}/events
   ├─ Receive chunked tool executions: (Bash command, File write, Git commit)
   └─ Render interactive ANSI terminal cards with exit codes & duration

3. PERMISSION ESCALATION
   ├─ High-risk tool call detected by backend policy
   ├─ Push notification sent + interactive notification buttons rendered
   ├─ User selects: Approve Once / Deny / Deny with comment
   └─ Session resumes without context reset

4. CLOUD MIGRATION ("Move to Cloud")
   ├─ If local laptop disconnects/sleeps:
   ├─ User taps "Continue in Cloud"
   ├─ POST /v1/code/sessions/{sessionId}/move-to-cloud
   ├─ Container provisioned from last pushed git commit
   └─ Mobile client reconnects to new cloud runner seamlessly
```

---

## 5. Stream Smoothing Algorithm (`StreamSmoothingConfig`)

To prevent jerky UI layout recalculations during bursty token deliveries, Claude Android implements a hardware-timed token buffer:

```
Incoming SSE Token Packets
  │ (Bursty: 0 to 120 tokens/sec)
  ▼
┌─────────────────────────────────┐
│     SMOOTHING RING BUFFER       │
└────────────────┬────────────────┘
                 │
                 │ 33ms Tick Timer (30 FPS Choreographer)
                 ▼
┌─────────────────────────────────┐
│   CHARACTER RENDER EMITTER      │
│   • min_markdown_group: 800     │
│   • fade_in_duration: 200ms     │
└────────────────┬────────────────┘
                 │
                 ▼
       Jetpack Compose Text
```
