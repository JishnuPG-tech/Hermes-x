# Model Routing & Upstream Gateways

## 1. Upstream OmniRoute & OpenCode Integration

Hermes integrates with OpenCode / OmniRoute as the upstream LLM inference provider:
- **Default Upstream Endpoint**: `https://jishnupg-opencode-cli.hf.space/v1`
- **Default Model**: `nvidia/nvidia/nemotron-3-super-120b-a12b`
- **Supported Capabilities**: Multi-step tool calling, structured JSON output, native thinking tokens, and continuous SSE streaming.

## 2. Dynamic Model Tiering

The user or client can select models per turn:
- **Reasoning Tier (`hermes-reasoning` / `nemotron-3-super-120b`)**: Complex software engineering, architectural analysis, multi-file refactoring, debugging, and verification.
- **Fast Tier (`hermes-fast` / `qwen-2.5-coder-32b`)**: Low-latency queries, voice turns, single-step tasks.
- **Vision Tier (`hermes-vision`)**: Multi-modal tasks, UI screenshot inspection, and visual verification.

## 3. Streaming Protocol Standard

Hermes communicates with upstream using standard OpenAI-compatible streaming chunks (`chat.completion.chunk`), extracting:
- `choices[0].delta.content`: User-facing output text.
- `choices[0].delta.reasoning_content` / `choices[0].delta.thinking`: Model chain-of-thought (streamed to `RuntimeEventBus` as `agent.thinking`, preserved for inspection without contaminating user-facing text).
- `choices[0].delta.tool_calls`: Tool call invocations.
