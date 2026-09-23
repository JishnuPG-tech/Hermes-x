# Failure Recovery & Loop Protection

## 1. Multi-Step Self-Healing

When an autonomous agent executes tools, real-world failures happen:
- Missing file paths or syntax errors.
- Network timeouts.
- Subprocess non-zero exit codes.

Instead of crashing or terminating early, Hermes passes the error output back into the conversation context as an observation. The model analyzes the error stack, diagnoses the cause, and formulates an alternate hypothesis in the next iteration.

## 2. Loop Protection & Circuit Breakers

A classic failure mode in agentic systems is repetitive tool calling:
- The model calls `read_file(path="missing.txt")`
- Output: `File not found`
- The model repeats `read_file(path="missing.txt")` indefinitely.

### Loop Detection Mechanism:
`ExecutionPlanner` (`hermes_core/runtime/planner.py`) computes a deterministic SHA-256 hash of each `(tool_name, arguments)` pair:
1. If the exact same tool call is repeated 3 times in a single turn trajectory with error outcomes, a `ToolLoopDetectedError` is raised.
2. The loop breaker injects a critical guidance message:
   ```text
   [SYSTEM ALERT: TOOL LOOP DETECTED]
   You have attempted tool 'read_file' with identical arguments 3 times without progress.
   Do not repeat this call. Re-evaluate your strategy, inspect other files, or explain the roadblock.
   ```
3. This breaks the infinite loop and forces the agent to replan or notify the user honestly.
