# Hermes Android — Implementation Mapping & Production Blueprint

This document specifies the exact mapping between the reverse-engineered **Claude Android** architecture and the **Hermes Autonomous AI Assistant** Android client.

---

## 1. Direct Architecture Mapping Matrix

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          CLAUDE ➔ HERMES ARCHITECTURE MAPPING                          │
├─────────────────────────┬─────────────────────────┬────────────────────────────────────┤
│ Claude Architecture     │ Hermes Equivalent       │ Implementation Location            │
├─────────────────────────┼─────────────────────────┼────────────────────────────────────┤
│ Claude Clay (#D97757)   │ Hermes Coral (#D97757)  │ ui/theme/Color.kt                  │
│ Dark Canvas (#141413)   │ Hermes Canvas (#141413) │ ui/theme/Color.kt                  │
│ Copernicus Serif        │ Hermes Serif (Copernicus│ ui/theme/Type.kt                   │
│                         │ / Tiempos Headline)     │                                    │
│ StyreneB Sans-Serif     │ Hermes Sans (StyreneB / │ ui/theme/Type.kt                   │
│                         │ Inter)                  │                                    │
│ Anthropicon Icons (292) │ Hermes Vector Icons     │ ui/components/HermesIcons.kt       │
│ Claude Code Remote (CCR)│ Hermes Agent Harness &  │ ui/workspace/WorkspaceScreen.kt    │
│                         │ Docker Workspaces       │                                    │
│ Bell Voice Mode (WSS)   │ Hermes Voice Engine     │ voice/HermesVoiceService.kt        │
│ Wiggle Artifacts        │ Hermes Artifact Sandbox │ ui/artifacts/ArtifactViewer.kt     │
│ Stream Smoothing Engine │ Hermes Stream Smoother  │ stream/StreamSmoothingEngine.kt    │
│ 8-Frame Thinking Spark  │ Hermes Thinking Spark   │ ui/components/HermesLogo.kt        │
└─────────────────────────┴─────────────────────────┴────────────────────────────────────┘
```

---

## 2. Hermes Jetpack Compose Architecture Structure

```
android/app/src/main/java/com/example/hermes/
├── MainActivity.kt                      # Root Compose Entry Point (SingleTop, adjustResize)
├── theme/
│   ├── Color.kt                         # Exact Palette (#141413, #1F1E1C, #242321, #D97757)
│   ├── Type.kt                          # Typography tokens (Serif Arrival, Sans Functional)
│   ├── Shape.kt                         # Corner radii (Pill 9999dp, Sheet 32dp, Card 20dp)
│   └── Theme.kt                         # HermesTheme wrapper
├── ui/
│   ├── components/
│   │   ├── HermesLogo.kt                # 8-point Starburst mark + 8-frame Thinking animation
│   │   ├── ChatComponents.kt            # Message bubbles, thinking boxes, tool cards
│   │   ├── ComposerCard.kt              # Floating pill composer with model chip & Voice FAB
│   │   ├── StatusChip.kt                # Ready, Running, Verifying, Needs Approval chips
│   │   └── DrawerContent.kt             # Navigation drawer (Chats, Projects, Code, Artifacts)
│   ├── chat/
│   │   ├── ChatScreen.kt                # Main conversation viewport & stream collector
│   │   └── ChatViewModel.kt             # MVI Reducer collecting SSE stream events
│   ├── voice/
│   │   ├── VoiceCallScreen.kt           # Ambient full-screen HUD with pulsing starburst
│   │   └── VoiceViewModel.kt            # WSS duplex audio coordinator
│   ├── workspace/
│   │   ├── WorkspaceScreen.kt           # Claude Code Remote (CCR) terminal & git diffs
│   │   └── ApprovalCard.kt              # Interactive one-shot vs persistent approval card
│   └── artifacts/
│       ├── ArtifactViewerScreen.kt      # Sandboxed WebView for HTML/JS and SVG artifacts
│       └── SandboxedPdfViewer.kt        # Virtualized PDF.js canvas viewer (max 7 pages)
├── network/
│   ├── HermesGatewayApi.kt              # Retrofit REST API interface
│   ├── SseStreamCollector.kt            # SSE chunk parser (thinking, text, tools)
│   ├── StreamSmoothingEngine.kt         # 30 FPS tick buffer for jitter-free rendering
│   └── VoiceWebSocketClient.kt          # WSS client with Opus audio streaming & clock sync
└── data/
    ├── DataRepository.kt                # Repository mediating Room DB and Network
    └── local/
        ├── HermesDatabase.kt            # Room DB for offline message caching
        └── DataStoreManager.kt          # Persistent preferences & SessionDraft storage
```

---

## 3. Production Readiness Verification

1. **Pixel-Perfect Alignment**: All typography sizes (`32sp` display greeting, `16sp` body, `13sp` mono), color hex codes, and spacing paddings match the decompiled production binary.
2. **Deterministic Motion**: Animations employ exact timing loops (`1000ms` thinking loop, `300ms` bottom sheet slide, `33ms` stream smoother tick).
3. **Robust Protocol Adherence**: SSE deltas and WebSocket duplex frames follow the reverse-engineered schemas without deviation.
