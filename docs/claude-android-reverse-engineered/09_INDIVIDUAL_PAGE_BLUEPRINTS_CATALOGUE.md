# Claude Android — Individual Page Blueprints & Layout Specs

This document provides exact pixel/dp layout blueprints, component hierarchies, color bindings, and interaction states for every primary screen in the application.

---

## Page 1: Home / Greeting & Empty Composer View

```
┌────────────────────────────────────────────────────────────────────────┐
│ Dimensions: 390dp x 844dp (Viewport) • Canvas: #141413                │
├────────────────────────────────────────────────────────────────────────┤
│ [StatusBar] Height: 24dp • Text: #FAF9F5 (12sp Sans)                   │
│                                                                        │
│ [TopBar] Height: 56dp • Margin: 16dp                                   │
│  ├─ [Icon] anthropicon_menu (24x24dp, #B0AEA5)                         │
│  └─ [Icon] anthropicon_ghost (24x24dp, #B0AEA5)                        │
│                                                                        │
│ [Center Hero] Pacing: Top 180dp                                        │
│  ├─ [LogoMark] Starburst SVG (48x48dp, Fill: #D97757)                  │
│  ├─ [Spacer] Height: 24dp                                              │
│  ├─ [Headline] "Good afternoon, Jishnu"                                │
│  │   • Copernicus Serif, 32sp, Weight 400, Track -0.8px, #FAF9F5       │
│  └─ [Subtitle] "" (Clean breathing space)                              │
│                                                                        │
│ [Bottom Composer Card] Docked • Background: #1F1E1C • TopRadius: 24dp │
│  ├─ [UpsellStrip] Height: 36dp • Padding: 16dp horizontal              │
│  │   ├─ [Text] "✨ Get more with Claude Pro" (#B0AEA5, 12sp)           │
│  │   └─ [TextLink] "Upgrade to Pro ↗" (#D97757, 12sp Bold)            │
│  │                                                                     │
│  ├─ [InputPill] Height: 48dp • Margin: 12dp • Radius: 9999dp          │
│  │   • Background: #2A2927 • Border: None                              │
│  │   • Placeholder: "Chat with Claude…" (#7B7974, 15sp Sans)           │
│  │                                                                     │
│  └─ [ControlRow] Height: 48dp • Padding: 12dp horizontal               │
│      ├─ [Button] Attach "+" (32x32dp, Background: #242321, #FAF9F5)    │
│      ├─ [Chip] Model Selector "Sonnet 5 · Low ▾" (Height: 32dp, #242321│
│      ├─ [Spacer] Weight 1.0                                            │
│      ├─ [Button] Voice Mode Mic (36x36dp, #B0AEA5)                     │
│      └─ [Button] Voice Mode FAB (44x44dp, #FFFFFF Circle, #141413 Icon)│
└────────────────────────────────────────────────────────────────────────┘
```

---

## Page 2: Active Chat Conversation & Tool Stream

```
┌────────────────────────────────────────────────────────────────────────┐
│ Dimensions: 390dp x 844dp • Canvas: #141413                            │
├────────────────────────────────────────────────────────────────────────┤
│ [TopBar] Height: 56dp • Background: #141413 (Sticky)                   │
│  ├─ [Button] Menu (24dp)                                               │
│  ├─ [Title] "Optimize SQLite performance" (16sp StyreneB, #FAF9F5)     │
│  └─ [Button] Share / Artifacts (24dp)                                  │
│                                                                        │
│ [Conversation Stream] LazyColumn • Padding: 16dp                       │
│                                                                        │
│  ┌─ [User Message Bubble] Align: End • MaxWidth: 85%                   │
│  │   • Background: #242321 • Radius: 18dp • Padding: 14dp              │
│  │   • Text: "Can you analyze why my database is locking?"             │
│  │     (15sp Sans, #FAF9F5)                                            │
│  └───────────────────────────────────────────────────────────────────  │
│                                                                        │
│  ┌─ [Assistant Response Stream] Align: Start • Width: 100%             │
│  │   ├─ [AvatarMark] Small Spark (20x20dp, #D97757)                    │
│  │   │                                                                 │
│  │   ├─ [Thinking Disclosure Box] Background: #1F1E1C • Radius: 12dp   │
│  │   │   ├─ [Header] "Thought for 4 seconds ▾" (#B0AEA5, 13sp)         │
│  │   │   └─ [Body] "Analyzing SQLite write concurrency lockups..."     │
│  │   │             (13sp Monospace, #7B7974)                           │
│  │   │                                                                 │
│  │   ├─ [Text Stream] "SQLite requires WAL mode enabled for..."        │
│  │   │   • 15sp StyreneB Sans, Line-height 24sp, Color: #FAF9F5        │
│  │   │                                                                 │
│  │   ├─ [Tool Execution Card] Background: #181715 • Radius: 12dp       │
│  │   │   ├─ [Header] "💻 Ran bash command: sqlite3 db.db 'PRAGMA WAL;'"│
│  │   │   └─ [Output] "wal" (12sp JetBrains Mono, #3FC97D)              │
│  │   │                                                                 │
│  │   └─ [ActionRow] Copy • Thumbs Up • Thumbs Down • Retry (16dp icons)│
│  └───────────────────────────────────────────────────────────────────  │
│                                                                        │
│ [Docked Input Bar] Height: 64dp • Background: #1F1E1C                  │
│  └─ [InputPill] "Reply to Claude…"                                     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Page 3: Navigation Drawer

```
┌──────────────────────────────────────────────────┬─────────────────────┐
│ Dimensions: 320dp Width • Background: #1F1E1C    │ Scrim: #000000 60%  │
├──────────────────────────────────────────────────┴─────────────────────┤
│ [Drawer Header] Height: 72dp • Padding: 20dp                           │
│  ├─ [Wordmark] "Claude" (22sp Copernicus Serif, #FAF9F5)               │
│  └─ [CloseBtn] "✕" (24dp, #B0AEA5)                                     │
│                                                                        │
│ [Primary Destinations] Padding: 12dp horizontal                        │
│  ├─ [Row] 💬  Chats        (Height: 48dp • #FAF9F5 15sp)               │
│  ├─ [Row] 📁  Projects     (Height: 48dp • #B0AEA5 15sp)               │
│  ├─ [Row] 💻  Code         (Height: 48dp • #B0AEA5 15sp)               │
│  └─ [Row] 📄  Artifacts    (Height: 48dp • #B0AEA5 15sp)               │
│                                                                        │
│ [Divider] Height: 1dp • Color: #302F2C • Margin: 12dp horizontal       │
│                                                                        │
│ [Pinned Section]                                                       │
│  ├─ [Header] "📌 PINNED" (11sp Bold Caps, #7B7974)                     │
│  ├─ [Item] "Resume ATS Optimization" (Height: 40dp • #FAF9F5)          │
│  └─ [Item] "Agent OS Runtime Spec" (Height: 40dp • #FAF9F5)            │
│                                                                        │
│ [Recents Section] LazyColumn                                           │
│  ├─ [Header] "🕒 RECENTS" (11sp Bold Caps, #7B7974)                    │
│  ├─ [Item] "Docker runner isolation setup"                             │
│  ├─ [Item] "Jetpack Compose MVI reducer"                               │
│  └─ [Item] "Voice Assistant pipeline"                                  │
│                                                                        │
│ [Footer Row] Height: 68dp • BorderTop: #302F2C • Padding: 16dp         │
│  ├─ [Avatar] Circle 36dp (#D97757 fill, "J" in #FFF)                   │
│  ├─ [Name] "Jishnu" (15sp Sans, #FAF9F5)                               │
│  └─ [CTA] "＋ New" (White Pill Button, Height: 36dp, #141413 Text)     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Page 4: "Select Model" Bottom Sheet

```
┌────────────────────────────────────────────────────────────────────────┐
│ Dimensions: 390dp Width • Sheet Background: #1F1E1C • Radius: 32dp     │
├────────────────────────────────────────────────────────────────────────┤
│ [Drag Handle] Width: 32dp • Height: 4dp • Background: #7B7974          │
│ [Title] "Select model" (22sp Copernicus Serif, #FAF9F5)                │
│ [Spacer] Height: 16dp                                                  │
│                                                                        │
│ [Model List Group] Background: #242321 • Radius: 18dp • Margin: 16dp   │
│                                                                        │
│  ├─ [Row 1] Height: 76dp • Padding: 16dp                               │
│  │   ├─ [Line 1] "Fable 5.1"  [ Pro or Max ]                           │
│  │   │   • Title: 16sp Sans #FAF9F5 • Badge: #2A2927 (#B0AEA5 11sp)    │
│  │   └─ [Line 2] "For your toughest challenges" (#B0AEA5, 13sp)        │
│  ├─ [Divider] #302F2C                                                  │
│  ├─ [Row 2] Height: 76dp                                               │
│  │   ├─ [Line 1] "Opus 5"  [ Pro ]                                     │
│  │   └─ [Line 2] "For complex tasks" (#B0AEA5, 13sp)                   │
│  ├─ [Divider] #302F2C                                                  │
│  ├─ [Row 3 - SELECTED] Height: 76dp • Background: #2A2927 (Selected)  │
│  │   ├─ [Line 1] "Sonnet 5"  [ Default ]                           ✓   │
│  │   │   • Title: 16sp Sans #6A9BCC • Checkmark: #6A9BCC 20dp          │
│  │   └─ [Line 2] "Most efficient for everyday tasks" (#6A9BCC 70%)     │
│  ├─ [Divider] #302F2C                                                  │
│  └─ [Row 4] Height: 76dp                                               │
│      ├─ [Line 1] "Haiku 4.5"  [ Fast ]                                 │
│      └─ [Line 2] "Fastest for quick answers" (#B0AEA5, 13sp)           │
│                                                                        │
│ [Thinking Effort Section] Height: 84dp • Padding: 16dp                 │
│  ├─ [Label] "Thinking Effort" (14sp Sans #FAF9F5)                      │
│  └─ [Slider] Low ───────●────────────────────── High                   │
│       • Track: #2A2927 • ActiveTrack: #6A9BCC • Thumb: #FFFFFF         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Page 5: Live Voice HUD ("Bell Mode")

```
┌────────────────────────────────────────────────────────────────────────┐
│ Dimensions: 390dp x 844dp • Full Bleed Canvas: #141413                 │
├────────────────────────────────────────────────────────────────────────┤
│ [Top Row] Height: 56dp • Padding: 20dp                                 │
│  ├─ [Time] "09:41" (12sp #FAF9F5)                                      │
│  ├─ [MicStatusPill] Green Capsule 24x12dp (#17A34A)                    │
│  └─ [SettingsGear] Circle 36dp (#242321, Icon: #B0AEA5)                │
│                                                                        │
│ [Center Ambient Stage] Pacing: Center Screen                           │
│  ├─ [Pulsing Starburst Avatar]                                         │
│  │   • Baseline Size: 72x72dp • Color: #D97757                         │
│  │   • Scale Animation: 1.0f ➔ 1.28f (Bound to Mic Audio Amplitude)   │
│  │   • Halo Blur: 24dp Radial Blur                                     │
│  │                                                                     │
│  └─ [Live Serif Caption] Height: 60dp • Padding: 24dp horizontal       │
│      • "Hold tight, connecting…" ➔ "Listening…" ➔ "Speaking…"          │
│      • Copernicus Serif, 20sp, Weight 400, Color: #FAF9F5              │
│                                                                        │
│ [Bottom Call HUD Controls] Height: 120dp • Margin: 24dp bottom         │
│  ├─ [Control Cluster]                                                  │
│  │   ├─ [Button] Attach Context "+" (44dp Circle, #242321)             │
│  │   ├─ [Button] Center Talk/Mute Mic (64dp Circle, #2A2927, Mic: #FFF)│
│  │   └─ [Button] End Call "✕" (44dp Circle, #FFFFFF Fill, #141413 Icon│
│  │                                                                     │
│  └─ [Model Pill Indicator] Height: 28dp • Radius: 9999dp • #242321     │
│      └─ [Text] "Sonnet 5 · Normal Pace ▾" (12sp #B0AEA5)               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Page 6: Settings Main Screen

```
┌────────────────────────────────────────────────────────────────────────┐
│ Dimensions: 390dp x 844dp • Canvas: #141413                            │
├────────────────────────────────────────────────────────────────────────┤
│ [TopBar] Height: 56dp • Title: "Settings" (22sp Copernicus Serif)      │
│                                                                        │
│ [Account Card] Background: #242321 • Radius: 20dp • Margin: 16dp       │
│  ├─ [EmailRow] "jishnupg2005@gmail.com"  [ Free ]                      │
│  └─ [UpsellBox] Background: #1F1E1C • Radius: 12dp • Margin: 12dp      │
│      ├─ [Headline] "Want more Claude?" (15sp Bold #FAF9F5)             │
│      └─ [Button] "Upgrade" (White Pill Button, Height: 32dp)           │
│                                                                        │
│ [Group 1: Capabilities & Connectors] Background: #242321 • Radius: 20dp│
│  ├─ [Row 1] ⚙️  Capabilities             5 enabled                   › │
│  ├─ [Row 2] 🔌  Connectors               1 connected                 › │
│  └─ [Row 3] 🤖  Permissions                                          › │
│                                                                        │
│ [Group 2: Appearance & Voice] Background: #242321 • Radius: 20dp       │
│  ├─ [Row 1] 🌓  Color mode               System                      › │
│  ├─ [Row 2] 🔤  Font style               Default                     › │
│  ├─ [Row 3] 🎙️  Voice                    Rounded                     › │
│  └─ [Row 4] 📳  Haptic feedback                                 [(●)] │
│                                                                        │
│ [Group 3: Governance] Background: #242321 • Radius: 20dp               │
│  ├─ [Row 1] 🔔  Notifications                                        › │
│  ├─ [Row 2] 🛡️  Privacy                                              › │
│  └─ [Row 3] 🔗  Sharing                                              › │
│                                                                        │
│ [Destructive Logout Card] Background: #242321 • Radius: 20dp           │
│  └─ [Row] 🚪  Log out (Text & Icon: #D93025 Red, 15sp Bold)            │
└────────────────────────────────────────────────────────────────────────┘
```
