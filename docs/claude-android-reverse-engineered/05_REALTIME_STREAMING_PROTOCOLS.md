# Claude Android — Realtime Streaming Protocols & WebSocket Contracts

---

## 1. Server-Sent Events (SSE) Streaming Protocol

Used by `POST /completion` and `POST /completion2` to deliver incremental assistant responses, extended thinking deltas, tool executions, and source citations.

### 1.1 Complete Event Flow Sequence

```
Client                                                  Server Edge
  │                                                           │
  ├────── POST /chat_conversations/{id}/completion2 ─────────►│
  │       (Accept: text/event-stream)                         │
  │                                                           │
  │◄───── event: message_start ───────────────────────────────┤ (Initial message metadata)
  │◄───── event: content_block_start (type: "thinking") ──────┤ (Extended thinking begins)
  │◄───── event: content_block_delta (thinking_delta) ────────┤ (Thinking tokens)
  │◄───── event: content_block_stop ──────────────────────────┤ (Thinking concludes)
  │                                                           │
  │◄───── event: content_block_start (type: "text") ──────────┤ (Response text begins)
  │◄───── event: content_block_delta (text_delta) ────────────┤ (Smoothed at 30 FPS)
  │◄───── event: content_block_stop ──────────────────────────┤
  │                                                           │
  │◄───── event: content_block_start (type: "tool_use") ──────┤ (Tool call intent)
  │◄───── event: content_block_delta (input_json_delta) ──────┤ (Streaming JSON params)
  │◄───── event: content_block_stop ──────────────────────────┤
  │                                                           │
  │◄───── event: message_delta (stop_reason: "tool_use") ─────┤
  │◄───── event: message_stop ────────────────────────────────┤ (Stream completes)
  │                                                           │
```

---

### 1.2 Detailed SSE Event & Delta Payload Specifications

#### 1. `message_start`
```json
{
  "type": "message_start",
  "message": {
    "id": "msg_01Abc...",
    "type": "message",
    "role": "assistant",
    "model": "claude-sonnet-5-20260620",
    "content": [],
    "stop_reason": null,
    "usage": { "input_tokens": 1420, "output_tokens": 0 }
  }
}
```

#### 2. `content_block_start` (Thinking Block)
```json
{
  "type": "content_block_start",
  "index": 0,
  "content_block": {
    "type": "thinking",
    "thinking": ""
  }
}
```

#### 3. `content_block_delta` (`thinking_delta`)
```json
{
  "type": "content_block_delta",
  "index": 0,
  "delta": {
    "type": "thinking_delta",
    "thinking": "The user is asking to optimize the SQLite query on line 42..."
  }
}
```

#### 4. `content_block_start` (`tool_use`)
```json
{
  "type": "content_block_start",
  "index": 2,
  "content_block": {
    "type": "tool_use",
    "id": "toolu_01Abc99",
    "name": "web_search",
    "input": {}
  }
}
```

#### 5. `content_block_delta` (`input_json_delta`)
```json
{
  "type": "content_block_delta",
  "index": 2,
  "delta": {
    "type": "input_json_delta",
    "partial_json": "{\"query\": \"Android 16 120Hz refresh rate API\"}"
  }
}
```

#### 6. `content_block_delta` (`citation_start_delta` & `citation_end_delta`)
```json
{
  "type": "content_block_delta",
  "index": 1,
  "delta": {
    "type": "citation_start",
    "citation": {
      "type": "web_search_result",
      "url": "https://developer.android.com/about/versions/16",
      "title": "Android 16 Features"
    }
  }
}
```

#### 7. `message_delta`
```json
{
  "type": "message_delta",
  "delta": {
    "stop_reason": "end_turn",
    "stop_sequence": null
  },
  "usage": { "output_tokens": 420 }
}
```

---

## 2. Voice Realtime WebSocket Protocol ("Bell Mode")

**Endpoint**: `wss://api.claude.ai/v1/voice/stream`  
**Audio Codec**: Opus (20ms packet frames, 24kHz sampling, mono)  
**Protocol Format**: JSON control text frames multiplexed with binary Opus audio packets.

### 2.1 Complete Message Type Mapping (`com.anthropic.claude.bell.api`)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        BELL REALTIME PROTOCOL MESSAGE TYPES                            │
├─────────────────────────┬──────────────────────────────────────────────────────────────┤
│ Frame Direction         │ Class Identifier & Payload                                   │
├─────────────────────────┼──────────────────────────────────────────────────────────────┤
│ Client ➔ Server         │ ToolsRegister(tools: List<ToolDefinition>)                   │
│                         │ VoiceSelect(voice: String, speed: Float, language: String)   │
│                         │ Interrupt()                                                  │
│                         │ ManualInputEnd()                                             │
│                         │ PlaybackComplete()                                           │
│                         │ ClientMetrics(latency_ms, latency_anchor, rtt_ms)            │
│                         │ ClockSyncPing(seq: Int)                                      │
│                         │ PauseEndpointing(timeout_ms: Long)                           │
│                         │ UnpauseEndpointing()                                         │
│                         │ AttachmentFlowStart()                                        │
│                         │ AttachmentFlowEnd(files: List, attachments: List)            │
├─────────────────────────┼──────────────────────────────────────────────────────────────┤
│ Server ➔ Client         │ SessionServerInitialized()                                   │
│                         │ SessionServerConfig(data: Map<String, Any>)                  │
│                         │ ClockSyncPong(seq: Int, server_t2_ms: Double, server_t3_ms)  │
│                         │ AudioCaptureStarted(voice_session_uuid: String)              │
│                         │ AudioCaptureStopped(voice_session_uuid: String)              │
│                         │ TranscriptionStart()                                         │
│                         │ TranscriptInterim(text: String)                              │
│                         │ TranscriptEmpty()                                            │
│                         │ UserInputEnd(speech_end_offset_ms: Long)                     │
│                         │ PlaybackStart(server_vad_to_tts_start_ms: Int)               │
│                         │ TTSWord(text: String, pts_ms: Int)                           │
│                         │ PlaybackEnd()                                                │
│                         │ ServerInterrupt(redelivery: Boolean)                         │
│                         │ ToolApprovalDismiss(tool_use_id: String)                     │
│                         │ MessageSse(event: MessageSseEvent)                           │
│                         │ BackgroundMessageSse(message_uuid: String, event)            │
│                         │ MessageComplete(message_uuid, sender, content, files)        │
│                         │ VoiceAudioSessionChanged(new_language, new_voice)            │
│                         │ Error(data: Fatal | Temporary)                               │
└─────────────────────────┴──────────────────────────────────────────────────────────────┘
```

---

### 2.2 Low-Latency Clock Synchronization Contract

To accurately compute round-trip time (RTT) and client-perceived audio latency, the client performs NTP-style clock sync:

```
Client                                                  Server
  │                                                       │
  ├────── ClockSyncPing(seq: 1) ─────────────────────────►│ (Client records t1)
  │                                                       │
  │◄───── ClockSyncPong(seq: 1, t2: 120.4, t3: 120.9) ────┤ (Client records t4)
  │                                                       │
  RTT = (t4 - t1) - (t3 - t2)
  Clock Offset = ((t2 - t1) + (t3 - t4)) / 2
```

---

### 2.3 Word-Level Lip-Sync & Captioning (`TTSWord`)

During audio playback, the server streams individual word timestamps (`TTSWord`):
```json
{ "type": "TTSWord", "text": "I", "pts_ms": 0 }
{ "type": "TTSWord", "text": "have", "pts_ms": 140 }
{ "type": "TTSWord", "text": "analyzed", "pts_ms": 320 }
{ "type": "TTSWord", "text": "your", "pts_ms": 680 }
{ "type": "TTSWord", "text": "code.", "pts_ms": 910 }
```
* The Compose UI binds these `pts_ms` presentation timestamps to the audio track, highlighting words synchronously with audio playback.
