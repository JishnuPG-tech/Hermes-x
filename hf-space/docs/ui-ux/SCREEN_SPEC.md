# Hermes Screen Specification

## Global shell

Every screen inherits:
- Hermes identity
- Connection state
- Current task context when active
- Consistent navigation
- Accessible focus order

## 1. Home / Chat

Purpose: primary Hermes conversation.

Layout:
- Quiet top identity/status row
- Large editorial greeting
- Contextual quick actions
- Conversation stream
- Pill composer
- Voice entry

States:
- First-run
- Ready
- Thinking
- Streaming
- Running tools
- Delegating
- Waiting
- Approval required
- Completed
- Failed
- Offline
- Reconnecting

## 2. Voice

Purpose: continuous voice interface to the same Hermes session.

Layout:
- Close/back
- Hermes status
- Large central voice control
- Transcript
- Listening/speaking indicator
- Stop/mute control

Behavior:
- Wake phrase
- VAD
- Streaming STT
- Streaming model output
- Streaming TTS
- Barge-in
- Resume same session after interruption

## 3. Tasks

Purpose: inspect and control autonomous work.

Task detail contains:
- Objective
- Plan
- Current step
- Progress
- Worker agents
- Dependencies
- Tool activity
- Verification
- Approvals
- Recovery history
- Artifacts
- Final result

Controls:
- Pause
- Resume
- Cancel
- Approve
- Retry
- Open workspace
- View trace

## 4. Agents

Purpose: inspect delegated workers.

Agent detail:
- Role
- Objective
- Parent task
- Status
- Allowed tools
- Workspace
- Current action
- Output
- Verification
- Failure/recovery state

## 5. Projects

Purpose: project-level view.

Sections:
- Overview
- Tasks
- Agents
- Files
- GitHub
- Notion
- Activity
- Artifacts
- Environment

## 6. GitHub

Purpose: show engineering state without making GitHub a second assistant.

Displays:
- Repository
- Branch
- Current commit
- Working tree
- Pull requests
- CI
- Releases
- Deployment status

Actions are routed through Hermes authority.

## 7. Notion

Purpose: structured project knowledge and planning.

Displays:
- Workspace/page
- Project knowledge
- PRDs
- Decisions
- Roadmap
- Feature contracts
- Sync status

## 8. Memory

Purpose: inspect durable Hermes knowledge.

Sections:
- Relevant memories
- Long-term facts
- Episodic history
- Project memory
- Preferences
- Memory controls

No sensitive information should be surfaced unless authorized and necessary.

## 9. Integrations

Purpose: configure capability providers.

Each integration displays:
- Provider
- Connection state
- Authentication method
- Scopes
- Credential reference
- Last verification
- Reauthorization/revoke

Secrets themselves are never displayed.

## 10. Approvals

Purpose: explicit authority escalation.

Every approval explains:
- What Hermes wants to do
- Why it needs approval
- What resources it affects
- Risk
- Scope
- Duration
- Resulting permission

## 11. Terminal

Purpose: inspect developer execution.

Displays:
- Workspace
- Command
- Live output
- Exit status
- Runtime
- Files/artifacts
- Stop action

## 12. Server health

Purpose: understand whether Hermes can operate.

Health groups:
- Hermes
- Storage
- Memory
- Background jobs
- Voice
- OmniRoute
- Model/provider availability
- Integrations

## 13. Artifacts

Purpose: inspect generated outputs.

Artifact types:
- Code
- Documents
- Images
- Reports
- Builds
- Logs
- Releases

## 14. Notifications

Events:
- Task completed
- Approval required
- Task failed
- Recovery succeeded
- Integration expired
- Server degraded
- New artifact ready

## 15. Settings

Groups:
- Profile
- Hermes capabilities
- Voice
- Appearance
- Notifications
- Memory
- Privacy and security
- Integrations
- Permissions
- Backend
- Routing
- Server health

## Screen quality contract

Every screen must specify:
- Purpose
- Primary action
- Secondary actions
- Navigation entry
- Loading state
- Empty state
- Error state
- Offline state
- Success state
- Running state
- Approval state where applicable
- Accessibility
- Mobile behavior
- Desktop behavior
