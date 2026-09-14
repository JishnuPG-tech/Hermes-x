# Implementation Plan

## Phase 0: Freeze and inventory

1. Record current OmniRoute version/image digest.
2. Record current `/data/omniroute` contents.
3. Record database size and SQLite integrity.
4. Create a verified backup before changing startup behavior.
5. Export non-secret configuration for human-readable comparison.
6. Confirm the HF Storage Bucket is read-write and mounted at `/data`.
7. Identify every process that accesses OmniRoute state.
8. Stop treating `/root/.omniroute` as authoritative.

Deliverable: baseline manifest and verified rollback backup.

## Phase 1: Direct persistent DATA_DIR

Change the deployment configuration to:

```bash
export DATA_DIR="${OMNIROUTE_DATA_DIR:-/data/omniroute}"
```

Do not use `/root/.omniroute` in production unless explicitly selected for local development.

Create the directory only after validating `/data`.

Required environment contract:

```text
OMNIROUTE_DATA_DIR=/data/omniroute
DATA_DIR=/data/omniroute
```

Do not rely only on shell exports hidden inside one script. Define the variables in the final runtime environment as well so child processes inherit the same contract.

## Phase 2: Startup persistence gate

Implement a shell/Python persistence preflight.

Checks:

```text
1. test -d /data
2. test -w /data
3. mkdir -p /data/omniroute
4. test -w /data/omniroute
5. resolve realpath
6. verify DATA_DIR points under /data
7. inspect storage.sqlite if present
8. run PRAGMA quick_check
9. inspect WAL/SHM presence
10. verify encryption configuration
11. verify required subdirectories
```

If any check fails, exit non-zero with a specific reason.

## Phase 3: Migration from the old design

The current deployment may have a persistent snapshot at `/data/omniroute/storage.sqlite` and a live database under `/root/.omniroute`.

Migration algorithm:

```text
Acquire startup lock
      |
      v
Does new persistent DB exist?
      |
   yes|no
      |  \
      |   -> Is old persistent snapshot valid?
      |          |
      |        yes -> validate -> adopt
      |        no  -> fail if old live DB contains data
      |
      v
Validate DB integrity
      |
      v
Set DATA_DIR directly to persistent path
      |
      v
Do not restore stale snapshots over a newer DB
      |
      v
Write migration marker
      |
      v
Start application
```

Before migration, compare timestamps, sizes, checksums and SQLite integrity. If both the old live DB and persistent snapshot contain different recent changes, do not guess. Preserve both and require an explicit reconciliation decision.

## Phase 4: Remove unsafe mirroring

Remove the recurring process that copies `/root/.omniroute` into `/data/omniroute` as the primary persistence mechanism.

Retain a backup system, but make it explicit:

```text
live DB -> SQLite-aware backup -> backup directory
```

not:

```text
live DB -> second live DB
```

## Phase 5: Stable encryption

Ensure the same `STORAGE_ENCRYPTION_KEY` is supplied by HF Space Secrets on every boot.

Rules:

- Never generate a new key on every boot.
- Never commit the key.
- Never log the key.
- If an encrypted DB exists and the key is missing, stop.
- If decryption fails, stop and preserve the DB.
- Document the key rotation procedure separately.

## Phase 6: SQLite lifecycle

1. Keep DB/WAL/SHM in the same persistent directory.
2. Preserve a sufficient shutdown grace period.
3. Allow OmniRoute to perform its shutdown checkpoint.
4. Run integrity checks during backup and recovery.
5. Use SQLite-aware backup procedures.
6. Monitor DB and WAL size.
7. Do not delete WAL files manually while the application is running.

## Phase 7: Backup service

Create a backup script with:

- lock
- integrity check
- SQLite-aware backup
- checksum
- manifest
- timestamped directory
- retention
- optional compression
- optional encryption
- restore verification

Suggested structure:

```text
/data/omniroute/backups/
  2026-09-14T100000Z/
    storage.sqlite
    manifest.json
    SHA256SUMS
```

Recommended retention example:

- 24 hourly backups
- 14 daily backups
- 8 weekly backups

Adjust for storage budget.

## Phase 8: Upgrade safety

Do not pull `:main` blindly for production. Prefer an immutable release tag or image digest.

Upgrade sequence:

```text
backup
  -> verify backup
  -> deploy new image
  -> mount same bucket
  -> persistence preflight
  -> migration
  -> integrity check
  -> health check
  -> smoke test
  -> enable traffic
```

If migration fails, preserve the original DB and roll back the image. Do not replace the DB with a fresh empty one.

## Phase 9: Remove Redis data loss hazards

Redis should be treated as cache/state that can be recreated. Remove unconditional `FLUSHALL` from startup.

If cache invalidation is required, use a namespaced key strategy or delete only application-owned keys.

## Phase 10: Permissions

At startup:

- detect owner and mode
- verify process user can read/write
- avoid blanket `chmod 777` in production
- use the least privilege compatible with HF runtime

A readonly SQLite database can occur even when the file itself looks writable because SQLite also needs directory write access for WAL/SHM and journaling.

## Phase 11: Testing

Run the complete test plan in `TEST_PLAN.md`.

Minimum gate:

- 10 restarts
- 10 rebuilds
- provider/model/routing persistence
- forced termination
- readonly test
- encryption key missing test
- backup and restore
- upgrade/downgrade smoke test

## Phase 12: Observability

Add startup and health diagnostics:

```text
[PERSISTENCE] data_root=/data/omniroute
[PERSISTENCE] bucket=present
[PERSISTENCE] writable=yes
[PERSISTENCE] database=present
[PERSISTENCE] sqlite_integrity=ok
[PERSISTENCE] encryption=enabled
[PERSISTENCE] backup_age=...
[PERSISTENCE] status=ready
```

Never print provider keys, cookies, authorization headers or encryption keys.

## Phase 13: Disaster recovery

Document and rehearse:

1. database corruption
2. accidental configuration deletion
3. bad application upgrade
4. encryption key mismatch
5. bucket unavailable
6. readonly bucket
7. partial backup
8. container killed during write
9. migration failure

## Phase 14: Final production gate

Only declare production ready after the acceptance criteria in `PRD.md` pass on the real HF Space with the real mounted bucket.
