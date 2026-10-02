# Storage and Durability

## Storage tiers

| Data | Primary location | Versioned | Backup |
|---|---|---:|---:|
| Source code | GitHub + server working copy | Yes | GitHub |
| Task state | Durable DB under `/data/jarvis` | No | Yes |
| Hermes memory | Durable Hermes state | Partly | Yes |
| Session history | Durable Hermes state DB | No | Yes |
| Artifacts | Storage Bucket | No | Important artifacts |
| Logs | Storage Bucket / log sink | No | Retention-based |
| Secrets | Space Secrets / secret manager | No | Provider-managed |
| Large AI datasets | HF Dataset or bucket | Dataset: yes | Policy dependent |
| Runtime cache | `/data/jarvis/cache` | No | No |

## Why the Space filesystem is not enough

Docker Space disk is ephemeral. A restart or stop can destroy data written only to the container filesystem. Persistent state therefore must use an attached Storage Bucket or another durable service. citehttps://huggingface.co/docs/hub/spaces-storage

## Recommended mount

```text
HF Storage Bucket
        ↓ read/write
/data
        ↓
/data/jarvis
```

Storage Buckets are mutable object storage and can be mounted read-write into Spaces. Dataset and model volumes are read-only when mounted. citehttps://huggingface.co/docs/hub/storage-bucketshttps://huggingface.co/docs/huggingface_hub/guides/manage-spaces

## Data classification

### Tier A — irreplaceable

- Git history
- task checkpoints
- memory
- production database
- configuration state
- user-generated important artifacts

Must have an independent backup.

### Tier B — reproducible

- package caches
- model caches
- temporary workspaces
- build outputs that can be regenerated

Backup optional.

### Tier C — disposable

- temporary logs
- scratch files
- incomplete downloads

May be deleted automatically.

## Database strategy

Start with SQLite for local single-runtime metadata. Enable WAL where appropriate and close databases cleanly. Move to PostgreSQL when concurrency or service separation makes SQLite unsuitable.

Do not place a database only in the image layer or ephemeral working directory.

## Backup strategy

Use at least three logical copies for critical state when practical:

1. live durable bucket/database
2. independent backup destination
3. source-controlled copy for configuration/code where applicable

Backups should be encrypted, integrity-checked, retention-managed, and periodically restored in a test environment.

## Recovery objectives

Initial target:

- RPO: ≤24 hours for critical application state
- RTO: ≤2 hours for full runtime recovery
- active task checkpoint: recover from last durable checkpoint

Tighter targets can be introduced after measuring actual workload and infrastructure.

## Startup durability checks

On startup:

1. verify `/data/jarvis` exists
2. verify read/write access
3. verify expected subdirectories
4. verify database open/migration state
5. verify registry integrity
6. verify Hermes state path
7. verify required secrets are present without logging values
8. emit a readiness event only after checks pass

## Dataset policy

HF Dataset repositories are suitable for large AI/data collections and versioned distribution. They are not the canonical mutable OS filesystem for Jarvis. Use them for read-mostly datasets, model assets, or public/shareable data where appropriate.

Never put secrets, private user data, production credentials, or private application state in a public dataset.

## Loss-prevention rule

“No data loss” is an engineering objective, not a guarantee. The system must make loss unlikely through durable storage, atomic writes where possible, checkpoints, backups, integrity checks, and restore drills.
