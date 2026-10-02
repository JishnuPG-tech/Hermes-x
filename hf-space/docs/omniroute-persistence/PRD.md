# Product Requirements Document: OmniRoute Permanent Persistence

## 1. Executive summary

Build a production-grade persistence layer for OmniRoute running inside a Hugging Face Docker Space. The system must treat the attached `/data` Storage Bucket as the durable source of truth instead of using an ephemeral container directory plus periodic snapshots.

The system must preserve state across normal restart, Space restart, container recreation, image rebuild, application upgrade, graceful shutdown, ungraceful termination, and recovery from a validated backup.

## 2. Current-state diagnosis

The current deployment sets `DATA_DIR` to `/root/.omniroute` while also maintaining `/data/omniroute` as a backup/snapshot location. OmniRoute therefore writes to an ephemeral filesystem. A synchronization script attempts to copy the live state into the persistent bucket.

This creates several failure classes:

1. Live DB and persistent DB can diverge.
2. SQLite WAL and SHM files can make a raw file copy inconsistent.
3. A restart during snapshotting can lose the newest transactions.
4. Startup restore can race with another service or stale local state.
5. Selected directories can be restored while newly introduced application state is missed.
6. Permissions on `/data` can cause SQLite readonly failures.
7. Fixed or generated encryption keys can make an existing encrypted database unreadable after rebuild.
8. Redis state is not durable and must never be the source of truth.
9. Application upgrades can change schema or migration behavior.
10. The upstream image is pulled from `:main`, so reproducibility is weak.

## 3. Goals

### P0 goals

- Set OmniRoute `DATA_DIR` directly to `/data/omniroute`.
- Eliminate the dual-live-database architecture.
- Persist the SQLite database and all OmniRoute filesystem state that belongs under `DATA_DIR`.
- Preserve the SQLite WAL lifecycle correctly.
- Validate `/data` is mounted read-write before startup.
- Validate the database is writable before declaring OmniRoute healthy.
- Make encryption keys stable through Hugging Face Secrets.
- Make startup idempotent.
- Make migration from the existing snapshot safe.
- Add automated persistence tests.
- Add backup and restore procedures.
- Pin or deliberately control the OmniRoute image version.

### P1 goals

- Add atomic metadata/config writes.
- Add periodic SQLite integrity checks.
- Add backup rotation and restore verification.
- Add a persistence health endpoint/report.
- Add migration markers and versioning.
- Add startup diagnostics that clearly distinguish missing bucket, permission error, corruption, encryption-key mismatch, migration failure and empty database.
- Add controlled graceful shutdown with enough time for SQLite checkpointing.

### P2 goals

- External backup destination.
- Backup encryption and retention policies.
- Optional object-store replication.
- Automated disaster-recovery drills.
- Observability dashboard for database size, WAL size, backup age and restore status.

## 4. Non-goals

- Storing API secrets in GitHub.
- Treating Redis as permanent storage.
- Assuming a Storage Bucket is an immutable backup.
- Guaranteeing zero data loss under physical storage failure.
- Running multiple writers against the same SQLite database from independent containers.

## 5. User stories

1. As an operator, I can add a provider and restart the Space without losing it.
2. As an operator, I can add models and routing rules and rebuild the Docker image without losing them.
3. As an operator, I can upgrade OmniRoute without silently replacing the database.
4. As an operator, I can see whether persistent storage is mounted and writable.
5. As an operator, I can restore the last known-good backup after corruption.
6. As an operator, I can rotate application images without rotating the database encryption key.
7. As a developer, I can run the same persistence test suite locally with a mounted directory.

## 6. Functional requirements

### FR-01 Durable data root

All OmniRoute durable data must resolve under `/data/omniroute` in the Hugging Face deployment.

### FR-02 Single source of truth

There must be exactly one live SQLite database. No background process may continuously mirror a live database to a second database and then use the second database for recovery without an explicit backup protocol.

### FR-03 SQLite safety

Use SQLite WAL mode as supported by OmniRoute. Graceful shutdown must allow checkpointing. Backups must use SQLite-aware backup/checkpoint procedures, not unsafe blind copies of only `storage.sqlite` while the database is actively changing.

### FR-04 Encryption key stability

`STORAGE_ENCRYPTION_KEY` must come from a stable Hugging Face Secret. The application must fail closed with an actionable error if an existing encrypted database is detected but the key is missing or changed.

### FR-05 Startup validation

Before OmniRoute starts, the entrypoint must verify:

- `/data` exists
- `/data/omniroute` exists or can be created
- directory is writable
- database is readable/writable if it exists
- SQLite integrity check passes
- encryption configuration is consistent
- required subdirectories exist
- no conflicting local `DATA_DIR` is active

### FR-06 Idempotent initialization

Running startup repeatedly must not reset provider configuration, regenerate encryption keys, delete the database, overwrite configuration with defaults, or restore stale snapshots over newer state.

### FR-07 Migration

The first deployment after the change must detect the current `/data/omniroute` snapshot and migrate it into the new direct data root without overwriting newer state.

### FR-08 Backup

Backups must be versioned by timestamp, include integrity metadata, and have a retention policy. Backup creation must not expose secrets in logs.

### FR-09 Restore

Restore must require an explicit operator action or a recovery mode. The system must verify the restored database before starting normal traffic.

### FR-10 Observability

The startup logs must report the resolved data root, persistence status, database path, database size, WAL size, last backup age and integrity result. Secrets must never be logged.

### FR-11 Redis

Redis may be used for ephemeral cache/session coordination only. `FLUSHALL` must not be used as a substitute for persistence and must be removed from normal startup unless a narrowly scoped cache reset is explicitly required.

### FR-12 Reproducible application version

The production image should use a pinned OmniRoute release or immutable image digest. Floating `:main` is allowed only in a deliberate development channel.

## 7. Reliability requirements

Target behavior:

- 10 consecutive normal restarts: zero configuration loss.
- 10 container rebuilds using the same bucket: zero configuration loss.
- graceful shutdown: zero configuration loss.
- forced termination test: recovery from latest valid transaction/backup with documented RPO.
- restore test: 100% successful restore of the selected backup in an isolated test directory.

## 8. Security requirements

- Secrets only through Hugging Face Secrets or equivalent secret manager.
- Stable encryption key must not be generated on every boot.
- `/data/omniroute` permissions must prevent unintended access by unrelated processes.
- Logs must redact API keys, tokens, cookies and authorization headers.
- Backups must be encrypted if exported outside the trusted bucket.
- GitHub repository must contain templates and documentation, never real credentials.

## 9. Acceptance criteria

The feature is accepted when all of the following pass:

1. Configure provider A.
2. Configure model B.
3. Configure routing rule C.
4. Restart container.
5. Verify A, B and C.
6. Rebuild image without changing the bucket.
7. Verify A, B and C.
8. Restart again.
9. Verify A, B and C.
10. Stop with graceful shutdown.
11. Verify database integrity.
12. Create backup.
13. Restore backup into an isolated directory.
14. Verify provider/model/routing records.
15. Simulate readonly `/data` and verify startup fails with a clear message rather than silently creating a fresh database.
16. Simulate missing encryption key with an encrypted database and verify startup fails closed.
17. Verify no secrets appear in Git or logs.

## 10. Success metrics

- Persistence loss incidents: zero under supported restart/rebuild scenarios.
- Backup verification success: 100% for scheduled backups.
- Startup persistence validation: under 10 seconds excluding application startup.
- Mean time to identify persistence failure: under 5 minutes with diagnostics.
- No silent fallback to a fresh empty database when a persistent database exists.
