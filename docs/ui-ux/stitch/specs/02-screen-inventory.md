# 02 — Screen Inventory (from your 10 screenshots)

## Screen 1 — Home / New Chat (Greeting)
- Status bar (time, connectivity, battery).
- Top bar: hamburger menu (left), ghost/ephemeral-chat icon (right).
- Centered: orange starburst logo mark, serif greeting "Good afternoon, Jishnu".
- Bottom docked composer card (rounded top corners, floats above a soft upsell strip):
  - Upsell strip: "Get more with Claude Pro" + "Upgrade to Pro" link (top of composer card).
  - Text input pill: "Chat with Claude…" placeholder.
  - Control row under input: "+" attach button (left), model selector pill "Sonnet 5 · Low" (effort/tier label), mic icon, and a filled white circular voice-mode button (right).
- Bottom Android nav bar (recents / home / back — this is a gesture-nav style hint row baked into screenshot, likely OS chrome not app chrome).

## Screen 2 — Side Drawer (Navigation)
- Header: serif "Claude" wordmark, top-right close/overflow area.
- Primary nav list (icon + label): Chats, Projects, Code, Artifacts.
- Divider.
- **Pinned** section: pinned conversation(s), e.g. "Resume optimization for ATS score i…" (truncated).
- Divider.
- **Recents** section: reverse-chronological chat list, e.g. "Free Docker hosting platforms 24/7", "Untitled", "Plan execution", "Greeting exchange".
- Footer bar: user avatar (circle, initial "J", brand-orange fill) bottom-left; "+ New chat" pill button bottom-right (white fill, dark text — the one high-contrast CTA on this screen).

## Screen 3 — Settings (scrolled to middle)
- Top bar: hamburger (left), centered serif "Settings" title, info "i" icon (right).
- Grouped list sections (rounded-rect row groups on dark surface):
  - Connectors group: "…connected" (truncated top), "Permissions" (android-robot icon).
  - Appearance group: "Color mode → System", "Font style → Default", "Voice" (each row = icon + label + current value beneath, chevron implied).
  - "Haptic feedback" row with an active (blue) toggle switch.
  - Behavior group: "Notifications", "Time & focus", "Privacy", "Sharing".
  - Destructive isolated row: "Log out" (red/coral text + icon), visually separated at the bottom.

## Screen 4 — "Add to chat" bottom sheet + inline system message
- Above the sheet: an inline system chip in the chat feed — "Voice chat ended · 2s" with thumbs-up/down feedback icons and a dismiss "×", i.e. a **transient system-event card**, feedback-collecting.
- Home screen visible behind (scrim-dimmed).
- Bottom sheet ("Add to chat"), drag handle, title, close "×":
  - 3-up icon grid: Camera / Photos / Files (each: circular icon bg + label).
  - List rows below with trailing toggle switches: "Web search" (globe icon, ON), "Memory" (restore/history icon, ON).
  - Partially visible next row: "Add to project → None" (tray icon) — sheet is scrollable, cut off with pagination arrows visible at very bottom (this looks like a carousel/pager control, possibly onboarding-tip arrows overlaying the sheet).

## Screen 5 — Settings → Account (top of Settings, scrolled up)
- Account row: email "jishnupg2005@gmail.com" + "Free" plan pill badge.
- Upsell card (elevated surface, rounded): "Want more Claude?" headline, body copy, white pill "Upgrade" button.
- List rows: "Profile" (person icon), "Billing" (dollar-in-circle icon).
- Second group: "Capabilities → 5 enabled" (sliders icon), "Connectors → 1 connected" (plug icon), "Permissions" (android icon).
- Third group starts: "Color mode → System".
- **Pattern:** every settings row that has a persistent sub-state shows that state as a muted-gray subtitle directly under the label (e.g. "5 enabled", "1 connected", "System") — this is the app's core settings-row anatomy.

## Screen 6 — "Select model" bottom sheet
- Home screen dimmed behind.
- Sheet: drag handle, centered title "Select model", no close X visible (dismiss via scrim/back-gesture).
- Model list, each row = Name + tier badge chip (e.g. "Pro or Max", "Pro") + one-line description, and a trailing checkmark on the selected row:
  - Fable 5.1 — "Pro or Max" badge — "For your toughest challenges"
  - Opus 5 — "Pro" badge — "For complex tasks"
  - Sonnet 5 — *(selected, text tinted blue, checkmark shown)* — "Most efficient for everyday tasks"
  - Haiku 4.5 — "Fastest for quick answers" (cut off)
- Row anatomy: bold title + inline tier chip on line 1, muted description on line 2, selection state = accent-colored text + trailing check icon (no radio buttons/checkboxes — text-color-shift is the selection affordance).

## Screen 7 — Settings → Connectors
- Back-chevron + centered "Connectors" title + top-right "+" add button.
- "Connector discovery" toggle card at top: icon, title, 3-line description, trailing toggle (ON) — same card pattern as the account-upsell card (elevated surface, larger than a normal row).
- Connector rows: icon (service logo, e.g. Hugging Face, Google Drive), name, trailing indicator — either a numeric badge (blue circle "5" = tools/items available) or an external-link/expand icon (for not-yet-configured or link-out connectors). Duplicate near-identical entries visible ("Hugging Face", "Huggingfac", "Huggingface") suggesting either search-typeahead results or multiple partial matches.

## Screen 8 — Voice settings
- Back chevron + centered "Voice settings" title.
- Horizontal carousel of voice-personality cards (only "Rounded" fully visible, neighbor peeking left), each card = large rounded-square swatch/avatar + name label, centered in a full-bleed dark card.
- Page-dot indicator below carousel (5 dots, current = 4th/5th filled solid).
- Below carousel: two settings rows — "English (United Kingdom)" with a "BETA" chip + chevron; "Pace → Normal" + chevron.

## Screen 9 — Live Voice Call (in-call)
- Minimal full-screen dark canvas — almost entirely empty/ambient (this is the "listening" state).
- Top-left: time. Top-right: green filled mic pill (call/mic active indicator).
- Top-right below status bar: gear/settings icon (circular dark button).
- Center: orange starburst logo (now static "idle" — likely animates/pulses live), serif status text "Hold tight, connecting…".
- Bottom control cluster: large circular mic button (center, primary), flanked by "+" (left) and model-switcher pill "Sonnet ⌃⌄" (center-bottom) and a white filled "×" end-call button (right).
- This screen swaps the standard composer for a **call HUD**: same bottom-row skeleton (+, model pill, mic) as the text composer, but adds a dedicated hang-up button and a connecting/status caption where the text input normally sits.

## Screen 10 — Settings → Capabilities
- Back chevron + centered "Capabilities" title.
- List of toggle-only rows (icon + title + 2–4 line description + trailing toggle), no chevrons since these are direct on/off switches, not drill-ins:
  - Web search — ON (blue)
  - Artifacts — ON but toggle rendered dim/disabled-looking ("Required by code execution" — a **locked-on dependency state**, toggle shown in a muted/desaturated blue to signal it can't be turned off independently)
  - Inline visualizations — "BETA" chip, ON
  - Code execution and file creation — ON
- **Pattern:** this screen establishes a "dependent toggle" affordance — when one capability requires another, the dependency's switch is shown visually disabled/locked rather than hidden.
