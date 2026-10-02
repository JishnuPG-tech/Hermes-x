# Claude Code Remote (CCR) & Agent Harness Architecture

---

## 1. Overview & Architecture Boundary

**Claude Code Remote (CCR)** (internal codename: *Cowork / Dispatch*) turns the Android mobile client into an active control surface for remote developer agents executing on a user's computer or in isolated cloud containers.

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLAUDE ANDROID CLIENT                      │
│     • Session List & Multi-Session Watch Stream                 │
│     • Live ANSI Terminal Card & Diffs                           │
│     • Action Approval Notifications (Approve / Deny / Comment)  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ SSE / REST
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   CLAUDE CODE BACKPLANE GATEWAY                 │
└───────────────┬─────────────────────────────────┬───────────────┘
                │ Bridge Protocol                 │ Cloud Dispatch
                ▼                                 ▼
┌───────────────────────────────┐ ┌───────────────────────────────┐
│     LOCAL DEVELOPER MACHINE   │ │   ANTHROPIC CLOUD CONTAINERS  │
│  • Claude Code CLI Daemon     │ │   • Ephemeral Docker / MicroVM│
│  • Git Worktree Isolation     │ │   • Cloned from latest git ref│
│  • Local Filesystem Access    │ │   • BYOC / Self-Hosted Pools  │
└───────────────────────────────┘ └───────────────────────────────┘
```

---

## 2. Worktree & Bridge Spawn Modes (`BridgeSpawnMode`)

When launching a remote coding session, the developer daemon supports 3 directory isolation strategies:

```
┌─────────────────────────────────────────────────────────────────┐
│                       BRIDGE SPAWN MODES                        │
├─────────────────┬───────────────────────────────────────────────┤
│ Mode            │ Operational Behavior & Isolation Strategy     │
├─────────────────┼───────────────────────────────────────────────┤
│ SingleSession   │ Dedicated single-purpose isolated session.    │
│ Worktree        │ Automatically provisions a new git worktree   │
│                 │ in `.claude/worktrees/<name>` to prevent local│
│                 │ branch conflicts with the developer's IDE.    │
│ SameDir         │ Executes directly in the active project       │
│                 │ root directory (shared workspace).            │
└─────────────────┴───────────────────────────────────────────────┘
```

---

## 3. Permission Modes & Security Policies (`PermissionMode`)

The client exposes 6 distinct security postures:

```
┌─────────────────────────────────────────────────────────────────┐
│                    CCR PERMISSION MODES                         │
├───────────────────┬─────────────────────────────────────────────┤
│ Mode Token        │ Security & Execution Behavior               │
├───────────────────┼─────────────────────────────────────────────┤
│ Default           │ Prompts the user before every mutating tool │
│                   │ call (file write, bash command, push).      │
│ Plan              │ Read-only exploration and planning pass;    │
│                   │ no mutations executed without sign-off.     │
│ Auto (Recommended)│ Evaluates tool calls against prompt-        │
│                   │ injection risk models. Safe actions run     │
│                   │ automatically; risky actions prompt user.   │
│ AcceptEdits       │ Automatically accepts local code edits;     │
│                   │ prompts for external network/shell actions. │
│ DontAsk           │ Suppresses prompts for standard dev tools.  │
│ BypassPermissions │ Fully autonomous execution (opt-in only).   │
└───────────────────┴─────────────────────────────────────────────┘
```

---

## 4. GitHub PR Automation & Sync Engine

The mobile client interacts with GitHub through Anthropic’s GitHub App integration:
* **`POST v1/code/github/subscribe-pr`**: Subscribes to CI webhook events for active PRs.
* **`POST v1/code/github/set-pr-auto-merge`**: Sets the PR to auto-merge once all required checks pass.
* **`POST api/organizations/{org}/code/shares/scan_secrets`**: Scans diffs and workspace files for leaked API keys before publishing public share URLs.
* **`POST api/organizations/{org}/dust/generate_title_and_branch`**: Uses fast reasoning models to auto-generate semantic git branch names (e.g. `feat/auth-token-refresh`) and PR titles from conversation goals.

---

## 5. Cloud Migration Flow ("Move to Cloud")

When a developer laptop goes offline or sleeps while a task is running:
1. The mobile client detects connection loss via the backplane stream (`ccr_devices_status_asleep`).
2. The UI renders a **"Continue in Cloud"** action banner.
3. Upon confirmation, the backend calls `POST /v1/code/sessions/{sessionId}/move-to-cloud`.
4. A new cloud container is provisioned, cloning the branch from the last pushed commit.
5. The Android client transitions its SSE listener to the cloud runner without losing chat context.

---

## 6. Android Notification Action Handler

When an agent needs approval for a command, the app displays an actionable notification:

```
┌─────────────────────────────────────────────────────────────────┐
│ 🔔 Claude Code · Action Required                                │
│ Claude wants to run: docker push registry.internal/app:v2       │
│                                                                 │
│   [ Deny ]      [ Approve Once ]      [ Deny with Comment ]     │
└─────────────────────────────────────────────────────────────────┘
```

* Intercepted by `CCRPermissionActionReceiver` (`com.anthropic.claude.action.CCR_PERMISSION_APPROVE`).
* Dispatches `POST /v1/code/sessions/{sessionId}/events` in the background with `ApprovalDecision`.
