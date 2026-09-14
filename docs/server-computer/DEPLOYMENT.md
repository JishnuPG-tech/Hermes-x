# Deployment Plan

## Target topology

```text
HF Space: Hermes / Jarvis
├── Gateway
├── Jarvis Core
├── Agent OS
├── Hermes
├── Voice Gateway
├── Kokoro TTS
└── /data/jarvis → HF Storage Bucket

HF Space: OmniRoute
└── model routing / provider abstraction

Reserved compute surface
└── future STT, browser worker, GPU worker, or other measured bottleneck
```

## Container rules

- Use Docker Space for the integrated runtime.
- Keep application code in the image/repository.
- Keep mutable state in `/data/jarvis`.
- Do not depend on `/tmp` or the image layer for durable state.
- Pin important base images and dependencies.
- Never bake credentials into the image.

HF Docker Space documentation explicitly notes that normal Space disk is lost on restart and that attached Storage Buckets are the persistence mechanism. The `/data` mount is available at runtime, not Docker build time. 

## Environment contract

```text
JARVIS_DATA_DIR=/data/jarvis
HERMES_HOME=/data/jarvis/hermes
JARVIS_PROJECTS_DIR=/data/jarvis/projects
JARVIS_WORKSPACES_DIR=/data/jarvis/workspaces
JARVIS_DB_DIR=/data/jarvis/databases
JARVIS_ARTIFACTS_DIR=/data/jarvis/artifacts
JARVIS_LOG_DIR=/data/jarvis/logs
JARVIS_STATE_DIR=/data/jarvis/agent-state
OMNIROUTE_URL=<private/internal endpoint>
```

Actual secret values must be configured through Space Secrets or a dedicated secret manager.

## Startup order

```text
filesystem
 → database
 → configuration
 → OmniRoute client
 → Hermes
 → tools
 → voice providers
 → gateway
 → scheduler
 → readiness
```

## Deployment gate

Before accepting traffic:

- persistent storage mounted
- write test passed
- database migrated
- secrets present
- OmniRoute reachable
- Hermes health check passed
- TTS warmup passed or fallback ready
- GitHub integration authenticated
- no stale task lock

## Upgrade strategy

Prefer immutable application versions. Deploy a tested commit, run smoke tests, then resume background tasks. Keep previous image/config available for rollback.

## When HF Space is no longer the right execution host

Consider a dedicated VM/server when the system requires:

- systemd-managed services
- Docker Compose with many long-lived containers
- privileged kernel features
- stable inbound networking beyond Space capabilities
- sustained heavy CPU workloads
- custom storage/backup topology
- strict always-on guarantees

The application architecture remains portable because the durable paths, task contracts, policies, and integrations are not tied to a particular host.
