# OmniRoute Permanent Persistence Program

## Purpose

This folder is the implementation blueprint for making OmniRoute configuration, provider connections, model routing state, credentials metadata, runtime settings, logs, and application state survive Hugging Face Space restarts and rebuilds reliably.

Target deployment:

- Hugging Face Docker Space
- Read-write Storage Bucket mounted at `/data`
- OmniRoute running as part of the OpenCode/Hermes gateway image
- OmniRoute `DATA_DIR` explicitly redirected to persistent storage
- SQLite used as the primary transactional state store

## Problem

The current deployment uses an ephemeral OmniRoute data directory and periodically mirrors it to `/data/omniroute`. This creates two sources of truth. A crash, forced restart, interrupted copy, SQLite WAL state, permissions problem, or startup restore race can cause the persistent snapshot to lag behind the live database.

The target architecture has one source of truth:

```text
OmniRoute
   |
   +--> DATA_DIR=/data/omniroute
          |
          +--> storage.sqlite
          +--> storage.sqlite-wal
          +--> storage.sqlite-shm
          +--> call_logs/
          +--> backups/
          +--> runtime/
          +--> oauth/
          +--> credentials/
          +--> config/
```

Hugging Face documents that normal Docker Space disk is ephemeral and that an attached Storage Bucket can provide persistent storage at runtime. OmniRoute documents `DATA_DIR` as the root for SQLite, backups, and data files. See `REFERENCES.md` for current source links.

## Non-goals

This program does not put secrets into Git. Hugging Face Space Secrets remain the authority for master encryption keys, JWT secrets, API authorization secrets, and other sensitive environment values.

A persistent bucket is not a disaster-recovery system by itself. Backups and restore verification are therefore first-class requirements.

## Documents

| Document | Purpose |
|---|---|
| `PRD.md` | Product requirements, acceptance criteria, failure modes, SLOs |
| `IMPLEMENTATION_PLAN.md` | Ordered engineering implementation plan |
| `ARCHITECTURE.md` | Target system architecture and data flows |
| `MIGRATION_PLAN.md` | Safe migration from the current dual-storage deployment |
| `OPERATIONS_RUNBOOK.md` | Startup, restart, incident response, backup and restore procedures |
| `TEST_PLAN.md` | Persistence, restart, corruption, permissions and disaster-recovery tests |
| `SECURITY.md` | Secret handling, encryption, permissions and threat model |
| `CONFIG_CONTRACT.md` | Required environment variables, paths and invariants |
| `ADRs.md` | Architecture decisions and rejected alternatives |
| `REFERENCES.md` | Current upstream and platform references |

## Definition of done

The work is complete only when a provider/model configuration is created, the Space is restarted multiple times, the container is rebuilt, the application is upgraded, and the configuration is still present and usable. The test must also cover a forced shutdown and a database backup/restore cycle.
