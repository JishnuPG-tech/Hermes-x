# Claude Android (`Claude.apk`) — System Overview & Architecture Topology

**Target Package**: `com.anthropic.claude`  
**Application Version**: `1.260828.0` (Build `26082800`)  
**Android Platform Target**: Target SDK `36` (Android 16), Min SDK `32` (Android 12L+), Compile SDK `37` (Android 17)  
**Language & Runtime**: 100% Kotlin + Jetpack Compose + Coroutines/StateFlow, Material 3, AndroidX Glance, Sentry  

---

## 1. System Topology & Communication Architecture

```
                                  ┌──────────────────────────────────────────────────┐
                                  │             CLAUDE ANDROID CLIENT                │
                                  │      Single-Activity Compose MVI Architecture    │
                                  └─────────┬───────────────────┬──────────────┬─────┘
                                            │                   │              │
                   HTTP REST & Multiparts   │   SSE Streams     │              │ WSS Duplex Audio
                   (OkHttp / Cronet / JSON) │ (text/event-stream)              │ (Opus / JSON Frames)
                                            ▼                   ▼              ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 │                                           ANTHROPIC EDGE GATEWAYS                                                │
 ├────────────────────────────────┬─────────────────────────────────┬───────────────────────────────────────────────┤
 │ 1. Mobile Edge REST Gateway    │ 2. Realtime SSE Event Streamer  │ 3. Voice Realtime Gateway ("Bell" Mode)       │
 │    • /organizations/{org}/..   │    • /chat/completion2          │    • wss://api.claude.ai/v1/voice/stream      │
 │    • /v1/code/sessions/..      │    • /code/sessions/watch       │    • Sub-350ms VAD, STT & TTS Pipeline       │
 │    • /v1/toolbox/shttp/mcp/..  │    • /code/events/stream        │    • Live Barge-in & Server Interrupt Engine  │
 └────────────────┬───────────────┴─────────────────┬───────────────┴───────────────────────┬───────────────────────┘
                  │                                 │                                       │
                  ▼                                 ▼                                       ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 │                                            BACKEND CORE SERVICES                                                 │
 ├────────────────────────────────┬─────────────────────────────────┬───────────────────────────────────────────────┤
 │ • Conversation & Chat Service  │ • Claude Code Remote (CCR) Core │ • Model Context Protocol (MCP) Directory      │
 │ • Memory & Fact Engine (CRUD)  │ • Cloud Runner & BYOC Manager   │ • Wiggle Artifact Sandbox & Storage           │
 │ • Safety & Constitution Filter │ • GitHub PR Automation & Sync   │ • Push Notification & Presence Router         │
 └────────────────────────────────┴─────────────────────────────────┴───────────────────────────────────────────────┘
```

---

## 2. Android Manifest Architecture & System Integrations

### 2.1 Activities & Entry Points
1. **`com.anthropic.claude.mainactivity.MainActivity`**
   * Primary user-facing Activity.
   * `launchMode="singleTop"`, `windowSoftInputMode="adjustResize"`.
   * Houses the root Compose Navigation Host (`NavHost`) switching between Chat, Projects, Artifact Viewer, Settings, and CCR Workspaces.
2. **`com.anthropic.claude.mainactivity.AssistantOverlayActivity`**
   * Configured as the **Native Android System Assistant**.
   * Intent filters: `android.intent.action.ASSIST`, `android.intent.action.VOICE_ASSIST`.
   * Invoked via Android hardware shortcut (long-press power button or corner swipe).
3. **`com.anthropic.claude.deeplink.DeepLinkActivity`**
   * Handles universal deep links (`https://claude.ai/*`, `claude://*`):
     * `/chat`, `/new`: Instant new session creation.
     * `/code/*`, `/code/artifact/*`, `/code/frame/*`: Claude Code Remote session navigation.
     * `/dispatch`, `/cowork`: Cowork autonomous task entry.
     * `/api/mcp/auth_callback`, `/connect/github/callback`: OAuth 2.0 PKCE redirects.
   * File Share Intent (`android.intent.action.SEND`, `SEND_MULTIPLE`): Ingests images, plain text, PDFs, Jupyter Notebooks (`.ipynb`), Word docs (`.docx`), Excel sheets (`.xlsx`), Shell scripts (`.sh`), and JSON/YAML.

---

### 2.2 Foreground Services & Realtime Daemons
1. **`com.anthropic.claude.bell.BellModeService`**
   * Foreground Service type: `microphone | mediaPlayback`.
   * Manages the persistent WebSocket lifecycle for low-latency Voice Mode ("Bell").
   * Holds wake locks (`android.permission.WAKE_LOCK`) and coordinates audio focus with Android `AudioManager`.
2. **`com.anthropic.claude.bell.tts.TTSPlaybackService`**
   * Foreground Service type: `mediaPlayback`.
   * Manages streaming Opus audio decoding, packet jitter buffers, and word-level PTS synchronization.
3. **`com.anthropic.claude.chat.MessageSseService`**
   * Manages long-running SSE event streams when the app is backgrounded during complex multi-step tool calls.
4. **`com.anthropic.claude.bell.assist.ClaudeVoiceInteractionService`**
   * Implements `android.service.voice.VoiceInteractionService` for OS-level assistant binding.

---

### 2.3 Receivers & Inter-Process Controls
1. **`CCRPermissionActionReceiver`**:
   * Intercepts notification action buttons: `CCR_PERMISSION_APPROVE`, `CCR_PERMISSION_DENY`, `CCR_PERMISSION_DENY_WITH_COMMENT`.
   * Allows developers to approve CLI bash commands or file mutations directly from the Android system notification tray.
2. **`SessionReplyActionReceiver`**:
   * Handles Android Direct Reply (`SESSION_REPLY`) from push notifications without opening the app.
3. **`ClaudeAppWidgetReceiver`**:
   * Android Glance home screen widget for quick prompting, voice trigger, and recent project switching.

---

## 3. Package Structure & Architectural Subsystems

```
com.anthropic.claude
├── analytics/events/           # Telemetry & Product Analytics (CodeEvents, ChatEvents, AccountEvents)
├── api/
│   ├── account/                # AccountProfile, AccountSettings, BootstrapResponse, RateLimitUpsell
│   ├── artifacts/              # PublishedArtifact, ArtifactVisibility, ArtifactVersionsResponse
│   ├── billing/                # PausedSubscriptionDetails, PrepaidCreditsResponse
│   ├── chat/                   # ChatCompletionRequest, MessageFile, InputMode, RenderingMode
│   │   ├── messages/           # StreamEvent, ContentBlock, TextDelta, ThinkingDelta, ToolUseBlock
│   │   └── tool/               # ResearchStatus, SourceMetadata, ToolDisplayContent
│   ├── common/                 # RateLimit, ConsistencyLevel, Amount, EmptyResponseWithSuccess
│   ├── consent/                # ConsentType, EntityType, CheckConsentResponse
│   ├── events/                 # EventLoggingRequest, BatchEventLoggingRequest
│   ├── feature/                # CoworkSettings, PermissionModePolicy
│   ├── kyc/                    # KycStatusResponse, KycStatus (Identity Verification)
│   ├── login/                  # ClientAttestation, CodeConfiguration, VerifyResponse
│   ├── mcp/                    # McpServer, McpTool, McpProbeResult, McpTransport, McpAuthPosture
│   ├── memory/                 # SensitiveCleanupState, MemoryMode
│   ├── notification/           # NotificationPreferencesSchema, NotificationChannelStatus
│   ├── project/                # ProjectType, ProjectFilter, ProjectOrganizationRole
│   ├── purchase/               # VerifyPurchaseResponse, PurchaseReceipt (Google Play Billing 9.0)
│   ├── result/                 # ApiResult, RetryBudgetExceededException, RetryAttemptTimeoutException
│   ├── skills/                 # Skill, ListSkillsResponse
│   └── voice/                  # ShareConsentDecision
├── artifact/model/             # WiggleArtifactIdentifier, ArtifactFile
├── bell/                       # Voice Engine ("Bell Mode")
│   ├── api/                    # BellApiClientMessage, BellApiServerMessage, BellApiData
│   ├── assist/                 # ClaudeVoiceInteractionService, ClaudeRecognitionService
│   └── tts/                    # TTSPlaybackService, AudioDecoderException, TTSApiMessage
├── code/remote/                # Claude Code Remote (CCR) & Cowork
│   ├── bottomsheet/            # CodeRemoteBottomSheetDestination
│   ├── devices/                # DeviceSessionsSheetDestination
│   ├── notification/           # CCRPermissionActionReceiver, SessionReplyActionReceiver
│   └── stores/                 # SessionDraft, CoworkUnsupervisedStickyConsent, SessionTarget
├── configs/flags/              # Remote Feature Flags (StreamSmoothingConfig, VoiceAdaptiveGainConfig)
├── connector/                  # MCP Connectors & OAuth PKCE
├── conversation/               # SSE Parser, TimelineConnection, BardHub
├── mcpapps/                    # SHTTP Tool Protocol & Domain Validation
├── sessions/types/             # PermissionMode, SessionStatus, ConnectionStatus, BridgeSpawnMode
├── stt/repo/                   # Whisper / Audio Encoding / VAD Engine
└── ui/                         # Jetpack Compose Themes, Components, Canvas
```

---

## 4. Key Architectural Patterns

1. **Unidirectional Data Flow (MVI / Redux-like StateFlow)**:
   * ViewModels emit immutable UI State (`StateFlow<UiState>`).
   * User interactions are dispatched as Events/Intents (`SharedFlow<UiEvent>`).
   * Side-effects (Navigation, Toast, Haptics) are handled via one-shot Channels.
2. **Stream Smoothing Engine (`StreamSmoothingConfig`)**:
   * Implements a 30 FPS tick buffer (`smoother_tick_interval_ms: 33ms`) to smooth chunked SSE text deltas.
   * Eliminates UI stutter during high-token-rate LLM completions.
3. **Adaptive Audio Gain & Voice Pipeline (`VoiceAdaptiveGainConfig`)**:
   * High-pass audio filtering + client-side VAD pre-classification before streaming Opus frames over WebSocket.
4. **Sandboxed WebView Execution**:
   * Artifact rendering and PDF.js visualization run in an isolated origin with strict Content Security Policy (`script-src 'self' cdnjs.cloudflare.com cdn.jsdelivr.net`).
   * No access to native Android JavaScript interfaces or filesystem access.
