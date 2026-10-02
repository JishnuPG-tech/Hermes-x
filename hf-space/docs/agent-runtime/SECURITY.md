# Security, Authorization & Sandboxing

## 1. Zero Trust Architecture

Hermes operates on a zero-trust model where all agent tool calls, network access, and filesystem interactions are mediated by security policy boundaries.

## 2. Core Security Controls

1. **Role-Based Access Control (RBAC)**:
   - `L0_OBSERVE`: Read-only telemetry and status.
   - `L1_PROJECT_READ`: File and repository reading.
   - `L2_PROJECT_EDIT`: File editing and branch creation.
   - `L3_TERMINAL_EXEC`: Sandboxed shell execution with time/output boundaries.
   - `L4_NETWORK_API`: Authorized outbound HTTP integrations.
   - `L5_GITHUB_WRITE`: PR creation and issue updates.
   - `L6_DEPLOY`: Staging and production container orchestration.
   - `L7_SYS_ADMIN`: Administrative configuration.
   - `L8_DESTRUCTIVE`: Permanent deletions, database drops, and schema purges (requires explicit two-man rule / biometric or mobile authorization).

2. **PolicyGuard Interception**:
   - `PolicyGuard` inspects every tool call before invocation.
   - Prohibits path traversal attacks (e.g. `../../etc/shadow`).
   - Restricts commands to authorized project workspaces.

3. **Credential & Secret Redaction**:
   - `RuntimeEventBus` and tool loggers automatically sanitize and redact API keys, Bearer tokens, passwords, and private SSH keys from telemetry and event streams before they are emitted or persisted.
