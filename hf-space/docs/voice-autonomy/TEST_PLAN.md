# Voice-First Jarvis Test Plan

## 1. Test objectives

Validate that the voice interface is conversational, fast, recoverable, secure, and fully integrated with durable Jarvis execution.

## 2. Test layers

```text
Unit
  -> Integration
  -> Contract
  -> Performance
  -> Chaos
  -> Security
  -> End-to-end
  -> Soak
```

## 3. Unit tests

### Voice session

- create session
- resume session
- duplicate open
- idle timeout
- session close
- state transition validation

### Turn detector

- speech start
- speech end
- short pause
- long pause
- maximum utterance
- noise-only input

### TTS manager

- provider selection
- successful Kokoro
- Kokoro timeout
- Kokoro exception
- empty audio
- EdgeTTS fallback
- both provider failure
- generation cancellation
- stale chunk rejection

### Chunker

- word boundary
- punctuation boundary
- short phrase
- long paragraph
- code block handling
- incomplete sentence buffering

### Policy

- automatic action
- approval-required action
- forbidden action
- expired approval
- wrong task approval

### Task persistence

- create
- checkpoint
- crash recovery
- reconnect
- idempotent retry
- completion with evidence

## 4. Contract tests

Validate that client and server agree on:

- message types
- required fields
- audio codec
- sequence numbers
- error schema
- session identifiers
- task identifiers

Validate the Jarvis Core API independently from the voice transport.

## 5. Integration tests

### STT

Speak fixed test phrases and verify transcript accuracy under supported languages and expected noise levels.

### OmniRoute

Verify streamed model output arrives incrementally and first-token timing is captured.

### Kokoro

Verify model loads, warmup succeeds, synthesis produces expected audio format, and cancellation works.

### EdgeTTS

Verify network synthesis, timeout handling, and cancellation.

### GitHub

Voice request:

> "Check repository status."

Expected: read-only operation and truthful result.

Voice request requiring write should enter the normal policy path.

## 6. Failover tests

### T1 Kokoro timeout

Inject a 5-second delay.

Expected:

- fallback begins near configured deadline
- EdgeTTS audio plays
- no duplicate audio
- task continues
- event log records provider switch

### T2 Kokoro exception

Expected immediate fallback for the affected chunk.

### T3 Invalid audio

Return malformed audio from the mocked Kokoro provider.

Expected fallback.

### T4 EdgeTTS failure

Expected text response and preserved task/session state.

### T5 Both fail

Expected no task loss, no endless retry loop, and clear degraded status.

## 7. Barge-in tests

While Jarvis is speaking:

1. speak an interruption
2. confirm playback stops
3. confirm current TTS generation is cancelled
4. confirm new STT turn is captured
5. confirm unrelated background task continues

Measure stop latency.

## 8. Performance tests

Create benchmark suites for:

- 5-word query
- 15-word query
- 30-word query
- tool-assisted query
- long autonomous task acknowledgement

For each capture:

```text
warm/cold
STT final
LLM first token
Kokoro first audio
EdgeTTS first audio
first playback
complete response
```

Release target:

```text
P50 <= 3.5 s
P95 <= 5 s
P99 <= 10 s
```

These are recommended gates and may be updated from real deployment evidence.

## 9. Load tests

Test concurrent sessions at increasing levels. Observe:

- CPU
- RAM
- STT throughput
- TTS queue delay
- connection count
- provider rate limits
- task worker contention

Kokoro may need bounded concurrency on CPU to protect tail latency.

## 10. Soak tests

Run voice sessions for hours with periodic requests and interruptions.

Look for:

- memory leaks
- audio buffer growth
- stale sessions
- file descriptor leaks
- task queue growth
- degraded latency over time

## 11. Chaos tests

Inject:

- WebSocket disconnect
- packet loss
- STT timeout
- OmniRoute timeout
- model provider failure
- Kokoro crash
- EdgeTTS outage
- process restart
- database lock
- disk pressure
- high CPU
- high memory

Expected outcome is graceful degradation and task persistence.

## 12. Security tests

- expired token
- invalid token
- replayed request
- duplicate request
- cross-user task identifier
- unauthorized GitHub write
- unauthorized deployment
- prompt injection from repository
- prompt injection from web result
- secret leakage to EdgeTTS
- oversized audio packet
- malformed JSON
- connection flooding

## 13. E2E scenarios

### Scenario A: simple question

```text
wake -> ask -> STT -> model -> Kokoro -> answer
```

Must meet warm latency target.

### Scenario B: Kokoro failure

```text
wake -> ask -> model -> Kokoro timeout -> EdgeTTS -> answer
```

Must remain conversational and not duplicate speech.

### Scenario C: long engineering task

```text
voice command
 -> acknowledgement
 -> durable task
 -> plan
 -> agents
 -> code
 -> tests
 -> PR
 -> CI
 -> recovery if needed
 -> verify
 -> notification
```

### Scenario D: reconnect

Disconnect during task execution. Reconnect from another device. Verify the same task appears.

### Scenario E: risky action

Voice requests destructive operation. Verify approval gate and audit trail.

## 14. Acceptance checklist

A build can be marked voice-ready only when:

- warm P95 first audio <= 5 seconds
- P99 first audio <= 10 seconds
- fallback tests pass
- interruption tests pass
- reconnect tests pass
- background task survives disconnect
- no secret leakage is detected
- high-risk operations require approval
- text fallback works
- all critical metrics are emitted
