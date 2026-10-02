# Autonomous Agent Loop

## 1. Loop Mechanics

The `AutonomousAgentLoop` (`hermes_core/runtime/agent_loop.py`) coordinates iterative model-driven reasoning and action cycles.

```mermaid
sequenceDiagram
    participant User
    participant Loop as AutonomousAgentLoop
    participant Model as LLM (OmniRoute / OpenCode)
    participant Index as CapabilityIndex
    participant Executor as ToolExecutor
    participant Verifier as VerificationGate

    User->>Loop: User Query / Objective
    Loop->>Index: Query Relevant Tools & Meta-Tools
    Index-->>Loop: Active Tool Schemas
    
    loop Max Iterations (default 8)
        Loop->>Model: Chat Context + Available Tools
        Model-->>Loop: Model Turn (Thinking + Tool Call OR Final Text)
        
        alt Tool Call Returned
            Loop->>Executor: Execute Tool Call (auth + sandbox)
            Executor-->>Loop: Structured ToolResult
            Loop->>Loop: Observe Result & Check Loop Detector
        else Final Text Returned
            Loop->>Verifier: Audit Completion Criteria
            Verifier-->>Loop: Verification Passed
            Loop-->>User: Final Response
        end
    end
```

## 2. Iteration Lifecycle States

1. **Intake & Context Assembly**: `ContextBuilder` prepares system instructions, workspace constraints, and the conversation history.
2. **Tool Schema Assembly**: `CapabilityIndex` selects available capabilities and always exposes `search_tools` and `create_durable_task`.
3. **Model Turn Generation**: The model streams tokens. `thinking` chunks are separated from standard `content` chunks, preventing internal scratchpads from leaking into user-facing text.
4. **Tool Call Execution**:
   - The model can invoke one or multiple tool calls in parallel or sequence.
   - If `search_tools` is called, newly discovered tool definitions are dynamically provided to subsequent iterations.
   - Every tool call executes via `ToolExecutor.execute()`.
5. **Observation**: Tool outcomes are formatted as OpenAI tool result messages and fed back into the context.
6. **Re-planning & Error Recovery**: If an error is observed, the model reflects on the error in the next turn and attempts an alternative approach.
7. **Verification**: When the model indicates completion of an actionable task, `VerificationGate` verifies empirical state.
