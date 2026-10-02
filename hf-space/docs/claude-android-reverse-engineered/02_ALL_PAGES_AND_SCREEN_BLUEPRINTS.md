# Claude Android — Comprehensive Page & Screen Blueprints

---

## Screen 1: Home / Greeting & New Chat

```
┌─────────────────────────────────────────────────────────────────┐
│ 09:41 📶 🔋                                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ☰                                                           👻 │ ◄── Minimal Top Bar
│                                                                 │
│                                                                 │
│                              ✴️                                 │ ◄── Terracotta Starburst Mark (#D97757)
│                                                                 │
│                       Good afternoon,                           │ ◄── Serif Headline (32sp, #FAF9F5)
│                            Jishnu                               │
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ ✨ Get more with Claude Pro                Upgrade to Pro ↗ │ │ ◄── Upsell Strip
│ ├─────────────────────────────────────────────────────────────┤ │
│ │ ┌─────────────────────────────────────────────────────────┐ │ │
│ │ │  Chat with Claude…                                      │ │ │ ◄── Input Pill (#2A2927, radius 9999dp)
│ │ └─────────────────────────────────────────────────────────┘ │ │
│ │                                                             │ │
│ │   [ + ]    [ Sonnet 5 · Low ▾ ]            ( 🎙️ )   [ (●) ]  │ │ ◄── Control Row
│ │   Attach      Model Selector                 Mic   Voice FAB│ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Starburst Brand Mark**: 8-point radial starburst SVG, dimensions `48 x 48 dp`, fill `#D97757`.
* **Serif Greeting**: `Copernicus` / `Tiempos Headline`, `32sp`, line-height `38sp`, tracking `-0.8px`.
* **Model Chip (`Sonnet 5 · Low ▾`)**: Height `32dp`, pill background `#242321`, text `#FAF9F5`, trailing caret icon.
* **Voice FAB**: Circular diameter `44dp`, fill `#FFFFFF`, icon `anthropicon_microphone` in `#141413`.

---

## Screen 2: Navigation Drawer

```
┌──────────────────────────────────────────────┬──────────────────┐
│                                              │                  │
│   Claude                                  ✕  │                  │ ◄── Serif Header (22sp)
│                                              │                  │
│   💬  Chats                                  │                  │ ◄── Primary Nav List
│   📁  Projects                               │                  │
│   💻  Code                                   │      SCRIM       │
│   📄  Artifacts                              │    BACKGROUND    │
│                                              │                  │
│  ──────────────────────────────────────────  │     (#000000     │
│   📌  PINNED                                 │       60%)       │
│   • Resume optimization for ATS score        │                  │
│   • Multi-agent orchestrator RFC             │                  │
│                                              │                  │
│  ──────────────────────────────────────────  │                  │
│   🕒  RECENTS                                │                  │
│   • Free Docker hosting platforms 24/7       │                  │
│   • Plan execution engine refactor           │                  │
│   • Greeting exchange test                   │                  │
│   • Android Compose architecture             │                  │
│                                              │                  │
│  ──────────────────────────────────────────  │                  │
│   (J)  Jishnu                    [ + New ]   │                  │ ◄── Footer Bar
│   Avatar (#D97757)            Pill CTA (#FFF)│                  │
└──────────────────────────────────────────────┴──────────────────┘
```

### Component Details
* **Drawer Width**: `320dp` (standard mobile width).
* **Pinned & Recents Items**: Single-line text with ellipsis truncation, font size `14sp`, height `44dp`.
* **User Avatar**: Circle diameter `36dp`, fill `#D97757`, centered bold text `"J"` in `#FFFFFF`.
* **New Chat CTA**: High-contrast white pill button, height `36dp`, padding `16dp`, text `"＋ New"` in `#141413`.

---

## Screen 3: "Select Model" Bottom Sheet

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    ─── [ Drag Handle ] ───                      │ ◄── Handle (32x4dp, #7B7974)
│                                                                 │
│                          Select model                           │ ◄── Serif Title (22sp)
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Fable 5.1  [ Pro or Max ]                                 │  │ ◄── Model 1
│  │ For your toughest challenges                              │  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │ Opus 5     [ Pro ]                                        │  │ ◄── Model 2
│  │ For complex tasks                                         │  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │ Sonnet 5   [ Default ]                                 ✓  │  │ ◄── Selected Model
│  │ Most efficient for everyday tasks                         │  │     (Text: #6A9BCC)
│  ├───────────────────────────────────────────────────────────┤  │
│  │ Haiku 4.5  [ Fast ]                                       │  │ ◄── Model 4
│  │ Fastest for quick answers                                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│   Thinking Effort                                               │ ◄── Effort Slider Section
│   [ Low ] ──────●──────────────────────── [ High ]              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Sheet Background**: `#1F1E1C`, top corner radii `32dp`.
* **Selection Mechanism**: Text color shifts from `#FAF9F5` to `#6A9BCC` (Selection Blue) and trailing checkmark `anthropicon_check` is rendered.
* **Tier Badge Chips**: Height `20dp`, background `#2A2927`, text `11sp` bold caps in `#B0AEA5`.

---

## Screen 4: Live Voice Call HUD ("Bell Mode")

```
┌─────────────────────────────────────────────────────────────────┐
│ 09:41                                              🟢   ⚙️       │ ◄── Green Active Mic Pill & Settings
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
│                              ✴️                                 │ ◄── Pulsing Starburst Mark
│                       (Pulsing Wave)                            │
│                                                                 │
│                                                                 │
│                     "Hold tight, connecting…"                   │ ◄── Realtime Serif Caption (20sp)
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
│                                                                 │
│         [ + ]                ( 🎙️ )                [ ✕ ]        │ ◄── Call HUD Controls
│      Attach Context       Hold / Mute Mic        End Call (White│
│                                                    Filled Pill) │
│                         Sonnet 5 · Normal ▾                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Ambient Canvas**: `#141413` full bleed.
* **Live Status Indicator**: Green pill `#17A34A` in status bar showing active microphone capture.
* **Center Starburst Animation**: Radial scale pulse synchronized with incoming audio amplitude.
* **End Call Button**: Circle diameter `56dp`, background `#FFFFFF`, icon `anthropicon_close` in `#141413`.

---

## Screen 5: "Add to Chat" Sheet & Transient Event Chips

```
┌─────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🔊 Voice chat ended · 2s            👍  👎              ✕   │ │ ◄── Transient Feedback Chip
│ └─────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    ─── [ Drag Handle ] ───                      │
│                                                                 │
│   Add to chat                                             ✕     │ ◄── Sheet Header
│                                                                 │
│     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     │
│     │     📷      │     │     🖼️      │     │     📁      │     │ ◄── 3-Up Media Grid
│     │   Camera    │     │   Photos    │     │    Files    │     │
│     └─────────────┘     └─────────────┘     └─────────────┘     │
│                                                                 │
│   ───────────────────────────────────────────────────────────   │
│   🌐  Web search                                    [  (●)  ]   │ ◄── Toggle ON (#3898EC)
│   🕒  Memory                                        [  (●)  ]   │ ◄── Toggle ON
│   📁  Add to project                                 None   ›   │ ◄── Project Picker Drill-in
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Transient System Feedback Chip**: In-stream message card `#1F1E1C`, rounded `16dp`, collect thumbs up/down rating on audio latency/transcription.
* **3-Up Media Tiles**: Rounded squares `72 x 72 dp`, background `#242321`, centered stroke icon + label (`12sp`).

---

## Screen 6: Connectors & Vault Discovery

```
┌─────────────────────────────────────────────────────────────────┐
│  ‹   Connectors                                              ＋ │ ◄── Top Bar + Add Custom Connector
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🔍  Connector discovery                         [  (●)  ] │  │ ◄── Discovery Toggle Card
│  │     Allow Claude to discover and suggest tools            │  │
│  │     from configured remote servers and MCP.               │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│   CONNECTED SERVICES                                            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ 🤗  Hugging Face                                 5 tools  ›│  │ ◄── Service Row with Tool Count
│  ├───────────────────────────────────────────────────────────┤  │
│  │ 🐙  GitHub                                     Connected  ›│  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │ 📑  Notion                                     Connected  ›│  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │ 📁  Google Drive                               Connected  ›│  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Tool Count Badge**: Rounded capsule, background `#2A2927`, text `"5 tools"` in `#6A9BCC`.
* **Security Rule**: API keys and OAuth secrets are never rendered in UI; credentials are stored securely via Android `EncryptedSharedPreferences` / Android Keystore.

---

## Screen 7: Claude Code Remote (CCR) / Cowork Workspace

```
┌─────────────────────────────────────────────────────────────────┐
│  ‹   task-isolate-workspaces                      ⚡ Cloud   ⋮  │ ◄── Environment Badge
├─────────────────────────────────────────────────────────────────┤
│  Context Window: 24k / 200k (12%)                               │ ◄── Context Usage Meter
│  [████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] │
│                                                                 │
│  🌿  Branch: feat/docker-isolate-worker                         │ ◄── Git Branch Bar
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 💻 Terminal Execution: bash                                 │ │ ◄── Monospace Terminal Box
│ │ $ docker build -t hermes-worker:latest .                    │ │
│ │ Step 1/8 : FROM node:20-alpine                              │ │
│ │  ---> 4a5e2f7b8c9d                                          │ │
│ │ Successfully built 4a5e2f7b8c9d                             │ │
│ │ Status: 🟢 Exited 0 (4.2s)                                  │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🛡️ Permission Request: git push origin staging               │ │ ◄── Action Approval Card
│ │ Risk: Medium · Target: JishnuPG-tech/Hermes-x               │ │
│ │                                                             │ │
│ │       [ Deny ]                   [ Approve Once ]           │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Mode: [ Auto ▾ ]  │ Write a message or feedback…       [ ➔ ]│ │ ◄── Multi-Mode Composer
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Component Details
* **Context Meter**: Linear progress bar (`#3898EC` filled portion, `#2A2927` unfilled track).
* **Terminal Box**: Jetpack Compose ANSI surface, font `JetBrains Mono` `12sp`, background `#181715`.
* **Approval Card**: Border `#EAB308` (amber warning), buttons for one-shot vs persistent grant.
