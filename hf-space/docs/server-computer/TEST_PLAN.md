# Test Plan

## 1. Persistence tests

- write a marker to `/data/jarvis`
- restart Space
- verify marker remains
- restart during active write
- verify database integrity
- verify task checkpoint recovery

## 2. Project tests

- clone new repository
- open existing project
- create concurrent workspaces
- prevent cross-workspace writes
- recover after failed Git operation
- verify branch/ref after restart

## 3. Task lifecycle tests

Test every transition:

```text
CREATE → INITIALIZE → AUTHORIZE → RUN → CHECKPOINT → VERIFY → COMPLETE
```

Also test PAUSE, RESUME, SPAWN, FAILED, RECOVER, ARCHIVE.

## 4. Recovery tests

Inject:

- model timeout
- network timeout
- tool crash
- process kill
- Space restart
- Git push timeout
- CI failure
- dependency installation failure
- database lock

Expected behavior is bounded recovery with evidence and no duplicate destructive side effects.

## 5. Security tests

Attempt:

- `../` traversal
- symlink escape
- unrelated project access
- secret-file read
- policy-file modification
- unauthorized GitHub write
- unauthorized production command
- voice command without valid authentication

All must be blocked or require the configured approval.

## 6. Verification tests

Create tasks that intentionally fail tests. The agent must not report completion until tests are repaired or the task is explicitly stopped with a failure state.

## 7. GitHub tests

- read repository
- create branch
- commit change
- push branch
- create PR when enabled
- read CI status
- diagnose failed CI
- update branch
- verify remote commit

## 8. Voice tests

- wake phrase
- continuous conversation
- barge-in
- reconnect
- STT timeout
- TTS timeout
- Kokoro unavailable
- EdgeTTS fallback
- task handoff from voice to background execution

## 9. Latency tests

Measure:

- wake detection
- end-of-turn detection
- STT first token
- model first token
- TTS first audio
- total first playable audio
- interruption latency

Acceptance target for the current design: first playable spoken audio ≤5 seconds in normal conditions, with a 10 second hard ceiling and graceful degradation when dependencies are slow.

## 10. Backup tests

- backup succeeds
- checksum/integrity check succeeds
- restore succeeds
- restored task resumes
- restored project state matches expected Git SHA

## 11. Load tests

Measure safe concurrency for:

- simultaneous voice sessions
- concurrent coding tasks
- subagent fan-out
- Git operations
- database access
- TTS requests

Use measured limits instead of assuming unlimited parallelism.

## 12. Release gate

A production release requires:

- unit tests green
- integration tests green
- persistence test green
- security tests green
- recovery tests green
- voice smoke test green
- backup restore test green
- no unresolved P0/P1 issues
