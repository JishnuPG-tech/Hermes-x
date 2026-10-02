# Claude Android App — UI/UX Kit
Compiled from: (1) public research on Anthropic's design system/brand tokens, and (2) direct inspection of your 10 uploaded screenshots of the Claude Android app (home/greeting screen, side drawer, Settings → main, Settings → account/plan, Settings → Capabilities, Settings → Connectors, model picker sheet, "Add to chat" sheet, Voice settings, live Voice call screen).

**Files in this kit**
1. `01-design-tokens.md` — color palette, typography, spacing, radius, elevation
2. `02-screen-inventory.md` — every screen observed in your screenshots, laid out element-by-element
3. `03-components.md` — reusable component specs (buttons, sheets, list rows, toggles, chat input bar)
4. `04-navigation-architecture.md` — information architecture / app map / navigation flows
5. `05-motion-interaction.md` — animation & interaction patterns (known + inferred)

**Caveat (important, read this):** Anthropic has not published a public Claude *mobile app* design system/Figma kit. What exists publicly is Anthropic's **brand identity** (colors, fonts, logo) and community-reconstructed "design system" clones (shadcn.io, open-design.ai, skills.sh) built by reverse-engineering claude.ai's *web* app — these are close but not official, and not mobile-specific. So this kit blends:
- ✅ **Confirmed from your screenshots** — marked "Observed"
- 🟡 **Confirmed from Anthropic's published brand/web tokens, likely shared with the app** — marked "Brand token"
- ⚪ **Inferred/standard Android-platform convention, not directly verifiable** — marked "Inferred"

Use the "Observed" items as ground truth; treat "Inferred" items as a reasonable starting point to redesign/rebuild from, not as leaked spec.
