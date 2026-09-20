# Hermes Stitch UI/UX Project Import

- **Project ID**: `10079330980620936979`
- **Project Title**: Claude Android UI Replica
- **Source**: [https://stitch.withgoogle.com/projects/10079330980620936979](https://stitch.withgoogle.com/projects/10079330980620936979)
- **Design Theme**: `Hermes-Claude-Mobile` (Dark Mode, Newsreader Serif + Inter Sans-serif, Roundness 8, Accent Coral `#c96442`, Background `#141413`)
- **Import Date**: 2026-09-19

---

## 1. Directory Structure

```text
docs/ui-ux/stitch/
├── README.md                                                   # This index
├── project.json                                                # Full Stitch project manifest, designTheme, and screen placement
├── screens.json                                                # Screen list and metadata
├── specs/                                                      # Architecture, design tokens & Jetpack Compose codebase specs
│   ├── 00-README.md
│   ├── 01-design-tokens.md
│   ├── 02-screen-inventory.md
│   ├── 03-components.md
│   ├── 04-navigation-architecture.md
│   ├── 05-motion-interaction.md
│   ├── claude.design.md
│   ├── DESIGN (1).md
│   ├── Hermes Android APK — Complete Architecture & Screen Flow Map.md
│   ├── Hermes Android APK — Complete Jetpack Compose Codebase.md
│   ├── Hermes_Claude_UI_Upgrade_Master_Prompt.md
│   └── plan.md
├── assets/                                                     # Official vector SVG brand assets
│   ├── Hermes Agent Official Vector Logo Mark (#c96442).svg
│   ├── Hermes Agent Logo White.svg
│   ├── Hermes Agent Black Hair White Face Logo.svg
│   ├── Hermes Agent Exact Logo.svg
│   ├── Hermes Agent Logo Vector.svg
│   └── hermesagent.svg
├── screens/                                                    # 27 HTML screen prototypes
│   ├── Claude Android - Home & Greeting.html
│   ├── Claude Android - Active Chat & Execution Sheet.html
│   ├── Claude Android - Add to Chat Sheet.html
│   ├── Claude Android - Navigation Drawer.html
│   ├── Claude Android - Select Model Sheet.html
│   ├── Claude Android - Voice Settings.html
│   ├── Claude Android - Live Voice Call.html
│   ├── Claude Android - Capabilities.html
│   ├── Claude Android - Settings Account.html
│   ├── Claude Android - Full-Screen Expanded Summary Sheet.html
│   ├── Hermes Android - Home & Greeting.html
│   ├── Hermes Android - Home & Greeting (White Logo).html
│   ├── Hermes Android - Welcome & Sign In.html
│   ├── Hermes Android - Welcome & Sign In (Exact Claude Match).html
│   ├── Hermes Android - Splash Screen.html
│   ├── Hermes Android - Splash Screen (Exact Claude Match).html
│   ├── Hermes Android - Active Chat & Execution Sheet.html
│   ├── Hermes Android - Live Voice Call.html
│   ├── Hermes Android - Full-Screen Summary Sheet.html
│   ├── Hermes - Live Chat & Task Progress.html
│   ├── Hermes - Autonomous Tasks.html
│   ├── Hermes - Server Terminal & Files.html
│   ├── Hermes - Connectors & Vault.html
│   ├── Hermes - Connectors & Vault Claude Match.html
│   ├── Hermes - Navigation Drawer.html
│   ├── Hermes - Claude Drawer Baseline Match.html
│   └── Hermes - Claude Chat Baseline Match.html
└── screenshots/                                                # 48 Reference screenshots and visual mockups
```

---

## 2. Design Tokens Summary

- **Primary Brand**: `#c96442` (Terracotta Coral)
- **Primary Coral**: `#d97757`
- **Canvas / Background**: `#141413`
- **Surface Card**: `#1f1e1c`
- **Surface Elevated**: `#2a2927`
- **Surface Soft Dark**: `#232220`
- **Hairline Border**: `#302f2c`
- **Text Ink / Primary**: `#faf9f5`
- **Text Muted**: `#b0aea5`
- **Text Secondary**: `#8e8b82`
- **Accent Blue**: `#3898ec`
- **Accent Green**: `#22c55e`
- **Accent Warning**: `#eab308`
- **Accent Danger**: `#e2726e`
- **Typography Display**: Newsreader / Copernicus Serif (28-34px)
- **Typography Interface**: Inter / Roboto Sans-serif (11-16px)
- **Typography Monospace**: JetBrains Mono (13px)
