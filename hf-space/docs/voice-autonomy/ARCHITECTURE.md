# Voice-First Jarvis Architecture

## 1. Architectural goal

Provide a low-latency conversational surface to the existing durable Jarvis runtime while preserving one execution model across voice, text, mobile, and laptop clients.

## 2. Reference topology

```text
                         MOBILE / LAPTOP
                               |
                     Mic + Speaker + VAD
                               |
                       Wake Word Detector
                               |
                        WebSocket / TLS
                               |
                 +-------------v--------------+
                 |       VOICE GATEWAY        |
                 | Session + Auth              |
                 | Audio transport             |
                 | Turn detection              |
                 | Streaming STT               |
                 | Barge-in                    |
                 | Streaming TTS arbitration   |
                 +-------------+--------------+
                               |
                         Jarvis Core API
                               |
        +----------------------+----------------------+
        |                      |                      |
        v                      v                      v
   Agent OS Core          Hermes Harness          Memory
        |                      |                      |
        |              tools / terminal / browser     |
        |                      |                      |
        +-------------+--------+----------------------+
                      |
                      v
                 OmniRoute Space
                      |
               model/provider set
                      |
      +---------------+----------------+
      |                                |
      v                                v
  fast model                     reasoning model

TTS inside Hermes Space initially:

Jarvis -> TTS Manager -> Kokoro -> audio
                         |
                         +-- timeout/error --> EdgeTTS
```

## 3. Responsibility boundaries

### Client

The mobile or laptop client is responsible for microphone capture, speaker playback, optional local wake-word detection, echo cancellation, VAD, reconnect, and presentation. It is not the source of truth for task state.

### Voice Gateway

The gateway owns authenticated voice sessions, streaming STT, turn detection, TTS arbitration, audio chunking, interruption, and latency instrumentation.

### Jarvis Core

Jarvis Core owns intent classification, planning, policy, task creation, orchestration, verification, recovery, memory retrieval, learning, and notification decisions.

### Hermes

Hermes remains the execution harness and tool surface. The Jarvis layer should call existing Hermes mechanisms instead of rebuilding equivalent terminal, browser, skill, scheduling, messaging, or agent capabilities.

### OmniRoute

OmniRoute decides which model/provider should process a turn or subtask. It should return streaming output where the selected provider supports it.

### Memory

Voice turns use the same memory system as text. Temporary speech transcription is session context. Durable facts, user preferences, project knowledge, and lessons enter long-term memory only through normal memory policy.

## 4. Voice request lifecycle

```text
IDLE
  |
WAKE_DETECTED
  |
LISTENING
  |
STT_PARTIAL
  |
TURN_FINAL
  |
JARVIS_CLASSIFY
  |
PLAN_OR_DIRECT
  |
LLM_STREAMING
  |
TTS_PRIMARY
  |
SPEAKING
  |
+------ user speaks ------> INTERRUPTED -> LISTENING
|
+------ task is long ------> BACKGROUND_RUNNING
|
+------ provider fails ----> TTS_FALLBACK
|
+------ complete ----------> IDLE
```

## 5. Streaming response pipeline

The system must not use a request pattern that waits for the full LLM output before speech.

```text
LLM token stream
     |
     v
Text chunker
     |
     +-- complete word/phrase
     |
     v
TTS queue
     |
     v
Kokoro synthesis
     |
     v
PCM/Opus encoder
     |
     v
WebSocket audio chunks
     |
     v
Client playback
```

The chunker should target natural phrase boundaries. It must avoid synthesizing tiny fragments that sound unnatural while also avoiding long waits for a paragraph.

## 6. TTS manager

Provider-neutral interface:

```python
class TTSProvider(Protocol):
    name: str

    async def warmup(self) -> None: ...
    async def synthesize_stream(self, text: str, *, session_id: str): ...
    async def health(self) -> dict: ...
    async def cancel(self, session_id: str) -> None: ...
```

The TTS manager owns:

- provider selection
- timeout policy
- first-audio deadline
- stream integrity checks
- fallback
- cancellation
- provider health state
- metrics

## 7. Kokoro primary design

Kokoro is an 82M parameter open-weight TTS model with Apache-2.0 licensed weights. It is small enough to be a practical CPU-first primary candidate. Current model variants report multilingual capabilities and 24 kHz output. Exact performance must be benchmarked in the target deployment.

Recommended behavior:

- load once at service startup
- warm the model with a short synthetic utterance
- keep the model resident
- serialize or safely queue synthesis when CPU contention would otherwise cause unstable latency
- synthesize sentence or phrase chunks
- expose first-audio timing

## 8. EdgeTTS fallback design

EdgeTTS is the emergency online provider. It is deliberately secondary because it depends on network connectivity and an external service.

Fallback triggers:

- Kokoro health check failure
- synthesis exception
- empty or invalid audio
- first-audio deadline exceeded
- repeated synthesis timeout

Failover should not recreate the Jarvis task. It only changes the current TTS provider.

## 9. Failover state machine

```text
PRIMARY_READY
     |
     v
SYNTHESIZING_KOKORO
     |
     +-- audio ready --> PLAYING_PRIMARY
     |
     +-- error --------> FALLBACK_START
     |
     +-- deadline ------> FALLBACK_START

FALLBACK_START
     |
     v
SYNTHESIZING_EDGETTS
     |
     +-- audio ready --> PLAYING_FALLBACK
     |
     +-- error --------> VOICE_DEGRADED
```

A degraded voice state should expose a text response and retry the primary provider on a later turn.

## 10. Barge-in architecture

Playback and capture run concurrently.

```text
TTS playback -----------------------> audio out
       |
       +-- interrupt detector <----- microphone
                                      |
                                      v
                                 STOP PLAYBACK
                                      |
                                 CANCEL TTS
                                      |
                                 FINALIZE STT
                                      |
                              new Jarvis turn
```

The interruption signal must be local to the active session. It must not cancel unrelated background tasks unless the user explicitly requests task cancellation.

## 11. Long-running task separation

Voice conversation and task execution are separate lifecycles.

```text
Voice turn
   |
   +-- short request --> finish conversationally
   |
   +-- long request ---> create durable Task
                            |
                            +--> worker/agents
                            +--> checkpoints
                            +--> verification
                            +--> notification
```

The voice session can return to idle while the durable task remains running.

## 12. Shared session model

Minimum session fields:

```yaml
voice_session_id:
jarvis_session_id:
user_id:
device_id:
active_task_id:
conversation_id:
language:
wake_phrase:
state:
last_turn_id:
last_provider:
connected_at:
last_seen_at:
```

A reconnect should restore the server-side session state instead of creating a duplicate active execution.

## 13. Error domains

Classify failures independently:

- audio capture
- transport
- STT
- LLM/provider
- task execution
- TTS primary
- TTS fallback
- authentication
- authorization
- persistence
- client playback

The recovery engine should not treat a TTS failure as a task failure.

## 14. Hosting topology

Initial topology:

```text
HF Hermes Space
  Hermes
  Jarvis Core
  Voice Gateway
  Kokoro
  Task worker
  Persistent application state

HF OmniRoute Space
  OmniRoute
  provider/model routing

Third slot
  Reserved for the bottleneck discovered by measurement
```

The third slot should not be consumed by a separate Kokoro service unless benchmarks prove in-process CPU synthesis is the limiting factor. Kokoro is lightweight enough that keeping it in the Hermes process avoids an extra network hop.

## 15. Cold-start policy

Hosted Spaces may experience lifecycle constraints. The runtime therefore records warm versus cold latency separately. A warmup endpoint or internal warmup task should initialize STT/TTS/model clients when a process starts. Client reconnect should retry with backoff.

## 16. Security boundaries

```text
User audio
   |
Authenticated transport
   |
Voice Gateway
   |
Intent + policy
   |
Jarvis task
   |
Hermes tools
   |
External systems
```

Audio or transcribed content does not receive privileged authority merely because it arrived through the voice channel. The same policy engine must govern voice and text commands.

## 17. Observability

Every turn gets:

```text
request_id
voice_session_id
turn_id
task_id
agent_id
provider_id
```

Timing events:

```text
wake_detected_at
speech_start_at
speech_end_at
stt_final_at
llm_first_token_at
tts_request_at
tts_first_audio_at
playback_start_at
fallback_at
interrupt_at
response_complete_at
```

## 18. Architectural rule

Optimize the voice path around first useful audio, not total response completion. The user should hear a useful acknowledgement or first clause while the underlying task continues.
