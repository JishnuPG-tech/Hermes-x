# Model Context Protocol (MCP) & Extensible Tool Ecosystem

---

## 1. Overview: MCP on Mobile

Claude Android includes comprehensive native support for **Model Context Protocol (MCP)** (`com.anthropic.claude.api.mcp.*`), enabling mobile conversations to connect to external databases, tools, APIs, and enterprise systems.

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLAUDE ANDROID CLIENT                      │
│   • MCP Directory & Connector Settings                          │
│   • OAuth 2.0 PKCE Authorization Handshake                      │
│   • Attach MCP Prompts & Dynamic Resources to Chat              │
└───────────────────────────────┬─────────────────────────────────┘
                                │ JSON / SHTTP
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MCP SHTTP GATEWAY                          │
│        (Endpoint: /v1/toolbox/shttp/mcp/{server_id})            │
└───────────────┬─────────────────────────────────┬───────────────┘
                │ SSE / STDIO                     │ HTTP POST
                ▼                                 ▼
┌───────────────────────────────┐ ┌───────────────────────────────┐
│     LOCAL / DESKTOP TOOLS     │ │      REMOTE CLOUD MCP SERVERS │
│  • Local Filesystem Servers   │ │  • GitHub, Notion, PostgreSQL │
│  • Memory & SQLite Databases  │ │  • Jira, Figma, Sentry, AWS   │
└───────────────────────────────┘ └───────────────────────────────┘
```

---

## 2. Supported Transports & Network Protocols

```
┌─────────────────────────────────────────────────────────────────┐
│                       MCP TRANSPORTS                            │
├───────────────┬─────────────────────────────────────────────────┤
│ Transport     │ Protocol Specification & Implementation         │
├───────────────┼─────────────────────────────────────────────────┤
│ STDIO         │ Standard Input/Output subprocess pipes (Desktop │
│               │ and Claude Code CLI daemons).                   │
│ SSE           │ HTTP Server-Sent Events for streaming updates   │
│               │ from cloud-hosted MCP servers.                  │
│ SHTTP         │ Streaming HTTP transport for direct low-latency │
│               │ tool invocation over `/v1/toolbox/shttp/mcp/*`. │
└───────────────┴─────────────────────────────────────────────────┘
```

---

## 3. Remote Server Probing & Capability Discovery

When a user adds an MCP server URL, the client dispatches an `McpProbeRequest` to validate connectivity:

```json
// POST /v1/mcp/probe
{
  "server_url": "https://mcp.internal.company.com/sse",
  "auth_type": "oauth2"
}
```

### Probing Lifecycle Steps (`McpProbeStepName`)
1. `PING`: Basic HTTP / SSL connectivity verification.
2. `INITIALIZE`: Handshake exchanging protocol version and client capabilities.
3. `TOOLS_LIST`: Discovers available tool schemas, parameter definitions, and descriptions.
4. `PROMPTS_LIST`: Discovers server-provided prompt templates.
5. `RESOURCES_LIST`: Discovers browsable resource trees (files, database tables).

---

## 4. OAuth 2.0 PKCE & Custom Auth Handshake

For secure remote MCP servers:

```
Mobile Client                                            MCP Auth Gateway
     │                                                          │
     ├────── POST /v1/mcp/auth/start ──────────────────────────►│
     │       { server_id, redirect_uri }                        │
     │                                                          │
     │◄───── 200 OK (authorization_url, code_verifier) ─────────┤
     │                                                          │
  (User completes browser login / deep-link redirect)           │
     │                                                          │
     ├────── POST /v1/mcp/auth/complete ───────────────────────►│
     │       { authorization_code, code_verifier }              │
     │                                                          │
     │◄───── 200 OK (access_token_secured_in_keystore) ─────────┤
```

---

## 5. Tool Permissions & Governance Model

Every MCP tool is governed by explicit permission policies:

```
┌─────────────────────────────────────────────────────────────────┐
│                   MCP TOOL GOVERNANCE LEVELS                    │
├───────────────────────┬─────────────────────────────────────────┤
│ Policy Option         │ Execution Behavior                      │
├───────────────────────┼─────────────────────────────────────────┤
│ ALWAYS_ALLOW          │ Tool executes automatically with no     │
│                       │ user prompt (e.g. Read-only queries).   │
│ PROMPT_EACH_TIME      │ Explicit user confirmation card is      │
│                       │ required before each execution.         │
│ SESSION_ALLOW         │ User approves once; permitted for the   │
│                       │ duration of the current active session. │
└───────────────────────┴─────────────────────────────────────────┘
```

---

## 6. Attaching MCP Resources & Prompts to Chat

* **Dynamic Resource Attachment (`AttachMcpResourceRequest`)**:
  * Users can attach live database queries or document trees into context:
  ```json
  {
    "resource_uri": "postgres://db.internal/customers/orders_view",
    "mime_type": "application/json"
  }
  ```
* **Prompt Attachment (`AttachMcpPromptRequest`)**:
  * Ingests standardized enterprise prompt workflows with parameters into the active composer.
