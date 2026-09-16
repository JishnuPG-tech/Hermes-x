# Hermes Android APK — Google AI Studio Master Implementation Prompt

## Mission

Build the **production-ready Hermes Android application** from the attached Hermes/Stitch UI/UX ZIP and the existing Hermes-x GitHub repository.

The ZIP is the **visual source of truth**.

The GitHub repository is the **engineering, architecture, backend-contract, security, and product source of truth**.

The result must be:

> **Claude's generated Android UI/UX, preserved as-is, upgraded into the complete Hermes autonomous agent experience.**

Do **not** redesign the UI. Do **not** replace the visual language. Do **not** invent a different navigation system. Add Hermes capabilities inside the generated design.

---

# 1. SOURCE MATERIALS

## 1.1 Hermes GitHub repository

Repository:

https://github.com/JishnuPG-tech/Hermes-x

Clone:

```bash
git clone https://github.com/JishnuPG-tech/Hermes-x.git
cd Hermes-x
```

Use the `main` branch unless the repository specifies otherwise.

Before writing code:

1. Inspect the complete repository.
2. Read the README.
3. Read `docs/blueprint/MASTER_BLUEPRINT.md`.
4. Read all relevant files under `docs/ui-ux/`.
5. Read Hermes architecture documentation.
6. Read Agent Harness / Agent OS documentation.
7. Read OmniRoute documentation.
8. Read server-computer documentation.
9. Read integration and security documentation.
10. Do not overwrite existing architecture decisions without a documented reason.

Important repository principle:

> Hermes is the system authority. OmniRoute is model/provider infrastructure beneath Hermes.

---

# 2. UI/UX ZIP IS THE VISUAL SOURCE OF TRUTH

The attached ZIP is:

`hermes-stitch-ui.zip`

It contains the generated Claude-style Android UI/UX package, including:

- `claude.design.md`
- design tokens
- component definitions
- screen inventory
- navigation architecture
- motion and interaction rules
- HTML screen implementations
- screen images
- Hermes-specific screens
- Hermes Android architecture
- Hermes Jetpack Compose codebase specification
- Hermes Claude UI upgrade master prompt
- Hermes logo assets

Read the entire ZIP before implementing.

Important files include:

```text
00_readme.md
01_design_tokens.md
02_screen_inventory.md
03_components.md
04_navigation_architecture.md
05_motion_interaction.md
claude.design.md
design_1.md
plan.md
hermes_android_apk_complete_architecture_screen_flow_map.md
hermes_android_apk_complete_jetpack_compose_codebase.md
hermes_claude_ui_upgrade_master_prompt.md
```

Also inspect every screen image and every HTML screen implementation.

Do not rely only on the Markdown documents.

---

# 3. ABSOLUTE VISUAL RULE

The generated Claude/Stitch UI is frozen.

### DO NOT

- redesign the UI
- introduce a new visual language
- replace the Claude-style layout
- replace typography
- replace spacing
- replace sheet behavior
- replace navigation behavior
- add flashy gradients
- create a generic AI dashboard
- create a completely different Hermes UI
- move features merely because another layout seems cleaner
- replace the generated screens with Material 3 defaults

### DO

- preserve the existing UI
- preserve the existing component geometry
- preserve colors and typography
- preserve spacing and radii
- preserve bottom sheets
- preserve navigation structure
- preserve animation principles
- preserve the visual hierarchy
- add Hermes functionality inside existing surfaces
- add new screens only when a Hermes capability genuinely requires one

The product should feel like:

> **Claude, upgraded into Hermes.**

Not:

> "A new app inspired by Claude."

---

# 4. ANDROID TECHNOLOGY

Build a native Android application using:

- Kotlin
- Jetpack Compose
- Material 3 only where it does not alter the provided design
- Coroutines
- Flow / StateFlow
- ViewModel
- Navigation Compose
- lifecycle-aware state
- secure local storage
- Room where local persistence is appropriate
- DataStore for preferences
- WebSocket for realtime Hermes communication
- REST only where appropriate
- background work using Android-compatible mechanisms
- accessibility semantics
- responsive layouts for phones and tablets

Follow the single-activity / Compose architecture expected by Google AI Studio.

Do not use:

- XML layouts
- Java UI
- a second UI framework
- React Native
- Flutter
- a competing rendering architecture

The Android client is the **Hermes control surface**, not the Hermes server itself.

---

# 5. CRITICAL GOOGLE AI STUDIO CONSTRAINT

Google AI Studio's native Android builder generates client-side Kotlin + Jetpack Compose applications.

Therefore:

**Do not attempt to put Hermes server infrastructure, secrets, provider keys, databases, or autonomous execution authority inside the APK.**

The APK communicates with the Hermes server.

Architecture:

```text
ANDROID APK
    |
    | HTTPS / WebSocket
    v
HERMES SERVER
    |
    +-- Hermes Agent
    |
    +-- Agent OS
    |
    +-- Memory
    |
    +-- Tools
    |
    +-- Terminal
    |
    +-- Browser
    |
    +-- GitHub
    |
    +-- Notion
    |
    +-- Integrations
    |
    +-- Voice Gateway
    |
    +-- Watchdog
    |
    +-- Task Scheduler
    |
    v
OMNIROUTE
    |
    v
MODELS / PROVIDERS
```

Never put production API keys into the APK.

Never expose:

- GitHub tokens
- Notion tokens
- provider keys
- OmniRoute credentials
- database credentials
- Hermes master keys
- secret-vault encryption keys

in source code, resources, logs, UI state, or APK assets.

---

# 6. HERMES AUTHORITY MODEL

The core hierarchy is:

```text
User
  ↓
Hermes Agent
  ├── Agent OS
  ├── Memory
  ├── Tools
  ├── Projects
  ├── Tasks
  ├── Agents
  ├── Permissions
  ├── Security
  ├── GitHub
  ├── Notion
  ├── Voice
  └── Server Computer
          ↓
      OmniRoute
          ↓
        Models
```

Canonical rule:

> **Hermes is the king. OmniRoute powers the king. Models power OmniRoute's routing targets. The user communicates with Hermes, never with OmniRoute directly.**

The Android UI must therefore never make OmniRoute appear to be a separate assistant.

---

# 7. HERMES FEATURES TO IMPLEMENT

Implement the complete Hermes feature set discussed in the repository architecture.

## Conversation

The Chat screen must support:

- normal conversations
- persistent sessions
- streaming responses
- markdown
- code blocks
- file attachments
- images
- context selection
- memory controls
- web search state
- tool execution indicators
- autonomous execution
- task progress
- agent delegation
- artifacts
- errors
- retry
- stop
- resume
- regenerate where supported
- background continuation
- reconnect
- offline state
- voice handoff

The conversation must remain a Hermes conversation even when different models are used.

---

# 8. AUTONOMOUS TASKS

Hermes must support long-running objectives.

Example:

> "Build this Android feature, test it, fix failures, commit it and open a PR."

The UI must show:

- objective
- status
- progress
- current phase
- current action
- workers
- subtasks
- dependencies
- checkpoints
- retries
- failures
- recovery
- verification
- final result

Task states:

```text
QUEUED
INITIALIZING
RUNNING
WAITING
BLOCKED
RETRYING
RECOVERING
VERIFYING
COMPLETED
FAILED
CANCELLED
PAUSED
```

The UI must update from real backend events.

Do not fake progress.

---

# 9. AGENTS AND AGENT TEAMS

Hermes can delegate work.

Example:

```text
Hermes
 ├── Backend Agent
 ├── UI/UX Agent
 ├── QA Agent
 ├── DevOps Agent
 ├── Security Agent
 └── Research Agent
```

The UI must support:

- agent list
- agent status
- assigned task
- current action
- tool activity
- output
- progress
- failures
- retry
- stop
- delegation
- dependencies
- team hierarchy

Agents remain subordinate to Hermes.

Hermes owns:

- creation
- authorization
- scheduling
- supervision
- verification
- completion

---

# 10. SERVER COMPUTER

The Hermes server is the agent's computer.

The Android UI must expose a safe control surface for:

- terminal
- filesystem
- projects
- logs
- processes
- git status
- git diff
- command execution
- task workspace
- artifacts

Primary workspace convention:

```text
/data/jarvis/
├── projects/
├── workspaces/
├── databases/
├── artifacts/
├── logs/
├── agent-state/
├── config/
├── memory/
└── voices/
```

Never assume the Android device filesystem is Hermes' primary workspace.

---

# 11. PROJECTS

Implement persistent projects.

Each project should expose:

- project name
- description
- local workspace
- GitHub repository
- branch
- status
- active tasks
- recent activity
- files
- artifacts
- agents
- logs
- deployment state

Project registry belongs to Hermes server infrastructure.

---

# 12. GITHUB

GitHub is the engineering source of truth.

The Android UI must allow Hermes to expose:

- repositories
- branches
- commits
- pull requests
- issues
- workflows
- CI status
- diffs
- releases
- project activity

Hermes should be able to perform authorized workflows through the server:

```text
inspect
→ plan
→ modify
→ test
→ build
→ security check
→ commit
→ push
→ CI
→ verify
→ PR
→ verify remote state
```

Never pretend an operation succeeded if backend verification did not confirm it.

---

# 13. NOTION

Notion is the primary structured workspace/project-management system.

The UI should expose:

- workspace
- projects
- pages
- databases
- tasks
- documentation
- activity
- connection state

Notion is not Hermes authority.

Notion stores structured human-visible information.

Hermes remains the authority.

---

# 14. MEMORY

Implement UI for:

- memory status
- saved knowledge
- project memory
- conversation memory
- task history
- memory search
- memory references
- memory controls

Memory belongs to Hermes.

Changing models must not reset memory.

---

# 15. LEARNING

Hermes can improve through:

```text
observe
→ record
→ propose
→ test
→ evaluate
→ approve
→ activate
→ monitor
→ rollback
```

The Android UI should expose meaningful learning/improvement events without exposing hidden chain-of-thought.

Never display private internal reasoning as raw chain-of-thought.

Display concise execution summaries, actions, tool activity, and verification results.

---

# 16. SKILLS

Support Hermes skills.

UI should expose:

- installed skills
- available skills
- skill description
- source
- version
- enabled state
- permissions
- update state
- validation state

Skills must execute through Hermes policy.

---

# 17. BROWSER AUTOMATION

Support browser capabilities through Hermes server-side tools.

UI should expose:

- browser task
- URL
- action status
- screenshots where available
- progress
- errors
- completion
- approval prompts

Never expose browser credentials to the client.

---

# 18. VOICE

Voice is another interface to the SAME Hermes session.

Architecture:

```text
Wake word
   ↓
VAD
   ↓
Streaming STT
   ↓
Hermes
   ↓
OmniRoute
   ↓
Model
   ↓
Hermes
   ↓
Streaming TTS
   ↓
Android audio
```

Support:

- wake word
- push-to-talk
- hands-free mode
- continuous conversation
- streaming STT
- streaming TTS
- interruption / barge-in
- mute
- reconnect
- speaking state
- listening state
- processing state
- tool execution state
- audio error state
- text fallback

Target:

- first playable audio <= 5 seconds
- hard maximum target <= 10 seconds
- stream output rather than waiting for the full response

Preferred TTS architecture:

```text
Primary: Kokoro
Fallback: Edge TTS
```

Voice settings must preserve the generated Claude-style UI.

---

# 19. WAKE WORD

Wake word is a client/server interface feature.

Do not create a separate assistant personality.

Wake word activates Hermes.

The same session continues after activation.

---

# 20. OMNIROUTE

OmniRoute is invisible infrastructure from the user's perspective.

The model selector can show model/provider choices, but the UX must remain Hermes-first.

OmniRoute is responsible for:

- provider connectivity
- model routing
- fallback
- availability
- routing telemetry
- latency/cost/quality signals

Hermes remains responsible for:

- conversation
- planning
- tasks
- memory
- tools
- permissions
- projects
- final decisions

Changing model must not reset:

- identity
- conversation
- task
- memory
- project
- tools
- voice session

---

# 21. MODEL SELECTOR

Preserve the generated model-selector visual design.

Add real backend information:

- current model
- provider
- availability
- routing mode
- fallback state
- latency
- context capacity where available

Never invent model availability.

---

# 22. MCP AND INTEGRATIONS

Support integration state for:

- GitHub
- Notion
- Gmail
- Telegram
- WhatsApp
- Hugging Face
- Google Drive
- other authorized MCP/integration providers

Lifecycle:

```text
DISCOVER
→ EXPLICIT AUTHORIZE
→ AUTHENTICATE
→ SCOPE
→ STORE SECRET REFERENCE
→ TEST
→ ENABLE
→ USE
→ VERIFY
→ AUDIT
→ REVOKE / ROTATE
```

The Android UI must never display secret values.

---

# 23. CREDENTIAL VAULT

Use server-side encrypted credentials.

Android receives:

```text
credential_id
provider
status
scopes
created_at
updated_at
expires_at
```

It must never receive the raw secret unless a flow explicitly requires a secure platform API and the architecture allows it.

Suggested server storage:

```text
/data/jarvis/secrets/vault.db
```

HF Space bootstrap secrets should be minimal.

---

# 24. SECURITY

Use this trust hierarchy:

```text
Owner Policy
 ↓
Hermes Policy
 ↓
Project Policy
 ↓
Task Policy
 ↓
Worker Policy
 ↓
Tool Permission
 ↓
Actual Action
```

High-risk actions require explicit approval.

Examples:

- destructive filesystem operations
- deleting repositories/files
- production deployment
- credential changes
- permission escalation
- sending sensitive messages
- irreversible external actions

The UI must show:

- what will happen
- why
- affected resource
- permission required
- approve
- deny
- cancel

---

# 25. CRON AND AUTOMATION

Support Hermes scheduled/background work.

UI should show:

- scheduled jobs
- schedule
- enabled/disabled
- next run
- previous run
- output
- failures
- logs
- pause
- resume
- delete

Examples:

```text
Every morning
Every hour
Every Monday
At 18:00
Conditional watch
```

Long-running work must persist beyond a single chat session.

---

# 26. NOTIFICATIONS

Support:

- task completion
- task failure
- approval required
- GitHub CI result
- deployment result
- integration failure
- scheduled task
- agent completion
- security warning
- server offline
- voice connection failure

Do not spam notifications.

---

# 27. SERVER HEALTH

Expose:

- Hermes status
- Agent OS status
- OmniRoute status
- model availability
- storage
- memory
- CPU
- uptime
- task queue
- active agents
- WebSocket status
- voice service status
- integration health

The health screen must reflect real backend data.

---

# 28. WATCHDOG

Hermes infrastructure includes a watchdog.

Show useful status only:

- healthy
- degraded
- recovering
- unavailable

If a service fails:

```text
detect
→ classify
→ retry
→ recover
→ verify
→ report
```

---

# 29. CHECKPOINTS AND RECOVERY

Tasks must support durable checkpoints.

Checkpoint data should include:

- task ID
- objective
- current phase
- state
- workspace
- agents
- dependencies
- completed actions
- pending actions
- verification state

Recovery:

```text
Failure
 ↓
Classify
 ↓
Transient? → Retry
Dependency? → Wait
Code? → Debug
Permission? → Request approval
Environment? → Repair
Unknown? → Diagnose
```

---

# 30. ARTIFACTS

Support:

- generated files
- reports
- APKs
- ZIPs
- logs
- screenshots
- patches
- build outputs
- documents

Show:

- artifact name
- type
- size
- source task
- created time
- download/open action
- verification state

---

# 31. CHAT EXECUTION UI

Preserve the generated execution-sheet visual style.

The execution sheet should show real events such as:

```text
Planning
Creating files
Running tests
Building
Checking security
Git commit
Git push
CI
Verification
Complete
```

Each event should have:

- status
- timestamp
- short description
- expandable details
- failure information if relevant

Do not expose hidden chain-of-thought.

---

# 32. VISUAL STATES

Every major surface must implement:

- initial
- loading
- empty
- populated
- streaming
- processing
- success
- error
- offline
- reconnecting
- permission required
- approval required
- disabled
- locked
- partial failure

Use the exact visual language from the ZIP.

---

# 33. OFFLINE / RECONNECT

The Android app should remain useful when the Hermes server is unavailable.

Support:

- cached conversations
- cached projects
- cached task summaries
- queued UI actions where safe
- reconnect
- retry
- clear server connection state

Do not pretend autonomous execution continues if the server is unreachable.

---

# 34. EVENT-DRIVEN BACKEND CONTRACT

Implement a typed event protocol.

Example:

```kotlin
sealed interface HermesEvent {
    data class TaskStarted(...)
    data class TaskProgress(...)
    data class ToolStarted(...)
    data class ToolOutput(...)
    data class AgentStarted(...)
    data class AgentCompleted(...)
    data class ApprovalRequired(...)
    data class TaskCompleted(...)
    data class TaskFailed(...)
    data class ServerStatus(...)
    data class VoiceState(...)
    data class ModelChanged(...)
}
```

Use stable IDs:

- sessionId
- messageId
- taskId
- agentId
- projectId
- toolCallId
- artifactId
- approvalId
- eventId

Events must be idempotent where practical.

---

# 35. UI STATE ARCHITECTURE

Use a unidirectional state model.

```text
Backend Event
      ↓
Repository
      ↓
ViewModel
      ↓
UiState
      ↓
Compose
      ↓
User Action
      ↓
Intent
      ↓
Repository / API
```

Avoid putting business logic directly inside composables.

---

# 36. NETWORKING

Create a dedicated Hermes API layer.

Example:

```text
data/
  remote/
    HermesApi
    HermesWebSocket
    dto/
  repository/
    ChatRepository
    TaskRepository
    ProjectRepository
    AgentRepository
    VoiceRepository
    IntegrationRepository
```

WebSocket should be the preferred transport for:

- streaming
- task events
- agent events
- tool events
- server status
- voice state

REST can handle:

- initial data
- CRUD
- artifact metadata
- settings
- historical data

---

# 37. DESIGN SYSTEM IMPLEMENTATION

Extract the exact values from the ZIP.

Create:

```text
ui/theme/
ui/components/
ui/foundation/
ui/motion/
```

Preserve:

- colors
- typography
- spacing
- radius
- elevation
- iconography
- sheet dimensions
- component dimensions
- animation timings
- transitions

Do not substitute generic Material defaults when the ZIP specifies custom behavior.

---

# 38. SCREEN INVENTORY

Implement the generated screen map, including:

1. Splash
2. Welcome / Sign In
3. Home / Greeting
4. Navigation Drawer
5. Add to Chat Sheet
6. Model Switcher
7. Active Chat
8. Execution Sheet
9. Full-Screen Summary
10. Autonomous Tasks
11. Server Terminal / Files
12. Live Voice Call
13. Voice Settings
14. Connectors / Vault
15. Capabilities
16. Settings / Account

Use the exact visual reference from the ZIP.

---

# 39. NEW HERMES SURFACES

Only add additional screens where the existing generated design cannot reasonably contain the feature.

Potential additions:

- Task Detail
- Agent Detail
- Project Detail
- GitHub Detail
- Memory Detail
- Notification Center
- Approval Detail
- Server Health
- Artifact Detail

These must inherit the existing design system.

Do not create a separate dashboard visual language.

---

# 40. LOGO AND BRANDING

Use the Hermes assets supplied in the ZIP.

Do not invent a new logo.

Do not use an incorrect logo.

Use vector assets where possible.

Ensure correct dark-background visibility.

---

# 41. AUTHENTICATION

Implement a real authentication boundary appropriate to the Hermes server.

Do not hard-code a fake successful login.

The app must support:

- authenticated state
- unauthenticated state
- expired session
- logout
- reconnect
- server unavailable

If the backend authentication contract is not yet implemented, create a clearly isolated interface and use a development/mock implementation only for local testing.

Do not confuse a mock with production functionality.

---

# 42. PRODUCTION CONFIGURATION

Never hard-code production server URLs where configuration is expected.

Support environment/build configuration such as:

```text
HERMES_BASE_URL
HERMES_WS_URL
```

Use safe defaults only for development.

Do not embed:

- API keys
- tokens
- private credentials
- secrets

---

# 43. TESTING

Implement tests for:

### Unit

- repositories
- state reducers
- event parsing
- authentication state
- task state
- reconnection
- permission logic

### UI

- navigation
- chat
- task execution
- agent views
- voice state
- settings
- connectors
- approvals

### Integration

- REST
- WebSocket
- authentication
- task streaming
- GitHub events
- Notion state
- server health

### Visual regression

Compare generated APK screenshots against the ZIP reference images.

The rule is:

> If a UI change is not required by a Hermes capability, do not change the visual design.

---

# 44. VISUAL QA LOOP

For every screen:

```text
Reference screenshot
       ↓
Build screen
       ↓
Capture screenshot
       ↓
Compare
       ↓
Measure visual differences
       ↓
Fix
       ↓
Repeat
```

Check:

- geometry
- spacing
- typography
- colors
- icon size
- alignment
- sheet position
- corner radius
- shadows
- animation
- status bar
- navigation bar
- keyboard behavior

Do not declare visual parity based only on compilation.

---

# 45. ACCESSIBILITY

Implement:

- content descriptions
- semantic labels
- sufficient contrast
- touch targets
- keyboard navigation where applicable
- TalkBack compatibility
- dynamic font handling
- reduced motion support

Do this without changing the visual baseline unnecessarily.

---

# 46. PERFORMANCE

Target:

- smooth scrolling
- low recomposition
- stable WebSocket connection
- efficient event processing
- no UI blocking
- lazy lists for long conversations/logs
- efficient image loading
- background work off the main thread
- bounded event history in memory

---

# 47. ERROR HANDLING

Never show generic "Something went wrong" when useful information exists.

Errors should identify:

- operation
- reason
- recoverability
- next action

Example:

```text
GitHub push failed

Reason:
Authentication expired.

Action:
Reconnect GitHub.
```

---

# 48. NO FAKE FUNCTIONALITY

This is critical.

Do not create buttons that only show:

- Toasts
- fake success messages
- fake progress
- fake agents
- fake GitHub actions
- fake task execution
- fake voice
- fake model routing
- fake terminal output

If a backend capability does not yet exist:

1. define the interface
2. connect the UI to that interface
3. implement a clearly marked development adapter if needed
4. never present simulated behavior as production functionality

---

# 49. GITHUB WORKFLOW

Use:

https://github.com/JishnuPG-tech/Hermes-x

Before implementation:

```bash
git clone https://github.com/JishnuPG-tech/Hermes-x.git
```

Work from the existing architecture.

Do not destroy existing documentation.

Add Android implementation in the appropriate repository location.

Suggested:

```text
android/
```

or the location established by the repository blueprint.

Preserve:

```text
docs/
```

and all existing Hermes backend/server architecture.

---

# 50. REQUIRED ANDROID STRUCTURE

Use a maintainable structure similar to:

```text
android/
├── app/
│   └── src/main/
│       ├── java/com/nousresearch/hermes/
│       │   ├── MainActivity.kt
│       │   ├── HermesApplication.kt
│       │   ├── navigation/
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   ├── components/
│       │   │   ├── screens/
│       │   │   └── sheets/
│       │   ├── data/
│       │   │   ├── remote/
│       │   │   ├── local/
│       │   │   ├── repository/
│       │   │   └── dto/
│       │   ├── domain/
│       │   ├── voice/
│       │   ├── security/
│       │   └── util/
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

Adapt this to the actual AI Studio constraints and existing repository structure.

---

# 51. IMPLEMENTATION ORDER

Do not attempt to write everything blindly in one pass.

Use this sequence:

## Phase 1

Inspect:

- ZIP
- GitHub repository
- architecture
- UI documents

## Phase 2

Build exact visual shell:

- theme
- typography
- components
- navigation
- screens
- sheets
- assets

## Phase 3

Implement local state:

- sessions
- settings
- cached state
- navigation

## Phase 4

Implement Hermes networking:

- REST
- WebSocket
- authentication
- event protocol

## Phase 5

Implement Chat:

- streaming
- attachments
- tools
- execution UI

## Phase 6

Implement autonomous tasks.

## Phase 7

Implement agents/teams.

## Phase 8

Implement projects and GitHub.

## Phase 9

Implement Notion and integrations.

## Phase 10

Implement terminal/server computer.

## Phase 11

Implement voice.

## Phase 12

Implement approvals/security.

## Phase 13

Implement notifications, watchdog, health and recovery.

## Phase 14

Visual regression and functional QA.

## Phase 15

Production build.

---

# 52. DEVELOPMENT LOOP

For every feature:

```text
READ
↓
PLAN
↓
IMPLEMENT
↓
BUILD
↓
RUN
↓
TEST
↓
SCREENSHOT
↓
VISUAL COMPARE
↓
FIX
↓
RETEST
↓
VERIFY
```

Never skip verification.

---

# 53. IMPORTANT SOURCE-OF-TRUTH RULE

Use this hierarchy:

```text
1. Actual generated ZIP UI
   = visual truth

2. GitHub Hermes architecture
   = functional architecture truth

3. Hermes backend implementation
   = runtime truth

4. Notion
   = structured product/project truth

5. Google AI Studio
   = implementation environment
```

If two sources conflict:

- preserve the actual UI visual design
- preserve Hermes authority architecture
- inspect the repository documentation
- do not silently invent a solution
- document any necessary compatibility decision

---

# 54. DEFINITION OF DONE

The APK is not complete merely because it compiles.

It is complete when:

### UI

- all generated screens implemented
- visual design matches reference
- navigation works
- sheets work
- animations work
- dark theme works
- assets are correct

### Hermes

- chat works
- streaming works
- autonomous tasks work
- agents work
- projects work
- terminal works
- artifacts work
- memory works
- GitHub works
- Notion works
- integrations work
- approvals work
- voice works
- notifications work
- server health works
- reconnection works

### Security

- no secrets in APK
- authentication works
- permissions work
- approval boundaries work

### Reliability

- WebSocket reconnects
- task state survives reconnect
- errors are recoverable
- backend failures are visible
- no fake success

### Quality

- unit tests
- UI tests
- integration tests
- visual QA
- accessibility QA
- performance QA
- release build verification

---

# 55. FINAL INSTRUCTION TO THE AI AGENT

You are not designing a new app.

You are implementing an existing, carefully designed UI and turning it into the Android client for Hermes.

Your first responsibility is to **understand the ZIP and repository completely**.

Your second responsibility is to **preserve the UI exactly**.

Your third responsibility is to **connect the UI to real Hermes capabilities**.

Your fourth responsibility is to **verify everything**.

Your fifth responsibility is to **never claim completion without evidence**.

The final experience must be:

> **A Claude-quality Android interface with the full autonomous power of Hermes behind it.**

Again:

> **DO NOT BUILD A NEW HERMES UI. BUILD HERMES INSIDE THE EXISTING CLAUDE/STITCH UI.**

---

# 56. REQUIRED FINAL REPORT

When implementation is complete, report:

1. Screens implemented
2. Features implemented
3. Backend endpoints used
4. WebSocket events implemented
5. Authentication implementation
6. Security model
7. Tests executed
8. Visual QA results
9. Accessibility results
10. Performance results
11. Known limitations
12. Remaining backend dependencies
13. APK build variant
14. Git commit
15. GitHub branch
16. Exact APK artifact path

Never say "production ready" if any critical item is incomplete.
