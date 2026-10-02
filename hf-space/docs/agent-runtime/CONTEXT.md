# Context Management & Prompt Assembly

## 1. ContextBuilder Design

`ContextBuilder` (`hermes_core/runtime/context.py`) standardizes prompt construction across all interfaces.

### Core Components:
1. **System Persona & Rules**:
   - Sovereign autonomous agent persona.
   - Clean, direct tone without unnecessary preambles or decorative emojis.
   - Strictly forbids hallucinating action completion.
2. **Dynamic Tool Catalog**:
   - Instead of injecting 50+ tool schemas indiscriminately into the context, `CapabilityIndex` selects high-relevance tools plus meta-discovery tools (`search_tools`, `create_durable_task`).
   - Keeps token overhead low while providing access to all server capabilities.
3. **Execution Context Injection**:
   - Workspace paths, active project metadata, user role/admin flags, and session IDs are dynamically inserted into the context.
4. **Token Budget & History Sliding Window**:
   - Conversation history is managed to preserve context without exceeding model limits.
   - Older tool outputs are summarized or pruned if total context length exceeds target token budgets.
