# Hermes-x

A persistent autonomous AI assistant built around **Hermes Agent** and **OmniRoute**.

## Product authority

**Hermes is the king. OmniRoute powers the king. Models power OmniRoute's routing targets. The user communicates with Hermes, never with OmniRoute directly.**

Hermes owns conversation, memory, planning, tools, terminal/files, GitHub, Notion, permissions, autonomous tasks, agent delegation, verification, recovery, voice sessions and final decisions.

OmniRoute provides model/provider connectivity, routing and fallback infrastructure only.

## Vision

Hermes is intended to provide a persistent voice/text interface for autonomous engineering, research, server operations, GitHub workflows, software lifecycle management, long-running tasks and continuous improvement.

## Source-of-truth model

- **Notion**: product planning, UX, structured knowledge and human-visible project management.
- **GitHub**: source code, version history, CI/CD and releases.
- **Hermes / Agent OS**: runtime execution, checkpoints and traces.
- **Persistent storage**: live application state, workspaces, memory, artifacts and logs.
- **OmniRoute**: model/provider routing infrastructure.

## Architecture

```text
User
 ↓
Hermes UI
 ↓
Hermes Agent
 ├─ Memory
 ├─ Agent OS
 ├─ Tasks / Agents
 ├─ Tools / Terminal / Files
 ├─ GitHub / Notion / Integrations
 ├─ Voice
 ├─ Security / Permissions
 └─ Verification / Recovery
 ↓
OmniRoute
 ↓
Models
```

## UI/UX

The UI follows the approved calm Claude-style visual language while adding Hermes-specific capabilities inside the same interaction model.

Documentation:

- [Master Blueprint](docs/blueprint/MASTER_BLUEPRINT.md)
- [Design System](docs/ui-ux/DESIGN_SYSTEM.md)
- [Screen Specification](docs/ui-ux/SCREEN_SPEC.md)
- [UI Architecture](docs/ui-ux/UI_ARCHITECTURE.md)
- [UX Flows](docs/ui-ux/UX_FLOWS.md)
- [State Machine](docs/ui-ux/STATE_MACHINE.md)
- [Visual QA](docs/ui-ux/QA_AND_VISUAL_REGRESSION.md)
- [Implementation Roadmap](docs/ui-ux/IMPLEMENTATION_ROADMAP.md)

## Android Application

The mobile client is a production-connected native Android application built with **Jetpack Compose**, **Kotlin Coroutines / Flow**, **Room Persistence**, and **AndroidX DataStore**:

- **Location**: `android/`
- **Design Language**: Authentic Anthropic Claude dark warm near-black canvas (`#161513`), serif editorial headers, Brand Coral accents (`#E27D60`), and high-information density components.
- **Data & Persistence**: Strict multi-account cache isolation with Room DAOs (`SessionDao`, `MessageDao`, `TaskDao`, `ProjectDao`, `ArtifactDao`) and persistent `PreferencesManager` DataStore for typography, voice personas, connector discovery, and capability flags.
- **Realtime Integration**:
  - **Chat & Thinking**: SSE streaming with real-time reasoning phase tags (`<thought>`, `<phase>`) and token-budget metrics.
  - **Interactive Terminal**: WebSocket client connected to `/api/pty` with terminal resizing, clean ANSI escape sequence stripping, and reconnection lifecycle.
  - **Live Voice**: Realtime voice WebSocket protocol on `/v1/voice/ws` with configurable pace, language, and voice personas.
  - **Autonomous Tasks & Workforce**: Dynamic live worker tracking, DAG subtask visualization, and 24x7 scheduled automations.
  - **Secure Auth**: Google OAuth verification with synchronous server validation (`POST /api/auth/google`) and zero synthetic fallback states.

## Engineering Rules

Hermes never treats an agent claim as proof of completion. Completion requires verification evidence, including local and remote state verification where applicable.

## Security

Autonomy is permission-aware. Production deployment, destructive actions, privileged system changes, credential operations and other high-risk operations are gated by policy and approval unless explicitly authorized by an existing trust policy.

Secrets must not be committed to GitHub, Notion, model prompts or execution traces. Use encrypted credential references and appropriate secret storage.
