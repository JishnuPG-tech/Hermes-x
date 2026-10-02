# Operations Runbook

## 1. Normal startup

Expected log sequence:

```text
[PERSISTENCE] checking /data
[PERSISTENCE] bucket present
[PERSISTENCE] data root=/data/omniroute
[PERSISTENCE] writable=yes
[PERSISTENCE] database=present
[PERSISTENCE] integrity=ok
[PERSISTENCE] encryption=verified
[PERSISTENCE] ready
```

Any failure should stop startup unless it is explicitly classified as non-critical cache loss.

## 2. Provider disappeared after restart

Do not immediately add the provider again.

1. Check resolved `DATA_DIR`.
2. Check whether `/data/omniroute/storage.sqlite` exists.
3. Run SQLite integrity check.
4. Check encryption key configuration.
5. Check database modification time and size.
6. Check startup logs for migration/restore operations.
7. Confirm the application did not fall back to `/root/.omniroute`.
8. Compare with the most recent backup.

## 3. SQLITE_READONLY

Likely causes:

- bucket mounted readonly
- directory ownership/mode prevents writes
- SQLite WAL/SHM cannot be created
- another process owns the DB incorrectly

Procedure:

1. Stop OmniRoute.
2. Verify mount mode.
3. Verify directory write permission.
4. Verify no second process uses the DB.
5. Restart only after the preflight passes.

Never solve this by creating a new empty DB.

## 4. Encryption key mismatch

Symptoms include inability to decrypt an existing encrypted database.

Procedure:

1. Stop the application.
2. Preserve the database.
3. Verify the HF Secret value.
4. Do not generate a replacement key.
5. Restore the known-good key.
6. Restart.

If the original key is permanently unavailable, restore a backup for which the key is known.

## 5. Corrupt database

1. Stop OmniRoute.
2. Copy the database and associated files to a quarantine directory.
3. Do not delete the original.
4. Validate the newest backup.
5. Restore to a new data directory.
6. Run integrity and application smoke tests.
7. Switch to the restored directory only after validation.

## 6. Backup procedure

A backup must be SQLite-aware and produce:

```text
backup/
  storage.sqlite
  manifest.json
  SHA256SUMS
```

The manifest should contain:

- timestamp
- OmniRoute version
- schema version
- DB size
- checksum
- backup method

No secrets.

## 7. Restore procedure

1. Stop application.
2. Preserve current data directory.
3. Restore selected backup to a new directory.
4. Run integrity check.
5. Verify encryption compatibility.
6. Point `DATA_DIR` to restored directory.
7. Start in maintenance mode.
8. Verify providers/models/routes.
9. Resume traffic.

## 8. Bad upgrade

If a new image causes migration failure:

1. Keep the DB.
2. Inspect logs.
3. Roll back image only if DB compatibility is safe.
4. Otherwise restore a pre-upgrade DB backup.
5. Never let a failed migration trigger automatic DB recreation.

## 9. Bucket unavailable

Treat the service as degraded or unavailable. Do not continue with an ephemeral writable database because that would create divergent state.

## 10. Backup monitoring

Alert when:

- no successful backup within the configured interval
- last backup fails integrity verification
- DB size grows unexpectedly
- WAL grows unexpectedly
- bucket free space is low
- persistence preflight fails

## 11. Security incident

If a credential may have leaked:

1. Rotate the affected provider credential.
2. Rotate application credentials if required.
3. Inspect logs and Git history.
4. Do not delete the database to hide the event.
5. Preserve relevant audit information.

## 12. Routine maintenance

Weekly:

- verify backup restore
- inspect database/WAL growth
- review failed startups
- review application upgrades

Monthly:

- perform disaster-recovery drill
- verify secrets are still stable
- test rollback documentation
- review storage consumption
