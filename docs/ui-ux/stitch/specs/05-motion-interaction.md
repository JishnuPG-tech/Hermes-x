# 05 — Motion & Interaction Patterns

⚠️ None of your screenshots are animated (they're static frames), so nothing here is directly "Observed" — everything is inferred from (a) what state each screenshot implies, (b) standard Android/Material motion conventions, and (c) how Anthropic's web app is known to animate. Treat this file as a **build spec proposal**, not a leaked spec.

## Implied states → likely transitions
| From screenshot | Implied motion |
|---|---|
| Starburst logo, static on Home; static-but-labeled "connecting…" on Voice Call | Logo likely has an idle **subtle rotation/shimmer loop** at rest, and a faster pulse/spin while "listening"/"thinking" — spinning-asterisk is Anthropic's known "thinking" motif on the web app, reused here. |
| Bottom sheets (Add to chat, Select model) | Standard Material bottom-sheet enter/exit: slide up from off-screen + scrim fade-in, ~250–300ms ease-out on entry, faster ease-in on dismiss; drag-to-dismiss with rubber-banding. |
| Toggle switches | Knob slide ~150–200ms, track color cross-fade. |
| "Voice chat ended · 2s" card | Suggests a **toast-like insert animation** (slide/fade into the feed) that also carries a live-updating relative timestamp ("2s" ticking upward), then eventually collapses/dismisses or gets left as a permanent low-emphasis log entry. |
| Model picker row selection | Text color cross-fades to accent blue + checkmark fades/scales in — no radio-button motion, purely color+icon. |
| Voice call connecting → connected | "Hold tight, connecting…" caption likely cross-fades to live captioning or an audio-reactive waveform once connected (the idle mic icon at bottom center is the natural anchor for a waveform/amplitude animation). |
| Drawer open | Standard Android nav-drawer slide-in from left edge with scrim fade, content beneath dims. |
| Settings drill-in (Settings → Connectors/Capabilities/Voice settings) | Horizontal push transition (new screen slides in from right, previous slides left/dims slightly) — standard Android forward-navigation, back-chevron reverses it. |

## Interaction conventions worth carrying into a rebuild
- **Feedback capture is lightweight and inline** (thumbs up/down directly on system-event cards) rather than a separate survey/modal.
- **Dependency-locked toggles** (Artifacts, locked ON because Code execution needs it) should visibly disable + likely show a tooltip/toast on tap-attempt explaining why, rather than silently doing nothing.
- **Selection affordance is color, not shape** — no checkboxes/radio circles anywhere observed; selected state = accent-colored label (+ checkmark only in the model list). Keep this consistent if extending the kit.
- **Every screen keeps the top status bar area calm** — no colored app bars; the dark canvas bleeds under the status bar everywhere, with just icon-row chrome floating on top.
