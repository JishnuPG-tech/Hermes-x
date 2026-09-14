# Required OmniRoute Changes

This is the implementation checklist for the `JishnuPG-tech/Omniroute` deployment.

## A. Entrypoint

Replace the production data-root behavior so the effective value is:

```bash
OMNIROUTE_DATA_DIR="${OMNIROUTE_DATA_DIR:-/data/omniroute}"
export DATA_DIR="$OMNIROUTE_DATA_DIR"
```

Before launching OmniRoute, run the persistence preflight.

## B. Remove dual live state

Do not run OmniRoute from `/root/.omniroute` and periodically mirror it to `/data/omniroute`.

Remove or disable logic whose purpose is to continuously copy the live database into the persistent directory as the normal persistence mechanism.

## C. Keep migration compatibility

For one controlled migration release, detect the old data location and persistent snapshot. Compare them before selecting the source. After successful migration, write a marker and stop running the migration on every startup.

## D. Backup instead of mirroring

Use a dedicated backup routine. Backups should be SQLite-aware and timestamped.

## E. SQLite shutdown

Give OmniRoute enough shutdown grace time for WAL checkpointing. Avoid abrupt process termination during normal container stops.

## F. Encryption

Use the stable `STORAGE_ENCRYPTION_KEY` from HF Secrets. Never generate a new production key when an encrypted DB exists.

## G. Persistence preflight

Implement checks for:

```text
/data exists
/data/omniroute exists
write test succeeds
DATA_DIR resolves correctly
storage.sqlite is readable/writable
SQLite integrity is OK
WAL/SHM can be created
required encryption key exists when needed
```

## H. Redis

Remove unconditional `redis-cli flushall` from normal startup. If cache invalidation is necessary, namespace application keys and delete only those keys.

## I. Permissions

Replace broad `chmod 777` where possible with ownership and least-privilege permissions. Remember SQLite needs directory write permission for journal/WAL/SHM operations.

## J. Image pinning

Replace floating upstream `main` with a tested release tag or immutable digest for production. Record the selected version in the deployment manifest.

## K. Health checks

Expose a persistence status that can answer:

```text
Is the bucket mounted?
Is DATA_DIR correct?
Is the DB writable?
Is SQLite healthy?
Is encryption configured?
When was the last verified backup?
```

## L. Upgrade behavior

Before each OmniRoute upgrade:

```text
backup -> verify -> deploy -> migrate -> integrity check -> smoke test
```

On migration failure, stop and preserve the DB. Never initialize an empty replacement automatically.

## M. Rollback behavior

Rollback should restore a compatible image and, when required, a compatible pre-upgrade database backup. Image rollback without DB compatibility validation is unsafe.

## N. Filesystem inventory

Every OmniRoute path that contains durable user state must be audited. The final deployment should have a documented mapping of each path to either `/data` durable storage or ephemeral cache.

## O. Verification

Do not mark the change complete until the real HF Space passes the complete `TEST_PLAN.md`.
