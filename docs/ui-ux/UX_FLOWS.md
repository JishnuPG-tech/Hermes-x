# Hermes UX Flows

## Chat flow

```text
Open Hermes
 → Read current context
 → Compose request
 → Submit
 → Hermes acknowledges
 → Stream response
 → Show tool/task activity when relevant
 → Verify
 → Present result
```

## Autonomous task flow

```text
User objective
 → Hermes clarifies only if necessary
 → Create task contract
 → Check authority
 → Plan
 → Execute
 → Delegate if useful
 → Checkpoint
 → Verify
 → Recover if needed
 → Complete
 → Report
```

## Approval flow

```text
Hermes identifies restricted action
 → Explain action and risk
 → Ask approval
 → User approves/denies
 → Record decision
 → Continue or stop
```

## Agent team flow

```text
Objective
 → Hermes decomposes
 → Assign worker contracts
 → Spawn workers
 → Monitor
 → Workers checkpoint
 → Hermes verifies outputs
 → Synthesize
 → Final verification
```

## Voice flow

```text
Wake
 → Listen
 → Transcribe
 → Hermes understands
 → Hermes acts
 → Speak response
 → User may interrupt
 → Continue same session
```

## Integration flow

```text
Settings
 → Integrations
 → Select provider
 → Explain requested scopes
 → Authenticate using official method
 → Store encrypted credential reference
 → Verify connection
 → Show connected state
```

## Project flow

```text
Projects
 → Select project
 → Overview
 → Tasks / Agents / Files / GitHub / Notion
 → Open detail
 → Act through Hermes
```

## Recovery flow

```text
Failure
 → Classify
 ├─ transient → retry
 ├─ dependency → wait
 ├─ code → debug
 ├─ permission → approval
 ├─ environment → repair
 └─ unknown → diagnose
 → Verify
 → Continue or report
```

## Memory flow

```text
Relevant memory discovered
 → Show concise context
 → Use in task
 → Record new durable knowledge when justified
 → Allow user control
```

## Settings flow

Settings are grouped by user intent rather than backend implementation details.

Primary groups:
- Account
- Appearance
- Voice
- Notifications
- Memory
- Integrations
- Security
- Developer
- Backend
- System health
