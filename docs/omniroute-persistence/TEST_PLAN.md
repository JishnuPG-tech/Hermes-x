# Persistence Test Plan

## Test environment

Run tests both locally with Docker and on the real Hugging Face Space using the attached Storage Bucket.

## Test data

Create unmistakable test records:

- provider: `PERSISTENCE_TEST_PROVIDER`
- model: `persistence-test-model`
- routing rule: `PERSISTENCE_TEST_ROUTE`
- setting: `persistence-test-value`

Do not use real secrets in automated test fixtures.

## T01: Fresh startup

Expected:

- `/data/omniroute` created
- database created
- SQLite integrity passes
- OmniRoute starts

## T02: Provider persistence

1. Add provider.
2. Record provider identifier.
3. Restart container.
4. Verify provider exists.
5. Repeat 10 times.

Expected: zero loss.

## T03: Model persistence

1. Configure model.
2. Restart 10 times.
3. Verify model each time.

Expected: zero loss.

## T04: Routing persistence

Configure routing rules and verify after 10 restarts and one rebuild.

## T05: Container recreation

Destroy and recreate the container while preserving the same bucket.

Expected: all durable state remains.

## T06: Space rebuild

Deploy a new image with identical `/data` mount.

Expected: state remains.

## T07: Application upgrade

Upgrade OmniRoute to a newer validated version.

Expected:

- backup created first
- migrations complete
- integrity passes
- state remains

## T08: Graceful shutdown

Stop the service with sufficient grace time.

Expected:

- clean shutdown
- WAL checkpoint as appropriate
- no lost configuration

## T09: Forced termination

Kill the process/container during active writes.

Expected:

- application recovers using SQLite WAL semantics or the documented RPO
- database integrity passes
- no silent replacement with an empty DB

## T10: Readonly mount

Make `/data/omniroute` readonly.

Expected: startup fails clearly with persistence error.

It must not silently fall back to `/root/.omniroute`.

## T11: Missing bucket

Hide/unmount `/data`.

Expected: startup fails before OmniRoute starts.

## T12: Encryption key missing

Use an existing encrypted database but remove `STORAGE_ENCRYPTION_KEY`.

Expected: fail closed with actionable error.

## T13: Encryption key changed

Use the wrong key against an existing encrypted database.

Expected: fail closed. Do not create a fresh database.

## T14: SQLite corruption

Use a deliberately corrupted test copy.

Expected:

- integrity check fails
- original is preserved
- recovery instructions are emitted
- no automatic destructive repair

## T15: Backup

Create backup from a live healthy DB.

Verify:

- backup exists
- checksum exists
- manifest exists
- backup opens
- backup integrity passes

## T16: Restore

Restore backup into an isolated directory.

Verify providers, models, routes and settings.

## T17: Backup retention

Create enough test backups to trigger retention.

Expected: policy removes only expired backups and preserves the newest required set.

## T18: Redis reset safety

Restart the Space.

Expected: Redis cache may be empty, but no authoritative OmniRoute configuration is lost.

## T19: No secret leakage

Search logs and Git diff for:

- API keys
- bearer tokens
- cookies
- encryption keys
- passwords

Expected: none.

## T20: Concurrent access

Generate normal API traffic while a backup is taken.

Expected: backup remains valid and application continues serving requests.

## T21: Disk pressure

Fill a test filesystem close to capacity.

Expected: backup/startup fails safely with clear diagnostics rather than producing silent partial state.

## T22: Migration regression

Run migration twice.

Expected: second execution is a no-op and does not overwrite state.

## T23: Version rollback

Upgrade to a validated version, then roll back the image without changing the DB.

Expected: only perform rollback where schema compatibility is verified. Otherwise restore a compatible DB backup.

## Production gate

All P0 tests must pass on the real Space before the persistence change is considered production-ready.
