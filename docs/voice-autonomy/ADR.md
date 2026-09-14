# Voice Autonomy Architectural Decision Records

## ADR-001: Voice is an interface to Jarvis, not a second agent

**Status:** Accepted

### Decision

All voice requests enter the same Jarvis Core task and session system used by text clients.

### Rationale

A second execution system would create duplicated memory, policy, task state, and recovery logic. A single runtime makes device switching and background execution reliable.

### Consequence

The voice gateway must remain thin and must call stable Jarvis APIs.

---

## ADR-002: Use WebSocket first

**Status:** Accepted

### Decision

Use authenticated WebSocket transport for v1 realtime voice.

### Rationale

It provides bidirectional streaming with a simpler operational model than starting with full WebRTC. WebRTC may be added later when browser-level media optimization or peer media requirements justify it.

### Consequence

Audio framing, backpressure, heartbeat, reconnect, and codec handling are explicit application responsibilities.

---

## ADR-003: Kokoro is the primary TTS engine

**Status:** Accepted

### Decision

Use Kokoro as the local primary TTS path when measured performance in the deployed environment is sufficient.

### Rationale

Kokoro is lightweight at 82M parameters and uses Apache-2.0 licensed weights. Its small footprint makes in-process CPU hosting practical compared with larger speech models.

### Consequence

The service must benchmark real deployment latency and keep the model warm. Published benchmarks on faster hardware cannot be used as a direct guarantee for the hosted environment.

---

## ADR-004: EdgeTTS is a bounded emergency fallback

**Status:** Accepted

### Decision

Use EdgeTTS as a secondary online TTS provider when Kokoro is late or unhealthy.

### Rationale

The primary user experience requires fast first audio and should not depend on one local TTS path. A second provider reduces single-provider failure risk.

### Consequence

EdgeTTS introduces network and external-data considerations. A sensitivity filter must prevent secrets and prohibited data from reaching the external provider.

---

## ADR-005: Fail over before the hard maximum

**Status:** Accepted

### Decision

Start fallback around 4 seconds by default when Kokoro has not produced playable first audio, while keeping a 10-second hard target.

### Rationale

Waiting 10 seconds before starting the fallback leaves no time for the secondary provider to connect and play audio. Earlier failover protects the user experience.

### Consequence

The deadline must be measured from the actual phrase synthesis start, and false failovers must be monitored.

---

## ADR-006: Stream LLM output into TTS

**Status:** Accepted

### Decision

TTS receives sentence or clause chunks from the streaming LLM output rather than waiting for full response completion.

### Rationale

This is the primary mechanism for reducing time-to-first-audio.

### Consequence

A phrase chunker is required to balance latency and natural speech. Tiny token-level synthesis is rejected because it creates choppy speech.

---

## ADR-007: Background work is durable

**Status:** Accepted

### Decision

Long-running operations create durable tasks independent from the voice session.

### Rationale

Users must be able to disconnect their phone, reconnect later, and still have the task progressing.

### Consequence

Task state requires persistence, checkpoints, idempotency, and verification.

---

## ADR-008: Barge-in cancels speech, not the task

**Status:** Accepted

### Decision

A user interruption stops active TTS playback and starts a new conversational turn. It does not automatically cancel a background task.

### Rationale

Talking over a status update should not accidentally terminate deployments, builds, or other autonomous work.

### Consequence

Cancellation must have explicit scopes: speech, turn, and task.

---

## ADR-009: Keep Kokoro inside the Hermes Space initially

**Status:** Accepted

### Decision

Do not create a separate Kokoro Space until measurement proves that in-process synthesis is the bottleneck.

### Rationale

Kokoro is lightweight and an extra service introduces network latency, another failure domain, and consumes valuable hosting capacity.

### Consequence

The Hermes Space must monitor CPU/RAM and TTS queueing carefully. A dedicated TTS worker remains an approved future scale-out option.

---

## ADR-010: Do not promise unlimited autonomy

**Status:** Accepted

### Decision

Document autonomy as bounded by provider, model, infrastructure, permissions, and safety constraints.

### Rationale

A trustworthy engineering system must expose limits and preserve approval controls rather than claim fictional omnipotence.

### Consequence

The product goal is reliable autonomous completion with recovery and evidence, not unrestricted execution.
