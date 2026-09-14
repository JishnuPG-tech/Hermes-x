# Persistence Security Model

## Principles

1. Durable state and secrets are different classes of data.
2. GitHub is for source code and documentation, not secrets.
3. Hugging Face Space Secrets are the authority for runtime secrets.
4. The persistent bucket should contain only data required by the application and backups.
5. Least privilege is preferred over blanket permissions.

## Secrets

The following must never be committed:

- `STORAGE_ENCRYPTION_KEY`
- `JWT_SECRET`
- `API_KEY_SECRET`
- `INITIAL_PASSWORD`
- provider API keys
- OAuth refresh tokens
- session cookies
- private keys

Use placeholder names in examples.

## Encryption

Where OmniRoute supports encrypted SQLite storage, keep the encryption key stable across deployments. A new key must not be generated automatically when an existing encrypted DB is detected.

Recommended generation method for initial provisioning:

```bash
openssl rand -hex 32
```

Store the result only in the secret manager.

## Access control

The OmniRoute data directory should be writable only by the runtime account and required helper processes. Avoid `chmod 777` in production unless the platform runtime makes it unavoidable and the risk is accepted.

## Backup security

Backups can contain provider credentials or encrypted application state. Treat them as sensitive.

If backups leave the trusted HF bucket:

- encrypt them
- restrict access
- use checksums
- maintain retention
- audit restores

## Logs

Redact:

- Authorization headers
- API keys
- cookies
- OAuth tokens
- encryption keys
- passwords
- complete provider credentials

Diagnostics may report identifiers, sizes, hashes and boolean health values.

## Threat model

### Threat: container filesystem reset

Mitigation: direct DATA_DIR to `/data/omniroute`.

### Threat: stale snapshot overwrite

Mitigation: eliminate startup copy-back and use explicit migration/backup logic.

### Threat: encryption key regeneration

Mitigation: stable HF Secret and fail-closed startup.

### Threat: readonly database

Mitigation: preflight checks for directory write access and SQLite write capability.

### Threat: backup corruption

Mitigation: SQLite-aware backup, checksum and restore verification.

### Threat: malicious application update

Mitigation: pin release/digest, backup before upgrade and verify migrations.

### Threat: accidental deletion

Mitigation: backup retention and explicit restore workflow.

### Threat: Redis data loss

Mitigation: Redis never stores authoritative configuration.

## GitHub policy

Never place real `.env` files, provider credentials or bucket snapshots in the repository. Add `.gitignore` rules and secret scanning where appropriate.
