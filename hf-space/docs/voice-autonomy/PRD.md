# Voice-First Jarvis Product Requirements Document

**Status:** Proposed production baseline  
**Version:** 1.0  
**Date:** 2026-09-14

## 1. Product definition

Jarvis is a persistent voice-first autonomous assistant built above Hermes Agent. A user can speak naturally from a phone or laptop, invoke Jarvis with a wake phrase, receive a spoken response quickly, interrupt it naturally, and launch work that continues independently in the background.

Voice is not a separate execution path. Every request enters the same Jarvis task system used by text clients. This guarantees continuity of memory, agents, tools, approvals, GitHub work, and task status.

## 2. Core user promise

The user should be able to say:

> "Hermes, inspect the failing deployment, fix it, run the tests, push the fix, watch CI, and tell me when it is ready."

Jarvis should acknowledge promptly, execute the work, recover from recoverable failures, verify each important milestone, and report a truthful final result.

## 3. Goals

### G-01 Natural voice conversation

Support wake word, streaming input, turn detection, streaming response, TTS, interruption, cancellation, and session continuity.

### G-02 Fast first response

The system shall target first playable spoken audio within 5 seconds after end of user speech and shall treat 10 seconds as the hard maximum target for normal conditions.

### G-03 Graceful TTS degradation

Kokoro shall be the default local TTS engine. If Kokoro fails or does not emit playable audio within the configured failover deadline, EdgeTTS shall take over automatically.

### G-04 Autonomous background execution

Requests that cannot finish within a conversational turn shall immediately transition to a durable background task. The spoken response shall be a concise acknowledgement plus the expected next event, not a wait for task completion.

### G-05 Shared context

Voice and text clients must share the same session and task state. Switching devices must not create a second copy of the task.

### G-06 Independent verification

Jarvis must never mark a task complete based only on model-generated claims. Completion requires verification evidence appropriate to the task.

### G-07 Safe autonomy

Read and routine low-risk operations should be automatic. High-risk and destructive operations require explicit approval and audit trails.

### G-08 Long-running reliability

Tasks must survive process restarts, network reconnects, client disconnects, and recoverable provider failures through durable checkpoints.

## 4. User stories

### Voice

- As a user, I can say a configured wake phrase and start talking without tapping a button.
- As a user, I can interrupt Jarvis while it is speaking.
- As a user, I can say stop or cancel and have active speech and execution respond appropriately.
- As a user, I can ask a follow-up question without re-explaining the project context.
- As a user, I hear the first useful part of a response before the model has finished generating the entire answer.

### Autonomous execution

- As a user, I can give a complex task and leave the app while the work continues.
- As a user, I can reconnect and see the same task state.
- As a user, I can receive a notification when the task completes, fails, or needs approval.

### Engineering

- As a user, Jarvis can inspect GitHub state, make changes, run tests, create PRs, monitor CI, fix failures, and verify remote state when policy permits.

### Operations

- As a user, Jarvis can diagnose configured services and safely repair known failures within its permission scope.

## 5. Functional requirements

### VR-01 Voice session manager

Maintain a stable `voice_session_id` mapped to the normal Jarvis session. Track listening, thinking, speaking, interrupted, reconnecting, and idle states.

### VR-02 Wake phrase

Support a configurable wake phrase. Wake detection should be local when the selected client stack supports it, reducing unnecessary network audio transport.

### VR-03 Streaming STT

Audio must be processed incrementally. Partial transcripts may be displayed but should not trigger final execution until turn detection confirms the user has completed the request, unless an explicit low-latency command mode is enabled.

### VR-04 Turn detection

Use VAD and silence detection. The client should stop sending a completed turn after a configurable silence interval while supporting barge-in during TTS playback.

### VR-05 Streaming LLM

Jarvis must request streaming text where available. The voice adapter should buffer complete words and sentence or clause boundaries so TTS can begin before the full answer is generated.

### VR-06 TTS arbitration

The TTS manager shall expose a provider-neutral interface:

```text
speak_stream(text_stream, session_id) -> audio_stream
cancel(session_id)
health() -> provider_health
```

Provider order:

1. Kokoro local.
2. EdgeTTS fallback.

### VR-07 Failover

The TTS manager shall activate EdgeTTS when any configured failover condition occurs, including primary timeout, synthesis exception, broken audio stream, repeated provider unhealthy state, or inability to produce first audio before the fallback deadline.

### VR-08 Barge-in

When speech is detected while TTS is playing, audio playback should stop within the configured interruption budget and the new user utterance should become the active turn.

### VR-09 Background task acknowledgement

For long tasks, Jarvis should speak a concise acknowledgement such as:

> "Got it. I am working on it now."

The full task proceeds asynchronously.

### VR-10 Voice-safe responses

Spoken responses should be concise. Long code, logs, diffs, research, and detailed reports should be available as text or artifacts while voice delivers the important summary.

## 6. Non-functional requirements

### NFR-01 Latency

- First audio target: <= 5,000 ms.
- Fallback activation target: approximately 4,000 ms with configurable jitter tolerance.
- Hard maximum target: <= 10,000 ms for first playable response audio under normal operating conditions.
- Long jobs: immediate acknowledgement before background execution.

The precise measured latency depends on network, STT, model provider, cold starts, CPU contention, and TTS implementation. Published Kokoro benchmarks are on hardware faster than a typical 2-vCPU hosted CPU instance, so production acceptance must be based on measurements from the actual deployment environment rather than published hardware results.

### NFR-02 Availability

The voice service should degrade gracefully if one provider is unavailable. Text interaction must remain usable even if voice is unavailable.

### NFR-03 Recovery

A transient TTS failure must not destroy the Jarvis task or voice session.

### NFR-04 Security

Audio transport must be authenticated and encrypted in transit. Secrets must not be exposed in prompts, logs, browser payloads, or audio transcripts unless explicitly required and authorized.

### NFR-05 Observability

Every voice turn should have correlation identifiers and timing markers for wake detection, STT, LLM first token, TTS start, first audio, provider switches, interruptions, and final completion.

### NFR-06 Resource efficiency

Kokoro should be loaded once and kept warm. Models should not be loaded and unloaded for each utterance.

## 7. Acceptance criteria

A release is voice-ready when all are true:

1. Wake phrase triggers a session reliably in supported clients.
2. User speech is transcribed and routed to Jarvis.
3. LLM streaming starts before the complete answer is available.
4. Kokoro produces first audio within the target for normal benchmark utterances.
5. EdgeTTS activates automatically during injected Kokoro delays and failures.
6. The user can interrupt speech and continue naturally.
7. A long-running task continues after the client disconnects.
8. Reconnection resumes the same task and session.
9. Task completion is backed by verification evidence.
10. Metrics expose the latency timeline and provider chosen.

## 8. Out of scope for v1

- Training a custom speech model.
- Always-on microphone capture without an explicit wake mechanism.
- Literal human-level autonomy claims.
- Building a new operating-system kernel.
- Guaranteed zero-downtime operation on constrained or sleeping hosting.
