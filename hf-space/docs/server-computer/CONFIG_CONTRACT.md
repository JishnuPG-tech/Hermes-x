# Configuration Contract

## Required paths

```text
JARVIS_DATA_DIR=/data/jarvis
HERMES_HOME=/data/jarvis/hermes
JARVIS_PROJECTS_DIR=/data/jarvis/projects
JARVIS_WORKSPACES_DIR=/data/jarvis/workspaces
JARVIS_DB_DIR=/data/jarvis/databases
JARVIS_ARTIFACTS_DIR=/data/jarvis/artifacts
JARVIS_LOG_DIR=/data/jarvis/logs
JARVIS_STATE_DIR=/data/jarvis/agent-state
```

## Optional integrations

```text
OMNIROUTE_URL
GITHUB_INTEGRATION
NOTION_INTEGRATION
OBSIDIAN_BRIDGE_URL
STT_PROVIDER
TTS_PROVIDER
```

## Voice defaults

```yaml
voice:
  enabled: true
  transport: websocket
  primary_tts: kokoro
  fallback_tts: edge_tts
  first_audio_target_ms: 5000
  hard_max_ms: 10000
  fallback_trigger_ms: 4000
  barge_in: true
  wake_word: configurable
```

## Permission defaults

```yaml
permissions:
  filesystem: project_scoped
  terminal: enabled
  network: declared
  github_read: enabled
  github_write: approval_required
  production_changes: approval_required
  destructive_database: approval_required
```

## Persistence defaults

```yaml
persistence:
  required_mount: /data
  required_root: /data/jarvis
  fail_startup_if_unwritable: true
  backup_required_for_critical_state: true
```

## Configuration rules

- Secrets are never stored in this file.
- Environment values are validated at startup.
- Unknown security-sensitive settings fail closed.
- Changes to policy require explicit review.
- Runtime state is not copied back into image-local directories as a second source of truth.
