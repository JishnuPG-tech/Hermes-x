# Hermes Voice Agent — Latency-Optimized Voice Service

A high-performance, duplex, interruptible voice backend engineered for Hermes on Hugging Face Spaces (or local containers).

## Architecture
- **VAD**: Silero VAD (ONNX) with pre-speech padding buffer and adaptive energy fallback.
- **ASR**: `faster-whisper` (`tiny.en` int8) running on CPU in ~150-250ms.
- **LLM**: Streaming SSE client chunking tokens at sentence/clause boundaries (`.`, `!`, `?`, `,`, `;`) to begin speech synthesis on the very first sentence.
- **TTS**: Piper ONNX (`en_US-lessac-medium`) streaming raw 16kHz PCM with Edge-TTS neural fallback.
- **Barge-in**: Sub-100ms cancellation loop aborting in-flight LLM/TTS generation upon user voice detection.

## Multiplexed Binary & Text Protocol
- `0x01` + PCM16 mono 16kHz (client -> server mic chunks).
- `0x02` + Audio bytes (server -> client synthesized speech).
- JSON control frames:
  - `session_start` / `session_ready`
  - `assistant_state` (`listening`, `capturing`, `thinking`, `speaking`, `idle`)
  - `transcript_partial` / `transcript_final`
  - `tts_start` / `tts_end`
  - `user_interrupt` / `agent_interrupted`

## Environment Variables
- `OMNIROUTE_URL`: URL to OmniRoute completions endpoint (default: `http://127.0.0.1:20128/v1/chat/completions`).
- `PORT`: Port to listen on (default: `7860`).
- `MODEL_CACHE_DIR`: Directory where models are baked (default: `/app/models`).
