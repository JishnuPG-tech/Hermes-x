# Server Computer Architecture

## Purpose

Define the production design that turns the Hermes Space into the execution computer for Jarvis while the phone and laptop act as control surfaces.

This layer is intentionally an application-level computer runtime, not a replacement Linux kernel.

## Core principle

```text
Phone / Laptop
   ↓ voice, chat, approvals, status
Jarvis Gateway
   ↓
Agent OS Core
   ↓
Hermes Agent Harness
   ↓
OmniRoute
   ↓
Models
   ↓
Tools: terminal, files, browser, GitHub, databases, services
   ↓
/data/jarvis persistent runtime
```

The client does not need the project files locally. Code execution, builds, tests, Git, database work, browser automation, and long-running jobs happen on the server runtime.

## Persistence rule

The normal Space filesystem is ephemeral. Production state MUST live under an attached read-write Hugging Face Storage Bucket mounted at `/data` or in an external durable database/object store. `/data/jarvis` is the canonical application root.

## Runtime layout

```text
/data/jarvis/
├── projects/             # durable Git working copies
├── workspaces/           # isolated task worktrees/sandboxes
├── databases/            # SQLite or local DB files where appropriate
├── artifacts/            # builds, reports, generated outputs
├── logs/                 # structured runtime logs
├── agent-state/          # checkpoints, task state, queues
├── memory/               # Jarvis-specific durable memory extensions
├── voices/               # voice configuration/reference assets
├── cache/                # disposable caches only
├── backups/              # local backup staging, never sole backup
└── registry/             # project/tool/runtime registry
```

## GitHub is the source of truth for code

Every managed project has a registry entry containing its local path, GitHub repository, default branch, and policy. The normal engineering loop is:

```text
sync → branch/worktree → implement → test → verify → commit → push → CI → deploy → verify
```

Local server copies are working state, not the only copy of source code.

## Dataset repositories versus Storage Buckets

HF Dataset repositories may be used as large read-mostly AI/data libraries. They are not the primary mutable filesystem for Jarvis because repository versioning and Git/Xet semantics are a poor fit for frequent application writes.

Storage Buckets are the preferred mutable runtime filesystem. They provide read-write mounted storage for Spaces.

## Execution model

A task gets:

- task ID
- project ID
- workspace path
- Git ref
- owner/project/task policy
- tool permissions
- timeout and retry policy
- checkpoint location
- verification contract

Destructive operations, secret access, production changes, external publishing, and irreversible data changes require explicit policy or approval.

## Failure model

The runtime must survive:

- process restart
- Space restart
- network interruption
- model timeout
- tool timeout
- partial Git operation
- dependency failure
- worker crash
- interrupted voice session

All recoverable work must checkpoint before long operations and resume from durable state.

## Non-goals

- pretending HF provides an unlimited always-on server
- storing secrets in Git, memory files, or public datasets
- replacing Linux with an agent-written kernel
- claiming lossless durability without backups

## Definition of done

The server-computer layer is complete when a new task can be accepted from mobile, execute entirely on the server, modify a project, run verification, persist its state, push approved changes to GitHub, survive a restart, and report a verifiable result.
