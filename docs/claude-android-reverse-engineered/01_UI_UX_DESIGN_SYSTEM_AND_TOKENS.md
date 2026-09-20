# Claude Android — UI/UX Design System, Typography & Token Specifications

---

## 1. Design Intent & Visual Philosophy

Anthropic’s Claude interface is built around **warmth, calm pacing, and literary editorial elegance**:
* **Warm Canvas**: Anchored on an olive-tinted near-black `#141413` in dark mode, and cream `#FAF9F5` in light mode. Pure `#000000` and `#FFFFFF` are deliberately avoided for base surfaces.
* **Terracotta Brand Voltage**: Signature terracotta-coral `#D97757` used purposefully on logo marks, user identity avatars, and primary CTAs.
* **The Serif Rule ("Moments of Arrival")**: Serif display typography (`Copernicus` / `Tiempos Headline`) is reserved strictly for greeting headlines, screen arrival titles, and dialog headers. All functional UI, lists, and form fields use a humanist sans-serif.
* **Restrained Chrome**: Flat surface-color depth stepping (`bg` ➔ `surface` ➔ `elevated`) without skeuomorphic drop shadows.

---

## 2. Complete Token System

### 2.1 Dark Mode Palette (Primary Mobile Experience)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               DARK MODE COLOR PALETTE                                  │
├─────────────────────────┬─────────────┬────────────────────────────────────────────────┤
│ Token Name              │ Hex Code    │ Usage / Placement                              │
├─────────────────────────┼─────────────┼────────────────────────────────────────────────┤
│ background-canvas       │ #141413     │ Primary app canvas background (warm olive-black│
│ surface-elevated        │ #1F1E1C     │ Bottom sheets, modal overlays, card surfaces   │
│ surface-card            │ #242321     │ Grouped settings cards, connector cards        │
│ surface-pill-input      │ #2A2927     │ Composer input bar, list row item background   │
│ surface-press-state     │ #33322E     │ Active touch highlight on list rows and buttons│
│ text-primary            │ #FAF9F5     │ Warm off-white primary text and headings       │
│ text-secondary-muted    │ #B0AEA5     │ Mid-gray subtitles, timestamps, descriptions   │
│ text-tertiary-dim       │ #7B7974     │ Disabled captions, placeholders, subtle meta   │
│ brand-coral-primary     │ #D97757     │ Starburst mark, user avatar circle, primary CTA│
│ brand-coral-active      │ #C96442     │ Pressed state for coral CTA buttons            │
│ brand-coral-disabled    │ #E6DFD8     │ Inactive / disabled coral CTA state            │
│ accent-selection-blue   │ #6A9BCC     │ Selected model text, selected checkmark tint   │
│ toggle-active-blue      │ #3898EC     │ Active toggle switches, focus outlines         │
│ toggle-disabled-blue    │ #2D5882     │ Locked-on dependency toggle track              │
│ status-success-green    │ #17A34A     │ Active call mic pill, passing test indicators  │
│ status-warning-amber    │ #EAB308     │ Warning alerts, rate-limit warnings            │
│ status-danger-red       │ #D93025     │ Log out button, delete session, fatal errors   │
│ hairline-divider        │ #302F2C     │ List row dividers, bottom sheet top borders    │
└─────────────────────────┴─────────────┴────────────────────────────────────────────────┘
```

---

### 2.2 Light Mode Palette (System Light Token Reference)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               LIGHT MODE COLOR PALETTE                                 │
├─────────────────────────┬─────────────┬────────────────────────────────────────────────┤
│ Token Name              │ Hex Code    │ Usage / Placement                              │
├─────────────────────────┼─────────────┼────────────────────────────────────────────────┤
│ background-canvas       │ #FAF9F5     │ Warm cream light canvas                        │
│ bg100                   │ #F9F9F7     │ Secondary light surface                        │
│ bg300                   │ #F0EFEC     │ Light cards, elevated sheets                   │
│ text100                 │ #131313     │ Primary high-contrast light mode text          │
│ text300                 │ #383835     │ Secondary body text                            │
│ text500                 │ #7B7974     │ Muted captions, metadata                       │
│ claudeManilla           │ #EBDBBC     │ Cream highlights and warm badges               │
│ oat                     │ #E3DACC     │ Light borders and neutral containers           │
│ hairline-divider-light  │ #E6DFD8     │ Light mode separators                          │
└─────────────────────────┴─────────────┴────────────────────────────────────────────────┘
```

---

### 2.3 Typography Scale & Font Rules

```
┌───────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     TYPOGRAPHY TOKENS                                             │
├───────────────────┬───────────────────────────────┬──────┬─────────┬─────────┬────────────────────┤
│ Token Name        │ Font Family                   │ Size │ Weight  │ Track   │ Leading (Line Ht)  │
├───────────────────┼───────────────────────────────┼──────┼─────────┼─────────┼────────────────────┤
│ display-greeting  │ Copernicus / Tiempos Headline │ 32sp │ 400     │ -0.8px  │ 38sp (1.18)        │
│ display-title     │ Copernicus / Tiempos Headline │ 26sp │ 400     │ -0.5px  │ 32sp (1.23)        │
│ section-header    │ Copernicus / Tiempos Headline │ 20sp │ 400     │ -0.2px  │ 26sp (1.30)        │
│ title-medium      │ StyreneB / Inter / Sans       │ 18sp │ 500     │ 0.0px   │ 24sp (1.33)        │
│ body-large        │ StyreneB / Inter / Sans       │ 16sp │ 400     │ 0.0px   │ 24sp (1.50)        │
│ body-medium       │ StyreneB / Inter / Sans       │ 14sp │ 400     │ 0.0px   │ 20sp (1.43)        │
│ label-strong      │ StyreneB / Inter / Sans       │ 14sp │ 600     │ +0.1px  │ 18sp (1.28)        │
│ subtitle-muted    │ StyreneB / Inter / Sans       │ 13sp │ 400     │ 0.0px   │ 18sp (1.38)        │
│ badge-caps        │ StyreneB / Inter / Sans       │ 11sp │ 600     │ +1.2px  │ 14sp (1.27)        │
│ caption           │ StyreneB / Inter / Sans       │ 12sp │ 400     │ 0.0px   │ 16sp (1.33)        │
│ code-terminal     │ JetBrains Mono / Monospace    │ 13sp │ 400     │ 0.0px   │ 20sp (1.54)        │
└───────────────────┴───────────────────────────────┴──────┴─────────┴─────────┴────────────────────┘
```

#### Typography Application Heuristics
1. **The Serif Arrival Heuristic**:
   * Use `Copernicus / Serif` when the user *arrives* at a new state: Home greeting, Settings screen title, Select Model sheet title, Voice Call connecting caption.
   * **Never** use Serif in text input fields, chat bubbles, list rows, code blocks, or button labels.
2. **The Subtitle State Pattern**:
   * Every settings list row with a persistent setting displays its current value as a `subtitle-muted` line directly below the `body-large` title (e.g. *"Color mode"* ➔ *"System"*, *"Capabilities"* ➔ *"5 enabled"*).

---

### 2.4 Spacing & Geometry Tokens

#### 4dp / 8dp Base Spacing Grid
* `spacing-xxs`: `4dp`
* `spacing-xs`: `8dp`
* `spacing-sm`: `12dp`
* `spacing-md`: `16dp` (Standard horizontal inner card padding)
* `spacing-lg`: `20dp`–`24dp` (Screen edge margin)
* `spacing-xl`: `32dp` (Section pacing)
* `spacing-xxl`: `48dp` (Hero pacing)

#### Corner Radii Scale
* `radius-xs`: `4dp` (Small inline chips, code tags)
* `radius-sm`: `8dp` (Toast notifications, tooltip containers)
* `radius-md`: `12dp` (Buttons, media preview boxes)
* `radius-lg`: `18dp`–`22dp` (Grouped settings cards, tool cards)
* `radius-sheet`: `28dp`–`32dp` (Top corners of modal bottom sheets)
* `radius-pill`: `9999dp` (Composer text bar, model selector pills, badge tags, FABs)

---

## 3. Anthropicon Vector Iconography Catalogue

All 292 icons follow a unified specification:
* **Stroke Width**: `1.5dp` (standard), `2.0dp` (bold state)
* **Cap & Join**: `strokeLineCap="round"`, `strokeLineJoin="round"`
* **Default ViewBox**: `24 x 24 dp` (Small variant: `16 x 16 dp`)
* **Color Binding**: Inherits `LocalContentColor.current` (`#FAF9F5` active, `#B0AEA5` inactive)

### Key Functional Icon Clusters
```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                            ANTHROPICON ICON CLUSTERS                                   │
├───────────────────┬────────────────────────────────────────────────────────────────────┤
│ Category          │ Extracted Icon Resource Identifiers                                │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ Agents & Tasks    │ anthropicon_agent, anthropicon_agents_simple, anthropicon_dispatch │
│                   │ anthropicon_computer, anthropicon_checklist, anthropicon_create     │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ Code & CLI        │ anthropicon_code, anthropicon_code_block, anthropicon_command_line │
│                   │ anthropicon_branch, anthropicon_atom, anthropicon_disconnect       │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ Artifacts & Docs  │ anthropicon_artifacts, anthropicon_artifact_file, anthropicon_book │
│                   │ anthropicon_book_text, anthropicon_cards, anthropicon_clipboard    │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ Media & Audio     │ anthropicon_camera, anthropicon_attachment, anthropicon_microphone │
│                   │ anthropicon_speaker, anthropicon_browse, anthropicon_play          │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ Navigation & UI   │ anthropicon_arrow_left, anthropicon_caret_down, anthropicon_close   │
│                   │ anthropicon_add_circle, anthropicon_check_circle, anthropicon_copy │
└───────────────────┴────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Anatomy & Styling Specifications

### 4.1 Floating Composer Card (Home Screen)
```
┌─────────────────────────────────────────────────────────────────┐
│ ✨ Get more with Claude Pro                    Upgrade to Pro ↗ │ ◄── Upsell Strip (12sp)
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │  Chat with Claude…                                          │ │ ◄── Input Pill (#2A2927)
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  [ + ]    [ Sonnet 5 · Low ▾ ]                ( 🎙️ )   [ (●) ]   │ ◄── Control Row
│  Attach       Model Selector Pill               Mic   Voice FAB │
└─────────────────────────────────────────────────────────────────┘
```
* **Card Surface**: Background `#1F1E1C`, rounded top corners `24dp`.
* **Input Pill**: Background `#2A2927`, height `48dp`, pill radius `9999dp`, text `#FAF9F5`.
* **Voice FAB**: High-contrast white circular button `#FFFFFF` with dark mic icon `#141413`.

### 4.2 Grouped Settings Card Anatomy
```
┌─────────────────────────────────────────────────────────────────┐
│  ⚙️  Capabilities                                                │
│      5 enabled                                                › │ ◄── Row 1 (Height: 72dp)
├─────────────────────────────────────────────────────────────────┤
│  🔌  Connectors                                                 │
│      1 connected                                              › │ ◄── Row 2 (Height: 72dp)
├─────────────────────────────────────────────────────────────────┤
│  🤖  Permissions                                              › │ ◄── Row 3 (Height: 56dp)
└─────────────────────────────────────────────────────────────────┘
```
* **Container**: Background `#242321`, corner radius `20dp`, margin `16dp`.
* **Dividers**: `#302F2C` hairline `1dp`, inset by `56dp` from the left (aligned with text, past icon).

### 4.3 Dependent Toggle Pattern (Capabilities Screen)
```
┌─────────────────────────────────────────────────────────────────┐
│  📄  Artifacts                                      [  (●)  ]   │ ◄── Dimmed/Locked Toggle (#2D5882)
│      Required by code execution                                 │ ◄── Dependency Explainer Subtitle
└─────────────────────────────────────────────────────────────────┘
```
* **Disabled Toggle Styling**: Track rendered in desaturated dark blue `#2D5882` with white knob; touch interactions intercepted with explanation toast.
