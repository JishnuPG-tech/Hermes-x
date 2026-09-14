# Configuration Contract

## Production values

```bash
OMNIROUTE_DATA_DIR=/data/omniroute
DATA_DIR=/data/omniroute
```

These two variables should resolve to the same canonical directory.

## Required secrets

```text
STORAGE_ENCRYPTION_KEY
JWT_SECRET
API_KEY_SECRET
INITIAL_PASSWORD
```

The exact set depends on enabled OmniRoute features. Secrets belong in the Hugging Face Space Secrets configuration.

## Persistence invariants

The following must always be true in production:

```text
/data exists
/data/omniroute exists
/data/omniroute is writable
DATA_DIR == /data/omniroute
storage.sqlite is under DATA_DIR
SQLite WAL/SHM are under DATA_DIR
no second live DB is used
stable encryption key is available when encryption is enabled
```

## Optional paths

```text
/data/omniroute/backups
/data/omniroute/call_logs
/data/omniroute/oauth
/data/omniroute/credentials
/data/omniroute/runtime
/data/omniroute/config
```

Only create paths required by the installed OmniRoute version.

## Local development

For local Docker development, an explicit mounted directory can be used:

```bash
docker run \
  -v "$PWD/.omniroute-data:/data/omniroute" \
  -e DATA_DIR=/data/omniroute \
  -e OMNIROUTE_DATA_DIR=/data/omniroute \
  ...
```

## Prohibited production fallbacks

Do not silently fall back to:

```text
/root/.omniroute
/home/*/.omniroute
/tmp/omniroute
/app/data
```

unless the deployment intentionally changes the persistent mount and updates this contract.

## Startup behavior

If the persistent mount is unavailable, startup must fail rather than silently use an ephemeral directory.

## Secret behavior

If an encrypted database exists and the encryption key is missing, startup must fail rather than creating a fresh database.
