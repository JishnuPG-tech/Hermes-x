# Architecture Decision Records

## ADR-001: `/data/omniroute` is the live data root

### Decision
Use `/data/omniroute` as OmniRoute's production `DATA_DIR`.

### Reason
Hugging Face Storage Buckets are designed to provide persistent files at a mounted runtime path, while the normal container filesystem is ephemeral. OmniRoute explicitly supports overriding `DATA_DIR`.

### Rejected
Using `/root/.omniroute` and copying to `/data` periodically.

Reason rejected: two sources of truth and synchronization races.

## ADR-002: SQLite remains the primary local state store

### Decision
Keep SQLite for the current deployment.

### Reason
OmniRoute already uses SQLite and its data model. The immediate reliability problem is storage placement and lifecycle, not the need to replace the database.

### Future
If multiple replicas or high write concurrency are required, move authoritative state to an external relational database supported by the application architecture.

## ADR-003: Backups are separate from the live database

### Decision
Create SQLite-aware backups under a backup directory and optionally replicate them externally.

### Reason
A backup must not become a second live database.

## ADR-004: Stable encryption key

### Decision
The encryption key is supplied by the secret manager and remains stable for the lifetime of the encrypted database.

### Reason
Changing the key can make an existing encrypted DB unreadable.

## ADR-005: Fail closed on persistence failure

### Decision
Do not silently start OmniRoute against ephemeral storage when the durable bucket is unavailable.

### Reason
An apparently healthy service with a disposable database is more dangerous than an unavailable service because operators can write data that disappears.

## ADR-006: Pin production image versions

### Decision
Use a release tag or immutable image digest for production.

### Reason
A floating `main` image can change independently of the deployment repository and introduce schema or migration changes unexpectedly.

## ADR-007: Redis is non-authoritative

### Decision
Redis is cache/session infrastructure only.

### Reason
Redis can be recreated without losing authoritative provider/model configuration.

## ADR-008: Explicit migration instead of automatic guessing

### Decision
If old live and persistent copies differ, preserve both and require reconciliation rather than guessing which is newer.

### Reason
Silent automatic selection can destroy valid recent configuration.
