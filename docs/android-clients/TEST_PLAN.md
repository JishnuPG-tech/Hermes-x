# Android Clients Test Plan

## 1. Voice tests

### Invocation

- tap-to-talk
- assistant invocation
- supported wake phrase
- screen on/off
- locked/unlocked device
- headset/Bluetooth

### Conversation

- one turn
- multi-turn
- interruption
- rapid follow-up
- silence
- background noise
- network transition

### Streaming

- partial STT
- final STT
- streamed Hermes text
- streamed TTS
- playback interruption

## 2. Chat tests

- authentication
- conversation creation
- streaming response
- reconnect
- task list
- task details
- project navigation
- GitHub state
- logs
- artifacts
- approvals

## 3. Cross-client tests

### Test A

Start task by voice, inspect it in chat.

### Test B

Start task in chat, ask voice for status.

### Test C

Start task in voice, close both clients, reopen chat and verify task state.

### Test D

Request approval in voice, approve from chat.

### Test E

Request approval in chat, answer by voice if the approval contract supports voice approval.

## 4. Backend failure tests

- OmniRoute unavailable
- model timeout
- Hermes restart
- WebSocket disconnect
- STT failure
- TTS failure
- storage unavailable

Expected result: client receives truthful state and does not fabricate completion.

## 5. Android lifecycle tests

- activity recreation
- process recreation
- app background/foreground
- device reboot
- rotation where applicable
- battery saver
- Doze behavior
- notification removal
- permission revocation

## 6. Security tests

Verify:

- no provider API keys in APK
- no GitHub token in APK
- no database password in APK
- TLS enforced
- expired sessions rejected
- revoked device rejected
- unauthorized task action rejected
- microphone state is visible

## 7. Performance tests

Voice:

- time to invocation
- STT partial latency
- first model token
- first playable audio
- interruption latency
- reconnect time

Chat:

- first token latency
- scroll/render performance
- large code block rendering
- large task event stream

## 8. Acceptance targets

Voice first playable response target: <= 5 seconds for normal short requests.

Hard target: <= 10 seconds before a fallback/error state is surfaced.

Long-running tasks must acknowledge quickly and continue server-side.
