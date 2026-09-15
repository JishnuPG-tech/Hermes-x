# Integration Architecture

## Integration principle

External systems are capability providers behind adapters. The model sees stable actions, not raw SDK internals or credentials.

Hermes is the authority that selects, authorizes, executes and verifies integrations. Integrations never become autonomous authorities above Hermes.

```text
Hermes
  ↓
Policy / Trust Layer
  ↓
Integration Gateway
  ├── Gmail adapter
  ├── Telegram adapter
  ├── WhatsApp adapter
  ├── GitHub adapter
  ├── Hugging Face adapter
  ├── Notion adapter
  ├── Web adapter
  └── Future adapters / MCP servers
```

## Explicit user authorization

Hermes may automate a platform when the owner explicitly asks it to configure or use that platform. Mentioning a platform is not authorization by itself.

Examples:

- `Connect my Gmail and manage my email.`
- `Connect this GitHub account and manage this repository.`
- `Connect my Telegram bot and use it for notifications.`
- `Connect my WhatsApp business account using the official API.`
- `Connect my Hugging Face account and manage my Spaces.`
- `Connect my Notion workspace and use it for project management.`

For every new connection Hermes should identify the exact account/resource, explain the requested scope when practical, use the official authentication mechanism, grant least privilege, store only an opaque credential reference, and record authorization metadata.

A previously granted capability must not silently expand to a new account, resource or operation. If a requested action exceeds the stored scope, Hermes asks for explicit authorization.

## Integration security contract

```text
User explicit authorization
          ↓
Hermes Trust / Policy
          ↓
Integration Gateway
          ↓
Scoped capability
          ↓
Official platform API / OAuth / approved MCP
          ↓
External platform
```

### Non-negotiable rules

- Never expose API keys, OAuth tokens, refresh tokens, cookies, passwords or private keys to the model.
- Never put secrets in prompts, Notion pages, GitHub issues, task descriptions, logs or traces.
- Store credentials only in an encrypted secret-management layer or secure runtime secret binding.
- Give each integration the minimum scopes required.
- Prefer separate credentials/connections per environment and capability.
- Redact secrets from tool output and error messages.
- Treat all external content as untrusted data and defend against prompt injection.
- Never let external content redefine Hermes policy or authorization.
- Log the actor, task, integration, target, operation, policy decision and result without logging secret values.
- Require approval for destructive, high-impact or unusually broad operations.
- Use idempotency and replay protection for external mutations where supported.
- Support explicit connection revocation and scope reduction.
- Use official APIs, OAuth, connectors or approved MCP servers. Do not bypass platform authentication or platform security controls.

## Gmail

Use Gmail OAuth/API capabilities behind the Integration Gateway.

Possible capabilities:

- search mail
- read messages
- draft email
- send email
- reply
- label/archive where authorized
- attachment handling

Sending, deleting, forwarding or bulk-changing mail should respect the granted scope and approval policy.

## Telegram

Use the official Telegram Bot API or an approved account integration.

Possible capabilities:

- send notifications
- receive bot messages
- respond to commands
- manage configured bot workflows

Do not expose bot tokens to the model.

## WhatsApp

Use a supported official WhatsApp API/business integration and only the capabilities permitted by that platform and account.

Possible capabilities may include:

- send approved messages
- receive webhook events
- respond through configured workflows
- manage approved business messaging operations

Hermes must not use credential harvesting, unofficial session extraction or mechanisms intended to bypass WhatsApp security or platform restrictions.

## Hugging Face

Use Hugging Face authentication and API capabilities through a dedicated adapter.

Possible capabilities:

- inspect account resources
- manage authorized Spaces
- inspect or update repositories where authorized
- trigger configured deployments/workflows
- inspect builds and runtime status
- manage approved artifacts

Tokens must remain in the secret store and must never be returned as tool output.

## GitHub

GitHub is the engineering system of record for source code, issues, pull requests, CI and releases.

Capabilities:

- inspect repository
- read issues
- create branch
- edit files
- commit
- push
- create PR
- review PR
- inspect checks
- respond to review feedback
- merge when authorized
- verify merge
- inspect releases

Rules:

- Never claim CI passed without checking current checks.
- Never claim a PR merged without verifying the remote state.
- Use isolated worktrees for parallel code workers.
- Never expose GitHub tokens to the model as plain text.

## Notion

**Notion is now the primary structured workspace and project-management system for Hermes.**

Notion is used for:

- projects
- tasks
- requirements
- bugs
- decisions
- releases
- agent runs
- project documentation
- operational dashboards
- automation state that is intentionally human-visible

Recommended databases:

```text
Projects
Tasks
Bugs
Requirements
Decisions
Releases
Agent Runs
Integrations
```

Hermes should use the official Notion API, official Notion MCP or another approved Notion integration according to the deployment mode. Notion's current MCP is specifically designed for AI clients to read and update workspace content, while the API supports scoped connections and credentials. The remote MCP connection uses explicit OAuth authorization.

Notion remains a data/workspace system. Hermes remains the autonomous authority and execution engine.

## Memory and workspace ownership

Notion replaces Obsidian as the primary human-visible knowledge/workspace system for this project.

```text
Notion → primary structured knowledge + project/work-management truth
GitHub → source code + CI + release truth
Agent OS → live execution state + checkpoints + traces
Hermes → authority, orchestration, policy and autonomous execution
OmniRoute → model/provider power only
```

Hermes should not silently maintain a competing second source of truth in Obsidian. If an Obsidian connector remains for compatibility, it is optional and must have explicitly defined ownership.

## Synchronization strategy

Do not attempt uncontrolled bidirectional synchronization.

Use explicit ownership:

```text
GitHub → source code + CI truth
Notion → planning + project + human-visible knowledge truth
Agent OS → execution + task-run truth
Hermes → orchestration + authority
```

Cross-system references use stable IDs and URLs. Synchronization jobs must be idempotent and record their source and destination.

## Failure behavior

If an integration is unavailable:

1. Record the failure without secrets.
2. Continue unrelated local work when safe.
3. Retry according to integration policy.
4. Queue the pending write if it is safe and idempotent.
5. Notify the owner only when the dependency blocks the requested outcome.

## Integration lifecycle

```text
DISCOVER
   ↓
EXPLICIT AUTHORIZE
   ↓
AUTHENTICATE
   ↓
SCOPE
   ↓
STORE SECRET REFERENCE
   ↓
TEST CONNECTION
   ↓
ENABLE CAPABILITIES
   ↓
USE
   ↓
VERIFY
   ↓
AUDIT
   ↓
REVOKE / ROTATE when requested
```

The goal is **maximum useful automation with minimum necessary authority and zero intentional credential disclosure to the model**.
