# Voice Runtime Operations Runbook

## 1. Production components

```text
Hermes Space
  - Hermes Agent
  - Jarvis Core
  - Voice Gateway
  - Streaming STT adapter
  - Kokoro
  - EdgeTTS adapter
  - task worker
  - persistent application state

OmniRoute Space
  - OmniRoute
  - model/provider routing

Optional third worker
  - only after measurement identifies a bottleneck
```

## 2. Startup order

Recommended startup:

1. load configuration
2. validate required secrets
3. validate persistent storage
4. initialize task database
5. initialize memory
6. initialize Hermes
7. initialize OmniRoute client
8. load Kokoro
9. warm Kokoro
10. initialize EdgeTTS adapter
11. start voice gateway
12. start task workers
13. start watchdog

The service should report readiness only after critical dependencies have passed their health checks or are explicitly allowed to be degraded.

## 3. Health endpoints

Conceptual:

```text
GET /health/live
GET /health/ready
GET /health/voice
GET /health/tts
GET /health/tasks
```

`/health/live` should remain lightweight. Readiness should fail when a dependency required for normal operation is unavailable.

## 4. Voice health

Report:

```yaml
voice:
  enabled:
  active_sessions:
  stt:
    status:
  tts:
    primary: kokoro
    primary_status:
    fallback: edge_tts
    fallback_status:
  last_first_audio_ms:
  fallback_rate:
```

## 5. Warmup

Kokoro should be warmed at process startup with a short non-sensitive phrase. Do not store warmup audio permanently.

Warmup should record:

```text
model_load_ms
warmup_ms
first_real_synthesis_ms
```

## 6. Incident: slow first audio

Symptoms:

- users report long silence
- P95 or P99 first audio exceeds target
- fallback frequency increases

Procedure:

1. inspect `voice_first_audio_latency_ms`
2. compare STT final latency
3. compare LLM first-token latency
4. compare Kokoro first-audio latency
5. compare cold versus warm turns
6. check CPU and memory pressure
7. inspect OmniRoute provider latency
8. test EdgeTTS independently
9. classify the dominant bottleneck

Do not change multiple layers at once before collecting baseline metrics.

## 7. Incident: Kokoro unhealthy

Procedure:

1. check process logs
2. check available RAM
3. check CPU saturation
4. run isolated synthesis probe
5. verify model files/checksum
6. restart provider if safe
7. allow EdgeTTS fallback
8. monitor fallback rate
9. retry Kokoro through circuit breaker

## 8. Incident: EdgeTTS unavailable

The system should not fail the Jarvis task. Continue with:

- text response
- client-side or alternate TTS if configured later
- normal durable task execution

Record a voice degradation event.

## 9. Incident: both TTS providers unavailable

Expected behavior:

```text
No audio
  -> send assistant text
  -> preserve session
  -> preserve task
  -> record voice outage
  -> notify if persistent
```

## 10. Incident: OmniRoute slow

The voice layer should not wait indefinitely.

Use:

- fast routing for simple conversational turns
- bounded model request timeouts
- streamed responses
- immediate progress acknowledgement for long work
- retry or alternate provider selection according to routing policy

## 11. Incident: task continues after disconnect

This is expected behavior for background tasks. Verify:

1. task state is durable
2. worker remains active
3. reconnect reports same `task_id`
4. no duplicate task was created
5. notifications still function

## 12. Recovery and restart

The service should restart safely. On recovery:

```text
load durable tasks
  |
identify running-at-crash
  |
mark interrupted/checkpoint state
  |
resume only idempotent/recoverable steps
  |
verify external state
  |
continue or require approval
```

Never blindly repeat a non-idempotent external operation after a crash without checking remote state.

## 13. Backups

Back up:

- task database
- memory metadata
- strategy/skill records
- configuration excluding secret material
- required operational state

Test restore, not just backup creation.

## 14. Deployment policy

Production images should pin versions of:

- Hermes
- TTS dependencies
- Python runtime
- system packages where practical

Record the exact deployed versions in the deployment metadata.

## 15. HF hosting considerations

CPU Basic is currently documented as 2 vCPU and 16 GB RAM with no hardware hourly charge. Creating a new compute Space such as Docker or Gradio currently requires a paid plan, so existing project slots should be treated as valuable capacity. Hosting lifecycle and sleeping behavior must be considered when claiming latency objectives.

The latency SLA should therefore have separate warm and cold measurements.

## 16. Operational dashboards

Minimum panels:

- active voice sessions
- first-audio P50/P95/P99
- STT latency
- LLM first-token latency
- Kokoro latency
- EdgeTTS latency
- fallback rate
- interruption rate
- task completion rate
- background task age
- CPU/RAM
- provider errors
- reconnect count

## 17. Change management

Any change to:

- STT model
- LLM route
- Kokoro model/runtime
- EdgeTTS voice
- audio codec
- VAD thresholds
- silence timeout
- scheduler
- security policy

requires regression testing against voice latency and correctness.
