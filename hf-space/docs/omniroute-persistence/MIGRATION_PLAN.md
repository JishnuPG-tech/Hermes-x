# Migration Plan

## Objective

Move from the current dual-storage model to direct persistent OmniRoute storage without losing configuration.

## Current model

```text
OmniRoute -> /root/.omniroute
                 |
                 +--> periodic snapshot --> /data/omniroute
```

## Target model

```text
OmniRoute -> /data/omniroute
                 |
                 +--> backups/
```

## Pre-migration checklist

- Confirm Storage Bucket is mounted read-write at `/data`.
- Confirm current provider configuration works.
- Confirm current model configuration works.
- Confirm current routing configuration works.
- Record OmniRoute version.
- Run SQLite integrity check on both live and persistent copies.
- Create an offline verified backup of both copies.
- Record checksums.
- Save the current deployment configuration without secrets.

## Reconciliation rules

If `/data/omniroute/storage.sqlite` exists:

1. Validate it.
2. Compare it with `/root/.omniroute/storage.sqlite`.
3. If they are byte-identical, adopt the persistent copy.
4. If they differ, inspect SQLite metadata and timestamps.
5. Prefer the database known to contain the latest successful user configuration only when this can be demonstrated safely.
6. If there is uncertainty, preserve both and stop. Never overwrite one with the other automatically.

## Migration steps

### Step 1: Maintenance mode

Temporarily stop configuration changes while migration is running.

### Step 2: Backup

Create:

```text
/data/omniroute/backups/pre-direct-data-dir-<timestamp>/
```

Include:

- SQLite-aware DB backup
- checksum
- manifest
- application version

### Step 3: Prepare directory

```bash
mkdir -p /data/omniroute
```

Verify write access by creating and deleting a small temporary file.

### Step 4: Select database

If the persistent DB is healthy and is the authoritative snapshot, use it directly. If the ephemeral DB has newer data, create a safe SQLite backup from it into the persistent directory.

### Step 5: Configure environment

```bash
OMNIROUTE_DATA_DIR=/data/omniroute
DATA_DIR=/data/omniroute
```

### Step 6: Remove startup restore behavior

Do not copy selected directories from the persistent directory into an ephemeral directory. The application must consume persistent state directly.

### Step 7: Start in persistence validation mode

Run database integrity and write tests before enabling traffic.

### Step 8: Smoke test

Verify:

- provider list
- model list
- routing rules
- authentication
- API request
- dashboard
- usage recording

### Step 9: Restart test

Restart the Space and repeat the smoke test.

### Step 10: Rebuild test

Rebuild/redeploy the same Space image while preserving the bucket. Repeat the smoke test.

## Rollback

If migration fails:

1. Stop OmniRoute.
2. Preserve `/data/omniroute` unchanged.
3. Restore the known-good deployment image.
4. Restore the pre-migration backup only if the persistent DB itself was modified incorrectly.
5. Validate integrity.
6. Restart.

Never delete the original database as part of automated rollback.

## Migration marker

Create a small non-secret marker such as:

```json
{
  "migration": "direct-data-dir-v1",
  "completed_at": "ISO-8601 timestamp",
  "application_version": "version",
  "database_sha256": "checksum"
}
```

This prevents repeated migration attempts on every startup.
