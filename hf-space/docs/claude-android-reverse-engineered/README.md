# Claude Android Reverse Engineering & Complete Specification Index

This directory contains the exhaustive, reverse-engineered technical blueprints and specifications extracted from the production Android APK (`Claude.apk`, package `com.anthropic.claude`, version `1.260828.0`).

---

## 📚 Document Catalogue & Technical Index

1. **[00. System Overview & Architecture Topology](./00_OVERVIEW_AND_SYSTEM_TOPOLOGY.md)**
   * System topology, Android Manifest analysis, foreground services, broadcast receivers, and full Kotlin package directory.
2. **[01. UI/UX Design System, Typography & Tokens](./01_UI_UX_DESIGN_SYSTEM_AND_TOKENS.md)**
   * Dark/light palettes, exact color tokens (`#141413`, `#D97757`, `#6A9BCC`), Copernicus/StyreneB/JetBrains typography scales, 4dp/8dp layout grid, and the complete 292 `anthropicon_*` icon catalogue.
3. **[02. All Pages & Screen Blueprints](./02_ALL_PAGES_AND_SCREEN_BLUEPRINTS.md)**
   * In-depth ASCII wireframes with unicode graphics covering Home/Greeting, Navigation Drawer, Model Selector Sheet, Live Voice ("Bell Mode"), Connectors, Settings, Capabilities, Claude Code Remote (CCR), and Sandboxed PDF.js Viewer.
4. **[03. Client State Machines & Execution Flows](./03_CLIENT_STATE_MACHINES_AND_FLOWS.md)**
   * Unidirectional MVI architecture, Chat SSE state machine, Voice low-latency VAD/Barge-in flow, CCR session lifecycle, and the 30 FPS Stream Smoothing algorithm.
5. **[04. Exact API Specifications & Payloads](./04_EXACT_API_SPECIFICATIONS_AND_PAYLOADS.md)**
   * Complete HTTP REST inventory (`POST /completion2`, `GET /chat_conversations`, `POST /cowork/sessions`), request/response JSON models, authentication headers, and error schemas.
6. **[05. Realtime Streaming Protocols & WebSocket Contracts](./05_REALTIME_STREAMING_PROTOCOLS.md)**
   * SSE chunked deltas (`thinking_delta`, `tool_use`, `citations`), and bidirectional Opus WebSocket Voice Protocol with NTP clock synchronization and `TTSWord` timing.
7. **[06. Claude Code Remote (CCR) & Agent Harness Architecture](./06_CLAUDE_CODE_REMOTE_AND_AGENT_HARNESS.md)**
   * Remote developer daemon, Worktree isolation modes (`SingleSession`, `Worktree`, `SameDir`), Permission Modes (`Auto`, `Plan`), GitHub PR automation, and "Move to Cloud" migration.
8. **[07. Model Context Protocol (MCP) & Extensible Tool Ecosystem](./07_MCP_APPS_AND_TOOL_ECOSYSTEM.md)**
   * MCP mobile integration, SHTTP transport (`/v1/toolbox/shttp/mcp/*`), server probing, OAuth 2.0 PKCE, and dynamic resource/prompt attachments.
9. **[08. Animation & Motion Specifications](./08_ANIMATIONS_AND_MOTION_SPECIFICATIONS.md)**
   * Complete animation engine: reasoning/thinking 8-frame loop (`claude_spark_animated_thinking`), writing 8-frame loop (`claude_spark_animated_writing`), idle shimmer (`claude_spark_animated_shimmer`), 30 FPS text smoother, voice waveform pulsing, and bottom sheet springs.
10. **[09. Individual Page Blueprints & Layout Specs](./09_INDIVIDUAL_PAGE_BLUEPRINTS_CATALOGUE.md)**
    * Detailed component hierarchies, exact dp dimensions, color bindings, and padding scales for every screen.
11. **[10. Security, Vault & Authentication Architecture](./10_SECURITY_VAULT_AND_AUTH_DEEP_DIVE.md)**
    * Hardware-backed keystore (AES-256 GCM), Google One-Tap & Magic Link auth, Play Integrity device attestation, and data leakage scanning.
12. **[11. Offline Cache, Storage Engine & Sync Architecture](./11_OFFLINE_CACHE_AND_STORAGE_ENGINE.md)**
    * Room database, DataStore preferences, `SessionDraft` persistence, timeline pagination cursors, and backoff retry logic.
13. **[12. Hermes Exact Implementation Mapping](./12_HERMES_EXACT_IMPLEMENTATION_MAPPING.md)**
    * Direct 1-to-1 Jetpack Compose implementation guide mapping every extracted token, screen, animation, and protocol directly into the Hermes Android APK.
14. **[13. In-Depth Analysis of Custom Android APK (`android/`)](./13_CUSTOM_HERMES_APK_INDEPTH_ANALYSIS.md)**
    * Exhaustive analysis of the custom codebase in `android/` (`com.example.hermes`): Gradle build, 20+ Navigation3 screens, embedded typography binaries, OkHttp SSE/WebSocket client, and MVI ViewModels.
15. **[14. Chat Streaming, Adaptive Reasoning & Thinking Animations Report](./14_CHAT_STREAMING_AND_THINKING_ANIMATIONS_REPORT.md)**
    * Detailed investigation of Fast vs Extended reasoning paths, 8-frame rotating thinking spark, 8-frame writing pulse, 15-frame shimmer, collapsible thinking disclosure box, tool step cards, and 30 FPS stream smoothing.
