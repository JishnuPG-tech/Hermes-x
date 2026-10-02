# Target Architecture

## 1. Design principle

The container is disposable. The `/data` Storage Bucket is durable. OmniRoute must write durable application state directly to the durable mount.

```text
                 Hugging Face Space
                        |
                 Docker container
                        |
        +---------------+----------------+
        |                                |
   application code                 ephemeral cache
        |                                |
        v                                v
  OmniRoute process                 /tmp /root/.cache
        |
        | DATA_DIR
        v
  /data/omniroute  <---------------- Storage Bucket
        |
        +-- storage.sqlite
        +-- storage.sqlite-wal
        +-- storage.sqlite-shm
        +-- call_logs/
        +-- backups/
        +-- oauth/
        +-- credentials/
        +-- runtime/
        +-- config/
```

## 2. Data classification

| Data | Durable? | Location |
|---|---:|---|
| Provider connections | Yes | SQLite under DATA_DIR |
| Model registry | Yes | SQLite under DATA_DIR |
| Routing configuration | Yes | SQLite/config under DATA_DIR |
| Application settings | Yes | SQLite/config under DATA_DIR |
| Usage history | Yes | SQLite under DATA_DIR |
| Call logs | Yes when required | DATA_DIR/call_logs |
| OAuth refresh state | Yes | DATA_DIR/oauth or encrypted store |
| Credential metadata | Yes | DATA_DIR/credentials or SQLite |
| API secrets | Yes, but secret-controlled | Space Secrets or encrypted store |
| Redis cache | No | Redis |
| Node modules | No | Image |
| Next build artifacts | No | Image |
| Python packages | No | Image |
| Temporary downloads | No | ephemeral filesystem |
| Backups | Yes | DATA_DIR/backups plus optional external copy |

## 3. Startup state machine

```text
START
  |
  v
Validate /data
  |-- missing --> FAIL: persistent storage unavailable
  |
  v
Validate /data/omniroute writable
  |-- no --> FAIL: permission/volume problem
  |
  v
Resolve DATA_DIR=/data/omniroute
  |
  v
Does storage.sqlite exist?
  |              |
 no              yes
  |              |
  v              v
Initialize      Validate SQLite
new DB          integrity/WAL/key
  |              |
  +------+-------+
         |
         v
Validate migrations
         |
         v
Validate durable subdirectories
         |
         v
Start OmniRoute
         |
         v
Health check
         |
         v
READY
```

## 4. No startup copy-back

The old model:

```text
/data DB -> /root DB -> run -> snapshot -> /data DB
```

is replaced with:

```text
/data DB -> run -> /data DB
```

Backups are separate immutable-ish snapshots created by an explicit backup mechanism.

## 5. SQLite strategy

OmniRoute uses SQLite and supports WAL. The application must keep the DB, WAL and SHM files together on the same durable filesystem. Graceful shutdown must be long enough to let OmniRoute checkpoint as documented by upstream.

Use SQLite-aware backup mechanisms. For example, a controlled backup can use `sqlite3 .backup` or the SQLite backup API after ensuring the source is healthy. Never treat a random copy of an active WAL database as a guaranteed consistent backup.

## 6. Backup architecture

```text
Live DB
  |
  +--> integrity check
  |
  +--> SQLite-aware backup
          |
          +--> /data/omniroute/backups/YYYYMMDD-HHMMSS/
          |       +-- storage.sqlite
          |       +-- manifest.json
          |       +-- checksum.sha256
          |
          +--> optional external backup
```

Each manifest records application version, schema version, creation time, DB size and checksums. It must never contain provider secrets.

## 7. Encryption architecture

```text
HF Secret: STORAGE_ENCRYPTION_KEY
             |
             v
      OmniRoute process
             |
             v
     encrypted DB / secret store
             |
             v
        /data/omniroute
```

The key is configuration, not data. It must survive application rebuilds and must not be regenerated when a persistent encrypted database already exists.

## 8. Failure isolation

A missing bucket is a hard persistence failure. The system must not silently create `/root/.omniroute` and continue as if everything is healthy.

A readonly bucket is a hard failure. Starting in read-only mode with stale configuration can cause operators to believe changes were saved when they were not.

A corrupt database enters recovery mode. The application should not delete it automatically. Preserve the original and restore from a verified backup.

## 9. Upgrade architecture

```text
New image
   |
   v
mount same /data
   |
   v
validate old DB
   |
   v
run upstream migrations
   |
   v
integrity check
   |
   v
start traffic
```

For risky upgrades, first create a verified backup and test the new image against a copy of the database.

## 10. Concurrency

Only one OmniRoute process should own the live SQLite database. Multiple replicas must not independently write the same SQLite file. If horizontal scaling becomes necessary, migrate durable relational state to an external database or introduce a service architecture that has one authoritative writer.

## 11. Health model

Persistence health should expose:

- `data_root`
- `database_path`
- `bucket_present`
- `writable`
- `sqlite_integrity`
- `encryption_configured`
- `backup_age`
- `database_size`
- `wal_size`
- `last_restore`
- `migration_version`

No health response should contain secrets.
