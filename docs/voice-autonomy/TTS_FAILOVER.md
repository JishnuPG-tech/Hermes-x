# TTS Failover Design: Kokoro -> EdgeTTS

## 1. Goal

Provide reliable spoken output without making the voice system wait for a single TTS provider indefinitely.

## 2. Provider roles

### Kokoro

Primary provider. Runs locally with the Jarvis/Hermes runtime. Kokoro is an 82M parameter open-weight TTS model with Apache-2.0 licensed weights. It is intended to provide low-cost, local, predictable synthesis.

### EdgeTTS

Fallback provider. Used when Kokoro cannot produce audio quickly enough or enters an unhealthy state. It is network dependent and therefore should not be treated as the only voice path.

## 3. Provider state

Each provider has:

```text
unknown
healthy
warming
busy
degraded
unhealthy
cooldown
```

The manager uses provider health plus per-request deadlines.

## 4. Normal path

```text
LLM phrase
   |
   v
TTS manager
   |
   v
Kokoro
   |
   v
first audio
   |
   v
client playback
```

## 5. Failover path

```text
LLM phrase
   |
   v
Kokoro request
   |
   +-- first audio before deadline -> play Kokoro
   |
   +-- error/timeout/invalid audio
                |
                v
           EdgeTTS request
                |
                v
          first audio -> play
```

## 6. Fallback triggers

EdgeTTS is eligible when any of the following is true:

1. Kokoro is unhealthy.
2. Kokoro synthesis throws an exception.
3. Kokoro returns an empty or invalid stream.
4. Kokoro misses the first-audio deadline.
5. Repeated Kokoro requests timeout.
6. Host resource pressure causes a defined overload condition.

## 7. Deadline behavior

Recommended default:

```yaml
fallback_trigger_ms: 4000
hard_max_ms: 10000
```

The fallback timer begins when a complete speakable phrase is available and the primary provider has been asked to synthesize it. For short phrases, a slightly shorter internal deadline may be used.

## 8. Do not duplicate speech

Once one provider begins valid playback, the other provider must be cancelled or its result discarded. The user should never hear the same phrase twice.

A provider generation ID prevents late packets from a cancelled stream from reaching playback.

```yaml
tts_generation_id:
provider:
session_id:
turn_id:
chunk_id:
```

## 9. Chunk-level failover

The system may switch providers between phrase chunks.

Example:

```text
Chunk 1 -> Kokoro succeeds
Chunk 2 -> Kokoro times out
Chunk 2 -> EdgeTTS succeeds
Chunk 3 -> EdgeTTS continues
```

This avoids restarting the entire answer. However, switching frequently can create voice inconsistency, so the manager should prefer a stable provider for the rest of a turn once a fallback becomes necessary.

## 10. Voice consistency

The configured EdgeTTS voice should be selected to match the desired speaking style as closely as possible. Exact acoustic identity between different engines is not guaranteed.

The voice personality configuration should be separate from the language model personality:

```yaml
voice_personality:
  style: young_adult_feminine
  tone: calm
  warmth: high
  energy: low
  clarity: high
  expressiveness: medium
  speed: 0.95
```

This describes a style and should not be interpreted as impersonation of a real person without consent.

## 11. Recovery after fallback

Fallback does not permanently disable Kokoro.

```text
Kokoro timeout
  -> EdgeTTS speaks
  -> record failure
  -> increment provider failure counter
  -> put Kokoro in cooldown if threshold reached
  -> continue with EdgeTTS
  -> health check Kokoro
  -> next turn may retry Kokoro
```

Use exponential backoff for provider health probing.

## 12. Circuit breaker

Recommended state machine:

```text
CLOSED
  |
  | repeated failures
  v
OPEN
  |
  | cooldown elapsed
  v
HALF_OPEN
  |
  +-- success --> CLOSED
  +-- failure --> OPEN
```

Suggested starting values:

```yaml
failure_threshold: 3
cooldown_seconds: 30
half_open_probe_timeout_ms: 3000
```

These are tunable defaults.

## 13. Security and privacy

Kokoro keeps synthesis local to the runtime. EdgeTTS sends synthesis input to an external online provider, so the application must classify what text is eligible for fallback.

Recommended policy:

- normal conversational text: allowed
- public or low-sensitivity task summaries: allowed
- secrets, tokens, credentials, private keys: never synthesize through EdgeTTS
- highly sensitive project content: configurable deny or redact policy

The TTS layer should redact obvious secrets before any external fallback request.

## 14. Failure handling

If both providers fail:

1. stop speaking attempts
2. send text response to the client
3. preserve task/session state
4. record voice degradation
5. retry according to bounded health policy
6. notify only when the failure materially affects the user

## 15. Testing matrix

| Failure | Expected behavior |
|---|---|
| Kokoro slow | EdgeTTS starts before hard timeout |
| Kokoro exception | immediate fallback |
| Empty Kokoro audio | fallback |
| EdgeTTS network loss | text fallback |
| Both unavailable | text response, task preserved |
| User barges in | cancel current provider and listen |
| Disconnect during fallback | task/session preserved |
| Provider recovers | future turn can use Kokoro |

## 16. Operational metrics

Track:

```text
kokoro_requests
kokoro_success
kokoro_timeout
kokoro_error
kokoro_first_audio_ms
edge_tts_requests
edge_tts_fallbacks
edge_tts_success
edge_tts_error
edge_tts_first_audio_ms
both_tts_failed
fallback_rate
provider_circuit_open_total
```

## 17. Key rule

The fallback exists to protect responsiveness. It must never change the user's requested task or silently alter execution permissions.
