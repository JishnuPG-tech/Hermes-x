# Hermes Stitch UI/UX Archive Manifest

The canonical generated Hermes Android UI/UX archive supplied for implementation is:

`hermes-stitch-ui.zip`

## Archive metadata

- Source: Google Stitch-generated Hermes/Claude Android UI/UX package supplied by the project owner
- Archive size: 8,189,577 bytes
- Files in archive: 148
- SHA-256: `e2b3bdf587e1cb60d944aab594c76ef1fbb7ca734a296b7647f00a56f5512513`

## Important contents

- `claude.design.md`
- `01_design_tokens.md`
- `02_screen_inventory.md`
- `03_components.md`
- `04_navigation_architecture.md`
- `05_motion_interaction.md`
- `hermes_android_apk_complete_architecture_screen_flow_map.md`
- `hermes_android_apk_complete_jetpack_compose_codebase.md`
- `hermes_claude_ui_upgrade_master_prompt.md`
- Hermes screen HTML files and reference screenshots
- Hermes vector logo assets

## Visual source-of-truth rule

The archive is the visual source of truth for the Hermes Android application. The implementation must preserve its UI/UX and add Hermes functionality inside it rather than redesigning the product.

## Repository implementation prompt

See:

`docs/ui-ux/HERMES_GOOGLE_AI_STUDIO_COMPLETE_ANDROID_APK_MASTER_PROMPT.md`

## Note

The GitHub connector used for repository operations supports UTF-8 repository files but does not provide a binary archive upload operation. Therefore this manifest records the exact canonical archive hash and contents, while the original ZIP remains available as the supplied implementation attachment. Do not substitute another archive without verifying its SHA-256 against the value above.
