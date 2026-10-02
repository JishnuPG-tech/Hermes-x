# Hermes Android Client Architecture Document

This document outlines the software engineering architecture of the native **Hermes Android Client** implemented in Kotlin and Jetpack Compose.

---

## 1. High-Level Architectural Flow

```
┌────────────────────────────────────────────────────────────┐
│                    COMPOSE UI LAYER                        │
│   (ChatScreen, HomeScreen, CodeScreen, TasksScreen, etc.)   │
└─────────────────────────────┬──────────────────────────────┘
                              │
                    Observes StateFlows
                    Dispatches UiEvents
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                    VIEWMODEL / MVI LAYER                   │
│      (ChatViewModel, TasksViewModel, VoiceViewModel)       │
└─────────────────────────────┬──────────────────────────────┘
                              │
                     Repository Calls
                              │
                              ▼
┌────────────────────────────────────────────────────────────┐
│                    DATA / REPOSITORY LAYER                 │
│      (HermesDataRepository, StreamSmoothingEngine)         │
└──────────────┬──────────────────────────────┬──────────────┘
               │                              │
     Remote API Operations          Local Cache Read/Write
               │                              │
               ▼                              ▼
┌────────────────────────────┐  ┌────────────────────────────┐
│      NETWORK CLIENTS       │  │     LOCAL PERSISTENCE      │
│  (OkHttp, SSE, WebSocket)  │  │   (Room DB, DataStore,     │
│   - HermesApiClient        │  │    EncryptedSharedPrefs)   │
└────────────────────────────┘  └────────────────────────────┘
               │
          HTTPS / WSS
               │
               ▼
┌────────────────────────────────────────────────────────────┐
│                HERMES AGENT SPACE SERVER                   │
│             (https://jishnupg-hermes.hf.space)             │
└────────────────────────────────────────────────────────────┘
```

---

## 2. Core Architectural Authority & Dependency Rule

### 2.1 The Dependency Rule
> **Never read or write Room or DataStore directly from a `@Composable`.**
>
> `UI → ViewModel → Repository → Local/Remote Data Source`

### 2.2 Authority Boundaries
- **Hermes Server**: Sole authority & source of truth for auth, sessions, messages, tasks, agents, approvals, projects, artifacts, voice, terminal, models, capabilities, connectors, and runtime state.
- **Android Client**: Presentation surface, local cache manager, offline queue coordinator, and real-time client.
- **Room**: Cached structured server state (`SessionEntity`, `MessageEntity`, `TaskEntity`, `ProjectEntity`, `ArtifactEntity`). The server remains the source of truth; Room is the offline/cache representation, not an independent authority.
- **DataStore**: Local preferences only (Theme mode, Selected model, Haptic preference, Notification preference, Base URL configuration, small UI/user preferences). Expose DataStore strictly through the data layer/ViewModel.
- **OmniRoute**: Model and provider routing infrastructure only.

---

## 3. Layer Definitions & Separation of Concerns

### 3.1 UI Layer (Jetpack Compose)
- **Design Philosophy:** Pure UI rendering conforming 1:1 to the Claude reference design tokens (`CanvasNearBlack #141413`, `BrandCoral #D97757`, typography, 8-frame sequential coral starburst, discreet stepper).
- **Rule:** Zero direct network calls. Zero direct Room or DataStore access. The UI only observes immutable states emitted by ViewModels via `collectAsStateWithLifecycle()` and sends user intents upwards.

### 2.2 ViewModel Layer
- Exposes typed `StateFlow<UiState>` for each screen.
- Collects coroutine flows from `HermesDataRepository`.
- Handles user intent dispatching (`sendMessage`, `loadSession`, `createProject`, `toggleMute`, `approveAction`).
- Survives configuration changes and process recreations.

### 2.3 Repository Layer (`HermesDataRepository`)
- Single source of truth for dynamic runtime state.
- Mediates between the remote API (`HermesApiClient`) and local persistence (Room / DataStore).
- Exposes hot `StateFlow` streams for:
  - `messages`: Active conversation messages.
  - `sessions`: Saved user chat sessions.
  - `projects`: Live Hermes projects.
  - `tasks`: Active autonomous tasks and DAG state.
  - `allArtifacts`: Dynamically parsed artifacts across sessions.
  - `availableModels`: Backend-reported model catalog.
  - `activeThinking` / `thinkingPhase`: Real-time reasoning telemetry.

### 2.4 Network & Streaming Layer
- **`HermesApiClient`**: Central OkHttp client with:
  - `InMemoryCookieJar` for automatic session authentication.
  - Connect and read timeouts configured for long-running SSE streams and WebSockets.
  - Typed DTO parsing using `kotlinx.serialization.json`.
- **`StreamSmoothingEngine`**: Buffers incoming network token deltas and flushes them in 33ms (~30 FPS) cadence to avoid UI frame drops and jitter.
- **WebSocket Clients**: Dedicated duplex connections for live voice autonomy (`/v1/voice/ws`) and terminal PTY (`/api/pty`).

---

## 3. Local Persistence & Offline Caching
- **Room Database:** Caches sessions, messages, projects, tasks, and artifacts to enable instant offline loading.
- **DataStore:** Stores user preferences (theme color mode, default model choice, haptic feedback, notification switches).
- **Encrypted Preferences / Keystore:** Stores authentication passwords/tokens securely without exposing them in memory dumps or UI states.

---

## 4. Error Handling & Reconnection Strategy
- Unified sealed hierarchy for error states:
  - `NetworkUnavailable`: Displays offline banner and serves cached Room state.
  - `Unauthorized`: Prompts re-authentication without clearing local drafts.
  - `StreamInterrupted`: Automatic exponential backoff reconnection for SSE and WebSockets with sequence cursor resumption.
