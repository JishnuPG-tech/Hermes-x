# 04 — Navigation / Information Architecture

## App map (from what's reachable in your screenshots)

```
Home (New Chat / Greeting)
├── ☰ Drawer
│   ├── Chats
│   ├── Projects
│   ├── Code
│   ├── Artifacts
│   ├── Pinned chats
│   ├── Recent chats  → tap → Conversation view (not captured)
│   ├── Account avatar → Settings (Account tab)
│   └── + New chat → Home (fresh)
│
├── Ghost icon (top-right) → likely "Incognito/temporary chat" toggle
│
├── Composer
│   ├── "+" → Add-to-chat sheet
│   │     ├── Camera / Photos / Files (attach)
│   │     ├── Web search (toggle)
│   │     ├── Memory (toggle)
│   │     └── Add to project → (picker, "None" default)
│   ├── Model pill → Select-model sheet
│   │     └── Fable 5.1 / Opus 5 / Sonnet 5 / Haiku 4.5 (select)
│   ├── Mic icon → dictation
│   └── Voice button (filled) → Voice Call (full-screen)
│         ├── gear icon → Voice settings
│         │     ├── Persona carousel (5 options, "Rounded" shown)
│         │     ├── Language ("English (UK)", BETA)
│         │     └── Pace ("Normal")
│         ├── "+" → (same Add-to-chat, presumably)
│         ├── Model pill (compact) → model switch mid-call
│         └── "×" → End call → returns to Home, drops an inline
│               system card "Voice chat ended" into the feed
│
└── Settings (reached via drawer avatar)
    ├── [Account block] email + plan badge, Upgrade card
    ├── Profile
    ├── Billing
    ├── Capabilities (5 enabled)
    │     ├── Web search (toggle)
    │     ├── Artifacts (locked ON — dependency of Code execution)
    │     ├── Inline visualizations — BETA (toggle)
    │     └── Code execution and file creation (toggle)
    ├── Connectors (1 connected)
    │     ├── Connector discovery (toggle)
    │     └── [service list: Hugging Face, Google Drive, …]
    ├── Permissions
    ├── Color mode (System)
    ├── Font style (Default)
    ├── Voice → Voice settings (see above)
    ├── Haptic feedback (toggle)
    ├── Notifications
    ├── Time & focus
    ├── Privacy
    ├── Sharing
    └── Log out
```

## Navigation patterns observed
1. **Drawer = primary nav**, reached only via hamburger (persistent top-left on Home) — no bottom tab bar. This is a single-stack, drawer-plus-sheets architecture, not tab-based.
2. **Bottom sheets, not full pages**, for anything transient/contextual: attach options, model selection, add-to-project. Full pages (`Settings`, `Connectors`, `Voice settings`, `Capabilities`) are used for anything with depth/multiple sub-settings — reached by drilling in with a back-chevron top-left, never a bottom sheet.
3. **Settings is a flat drill-down, not nested tabs**: Settings → Connectors is a full push navigation (back chevron), same for → Capabilities, → Voice settings. Depth appears to be capped at 2 levels (Settings → sub-page), no sub-sub-pages observed.
4. **The composer's bottom row (+ / model / mic) is a persistent control cluster** reused identically across Home, in-conversation (implied), and Voice Call — this is the one piece of "chrome" that survives every context.
5. **Voice Call is a modal full-screen takeover**, not a normal push — closing it (×) drops you back to Home/conversation and leaves a receipt (system card) rather than silently discarding the session.
6. **Free-tier upsell is inserted as content, not a banner/interstitial** — it appears as a normal-looking card inline in Settings and as a strip inside the composer card, never as a blocking modal.
