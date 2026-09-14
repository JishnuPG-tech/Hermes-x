# Voice Runtime Contract

## 1. Purpose

This contract defines the stable boundary between the client, voice gateway, Jarvis Core, TTS providers, and durable task system.

## 2. Transport

Preferred v1 transport: authenticated WebSocket over TLS.

Conceptual endpoint:

```text
wss://<host>/voice
```

The connection is session-oriented but execution is server-owned.

## 3. Client to server messages

### `session.open`

```json
{
  "type": "session.open",
  "voice_session_id": "optional-existing-id",
  "client_id": "android-01",
  "language": "en",
  "capabilities": {
    "wake_word": true,
    "barge_in": true,
    "audio_codec": "opus"
  }
}
```

### `audio.start`

Starts a user speech segment.

### `audio.chunk`

Carries an encoded audio frame. Frames are bounded and sequence-numbered.

### `audio.end`

Signals that the capture layer believes the user turn is complete.

### `command.cancel`

Cancels speech synthesis or, only when explicitly scoped, a running task.

## 4. Server to client messages

### `session.ready`

Contains session identifiers and server capabilities.

### `stt.partial`

Provides a partial transcript for UI feedback.

### `stt.final`

Provides the finalized user text and turn identifier.

### `assistant.text`

Carries response text chunks.

### `assistant.audio`

Carries streaming audio chunks and provider metadata.

### `assistant.state`

States include:

```text
thinking
speaking
interrupted
waiting
background_running
degraded
```

### `task.update`

Carries a durable task status update.

### `approval.request`

Requests user approval for an action classified above the automatic policy threshold.

## 5. TTS provider interface

```python
class TTSProvider:
    async def warmup(self) -> None: ...
    async def health(self) -> dict: ...
    async def start(self, text: str, session_id: str): ...
    async def cancel(self, session_id: str) -> None: ...
```

Provider implementations must never mutate global task state.

## 6. TTS manager contract

Input:

```yaml
session_id:
turn_id:
text_stream:
language:
voice_profile:
```

Output events:

```text
tts.started
tts.first_audio
tts.audio
tts.completed
tts.failed
tts.fallback_started
tts.fallback_completed
tts.cancelled
```

## 7. Task contract

A voice request that requires durable work creates the same task entity used by non-voice clients.

```yaml
task_id:
objective:
source_channel: voice
voice_session_id:
priority:
risk:
status:
plan:
current_step:
assigned_agents:
checkpoint:
verification:
next_action:
```

## 8. State ownership

| State | Source of truth |
|---|---|
| audio playback | client |
| voice connection | gateway |
| voice session | gateway + durable session store |
| transcript | gateway event store/session context |
| task | Jarvis task manager |
| model routing | OmniRoute + routing telemetry |
| memory | memory subsystem |
| approvals | policy/approval store |
| final completion | task manager + verification evidence |

## 9. Idempotency

Client retries may occur. Messages that create a durable task must contain an idempotency key. The server must return the existing task for duplicate submissions instead of creating a second task.

## 10. Reconnect

On reconnect:

1. authenticate client
2. present `voice_session_id`
3. resume session if allowed
4. report active task
5. send missed important task events
6. restore conversation context as permitted

A disconnected client must not cancel the server task unless policy explicitly defines disconnect-as-cancel.

## 11. Cancellation semantics

Three scopes:

- `speech_only`: stop current TTS.
- `turn`: stop current conversational turn where possible.
- `task`: stop the durable task and initiate safe cancellation/cleanup.

Voice interruption during playback defaults to `speech_only` and then creates a new turn. It must not silently terminate a background deployment or coding task.

## 12. Error contract

Errors should contain:

```yaml
error_id:
code:
category:
message:
retryable:
provider:
request_id:
turn_id:
task_id:
```

Never include secret values.

## 13. Event schema

```json
{
  "event_id": "evt_01",
  "timestamp": "2026-09-14T00:00:00Z",
  "request_id": "req_01",
  "voice_session_id": "vs_01",
  "turn_id": "turn_01",
  "task_id": null,
  "type": "tts.first_audio",
  "provider": "kokoro",
  "data": {
    "latency_ms": 2840
  }
}
```

## 14. Compatibility rule

The voice adapter must call stable Jarvis task APIs rather than internal implementation details of individual Hermes skills. Hermes upgrades should therefore require updating the adapter only when the upstream contract changes.
