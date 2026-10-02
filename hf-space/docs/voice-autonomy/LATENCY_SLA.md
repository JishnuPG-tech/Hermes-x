# Realtime Voice Latency SLA

## 1. Objective

The Jarvis voice interface must feel conversational rather than request-response. The primary performance objective is rapid first useful audio, not completion of the entire model response.

## 2. SLA

### Primary target

First playable spoken audio should begin within **5 seconds** after the user finishes speaking.

### Hard maximum target

The runtime should target a maximum of **10 seconds** for first playable response audio under normal operating conditions.

Exceeding 10 seconds is a voice reliability incident for the turn and must be visible in telemetry.

## 3. Latency budget

Indicative budget:

| Stage | Target |
|---|---:|
| end of speech -> STT final | 0.5-1.5 s |
| STT final -> Jarvis/OmniRoute start | <=0.3 s |
| model request -> first token | 0.5-2.0 s |
| token stream -> complete first phrase | 0.2-0.8 s |
| phrase -> TTS first audio | 0.5-2.0 s |
| audio transport/playback startup | 0.1-0.5 s |
| total | aim <=5 s |

These are engineering targets, not guarantees. Provider and host performance must be measured in the deployed environment.

## 4. Fallback deadline

Kokoro is allowed a shorter primary deadline than the 10-second hard maximum.

Recommended default:

```yaml
first_audio_target_ms: 5000
fallback_trigger_ms: 4000
hard_max_ms: 10000
```

If Kokoro has not produced valid playable audio by approximately 4 seconds after receiving the first speakable phrase, EdgeTTS should be started. The exact value should be tuned from production measurements.

## 5. Why not wait 10 seconds

Waiting until the hard maximum creates an unnecessarily silent user experience. The fallback should begin early enough to leave room for the network and streaming overhead of the secondary provider.

## 6. Streaming requirement

The full LLM answer must never be required before TTS begins.

Example:

```text
LLM: "Yes, I found the issue."
      -> speak immediately

LLM: "The container is restarting because..."
      -> speak next

LLM: "I am applying the fix now."
      -> speak next
```

## 7. Metrics

Required metrics:

```text
voice_turn_total
voice_turn_error_total
stt_final_latency_ms
llm_first_token_latency_ms
tts_first_audio_latency_ms
voice_first_audio_latency_ms
voice_turn_duration_ms
kokoro_timeout_total
kokoro_error_total
edge_tts_fallback_total
edge_tts_error_total
barge_in_total
barge_in_stop_latency_ms
cold_start_first_audio_ms
warm_start_first_audio_ms
```

Use histogram buckets rather than only averages.

## 8. Percentiles

Track at minimum:

- P50
- P90
- P95
- P99

Release gate recommendation:

```text
P50 first audio <= 3.5 s
P95 first audio <= 5.0 s
P99 first audio <= 10.0 s
```

These are recommended engineering gates, not promises. The final release thresholds should be adjusted after baseline measurements.

## 9. Performance classes

Classify turns:

### Fast conversational

Examples:

- "What time is it?"
- "Explain this error."
- "Open the project notes."

Expected to meet the 5-second target consistently.

### Medium tool-assisted

Examples:

- inspect GitHub issue
- query project memory
- check server health

Still target first audio within 5 seconds, with the spoken response allowed to acknowledge and continue.

### Long autonomous

Examples:

- build a feature
- deploy a service
- run a migration
- conduct broad research

The first spoken response should be a quick acknowledgement. Completion can take minutes or longer in the background.

## 10. Cold start

Cold starts must be recorded separately because they may dominate latency in hosted environments. A cold start may prevent the normal SLA from being met even if the warm path is healthy.

Mitigations:

- keep the service warm where hosting policy permits
- preload Kokoro
- warm the STT client/model
- reuse provider connections
- initialize caches at process startup
- return a lightweight acknowledgement if a backend is warming

## 11. Regression thresholds

Any change that increases warm P95 first audio latency by more than 15% should require investigation. Changes that increase fallback frequency or P99 beyond the hard target should block release until explained.

## 12. Measurement harness

Every benchmark run should capture:

```yaml
hardware:
hermes_version:
omniroute_version:
model:
stt_provider:
tts_primary:
tts_fallback:
warm_or_cold:
utterance_length:
network_class:
first_token_ms:
kokoro_first_audio_ms:
edge_tts_first_audio_ms:
first_audio_ms:
completed_ms:
fallback_used:
```

## 13. User experience rule

The voice system should prefer saying something useful quickly over remaining silent while trying to produce a perfect long response.
