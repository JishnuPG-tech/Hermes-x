# Voice-First Jarvis Implementation Plan

## 1. Delivery strategy

Implement the voice system as a thin realtime interface over the existing Jarvis and Hermes runtime. Do not create a second task engine or second memory system.

Priority order:

```text
Correctness
-> durable sessions
-> low latency
-> failover
-> interruption
-> observability
-> security hardening
-> optimization
```

## 2. Workstreams

### W1 Voice transport

Build authenticated WebSocket transport with reconnect, heartbeat, audio framing, session binding, and client capabilities.

Deliverables:

- `/voice` WebSocket endpoint
- session manager
- audio frame protocol
- heartbeat
- reconnect semantics
- max frame and rate limits

### W2 Input pipeline

Implement:

- wake trigger integration
- VAD
- streaming STT adapter
- final turn detector
- partial transcript events
- interruption detection

### W3 Jarvis voice adapter

Map a final transcript to the same Jarvis request API used by text clients.

Input contract:

```json
{
  "session_id": "...",
  "channel": "voice",
  "text": "inspect the deployment",
  "language": "en",
  "attachments": [],
  "timestamp": "..."
}
```

### W4 LLM streaming

Verify that the OmniRoute request path supports streamed output. Expose first-token timing. Add a phrase chunker so TTS receives usable text before the final answer is complete.

### W5 Kokoro provider

Implement local `KokoroProvider`.

Tasks:

1. Pin a tested model version.
2. Download/cache weights during image build or first boot depending image size policy.
3. Load once.
4. Warm up at startup.
5. Expose synthesis and health APIs.
6. Convert output into the gateway audio format.
7. Add cancellation support.
8. Add CPU contention safeguards.

### W6 EdgeTTS provider

Implement `EdgeTTSProvider` behind the same interface.

Tasks:

1. Configure a selected voice.
2. Create streaming synthesis adapter.
3. Add network timeout.
4. Add provider health state.
5. Add retry policy with bounded attempts.
6. Ensure transcripts and secrets are not logged unnecessarily.

### W7 TTS failover manager

Implement a state machine rather than scattered timeout logic.

```text
READY
-> PRIMARY_SYNTHESIS
-> PRIMARY_PLAYBACK
or
-> FALLBACK_SYNTHESIS
-> FALLBACK_PLAYBACK
or
-> DEGRADED
```

### W8 Barge-in

Implement a session-local cancellation path that can stop TTS while retaining the parent task.

### W9 Durable background tasks

Voice commands such as deployment, code generation, repository migration, or long research must create durable tasks. Worker state must survive voice client disconnects.

### W10 Observability

Emit structured events and metrics described in `RUNTIME_CONTRACT.md` and `LATENCY_SLA.md`.

### W11 Security

Bind voice actions to the same authentication and authorization policies as text actions. Add risk labels and approval gates.

### W12 Mobile/laptop client

Start with a simple WebSocket test client. Add a production client only after the server pipeline passes latency and failover tests.

## 3. Suggested code layout

```text
src/jarvis/
  voice/
    __init__.py
    gateway.py
    session.py
    protocol.py
    wake.py
    vad.py
    stt/
      base.py
      streaming.py
    llm/
      stream.py
      chunker.py
    tts/
      base.py
      kokoro.py
      edge_tts.py
      manager.py
      queue.py
    interruption.py
    metrics.py

  core/
  tasks/
  orchestration/
  verification/
  recovery/
  memory/
  policy/
```

## 4. Suggested configuration

```yaml
voice:
  enabled: true
  transport: websocket
  max_session_idle_seconds: 1800

  wake:
    enabled: true
    phrase: "Hermes"

  turn:
    silence_ms: 650
    max_utterance_ms: 30000

  latency:
    first_audio_target_ms: 5000
    fallback_trigger_ms: 4000
    hard_max_ms: 10000

  tts:
    primary: kokoro
    fallback: edge_tts
    preload: true
    warmup: true
    phrase_buffer_chars: 120

  edge_tts:
    enabled: true
    voice: "en-US-EmmaMultilingualNeural"
    connect_timeout_ms: 1500
    first_audio_timeout_ms: 5000

  barge_in:
    enabled: true
    stop_playback_ms: 300
```

Values are defaults for implementation, not universal truths. They must be tuned from production measurements.

## 5. Milestones

### M1 Protocol

- WebSocket connects.
- Authentication succeeds.
- Audio frames arrive.
- Session is durable.

### M2 STT

- Streaming transcript.
- Turn finalization.
- Reconnect.

### M3 LLM streaming

- First token measured.
- Phrase chunking.
- Text fallback.

### M4 Kokoro

- Warm startup.
- Synthesis benchmark.
- First audio measured.

### M5 EdgeTTS failover

Inject Kokoro delays and errors. Confirm fallback starts without losing the request.

### M6 Barge-in

Speak while Jarvis is speaking. Confirm audio stops quickly and new request continues.

### M7 Background autonomy

Start a task, disconnect client, reconnect, confirm task state and eventual notification.

### M8 Security

Test cross-session access, invalid tokens, unauthorized high-risk commands, and malicious prompt content.

### M9 Production acceptance

Run 100+ warm turns across short, medium, and long responses. Record P50, P95, and failure rate.

## 6. Latency optimization sequence

Do not optimize randomly. Measure each stage:

```text
capture
-> STT final
-> Jarvis classify
-> OmniRoute request
-> LLM first token
-> phrase chunk
-> Kokoro first audio
-> playback
```

Optimize the largest contributor first.

Likely optimizations:

- Keep Kokoro loaded.
- Keep connections warm.
- Reuse HTTP/WebSocket connections.
- Use streaming LLM output.
- Speak the first complete clause quickly.
- Avoid excessive context retrieval for simple commands.
- Use fast models for short conversational turns.
- Run long tasks asynchronously.

## 7. Rollout policy

Start behind a feature flag:

```text
voice_enabled=false
```

Then enable for the owner account only.

Recommended rollout:

1. local test client
2. owner device
3. owner plus one backup device
4. continuous benchmark
5. broader access only after security review

## 8. Definition of done

A voice release is complete only when:

- all required tests pass
- latency metrics exist
- fallback is tested with injected failures
- reconnect is tested
- task persistence is tested
- approval gates are tested
- no secrets appear in logs
- actual deployment latency is documented
