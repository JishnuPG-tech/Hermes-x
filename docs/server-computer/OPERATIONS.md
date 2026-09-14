# Operations Runbook

## Startup

1. Start the Space.
2. Verify Storage Bucket is mounted read-write.
3. Run persistence health checks.
4. Start Jarvis Gateway.
5. Start Hermes.
6. Verify OmniRoute connectivity.
7. Verify database migrations.
8. Verify project registry.
9. Verify voice providers.
10. Mark runtime ready.

## Health endpoints

Recommended checks:

```text
GET /health/live
GET /health/ready
GET /health/storage
GET /health/database
GET /health/omniroute
GET /health/voice
GET /health/tasks
```

Readiness must fail if critical durable state is unavailable.

## Restart procedure

Before planned restart:

- stop accepting new tasks
- checkpoint active tasks
- finish/abort active tool calls safely
- flush logs
- close databases
- persist scheduler state
- stop services cleanly

After restart:

- validate storage
- restore task queue
- reconcile in-flight tasks
- resume only tasks whose contract allows automatic recovery

## Crash recovery

If the Space crashes:

1. restore container
2. validate `/data/jarvis`
3. load durable task records
4. mark interrupted operations as `UNKNOWN` until reconciled
5. inspect Git and external state before retrying
6. resume from the last safe checkpoint

Never blindly replay an unknown external mutation.

## Long-running jobs

Use Hermes cron/background execution for scheduled and durable jobs. Interactive voice or browser sessions are only control surfaces. Hermes cron supports fresh sessions, persistent job definitions/run history, work directories, and recovery-oriented scheduling. 

## Log retention

Keep:

- security/audit events longer
- task traces according to debugging needs
- verbose tool output for a shorter period
- sensitive data redacted

## Incident levels

### P0
Data corruption, credential exposure, unauthorized production action.

Immediately stop affected automation, preserve evidence, revoke credentials if needed, and restore from known-good state.

### P1
Major runtime outage, task corruption, repeated failed deployments.

Disable unattended high-risk jobs and recover service.

### P2
Single feature/tool failure.

Keep unrelated work running and repair the component.

## Backup drill

At least periodically:

1. select a real backup
2. restore to isolated environment
3. verify databases
4. verify memory
5. verify project registry
6. verify task checkpoint
7. run smoke test
8. record restore result

## Capacity management

Monitor:

- CPU
- RAM
- disk/bucket usage
- process count
- concurrent agents
- queue depth
- model latency
- TTS latency
- network errors
- database size

When capacity is exhausted, queue or degrade work rather than allowing uncontrolled resource contention.

## Upgrade procedure

1. pin the intended Hermes/OmniRoute versions
2. review release notes
3. back up state
4. test in a staging Space when possible
5. deploy
6. run health and regression checks
7. verify task resumption
8. keep rollback path ready
