# Hermes Agent Chat APK PRD

## 1. Product

**Hermes Agent Chat** is the visual Android workspace for controlling and observing Hermes.

It is intended for complex work where the user wants to see what Hermes is doing.

## 2. Core experience

```text
User
 |
 v
Agent Chat APK
 |
 | HTTPS/WebSocket
 v
Hermes Agent
 |
 +--> memory
 +--> planning
 +--> tools
 +--> terminal
 +--> filesystem
 +--> GitHub
 +--> subagents
 +--> verification
 |
 v
OmniRoute
 |
v
Models
```

## 3. Main screens

### Home

- active conversation
- task summary
- Hermes status
- quick actions

### Conversation

- streaming messages
- Markdown
- code blocks
- tool activity
- citations/links where available
- attachments
- voice playback

### Tasks

- running tasks
- queued tasks
- completed tasks
- failed tasks
- paused tasks
- retry/resume controls

### Project

- project list
- current branch
- recent commits
- pull requests
- CI status
- changed files
- deployment status

### Execution

- terminal output
- worker/subagent status
- current step
- logs
- artifacts

### Approvals

Explicit approval cards for sensitive actions such as:

- destructive filesystem changes
- production deployment
- credential/security changes
- irreversible external operations

## 4. Functional requirements

### FR-01 Streaming chat

Render Hermes response tokens/events as they arrive.

### FR-02 Agent activity

Display useful high-level progress without exposing private chain-of-thought.

Examples:

- Planning task
- Running tests
- Investigating CI
- Waiting for approval
- Retrying failed command
- Preparing commit

### FR-03 Code and files

Provide read-only or controlled editing views backed by Hermes APIs.

The APK does not directly access `/data/jarvis`.

### FR-04 GitHub

Show:

- repository
- branch
- commit
- PR
- checks
- issue
- deployment state

Actions are requested from Hermes, which performs authorization and GitHub operations.

### FR-05 Voice shortcut

Allow the user to start a voice session and hand the same conversation/task to the Voice APK.

### FR-06 Task continuity

A task must remain alive when the APK is closed or disconnected.

The APK reconnects and retrieves server-authoritative state.

## 5. Notifications

Useful notifications include:

- Hermes needs approval
- task completed
- task failed
- CI failed
- deployment completed
- background task requires attention

Avoid notification spam.

## 6. Security

The APK never receives unrestricted server shell access.

Every operation is an API request to Hermes and is checked against the Hermes trust hierarchy.

## 7. Offline behavior

The app can cache conversation and task state for viewing.

It must not claim that a server action happened while offline.

## 8. Acceptance criteria

- User can maintain an agentic conversation.
- Streaming responses render correctly.
- Tool/task progress is visible.
- Long-running tasks survive app closure.
- GitHub and project status can be inspected.
- Sensitive actions require the appropriate Hermes approval.
- Voice sessions can hand off to the voice client without creating a second assistant.
