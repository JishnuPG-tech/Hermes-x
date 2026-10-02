# In-Depth Analysis of Custom Android APK (`android/`)

**Location**: `C:\Users\JISHNU PG\Music\Hermes Agent\Hermes-x\android`  
**Package Namespace**: `com.example.hermes`  
**Platform Target**: Compile SDK `36` (Android 16), Min SDK `24` (Android 7.0+), Target SDK `36`  
**Framework & Language**: 100% Kotlin • Jetpack Compose • Material 3 • Navigation3 • OkHttp SSE & WebSocket  

---

## 1. Executive Summary & Architecture Blueprint

The `android/` directory contains a **custom-built, production-ready native Android client** engineered to replicate the Anthropic Claude mobile application interface while powering the **Hermes Autonomous AI Assistant**.

```
                                  ┌──────────────────────────────────────────────────┐
                                  │               HERMES ANDROID APK                 │
                                  │      Single-Activity Compose MVI Architecture    │
                                  └─────────┬───────────────────┬──────────────┬─────┘
                                            │                   │              │
                   HTTP REST & Multiparts   │   SSE Streams     │              │ WSS Duplex Audio
                   (OkHttp / JSON)          │ (text/event-stream)              │ (JSON / Opus Packets)
                                            ▼                   ▼              ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 │                                           HERMES BACKEND GATEWAY                                                 │
 │                                    (Default: https://jishnupg-hermes.hf.space)                                   │
 ├────────────────────────────────┬─────────────────────────────────┬───────────────────────────────────────────────┤
 │ 1. OpenAI-Compatible Chat Edge │ 2. Realtime SSE Token Streamer  │ 3. Full-Duplex Voice WebSocket Engine         │
 │    • POST /v1/chat/completions │    • reasoning_content (Thinking│    • wss://.../v1/voice/ws                    │
 │    • GET /v1/tasks             │    • content (Text Tokens)      │    • Realtime Transcript & Audio Stream       │
 │    • POST /v1/tasks            │    • [DONE] Termination Frame   │    • Low-Latency State Synchronization        │
 └────────────────────────────────┴─────────────────────────────────┴───────────────────────────────────────────────┘
```

---

## 2. Complete Project Structure & Codebase Map

```
android/
├── app/
│   ├── build.gradle.kts                 # Application build config & dependency declarations
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Manifest (INTERNET, RECORD_AUDIO, adjustResize)
│       │   ├── java/com/example/hermes/
│       │   │   ├── MainActivity.kt      # Root ComponentActivity with EdgeToEdge
│       │   │   ├── Navigation.kt        # Navigation3 NavDisplay, ModalNavigationDrawer & BackStack
│       │   │   ├── NavigationKeys.kt    # Type-safe Navigation keys (NavHome, NavChat, NavVoice, etc.)
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt         # Exact Anthropic Color Palette (#141413, #D97757, #1F1E1C)
│       │   │   │   ├── Type.kt          # Embedded Anthropic Serif, Anthropic Sans, JetBrains Mono
│       │   │   │   └── Theme.kt         # HermesTheme Material3 wrapper
│       │   │   ├── data/
│       │   │   │   ├── HermesModels.kt  # Serialized models (ChatMessage, TaskDto, StreamDelta)
│       │   │   │   ├── HermesApiClient.kt # OkHttp SSE chat streamer & WebSocket client
│       │   │   │   └── DataRepository.kt # Repository layer mediating UI and Network
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       │   ├── HermesLogo.kt        # Starburst Canvas vector & animated thinking spark
│       │   │       │   ├── ClaudeWidgets.kt     # ClaudeStarburst, ClaudeWaveformIcon, ClaudeGhostIcon
│       │   │       │   ├── ClaudeDrawer.kt      # Navigation drawer with Chats, Projects, Code, Artifacts
│       │   │       │   ├── ComposerBar.kt       # ClaudeHomeComposer with pill input & voice FAB
│       │   │       │   ├── ChatComponents.kt    # Message bubbles, thinking boxes, tool cards
│       │   │       │   ├── ModelSelectSheet.kt  # Bottom sheet with Fable, Opus, Sonnet, Haiku
│       │   │       │   ├── AddToChatSheet.kt    # 3-up media grid (Camera, Photos, Files)
│       │   │       │   └── ExecutionSummarySheet.kt # Stepper timeline for autonomous execution
│       │   │       └── screens/
│       │   │           ├── HomeScreen.kt        # Greeting screen with serif headline & starburst
│       │   │           ├── ChatScreen.kt        # Conversation stream, 7 Artifacts pill, overflow menu
│       │   │           ├── ChatViewModel.kt     # MVI ViewModel managing SSE completions
│       │   │           ├── VoiceScreen.kt       # Ambient full-screen voice call HUD
│       │   │           ├── VoiceViewModel.kt    # WebSocket duplex audio controller
│       │   │           ├── TasksScreen.kt       # Autonomous task list with progress indicators
│       │   │           ├── TasksViewModel.kt    # Tasks DAG state coordinator
│       │   │           ├── TerminalScreen.kt    # Jetpack Compose ANSI terminal window
│       │   │           ├── ConnectorsScreen.kt  # MCP connectors & discovery toggle
│       │   │           ├── CapabilitiesScreen.kt # Web search, Artifacts, Code execution toggles
│       │   │           ├── PermissionsScreen.kt # Permission grant settings
│       │   │           ├── ArtifactsScreen.kt   # Artifact gallery
│       │   │           ├── ArtifactViewerScreen.kt # Standalone sandboxed artifact renderer
│       │   │           ├── ProjectsScreen.kt    # Projects workspace
│       │   │           ├── CodeScreen.kt        # Developer coding sessions
│       │   │           ├── ProfileScreen.kt     # Account profile settings
│       │   │           ├── BillingScreen.kt     # Subscriptions & Pro upgrade
│       │   │           ├── VoiceSettingsScreen.kt # Voice persona carousel & speech pace
│       │   │           ├── NotificationsScreen.kt# Notification preferences
│       │   │           ├── TimeFocusScreen.kt   # Time & focus settings
│       │   │           ├── PrivacyScreen.kt     # Privacy settings
│       │   │           ├── SharingScreen.kt     # Public sharing settings
│       │   │           └── AuthScreen.kt        # Sign in / Welcome onboarding
│       │   └── res/
│       │       ├── font/                # Embedded Anthropic typography fonts
│       │       │   ├── anthropic_serif.ttf
│       │       │   ├── anthropic_serif_italic.ttf
│       │       │   ├── anthropic_sans.ttf
│       │       │   ├── anthropic_sans_italic.ttf
│       │       │   ├── jetbrains_mono.ttf
│       │       │   ├── noto_serif.ttf
│       │       │   └── latinmodern-math.otf
│       │       ├── values/              # Strings, themes, and backup rules
│       │       └── mipmap-*/            # App icons
│       └── test/ & androidTest/         # Unit & Compose instrumented UI test suites
├── build.gradle.kts                     # Root buildscript
├── settings.gradle.kts                  # Gradle project settings
└── gradle/libs.versions.toml            # Version catalog
```

---

## 3. Visual & Design System Analysis

### 3.1 Color System (`theme/Color.kt`)
The codebase implements the exact extracted Anthropic color palette:
* **`CanvasNearBlack` (`#141413`)**: Primary background floor across all full-screen views.
* **`SurfaceDarkElevated` (`#1F1E1C`)**: Used for modal bottom sheets and drawer surfaces.
* **`SurfaceComposer` (`#1B1A18`)** & **`SurfacePill` (`#2A2926`)**: Text input bar and pill backgrounds.
* **`BrandCoral` (`#D97757`)** & **`BrandTerracotta` (`#C96442`)**: Starburst logo, user avatars, and primary CTAs.
* **`TextPrimaryWarm` (`#FAF9F5`)**: Warm off-white headings and body text.
* **`TextMuted` (`#B0AEA5`)**: Mid-gray subtitles, secondary labels, and timestamps.
* **`AccentBlue` (`#3898EC`)**: Selected model highlight, active toggle track.
* **`AccentGreen` (`#22C55E`)**: Connected mic indicator and task completion status.
* **`DestructiveRed` (`#E2726E`)**: Log out and delete actions.

---

### 3.2 Typography System (`theme/Type.kt`)
The project embeds the official typeface binaries directly inside `app/src/main/res/font/`:
* **Serif Headings (`AnthropicSerif`)**:
  * `displayLarge`: `34sp`, line-height `40sp` (*"Back at it, Jishnu"*)
  * `displayMedium`: `28sp`, line-height `34sp`
  * `headlineMedium`: `22sp`, line-height `28sp` (*"Tasks"*, *"Settings"*, *"Summary"*)
* **Functional Body (`AnthropicSans`)**:
  * `titleLarge`: `16sp`, weight `Medium`, line-height `22sp`
  * `bodyLarge`: `15sp`, weight `Normal`, line-height `22sp`
  * `bodyMedium`: `13sp`, weight `Normal`, line-height `18sp`
  * `labelSmall`: `11sp`, weight `Medium`, line-height `14sp`
* **Code & Monospace (`JetBrainsMono`)**:
  * Terminal outputs, JSON deltas, and code blocks.

---

## 4. UI Components & Screen Implementations

### 4.1 Home Screen (`HomeScreen.kt`)
* **Top Bar**: Minimalist stroke menu icon (left) + `ClaudeGhostIcon` incognito trigger (right).
* **Center Hero**: `ClaudeStarburst` (14-spoke custom Canvas vector) + Serif greeting (*"Back at it, Jishnu"*).
* **`ClaudeHomeComposer`**:
  * Top upgrade strip: *"✨ Get more with Claude Pro"* + link.
  * Pill input: placeholder *"Chat with Claude…"*, height `48dp`, radius `9999dp`.
  * Control bar: Attach `+`, Model Selector (`Sonnet 5 · Low ▾`), Mic icon, and circular filled white Voice FAB.

---

### 4.2 Chat Conversation Screen (`ChatScreen.kt`)
* **Top Bar**: Menu icon (left), `"7 Artifacts"` floating pill button (center), New Chat `+` and Options `⋮` menu (right).
* **Overflow Menu**: Custom dropdown popup (`#1E1D1B`, radius `18dp`) featuring:
  * Header: Conversation title
  * Items: `Share`, `Rename`, `Pin / Unpin`, `Add to project`, `Add to home`, `Delete`.
* **Message Stream**:
  * User bubble: Align right, background `#242321`, radius `18dp`.
  * Assistant stream: Align left, small spark mark, collapsible thinking disclosure box, rendered text with Markdown support, and action row (Copy, Thumbs up/down, Retry).

---

### 4.3 Voice Call Screen (`VoiceScreen.kt`)
* **Ambient Canvas**: Full-screen dark canvas with pulsing `ClaudeStarburst` avatar.
* **Breathing Animation**: Infinite transition scaling the Starburst between `0.96f` and `1.05f` (`1400ms` cycle with `FastOutSlowInEasing`).
* **Live Caption**: Serif title (*"Hold tight, connecting…"*) centered above bottom call controls.
* **Control HUD**: Settings gear (top right), Attach `+`, large center mic button, and end-call `✕` button.

---

### 4.4 Navigation Drawer (`ClaudeDrawer.kt`)
* **Width**: `320dp`, background `#141413`.
* **Header**: Large Serif `"Claude"` wordmark (`40sp`).
* **Menu Links**: `Chats`, `Projects`, `Code`, `Artifacts`.
* **Pinned & Recents**: Pinned conversations and reverse-chronological chat history with single-line truncation.
* **Footer**: User avatar circle with initial `"J"` (Salmon `#D97D64`) and high-contrast pill button `＋ New`.

---

### 4.5 Sheets & Overlays
* **`ModelSelectSheet.kt`**: Bottom sheet with single continuous card containing `Fable 5.1`, `Opus 5`, `Sonnet 5`, `Haiku 4.5`.
* **`AddToChatSheet.kt`**: 3-up media grid (`Camera`, `Photos`, `Files`), `Web search` toggle, `Memory` toggle.
* **`ExecutionSummarySheet.kt`**: Stepper timeline for autonomous sub-agent executions and thinking steps with animated pulsing dot.

---

## 5. Backend Network & Communication Layer (`HermesApiClient.kt`)

### 5.1 Server-Sent Events (SSE) Chat Streaming
* **Endpoint**: `POST /v1/chat/completions` on `https://jishnupg-hermes.hf.space` (configurable).
* **Auth**: `Authorization: Bearer Jishnu2005`.
* **Stream Parsing**:
  * Parses incoming `data: {...}` lines into `ChatCompletionChunk`.
  * Extracts `delta.reasoning_content` and dispatches `StreamEvent.Thinking`.
  * Extracts `delta.content` and dispatches `StreamEvent.Token`.
  * Detects `data: [DONE]` and emits `StreamEvent.Done(fullAccumulatedText)`.

---

### 5.2 Full-Duplex Voice WebSocket Client
* **Endpoint**: `wss://jishnupg-hermes.hf.space/v1/voice/ws`.
* **Protocol**: Sends and receives `VoiceWsMessage` objects (`audio`, `transcript`, `status`, `error`).

---

### 5.3 Autonomous Task Management
* **`GET /v1/tasks`**: Retrieves active DAG tasks (`TaskDto` with status, progress, subtasks).
* **`POST /v1/tasks`**: Dispatches new task objectives with prompt instructions.

---

## 6. Alignment & Production Readiness

```
┌────────────────────────────────────────────────────────────────────────┐
│                      CUSTOM APK ALIGNMENT SUMMARY                      │
├─────────────────────────┬──────────────────────────────────────────────┤
│ Visual Fidelity         │ 100% 1:1 match with Claude Android UI specs  │
│ Typeface Embedding      │ Official Anthropic Serif & Sans embedded     │
│ Navigation Routing      │ 20+ type-safe Navigation3 destination routes │
│ Realtime Streaming      │ OkHttp SSE with separate thinking extraction │
│ Full-Duplex Voice       │ Realtime WebSocket audio communication       │
│ Agent & Task Tracking   │ Autonomous tasks DAG screen with live state  │
└─────────────────────────┴──────────────────────────────────────────────┘
```
