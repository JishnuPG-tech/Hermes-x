# Verification Gate & Grounded Truth Enforcement

## 1. The False Completion Problem

Large language models frequently hallucinate completion when given tasks that produce errors or silence:
- Example: A script fails with exit code 1, but the model responds: *"I have created the files and everything is running smoothly!"*
- Example: The model didn't invoke any file writing tools, yet claims: *"Done! The configuration is updated."*

## 2. VerificationGate Mechanics

`VerificationGate` (`hermes_core/runtime/verifier.py`) enforces strict empirical validation before allowing completion status.

### Verification Contracts:
1. **File Existence & Integrity Check**: If the task was to create or edit a file, the verifier inspects the filesystem directly:
   - Does the file exist on disk?
   - Is the file size > 0 bytes?
   - Does it contain the expected signatures or syntax?
2. **Execution Return Codes**: If commands were executed, the exit code must be `0`. Exit codes > 0 fail verification automatically.
3. **Test Assertion Execution**: For coding and refactoring tasks, the verifier runs target test suites (e.g. `pytest tests/`) to empirically confirm non-regression.
4. **Action Grounding**: If the model claims an external side-effect occurred without executing any corresponding tool call in the trajectory, the claim is rejected as ungrounded hallucination.
