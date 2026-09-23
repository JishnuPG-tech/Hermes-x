# Hermes System Prompt Analysis Report

## Scope

The public `asgeirtj/system_prompts_leaks` repository currently exposes a large corpus spanning Anthropic, OpenAI, Google, xAI, Perplexity, Microsoft, Cursor, Meta, Mistral, Kimi, DeepSeek, OpenCode, Pi, Qwen, Notion, and other products. The repository tree contains 704 files, including 458 Markdown/text/YAML/JSON-style files.

The repository README organizes the major model/product captures by family. The current README specifically lists recent Claude, Claude Code, ChatGPT, Codex, Gemini, Antigravity, Grok, Perplexity, OpenCode, Cursor, Microsoft Copilot, and related agent prompts.

The local container could not resolve `github.com` for a normal `git clone`, so the repository was inspected through the connected GitHub repository interface instead. The analysis therefore uses the live repository contents exposed by GitHub rather than pretending a local clone succeeded.

## Major patterns found

### Anthropic / Claude

The Claude web prompts are strongly organized around behavioral domains such as product context, refusal/safety behavior, tone and formatting, wellbeing, and dynamic product instructions.

Claude Code adds a much more explicit agent harness contract. Important recurring concepts are:

- permission-aware tool use
- dedicated tools over generic shell commands when appropriate
- handling denied actions without blindly retrying
- persistent memory
- context management and long-context continuation
- skills and user-invocable skills
- subagents
- project instructions
- browser automation discipline
- verification of external and destructive actions
- truthful reporting of actual outcomes
- codebase convention matching

Claude Code also demonstrates a modular architecture where agents, skills, commands, output styles, and specialized prompts live as separate assets.

### OpenAI / ChatGPT / Codex

The ChatGPT captures contain extensive environment and tool contracts. The agent-mode capture is particularly focused on computer interaction, browsing, research, citations, recency, clarification, and autonomy.

Codex prompts emphasize software-engineering execution discipline, project conventions, skills, verification, autonomy, file editing, user communication, and explicit handling of destructive actions.

The recurring lesson is that model behavior is shaped by a combination of:

- system instructions
- tool contracts
- environment context
- task-specific rules
- user instructions
- skills/modes
- verification behavior

### Google / Gemini / Antigravity

Gemini CLI strongly emphasizes a development lifecycle, security and system integrity, context efficiency, engineering standards, subagent orchestration, skills, hooks, tool usage, autonomous modes, Git workflows, and memory.

Antigravity emphasizes planning, user review, execution, verification, artifact handling, subagents, implementation plans, walkthroughs, research, and structured project work.

### Perplexity Computer / Research

Perplexity Computer is strongly tool-first. Its captured behavior emphasizes autonomous exploration, external connectors, browser/computer interaction, memory, skills, subagents, parallel work, and alternatives when a path is blocked.

The main lesson for Hermes is to treat tools as the primary means of obtaining current external state rather than allowing the model to guess.

### xAI / Grok

The Grok captures contain broad tool catalogs plus memory, subagents, planning, scheduling, workflow tools, monitoring, skill/context concepts, and computer/browser capabilities.

The important architectural pattern is a large capability surface paired with explicit operational rules for using that capability surface.

### Cursor

Cursor places considerable emphasis on precise tool definitions, code references, repository-aware editing, formatting, skills, agent transcripts, and explicit tool access.

The main lesson is that high-quality coding behavior comes from a contract between the model and the tool/runtime layer, not from prose alone.

### OpenCode

OpenCode is intentionally concise in ordinary communication. Its system contract still contains strong engineering practices: inspect the codebase first, follow conventions, use specialized tools, verify work, keep security in mind, and do not commit changes unless authorized.

### Existing Hermes prompt

The existing Hermes source already contains useful concepts:

- Hermes identity
- sovereign runtime ownership
- tool use
- encrypted credentials
- dynamic thinking/status events
- semantic memory recall
- tool execution loops
- persistent task infrastructure
- Notion/knowledge integration
- multimodal handling

However, it also contains the current weaknesses we identified:

- keyword-based task routing
- keyword-based proactive tool selection
- fixed model tier classification
- fixed greetings
- fixed persona responses
- dynamic thinking strings derived from keyword checks
- a separate synthesis phase with additional keyword classification
- generic fallback messages
- promotion of reasoning to final output

Those behaviors can directly cause the Notion/document problem discussed earlier, because a failed agent/tool path can fall through into a generic response rather than recovering through capability discovery and continued execution.

## Cross-family convergence

The strongest common pattern across the corpus is:

```text
System Contract
      +
Runtime / Harness
      +
Capabilities / Tools
      +
Skills / Modes
      +
Memory / Context
      +
Task State
      +
Permissions / Approvals
      +
Verification
      =
Agent Behavior
```

A master prompt should therefore define behavior, priorities, authority boundaries, tool-use principles, skill selection, memory discipline, task persistence, verification, error recovery, and response truthfulness.

It should not become a giant collection of task-specific keyword rules.

## Hermes architecture derived from the corpus

Recommended runtime structure:

```text
User
  ↓
Hermes Master System Prompt
  ↓
Context Builder
  ├── authenticated identity
  ├── project context
  ├── task state
  ├── relevant memory
  ├── capability catalog
  └── active skills
  ↓
AgentRuntime
  ↓
Capability Discovery
  ↓
Tool / Skill Selection
  ↓
PolicyGuard
  ↓
ToolExecutor / Harness
  ↓
Result Inspection
  ↓
Verification Gate
  ↓
Message AST / Renderer
  ↓
Hermes UI / Voice / Channel
```

## What the system prompt should control

The master prompt should control:

- Hermes identity
- semantic interpretation of user goals
- autonomy boundaries
- tool-selection behavior
- tool-result interpretation
- skill selection behavior
- memory behavior
- planning behavior
- verification behavior
- error recovery behavior
- security and prompt-injection resistance
- external side-effect handling
- subagent delegation principles
- truthful reporting
- response style
- modality consistency

## What the system prompt should not control

These should remain runtime responsibilities:

- authentication
- authorization
- user identity resolution
- tool availability
- tool execution
- filesystem sandboxing
- network restrictions
- resource limits
- task durability
- scheduling
- credential storage
- actual verification evidence
- model routing implementation
- database state

## Final design principle

The target is not "Claude inside Hermes".

The target is a Hermes agent with the strongest recurring operational patterns found across modern agent systems, adapted to Hermes-x's own architecture.

The prompt should tell the model how to behave as an agent. The runtime must make that behavior real.
