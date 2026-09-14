# Hugging Face Deployment Specification

## 1. Storage Bucket

Attach a read-write Hugging Face Storage Bucket to the Space and mount it at:

```text
/data
```

The application must not assume the bucket exists during Docker build. The mount exists at runtime.

## 2. Space Secrets

Configure stable values for:

```text
STORAGE_ENCRYPTION_KEY
JWT_SECRET
API_KEY_SECRET
INITIAL_PASSWORD
```

Use strong random values. Never place them in the Dockerfile, GitHub repository, README, logs or image layers.

## 3. Runtime environment

Production must resolve:

```text
DATA_DIR=/data/omniroute
OMNIROUTE_DATA_DIR=/data/omniroute
```

If XDG paths are used by surrounding services, keep their durable locations under `/data` as appropriate:

```text
XDG_DATA_HOME=/data/share
XDG_CONFIG_HOME=/data/config
XDG_STATE_HOME=/data/state
```

Do not move application binaries or the complete Docker filesystem to the bucket. Keep code and dependencies in the image. Persist only application state.

## 4. Startup ordering

The entrypoint should run in this order:

```text
1. Validate /data
2. Validate /data/omniroute
3. Resolve DATA_DIR
4. Run persistence preflight
5. Run migration if required
6. Start Redis/cache if needed
7. Start OmniRoute
8. Wait for health
9. Start gateway/other services
10. Report persistence status
```

## 5. Stop ordering

```text
1. Stop accepting new work
2. Stop OmniRoute gracefully
3. Allow SQLite shutdown/checkpoint
4. Stop gateway helpers
5. Stop Redis
6. Exit container
```

Use a sufficient Docker stop grace period. Do not kill the database process immediately.

## 6. Production image

Avoid:

```dockerfile
FROM diegosouzapw/omniroute:main
```

for a critical production release unless the risk is intentional.

Prefer an immutable release tag or digest after testing.

## 7. Required startup assertions

The container should refuse to start OmniRoute if:

```text
/data missing
/data not writable
/data/omniroute not writable
DATA_DIR is not /data/omniroute
existing DB cannot be validated
existing encrypted DB has no encryption key
```

## 8. Post-deployment validation

After deployment:

1. Add one provider.
2. Add one model.
3. Add one routing rule.
4. Make one API request.
5. Restart Space.
6. Verify all four.
7. Rebuild image.
8. Verify all four again.
9. Create and verify a backup.

## 9. Important platform constraint

The bucket is durable storage, not a version-control repository and not a complete disaster-recovery guarantee. Maintain verified backups separately for critical state.
