# HERMES — CLAUDE UI/UX UPGRADE MASTER PROMPT
Exact Stitch UI preservation + complete Hermes capability integration
MASTER PURPOSE: Upgrade the Google Stitch-generated Claude-style Android UI with every Hermes capability and architecture decision already defined, without redesigning the existing UI/UX. The Stitch UI is the immutable visual baseline.
## 1. ABSOLUTE DIRECTIVE
```text
You are upgrading an existing Android application generated in Google Stitch.

DO NOT REDESIGN THE EXISTING CLAUDE-STYLE UI/UX.

Preserve the existing visual language, screen composition, typography, spacing, colors, navigation, interaction feel, component hierarchy, icons, motion, and overall calm/minimal character.

Your job is to BUILD HERMES INSIDE THIS UI.

Do not replace the design with a generic AI dashboard.
Do not introduce flashy gradients or unrelated chrome.
Do not create a second visual system.
Add Hermes capabilities contextually inside existing surfaces.
Create a new screen only when a capability genuinely cannot fit an existing surface.
Never claim completion unless the feature is implemented and tested.
```
## 2. AUTHORITIES AND SOURCES
```text
VISUAL SOURCE OF TRUTH
Google Stitch:
https://stitch.withgoogle.com/projects/10079330980620936979

PRODUCT / UX SOURCE OF TRUTH
Notion:
Hermes UI Kit + Hermes Product Blueprint

ENGINEERING SOURCE OF TRUTH
https://github.com/JishnuPG-tech/Hermes-x

MASTER BLUEPRINT
docs/blueprint/MASTER_BLUEPRINT.md

UI/UX DOCUMENTATION
docs/ui-ux/
DESIGN_SYSTEM.md
IMPLEMENTATION_ROADMAP.md
QA_AND_VISUAL_REGRESSION.md
SCREEN_SPEC.md
STATE_MACHINE.md
UI_ARCHITECTURE.md
UX_FLOWS.md

First inspect the repository, existing implementation, documentation, and Stitch/exported design assets. Do not blindly overwrite existing work.
```
## 3. HERMES AUTHORITY MODEL
```text
HERMES IS THE KING.

User
 ↓
Hermes Agent
 ├─ Conversation
 ├─ Memory
 ├─ Planning
 ├─ Tools
 ├─ Terminal
 ├─ Files
 ├─ Browser
 ├─ GitHub
 ├─ Notion
 ├─ Agents
 ├─ Tasks
 ├─ Automation
 ├─ Voice
 ├─ Permissions
 ├─ Verification
 ├─ Recovery
 └─ Agent OS
       ↓
   OmniRoute
       ↓
     Models

Canonical:
"Hermes is the king. OmniRoute powers the king. Models power OmniRoute's routing targets. The user communicates with Hermes, never with OmniRoute directly."

OmniRoute only provides model/provider connectivity, routing, fallback, availability and routing telemetry.
It does not own conversation, memory, project ownership, permissions, GitHub authority, task lifecycle, final decisions or voice identity.

Changing models must never reset Hermes identity, memory, sessions, tasks, tools, workspace or voice session.
```
## 4. SERVER-COMPUTER ARCHITECTURE
```text
The server is Hermes' computer.
The Android app and laptop are control/voice interfaces.

SERVER:
Hermes + Agent OS + memory + projects + tools + terminal + browser +
GitHub + Notion + integrations + voice gateway + background execution.

CLIENT:
conversation + voice + task monitoring + projects + approvals +
artifacts + notifications + settings.

Durable filesystem:
 /data/jarvis/
 ├── projects/
 ├── workspaces/
 ├── databases/
 ├── artifacts/
 ├── logs/
 ├── agent-state/
 ├── memory/
 ├── config/
 ├── secrets/
 └── voices/

HF Storage Bucket is persistent live storage.
GitHub is code/version history.
Backups are recovery infrastructure.
Do not treat ephemeral Space disk as durable state.
```
## 5. ANDROID CLIENT
```text
Target Android APK.
Preferred client architecture:
Kotlin + Jetpack Compose + Material 3 primitives only where they preserve the Stitch design.
Single Activity.
Clean Architecture.
MVVM.
Repository pattern.
StateFlow/reactive state.
WebSocket for real-time events.
HTTP for REST/configuration.
Local persistence for safe offline UI/session cache.
Secure Android credential storage.

Suggested:
ui/design
ui/screens
ui/components
ui/navigation
ui/state
domain/models
domain/usecases
data/api
data/websocket
data/repositories
data/local
security
voice
notifications
sync

The APK is a client, not the authority.
Never put server-side API keys or infrastructure secrets in the APK.
```
## 6. IMMUTABLE VISUAL BASELINE
- Preserve screen composition.
- Preserve typography and hierarchy.
- Preserve colors.
- Preserve spacing and padding.
- Preserve corner radii/borders.
- Preserve icons.
- Preserve navigation.
- Preserve chat/message composition.
- Preserve composer design.
- Preserve animation feel.
- Preserve loading/empty/error language.
- Reuse existing Stitch components before creating new ones.
- New Hermes components must look native to the existing design.
## 7. CHAT + COMPOSER
- Persistent sessions and full history.
- Streaming responses.
- Streaming tool activity.
- Tool status.
- Autonomous task progress inside conversation.
- Agent delegation/team status.
- Approval requests.
- Files/images.
- @ references for files, folders, git diffs and URLs.
- Project context.
- Artifacts.
- Retry/continue/stop.
- Session resume/fork/search.
- Offline/reconnect.
- Voice continuation.
- Long-running task acknowledgement followed by background execution.
## 8. AUTONOMOUS TASKS
```text
Objective:
Work until the user's objective is actually complete, subject to permissions, safety, compute, provider and host limits.

Lifecycle:
CREATE → INITIALIZE → AUTHORIZE → RUN → PAUSE/RESUME → SPAWN →
CHECKPOINT → VERIFY → COMPLETE → ARCHIVE

Behavior:
plan → execute → observe → verify → retry recoverable failures →
change strategy after repeated failures → wait for dependencies →
request necessary approval → checkpoint → resume → report → verify.

Never fake progress.
Never claim success from an LLM statement alone.
Verification-first:
Implement → Test → Build → Security → CI → Remote-state verify → Done.
```
- Task creation.
- Progress timeline.
- Current step.
- Workers.
- Logs.
- Artifacts.
- Failures.
- Retry.
- Pause/resume.
- Cancel.
- Approval gates.
- Completion evidence.
## 9. AGENTS AND TEAMS
```text
Hermes is the supervisor.
Workers are specialized agents.

Backend Agent
UI/UX Agent
QA Agent
Security Agent
DevOps Agent
Research Agent
Documentation Agent
GitHub Agent
Database Agent

Every delegation has:
objective, scope, inputs, tools, workspace, outputs, verification criteria,
limits, parent task and status.

Use isolated worktrees/workspaces for conflicting parallel code work.

Lifecycle:
CREATE → INITIALIZE → AUTHORIZE → RUN → CHECKPOINT → REPORT → COMPLETE/FAIL → ARCHIVE
```
- Delegate task.
- Parallel agents.
- Agent status.
- Agent output.
- Agent logs.
- Agent communication.
- Retry/recovery.
- Team status.
- Worker permissions.
## 10. TERMINAL, FILES AND SERVER COMPUTER
- Terminal execution.
- Process management.
- File read/edit/patch.
- Directory browsing.
- Streaming command output.
- Long-running process monitoring.
- Workspace selection.
- Git status/diff/log.
- Checkpoint/rollback.
- Artifacts.
- Server health.
- Resource status.
- Reconnect after restart.
- Permission gates for destructive commands.
## 11. GITHUB
- Repositories.
- Issues.
- Branches.
- Commits.
- Diffs.
- Pull requests.
- Reviews.
- CI.
- Releases.
- Deployments.
- Automated implementation.
- Verification before push.
```text
git pull
→ branch/worktree
→ modify
→ test
→ build
→ security checks
→ commit
→ push
→ CI
→ remote verification
→ report

Use least-privilege credentials.
Never expose GitHub tokens to the model.
```
## 12. NOTION
- Primary human-visible structured workspace.
- PRDs.
- Project plans.
- Tasks.
- Architecture.
- Decisions.
- Roadmaps.
- Documentation.
- Knowledge.
- Human-visible connection metadata.
- Status synchronization.
Notion is not Hermes authority and must never contain plaintext credentials.
## 13. MEMORY + LEARNING
```text
Hermes owns memory.

Memory:
- user preferences
- project facts
- environment facts
- conventions
- decisions
- learned procedures
- recurring patterns
- task history references

Provide:
persistent memory
session history
session search
relevant recall
memory management
learning journey
skill creation/improvement

Self-evolution:
Propose improvement → sandbox/test → evaluate → security review →
approval/trusted policy → activate → monitor → rollback
```
- Memory inspection where appropriate.
- Memory provenance.
- Session search.
- Learning journey.
- Reusable skills.
- Skill version/security review.
## 14. BROWSER AUTOMATION
- Navigate.
- Inspect.
- Click/type/select.
- Extract.
- Vision.
- Browser task status.
- Approval before sensitive external actions.
- Final-state verification.
- Recovery/persistence where appropriate.
## 15. VOICE
```text
Voice is another interface to the SAME Hermes session.

Wake word
↓
Continuous listening
↓
Streaming STT
↓
Hermes
↓
OmniRoute → model
↓
Hermes action
↓
Streaming TTS
↓
Barge-in
↓
Same session/context

Requirements:
VAD, streaming STT, streaming model output, streaming TTS,
echo cancellation, silence detection, barge-in, reconnect,
text fallback, memory continuity and task continuity.

First playable audio target <=5 seconds.
Hard maximum 10 seconds.
Start TTS from sentence/phrase buffers instead of waiting for the full response.

Primary TTS: Kokoro-82M where resources permit.
Fallback: Edge TTS.
Desired voice: young-adult feminine, warm, gentle, calm, intelligent,
soft, natural, medium-slow, not overly cheerful, confident/reassuring.
Custom voice cloning only with explicit consent.
```
```text
voice:
  enabled: true
  primary:
    provider: kokoro
    model: Kokoro-82M
    preload: true
    warmup: true
  fallback:
    provider: edge_tts
    enabled: true
    voice: en-US-EmmaMultilingualNeural
    streaming: true
  latency:
    first_audio_target_ms: 5000
    fallback_trigger_ms: 4000
    hard_max_ms: 10000
  failover:
    on_timeout: true
    on_error: true
    on_no_audio: true
    retry_primary_next_turn: true
```
## 16. WAKE WORD / HANDS-FREE
- Wake phrase such as Hermes.
- On-device/local listener where supported.
- Same Hermes session.
- Listening/speaking/processing/barging-in states.
- Microphone permission.
- Reconnect.
- Mute/disable.
- Text fallback.
## 17. MODELS + OMNIROUTE
- Model switching without state reset.
- Provider fallback.
- Availability.
- Latency/capability information.
- Routing status only when useful.
- Never expose OmniRoute as a second assistant.
- Never let model changes modify Hermes memory or authority.
## 18. MCP + INTEGRATIONS
- GitHub.
- Notion.
- Gmail.
- Telegram.
- WhatsApp official API.
- Hugging Face.
- Databases.
- File systems.
- Internal APIs.
- Browser/tool servers.
- Other MCP servers.
```text
DISCOVER
→ EXPLICIT AUTHORIZE
→ AUTHENTICATE
→ SCOPE
→ STORE SECRET REFERENCE
→ TEST
→ ENABLE
→ USE
→ VERIFY
→ AUDIT
→ REVOKE/ROTATE

Least privilege.
Secrets never in prompts, model context, Notion, GitHub or logs.
```
## 19. CREDENTIAL VAULT
```text
HF Space Secrets = bootstrap/root-of-trust only.

Persistent:
 /data/jarvis/secrets/vault.db

Credential record:
credential_id
type
scope
status
created_at
last_used_at
last_verified_at

UI may show provider, status, scopes and timestamps.
UI must never show API keys, OAuth secrets, tokens or passwords.

The model receives capability results, not raw credentials.
```
Use encrypted storage and strong isolation. The goal is zero intentional credential disclosure to the model and minimal data exposure.
## 20. SECURITY + TRUST
```text
Owner Policy
 ↓
Hermes Policy
 ↓
Project Policy
 ↓
Task Policy
 ↓
Worker Policy
 ↓
Tool Permission
 ↓
Actual Action

Protect against:
prompt injection
credential exfiltration
malicious tool output
unsafe shell commands
privilege escalation
destructive actions
secret logging
unauthorized scope expansion
cross-project leakage

High-risk/destructive/scope-expanding actions require explicit approval
unless an equivalent owner policy explicitly authorizes them.
```
- Show intended action.
- Show scope.
- Record authorization.
- Execute.
- Verify result.
- Audit.
## 21. CRON + AUTOMATION
- One-shot jobs.
- Recurring jobs.
- Natural language schedules.
- Cron expressions.
- Pause/resume/edit.
- Trigger now.
- Remove.
- Skills.
- Delivery to chat/files/platforms.
- Event-triggered jobs.
- Execution history.
- Failure reporting.
- Per-job model/provider where supported.
- Fresh isolated sessions.
Durable automation must survive process restarts through persistent state. Do not depend only on a foreground chat session.
## 22. PROJECTS
- Create/open.
- Overview.
- Workspace path.
- GitHub repository.
- Default branch.
- Agents.
- Tasks.
- Memory/context.
- Artifacts.
- Logs/traces.
- Integrations.
- Permissions.
- Environment health.
- Recent changes.
```text
Project registry:
project_name
local_path
github_repo
default_branch
environment
permissions
active_tasks
last_verified_state
```
## 23. ARTIFACTS
- Generated files.
- Build outputs.
- Reports.
- Images.
- Logs.
- Archives.
- Tests.
- Deployment outputs.
- Open/download/share.
- Provenance.
- Retention where appropriate.
## 24. NOTIFICATIONS
- Task completed/failed.
- Approval required.
- Agent completed/failed.
- CI failed.
- Deployment changed.
- Scheduled job.
- Server health.
- Voice disconnect.
- Integration expiry.
- Security event.
## 25. WATCHDOG + SERVER HEALTH
- Hermes health.
- OmniRoute health.
- Model availability.
- Storage.
- Database.
- WebSocket.
- Voice.
- Workers.
- Background tasks.
- CPU/RAM/disk.
- Heartbeat.
- Recovery state.
```text
Failure → classify
transient → retry
dependency → wait
code → debug
permission → request approval
environment → repair
unknown → diagnose and surface evidence
```
## 26. OBSERVABILITY
- Execution traces.
- Task timeline.
- Tool calls.
- Agent events.
- Model metadata.
- Latency.
- Errors.
- Retries.
- Approvals.
- Security events.
- Git operations.
- CI.
- Deployment verification.
- Audit records.
Never put secrets into logs.
## 27. CHECKPOINTS + RECOVERY
- Snapshot before risky file changes.
- Rollback.
- Task checkpoints.
- Agent checkpoints.
- Resume after reconnect.
- Resume after restart.
- Recover incomplete tasks.
- Preserve durable state.
- Show recovery status.
## 28. OFFLINE + RECONNECT
- Cache recent sessions.
- Cache safe project metadata.
- Show connection state.
- Reconnect automatically.
- Deduplicate events.
- Recover active task state.
- Queue safe local actions.
- Never claim server execution while disconnected.
## 29. AGENT OS
```text
Agent OS is Hermes infrastructure, not a competing agent.

Spec = task contract
Trace = execution/audit
Trust = permission/authority
Memory = durable knowledge
Pipe = agent communication

Do not rebuild Linux.
Build the Agent OS layer around Hermes.
```
```text
Hermes
 ├─ Agent OS primitives
 ├─ tools / terminal / files / GitHub / browser
 ↓
OmniRoute
 ↓
Models
```
## 30. FEATURE → EXISTING UI MAPPING
```text
CHAT
→ streaming, tools, tasks, agents, artifacts, approvals, memory

COMPOSER
→ files, images, voice, references, project context

PROJECTS
→ workspace, GitHub, tasks, agents, artifacts, health

TASKS
→ autonomous jobs, progress, logs, checkpoints, retry, approval

AGENTS
→ delegation, parallel workers, teams, outputs

VOICE
→ wake word, STT, TTS, barge-in

MEMORY
→ persistent memory, session search, learning, skills

GITHUB
→ repo, branch, commit, PR, CI, release, deployment

NOTION
→ pages, databases, project state, docs, planning

INTEGRATIONS
→ OAuth/API/MCP, scopes, permissions

TERMINAL
→ server computer, commands, files, processes

ARTIFACTS
→ outputs and provenance

SETTINGS
→ account, model, voice, integrations, security, permissions

NOTIFICATIONS
→ background events and approvals

SERVER
→ health, runtime, storage, workers, model gateway
```
## 31. UI STATE MACHINE
```text
Every asynchronous feature supports:
IDLE
LOADING
STREAMING
WAITING
RUNNING
PAUSED
AWAITING_APPROVAL
RETRYING
FAILED
RECOVERING
COMPLETED
CANCELLED
OFFLINE
RECONNECTING

Never hide important state.
Never show COMPLETED without backend evidence.
```
## 32. EVENT CONTRACT
```text
Typed real-time events:
SESSION_CREATED
MESSAGE_CREATED
MESSAGE_DELTA
MESSAGE_COMPLETED
TOOL_STARTED
TOOL_OUTPUT
TOOL_COMPLETED
TASK_CREATED
TASK_UPDATED
TASK_COMPLETED
TASK_FAILED
AGENT_CREATED
AGENT_UPDATED
AGENT_COMPLETED
APPROVAL_REQUIRED
APPROVAL_RESOLVED
ARTIFACT_CREATED
GITHUB_UPDATED
CRON_UPDATED
VOICE_STARTED
VOICE_TRANSCRIPT
VOICE_AUDIO
VOICE_INTERRUPTED
SERVER_STATUS
ERROR
RECONNECT_REQUIRED

Events should include:
event_id
timestamp
session_id
task_id when applicable
source
type
payload
sequence/version where needed
```
## 33. API BOUNDARY
- Authentication.
- Sessions.
- Streaming conversation.
- Tasks.
- Agents.
- Projects.
- Memory.
- GitHub.
- Notion.
- Credential connections.
- Voice WebSocket.
- Notifications.
- Artifacts.
- Health.
- Server event stream.
## 34. VISUAL QA
```text
For every Stitch screen:
1. Render at target Android dimensions.
2. Capture screenshot.
3. Compare to Stitch baseline.
4. Check layout, typography, spacing, colors, icons and states.
5. Fix.
6. Repeat.
7. Run functional QA.
8. Run accessibility QA.
9. Run performance QA.

The goal is faithful reproduction, not "similar enough".
Use screenshot regression to prevent UI drift.
```
Where available, use Stitch DESIGN.md as a portable design-rule contract. Google describes DESIGN.md as an agent-friendly format for exporting/importing design rules across tools.
## 35. ACCESSIBILITY + PERFORMANCE
- Content descriptions.
- Semantic labels.
- Touch targets.
- Text scaling.
- Contrast.
- Keyboard navigation where relevant.
- Screen reader support.
- Reduced motion.
- Voice fallback.
- Do not communicate errors by color alone.
- Fast first render.
- Lazy lists.
- Non-blocking network work.
- Efficient WebSocket processing.
- Efficient terminal logs.
- Battery-aware voice.
- Graceful reconnect.
## 36. IMPLEMENTATION ORDER
```text
PHASE 1: Inspect Stitch/export and repository. Freeze baseline.
PHASE 2: Extract tokens/components. Create screenshot baselines.
PHASE 3: Shell + navigation + chat + streaming.
PHASE 4: Sessions + memory + context references.
PHASE 5: Autonomous tasks + agents.
PHASE 6: Projects + terminal + server computer.
PHASE 7: GitHub + CI + artifacts + Notion.
PHASE 8: Integrations + credential vault + approvals.
PHASE 9: Voice + wake word + streaming TTS/STT + barge-in.
PHASE 10: Cron + notifications + watchdog.
PHASE 11: Observability + recovery + security hardening.
PHASE 12: Functional + visual + accessibility + performance + restart/reconnect tests + release build.
```
## 37. DEFINITION OF DONE
- Stitch visual baseline preserved.
- All existing screens compile/run.
- Every Hermes core capability has an appropriate UI surface.
- Streaming works.
- Sessions persist/resume.
- Memory works.
- Tasks run durably.
- Agents delegate/report.
- Terminal/files work.
- Projects persist.
- GitHub works.
- Notion works.
- Integrations use secure authorization.
- Secrets never appear in UI/model/logs.
- Voice streams and supports interruption.
- Wake word works where supported.
- Cron works.
- Notifications work.
- Server health works.
- Recovery works after disconnect/restart.
- Visual regression passes.
- Accessibility passes.
- Functional tests pass.
- Build and CI pass.
- Remote state is verified.
- No fake/mock completion is presented as production functionality.
## 38. NON-NEGOTIABLE PROHIBITIONS
- Do not redesign the Stitch UI.
- Do not replace the Claude-style visual language.
- Do not introduce a competing design system.
- Do not make OmniRoute a visible assistant.
- Do not create another authority above Hermes.
- Do not store secrets in source code, Notion, GitHub or logs.
- Do not expose secrets to the model.
- Do not rely on ephemeral Space disk for durable state.
- Do not claim mocked features are complete.
- Do not blindly overwrite existing work.
- Do not remove working functionality without justification.
- Do not change backend architecture merely for UI convenience.
- Do not sacrifice the visual baseline. Integrate additively.
## 39. FINAL AGENT INSTRUCTION
```text
Before coding:
1. Inspect repository.
2. Inspect Hermes docs.
3. Inspect Stitch/export.
4. Build screen inventory.
5. Map every Hermes capability to existing screens.
6. Identify only necessary additive surfaces.
7. Implement incrementally.
8. Run the app.
9. Capture screenshots.
10. Compare against Stitch.
11. Fix drift.
12. Run functional, accessibility, security and performance tests.
13. Build.
14. Run CI.
15. Verify remote state.
16. Report only verified completion.

When uncertain:
PRESERVE THE UI.
PRESERVE HERMES AUTHORITY.
PRESERVE EXISTING ARCHITECTURE.
ADD CAPABILITY RATHER THAN REDESIGN.

FINAL PRINCIPLE:
DO NOT BUILD A NEW HERMES UI.
BUILD HERMES INSIDE THE EXISTING CLAUDE/STITCH UI.

The finished product should feel like:
"Claude, upgraded into Hermes."

One coherent assistant.
Full autonomous Hermes capability underneath the familiar UI.
```
## 40. CURRENT REFERENCE LINKS
- Stitch project: https://stitch.withgoogle.com/projects/10079330980620936979
- GitHub: https://github.com/JishnuPG-tech/Hermes-x
- Stitch DESIGN.md: https://blog.google/innovation-and-ai/models-and-research/google-labs/stitch-design-md/
- Hermes features: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/overview.md
- Hermes memory: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/memory.md
- Hermes cron: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/cron.md
- Hermes tools: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/tools.md
- Hermes sessions: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/sessions.md

## Reference Note

Google documents Stitch as a high-fidelity UI design canvas and describes DESIGN.md as an agent-friendly format for exporting/importing design rules across tools. citeturn0search0turn0search1

## Reference Links

- Stitch project: https://stitch.withgoogle.com/projects/10079330980620936979
- GitHub: https://github.com/JishnuPG-tech/Hermes-x
- Stitch DESIGN.md: https://blog.google/innovation-and-ai/models-and-research/google-labs/stitch-design-md/
- Hermes features: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/overview.md
- Hermes memory: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/memory.md
- Hermes cron: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/cron.md
- Hermes tools: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/features/tools.md
- Hermes sessions: https://github.com/NousResearch/hermes-agent/blob/main/website/docs/user-guide/sessions.md