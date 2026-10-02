These files are much more important than just a UI reference. Your reverse-engineering package describes the client architecture, state machines, network protocols, persistence, security model, CCR/agent behavior, MCP integration, and exact screen behavior of the Claude Android reference you inspected. The package explicitly maps those pieces into a proposed Hermes Android implementation.

So I would change the plan from:

"Build an Android UI and connect some APIs"

to:

Build a native Hermes Android client using the same architectural class of client that your Claude analysis describes, while replacing Claude's backend with Hermes.

1. The big discovery

The reference client is not simply:

Compose
   ↓
REST API

Your documents describe:

             HERMES ANDROID CLIENT
                    │
       ┌────────────┼────────────┐
       │            │            │
      REST          SSE          WSS
       │            │            │
       ▼            ▼            ▼
   State/Data   Chat/Event    Voice/Realtime
       │            │            │
       └────────────┼────────────┘
                    ▼
              Hermes Server

The reference topology explicitly separates REST, SSE streaming, and duplex voice WebSocket communication.

That is exactly the direction I want for Hermes.

2. The Android architecture should now be fixed

Your extracted client-state document describes strict MVI:

Compose UI
   ↓
UiIntent
   ↓
ViewModel / Reducer
   ↓
Immutable UiState
   ↓
StateFlow
   ↓
Compose

That is directly stated in your reverse-engineering notes.

So Hermes should use:

ui/
   screens
   components

presentation/
   ViewModels
   UiState
   UiIntent
   reducers

domain/
   models
   usecases

data/
   repositories
   dto
   local
   remote

network/
   REST
   SSE
   WebSocket

This also aligns with current Android architecture guidance around layered architecture, UDF, repositories, persistent data models, and a single source of truth.

3. Chat should NOT use generic WebSocket streaming

This is one of the most important findings.

Your reference documents specify a real SSE lifecycle:

POST completion
      ↓
message_start
      ↓
thinking block
      ↓
thinking deltas
      ↓
text block
      ↓
text deltas
      ↓
tool_use block
      ↓
tool input deltas
      ↓
message_delta
      ↓
message_stop

The exact event structure is documented in your streaming protocol file.

And the state machine maps that into:

IDLE
 ↓
SENDING
 ↓
STREAMING
 ├── Thinking
 ├── Text
 ├── Tool
 └── Citations
 ↓
NEEDS_TOOL_APPROVAL
 ↓
COMPLETED

Hermes should therefore have:
SseStreamCollector
        ↓
HermesStreamEvent
        ↓
ChatReducer
        ↓
ChatUiState
        ↓
Compose

Not:

HTTP response text
     ↓
append String

That is what will let your APK reproduce the Claude-style live thinking/tool/text presentation correctly.

4. Hermes should introduce a protocol adapter

This is where I would be careful.

We should not blindly make Hermes imitate Anthropic's backend everywhere.

Instead:

                    Android
                       │
                 HermesGatewayApi
                       │
              Hermes protocol layer
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
      REST             SSE            WSS
        │              │              │
        └──────────────┼──────────────┘
                       ▼
                 Hermes Core

The Android API models can look conceptually like the Claude reference, but underneath they map to Hermes-native concepts.

For example:

Claude:
completion2

Hermes:
POST /v1/chat/completions
or a dedicated Hermes streaming endpoint

The UI does not care.

It only receives:

MessageStart
ThinkingDelta
TextDelta
ToolStart
ToolInputDelta
ApprovalRequired
ToolResult
MessageStop

This gives you Claude-level client behavior without making Hermes structurally dependent on Anthropic's API.

5. Your current Hermes backend is already closer than I thought

From the GitHub review, Hermes already has several pieces required for this architecture:

Authentication
Sessions
Messages
Session watchers
Voice WebSocket
Dashboard WebSockets
PTY/terminal
Model discovery

For example, the current backend exposes session REST routes under /v1/sessions and /api/v1/sessions, plus session watch WebSockets.

Your current voice subsystem already exposes:

/v1/voice/ws
/voice

and has its own realtime voice gateway.

So we don't need to invent the entire backend transport layer again.

What we need is to formalize it into one canonical Android contract.

6. Voice is its own realtime system

Your reference architecture describes a distinct "Bell Mode" with:

VOICE_CONNECTING
        ↓
LISTENING
        ↓
USER_SPEAKING
        ↓
PROCESSING
        ↓
ASSISTANT_SPEAKING
        ↓
INTERRUPTED / ENDED

The state-machine document explicitly separates this from ordinary chat.

Your Hermes backend already has a duplex voice WebSocket.

So the Android architecture should be:

VoiceViewModel
      ↓
HermesVoiceSession
      ↓
VoiceWebSocketClient
      ↓
/v1/voice/ws
      ↓
Hermes Voice Gateway

Not:

voice → HTTP POST → wait → audio

for the main Bell-style session.

7. Offline support needs to be real

This is another important discovery.

Your reference storage architecture is:

DataStore
    ↓
preferences / model / theme / draft

Room
    ↓
conversations
messages
projects
artifacts

Encrypted storage
    ↓
credentials
session token
device credentials

And the synchronization model uses:

Room cache
    ↓
initial server sync
    ↓
cursor
    ↓
incremental synchronization

with LOCAL_FIRST and STRONG consistency modes.

For Hermes, I would use:

Hermes Server
      ↕
Repository
      ↕
Room
      ↕
ViewModel
      ↕
Compose

So opening the app feels instant from the local cache, while Hermes refreshes it in the background.

This is much better than making every screen perform a fresh network request.

8. Drafts are also important

The reference stores composer drafts independently so an app process death, call, or navigation does not destroy the user's unsent message.

Hermes should therefore have:

SessionDraft
 ├── sessionId
 ├── draftText
 ├── selectedModel
 ├── attachments
 └── updatedAt

persisted locally.

This is a small feature, but it makes the APK feel like a genuine native client instead of a web wrapper.

9. Security architecture should be split

Your security notes describe an important rule:

secret material should stay out of the Compose/ViewModel layer.

The reference uses Android Keystore-backed encryption and keeps only public metadata in UI state.

Hermes should use:

Android UI
   │
   │ NEVER sees provider secrets
   ▼
SecureCredentialStore
   │
   ▼
Android Keystore

The APK should authenticate to Hermes.

It should not contain:

OpenAI key
Anthropic key
Gemini key
OmniRoute provider keys
Notion secret
GitHub app secret

Those remain server-side.

That keeps your earlier architecture intact:

User
 ↓
Hermes Android
 ↓
Hermes
 ↓
OmniRoute
 ↓
Models
10. Claude Code Remote maps extremely well to Hermes

This is probably the most useful discovery for your project.

Your CCR notes describe:

Android
  ↓
remote coding session
  ↓
backplane
  ↓
isolated workspace
  ↓
agent

with worktree/isolation modes such as:

SingleSession
Worktree
SameDir

and distinct permission modes.

Hermes already has:

Agent Teams
Worker workspaces
Tasks
Projects
Checkpoints
Terminal
Browser
Verification
Trust Hierarchy

So your Android WorkspaceScreen can become:

Hermes Workspace
 ├── task/session header
 ├── live terminal
 ├── changed files
 ├── diff
 ├── agent steps
 ├── checkpoints
 ├── worker status
 ├── approval cards
 └── recovery state

That is a very natural mapping.

11. The permission system can actually be better than the reference

Your Claude-derived notes describe multiple permission modes.

Hermes already has its six-layer Trust Hierarchy.

So instead of merely copying:

Allow
Deny

we can expose Hermes-specific decisions:

SAFE
  ↓
AUTO

ELEVATED
  ↓
ASK ONCE

HIGH RISK
  ↓
REQUIRE APPROVAL

FORBIDDEN
  ↓
DENY

And the existing Hermes action overlay idea is useful here.

Your design documentation proposes:

amber action overlay
+
permission card
+
step counter
+
tool execution status

for autonomous device actions.

That is exactly the kind of UI information the Android client should receive from Hermes events.

12. MCP should not be hardcoded into screens

Your reverse-engineering notes show that the reference client treats MCP as a real subsystem:

probe
 ↓
initialize
 ↓
tools list
 ↓
prompts list
 ↓
resources list

and supports OAuth PKCE.

Hermes should therefore expose:

McpRepository
McpViewModel
McpServer
McpTool
McpPrompt
McpResource
McpConnectionState

The UI simply renders whatever Hermes says exists.

For example:

Notion
  Connected
  12 tools

GitHub
  Connected
  8 tools

Custom MCP
  Authenticating

No fake tool counts.

13. The screen architecture should become data-driven

Your screen blueprint includes dynamic values such as:

"5 enabled"
"1 connected"
"Sonnet 5 · Low"
"Good afternoon, Jishnu"

For Hermes, every dynamic value should be modeled.

Example:

data class HomeUiState(
    val userName: String?,
    val activeModel: ModelSummary?,
    val connectedIntegrations: Int,
    val enabledSkills: Int,
    val activeAgentState: AgentState,
    val draft: String
)

Then:

Hermes API / cache
       ↓
HomeRepository
       ↓
HomeViewModel
       ↓
HomeUiState
       ↓
exact existing Claude-style UI

This is exactly the change you asked about earlier when you said:

remove predefined demo values and replace them with realtime original server values.

Now we have a much more precise blueprint for doing it.

14. Do NOT copy all Claude APIs literally

This is important.

The documents contain endpoints such as:

/organizations/{org}/chat_conversations/.../completion2

because that is how the reference client communicates with its backend.

Hermes should instead expose an internal contract designed around Hermes entities:

sessions
tasks
projects
agents
workers
messages
events
artifacts
memory
skills
integrations
voice
terminal
browser
checkpoints
recovery

The Android client's behavior can mirror Claude while its data model is genuinely Hermes.

15. This gives us the final architecture
                         HERMES ANDROID
┌───────────────────────────────────────────────────────────┐
│ Exact Claude-style Compose UI                             │
│                                                           │
│ Home  Chat  Projects  Code  Artifacts  Settings  Voice   │
└──────────────────────────┬────────────────────────────────┘
                           │
                     MVI / StateFlow
                           │
                    ViewModels / Reducers
                           │
                       Repositories
                           │
              ┌────────────┼────────────┐
              │            │            │
             REST          SSE          WSS
              │            │            │
              ▼            ▼            ▼
        HermesGateway   ChatStream   VoiceSession
              │            │            │
              └────────────┼────────────┘
                           ▼
                     HERMES SERVER
                           │
      ┌────────────────────┼─────────────────────┐
      ▼                    ▼                     ▼
   Harness             OmniRoute             Storage
      │                    │                     │
      ├─ Tasks             ├─ Models             ├─ Memory
      ├─ Agents            ├─ Routing             ├─ Projects
      ├─ Browser           ├─ Failover            ├─ Sessions
      ├─ Terminal          └─ Telemetry           └─ Checkpoints
      ├─ Voice
      ├─ Skills
      ├─ MCP
      └─ Trust
What these documents change for our implementation

We now have a much clearer contract:

UI fidelity: your existing Claude-style APK stays essentially frozen. The visual system already specifies the canvas, typography, component grammar, screens, animations and motion model.

Client architecture: native Kotlin + Compose + coroutines + StateFlow + MVI.

Networking: REST for state/commands, SSE for streaming agent/chat events, WSS for realtime voice.

Persistence: Room + DataStore + secure credential storage + synchronization.

Execution: Hermes tasks/workspaces/checkpoints/agents map naturally to the CCR concepts you documented.

MCP: dynamic discovery and OAuth-aware connector architecture.

Security: secrets stay behind the secure storage boundary and never become normal UI state.

And most importantly, your final mapping document already proposes the exact Android project structure:

MainActivity
theme
ui
chat
voice
workspace
artifacts
network
data

including HermesGatewayApi, SseStreamCollector, StreamSmoothingEngine, VoiceWebSocketClient, and DataRepository.

That is the blueprint I would use from here. We should not keep inventing the Android architecture from scratch. We can now take the existing APK UI and wire every screen to Hermes through this architecture, replacing every static demo value with authoritative Hermes state.