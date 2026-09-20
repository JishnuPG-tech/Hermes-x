# 01 — Design Tokens

## Color

### Dark theme (your screenshots are all dark mode) — Observed + Brand token
| Token | Hex | Where seen |
|---|---|---|
| App background | `#141413` (Anthropic's "near-black," olive-warm, not pure black) | Every screen background |
| Elevated surface (bottom sheets, "Add to chat", "Select model", cards) | `#1f1e1c`–`#242321` (dark warm gray, slightly lighter than bg) | Model picker sheet, Add-to-chat sheet, settings rows |
| Row / pill background | `#2a2927` approx | Settings list items, "Chat with Claude…" input pill |
| Primary text | `#faf9f5` (warm off-white) | Headings, body |
| Secondary/muted text | `#b0aea5` (mid gray) | "System", "Default", "Free" tag text, timestamps |
| Brand accent (logo, active states) | `#d97757` / `#c96442` terracotta-coral | Starburst logo, avatar circle "J", selected model checkmark tint on sheet header |
| Selected/active text accent (blue-leaning in your screenshots) | `#6a9bcc`-family blue | "Sonnet 5" selected row text + checkmark in model picker |
| Toggle ON | `#3898ec`/blue | Web search, Memory, Haptic feedback, Inline visualizations toggles |
| Toggle knob | white `#ffffff` | same toggles |
| Destructive / logout | warm red `#e2726e`-ish | "Log out" row |
| Live voice call state | green mic pill `#22c55e`-ish | Voice call top-right mic icon |
| Divider | `#302f2c` hairline | Between settings rows |

### Light theme — Brand token (not in your screenshots, inferred from brand system)
| Token | Hex |
|---|---|
| Canvas | `#faf9f5` |
| Parchment secondary bg | `#f5f4ed` / `#e8e6dc` |
| Primary text | `#141413` |
| Border | `#f0eee6` |

### Semantic
| Token | Hex | Use |
|---|---|---|
| Success | `#17a34a`/green | connected states |
| Warning | `#eab308` | — |
| Error | `#b53333` | error banners |
| Focus ring | `#3898ec` blue | the one deliberately "cool" color, used for focus/selection only |

## Typography — Observed + Brand token
- **Display/greeting headline** ("Good afternoon, Jishnu", "Claude" wordmark on drawer, "Settings" title): a warm **serif**, editorial in feel — matches Anthropic's brand serif ("Copernicus"/"Anthropic Serif", fallback Georgia/Times). Large size (~28–34sp), regular weight, generous letter-spacing, warm-white color.
- **Body / UI text** (settings labels, chat input placeholder, sheet rows): a **humanist sans-serif** (Anthropic Sans, fallback system default / Roboto on Android). Regular weight ~16sp for row titles, ~13–14sp for secondary description text under each row (e.g. "Allow Claude to execute code and create and edit docs…").
- **Small/meta text** (status bar, "2s" timestamp, badges like "BETA", "Pro or Max"): ~11–12sp, muted gray, sans, sometimes inside a pill/chip.
- Hierarchy is created almost entirely by **serif-for-moments-of-arrival / sans-for-everything-functional** — i.e. serif is reserved for the greeting screen and section titles ("Settings", "Voice settings", "Connectors", "Select model"), never for body copy or controls.

## Spacing & layout — Inferred (standard Android + observed proportions)
- Base unit: 4dp/8dp grid.
- Screen horizontal padding: ~20–24dp.
- Row height (settings list item): ~64–72dp single-line, ~88–110dp when a description line is present.
- Bottom sheet corner radius: large, ~28–32dp top corners only, with a drag-handle bar at top center.
- Pills (input bar, model badge, toggle track): fully rounded (`radius-pill`, 9999).
- Cards/rows: ~16–20dp corner radius.

## Iconography — Observed
- Line-style (stroke, not filled) icons throughout: hamburger menu (3 lines), ghost/incognito icon (top right of chat), settings gear, microphone, plus "+", chat bubble, project/tray, code `</>`, artifacts (interlocking rounded squares), globe (web search), shield (privacy), share arrow, android robot head (Permissions), moon (color mode / focus), "Aa" (font style), bell (notifications), dollar-circle (billing), sliders (capabilities), puzzle/plug (connectors).
- Icon weight: consistent thin stroke, ~1.5–2dp, rounded caps.
- Icon color: matches muted gray text color when inactive; app-accent or blue when representing an active/connected state.

## Motion tokens — Inferred
- Standard Android Material transition curves are the most likely base (fast-out-slow-in ease for sheet entrance, ~250–300ms).
- Bottom sheets slide up from bottom edge with a scrim fade-in behind.
- No visible skeuomorphic shadow system — depth communicated by flat surface-color steps (bg → surface → elevated), consistent with the "ring-based/no-shadow" pattern documented in Anthropic's web design system.
