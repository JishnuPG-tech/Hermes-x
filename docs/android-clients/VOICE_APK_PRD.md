# Hermes Voice APK PRD

## 1. Product

**Hermes Voice** is the Android voice interface for the Hermes Agent.

Its goal is to provide a natural, assistant-like experience where the user can speak to Hermes without opening a chat workspace.

## 2. Core principle

The APK is a thin voice client.

```text
Mic / Speaker / Android assistant integration
                 |
                 v
          Hermes Gateway
                 |
                 v
           Hermes Agent
                 |
                 v
             OmniRoute
                 |
                 v
               Models
```

## 3. Primary user experience

Example:

```text
User: "Hermes"

Hermes: "Yes?"

User: "Check my GitHub project and tell me why CI failed."

Hermes:
  - streams acknowledgement
  - performs task on server
  - investigates GitHub
  - reports findings by voice
```

The user should not need to understand STT, TTS, OmniRoute, model providers or server infrastructure.

## 4. Functional requirements

### FR-01 Invocation

Support:

- Android assistant invocation where the app is selected as the system assistant
- configured wake phrase where technically and legally supported
- tap-to-talk fallback
- headset/Bluetooth invocation where supported

### FR-02 Continuous conversation

Support:

- turn detection
- silence detection
- follow-up turns without reopening the app
- interruption while Hermes is speaking
- automatic return to listening state
- session persistence/reconnect

### FR-03 Speech recognition

Use a streaming STT provider behind an interface.

Required abstraction:

```text
STTProvider
  start()
  streamAudio()
  partialTranscript()
  finalTranscript()
  stop()
```

### FR-04 Hermes connection

Use WebSocket for realtime events.

Events should include:

- `session.started`
- `transcript.partial`
- `transcript.final`
- `assistant.text.delta`
- `assistant.audio.delta`
- `assistant.speaking.started`
- `assistant.speaking.stopped`
- `task.started`
- `task.progress`
- `task.completed`
- `task.failed`
- `approval.required`
- `error`

### FR-05 TTS

The client should normally receive streamed audio from the Hermes voice gateway.

The APK must not duplicate the server's model/TTS architecture unless an explicit offline mode is later introduced.

### FR-06 Barge-in

When the user speaks while Hermes is talking:

1. Detect user speech.
2. Stop/duck playback immediately.
3. Signal interruption to Hermes.
4. Preserve the conversation.
5. Process the new turn.

### FR-07 Long tasks

For long-running work:

```text
User request
    |
    v
Hermes acknowledges immediately
    |
    v
Background task
    |
    +--> progress events
    +--> completion/failure
    |
    v
Voice notification
```

The APK must never imply that a long task finished merely because the HTTP/WebSocket request returned.

## 5. UI

The default screen should be intentionally minimal:

- Hermes avatar/status
- listening indicator
- speaking indicator
- connection indicator
- transcript
- cancel/stop button
- optional task card

The app can provide a richer history screen, but the primary interaction is voice.

## 6. Permissions

Only request permissions required by implemented features.

Likely permissions:

- `RECORD_AUDIO`
- foreground-service permissions/types where required
- notification permission where required for task/foreground-service UX

Do not request contacts, SMS, location, camera or accessibility access unless a separately justified feature requires them.

## 7. Assistant integration

Implement Android's `VoiceInteractionService` and associated session architecture where the application is intended to act as the user's selected digital assistant.

The app should make becoming the default assistant an explicit user action.

## 8. Reliability

The client must tolerate:

- network loss
- WebSocket disconnect
- server restart
- Hermes restart
- TTS stream interruption
- STT provider failure
- app process recreation
- Android configuration changes

The authoritative task state remains server-side.

## 9. Privacy

Show a persistent and obvious microphone/listening state.

Do not upload audio before the voice session is active according to the configured policy.

Do not store raw microphone audio permanently unless the user explicitly enables a feature requiring it.

## 10. Acceptance criteria

- User can invoke Hermes using the supported Android assistant mechanism.
- User can speak naturally in multiple turns.
- Hermes can interrupt and resume correctly.
- Streaming speech begins without waiting for the complete answer.
- Long-running tasks continue if the UI changes or reconnects.
- Voice and chat clients can observe the same task.
- No OmniRoute endpoint is exposed to the APK.
- No provider secret is shipped in the APK.
