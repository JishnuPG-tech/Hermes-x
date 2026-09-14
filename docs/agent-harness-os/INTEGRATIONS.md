# Integration Architecture

## Integration principle

External systems are capability providers behind adapters. The model sees stable actions, not raw SDK internals or credentials.

```text
Hermes
  ↓
Integration Gateway
  ├── GitHub adapter
  ├── Obsidian adapter
  ├── Notion adapter
  ├── Web adapter
  └── Future adapters
```

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

## Obsidian

Obsidian is the local/project knowledge layer.

Recommended modes:

### Mode A: vault mounted to the server

```text
Obsidian Vault
      ↓
read-only indexer
      ↓
knowledge store
      ↓
Hermes context retrieval
```

Writes should be limited to explicitly approved folders such as:

```text
00-Inbox/
Projects/<project>/Agent Logs/
Projects/<project>/Decisions/
Projects/<project>/Lessons/
```

### Mode B: Obsidian Sync or remote storage

Use a connector/API layer rather than giving the agent broad filesystem access.

### Knowledge extraction

Index:

- headings
- tags
- links/backlinks
- frontmatter
- modified time
- project paths

Avoid indexing private folders unless explicitly allowed.

## Notion

Notion is the structured workspace layer.

Recommended database mappings:

```text
Projects
Tasks
Bugs
Decisions
Releases
Agent Runs
```

Capabilities:

- search pages
- retrieve page content
- query databases
- create task
- update task status
- append progress
- create completion report

The integration service owns OAuth/API credentials. Hermes receives a capability result, never the credential itself.

## Synchronization strategy

Do not attempt uncontrolled bidirectional synchronization.

Use explicit ownership:

```text
GitHub → source code + CI truth
Notion → planning/task truth
Obsidian → knowledge/notes truth
Agent OS → execution/task-run truth
```

Cross-system references use stable IDs and URLs.

## Failure behavior

If an integration is unavailable:

1. Record the failure.
2. Continue unrelated local work when safe.
3. Retry according to integration policy.
4. Queue the pending write if it is safe and idempotent.
5. Notify the owner only when the dependency blocks the requested outcome.

## Security

- OAuth/API keys live in a secret store or runtime environment.
- Never write credentials into Obsidian, Notion, GitHub issues or logs.
- Use least-privilege scopes.
- Separate read and write capabilities.
- Require approval for broad or destructive writes.
- Log actor, task, target and operation for every mutation.
