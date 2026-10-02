# 03 — Component Specs

## 1. Settings row (core atom of the whole Settings surface) — Observed
```
[icon]  Label                              >
        Muted value / description
```
- Container: full-width, sits inside a rounded-rect group with siblings (grouped list, iOS/Material-hybrid style — adjacent related rows share one rounded container, hairline divider between them, gap between groups).
- Icon: 24dp line icon, muted-gray, left-aligned with fixed gutter.
- Label: sans, ~16sp, primary text color.
- Secondary line (optional): sans, ~13sp, muted gray — either a static description ("Allow Claude to execute code…") or a live state value ("System", "5 enabled", "1 connected").
- Trailing: either a chevron (drill-in), a toggle switch (direct on/off), or a numeric/status badge.
- States: default / pressed (row darkens slightly) / disabled (icon+text at lower opacity, toggle shown "locked").

## 2. Toggle switch — Observed
- Track: pill shape, OFF = dark-gray outline, ON = solid blue `#3898ec`.
- Knob: white circle, animates slide left↔right.
- Disabled/dependency-locked state: same ON track but desaturated (~50–60% opacity blue), non-interactive.

## 3. Bottom sheet — Observed (Add to chat, Select model)
- Full-width, anchored to bottom, rounded top corners only.
- Drag handle: short horizontal bar, centered, ~4dp height, top padding ~12dp.
- Header row: title centered, optional "×" close top-left, optional "+" top-right.
- Content: list rows or icon grid, standard row anatomy as above.
- Scrim: dims the screen behind to ~50–60% black.
- Dismiss: tap scrim, swipe down, or explicit "×".

## 4. Chat composer bar (home / in-conversation) — Observed
```
┌─────────────────────────────┐
│  Chat with Claude…           │   ← input pill, full width, rounded
├───────────────────────────────┤
│ [+]  [Model ▾]      [🎤] (●) │   ← control row
└─────────────────────────────┘
```
- Sits inside a rounded card that also hosts the "Upgrade to Pro" strip above it on free accounts.
- "+": circular icon button, opens Add-to-chat sheet.
- Model pill: rounded chip, "<Model name> · <effort tier>", tap opens Select-model sheet.
- Mic icon: outline, tap-to-dictate.
- Voice-mode button: filled white circle, distinct from the rest (highest-contrast element in the bar) — enters full-screen Voice Call.

## 5. Voice-call HUD — Observed
- Reuses the +/model-pill/mic row from the composer, but center mic button becomes large and primary, and an "×" end-call button (white filled circle) replaces the send/voice-mode affordance.
- Status caption above controls, center-screen, serif italic-weight-like treatment ("Hold tight, connecting…") — likely swaps to live captions once connected.
- Top-right green pill = system mic-active indicator (OS-level, not app-drawn) sitting above the app's own settings-gear button.

## 6. Inline system/event card (in chat feed) — Observed
- Compact horizontal card: icon, label + meta ("Voice chat ended · 2s"), thumbs-up/thumbs-down feedback icons, divider, "×" dismiss.
- Used for transient, non-conversational system events (call ended, tool completed, etc.) that still want lightweight feedback capture.

## 7. Upsell / promo card — Observed
- Elevated surface, larger corner radius than rows, contains: bold serif or bold-sans headline, 1–2 line body copy, single white pill CTA button.
- Seen twice with identical anatomy: "Get more with Claude Pro" (compact strip form) and "Want more Claude?" (full card form) — same component, two density variants.

## 8. Primary CTA button ("+ New chat", "Upgrade") — Observed
- Pill shape, solid white fill, dark text, plus-icon optional prefix.
- The *only* solid-white filled buttons in the whole app — reserved for the single highest-priority action per screen (new chat, upgrade). Everything else uses toggles, chips, or plain rows.

## 9. Model-tier badge chip — Observed
- Small rounded-rect chip, muted-navy/gray fill, small-caps-like label ("Pro or Max", "Pro", "BETA"), sits inline next to a title.

## 10. Avatar — Observed
- Circle, solid brand-accent orange fill, single-letter initial, white text — used for user identity (drawer footer).
