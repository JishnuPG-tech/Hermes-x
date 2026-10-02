# Long-Running Task Integration

## 1. Bridging Ephemeral Chat to Durable HarnessEngine

Conversational turns are bound to immediate request/response lifecycles. When an objective requires multi-step autonomous work, extensive compilation, repository-wide audits, or scheduled recurrence, the model delegates to `create_durable_task` (`hermes_core/runtime/planner.py`).

## 2. Architecture & Lifecycle

```mermaid
sequenceDiagram
    participant User
    participant Agent as AutonomousAgentLoop
    participant Planner as ExecutionPlanner
    participant Harness as HarnessEngine
    participant DB as TaskDB
    participant Worker as DurableWorker

    User->>Agent: "Refactor all async handlers across the repository and run the test suite"
    Agent->>Planner: create_durable_task(objective, risk_level="medium")
    Planner->>Harness: create_and_run_task(objective, project_id)
    Harness->>DB: Persist Task(status="RUNNING")
    Harness->>Worker: Spawn background task runner
    Planner-->>Agent: Task ID (e.g. task_3a9f018d) + Tracking URL
    Agent-->>User: "Dispatched durable task task_3a9f018d to HarnessEngine. Monitoring execution..."
```

## 3. Resilience & Checkpointing

- **Task Checkpointing**: `TaskDB` saves state checkpoints after each major subtask.
- **Process Isolation**: The background task runs independently of whether the user closes their Android app, disconnects from Wi-Fi, or navigates away.
- **Progress Streaming**: Real-time progress is streamed via SSE (`/v1/tasks/{task_id}/events`) and can notify mobile push channels upon completion.
