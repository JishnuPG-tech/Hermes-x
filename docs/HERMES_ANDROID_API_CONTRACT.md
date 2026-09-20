# Hermes Android API Contract Specification

This document defines the canonical HTTP, SSE, and WebSocket wire protocols utilized by the native Android client to interact with the Hermes Agent server (`https://jishnupg-hermes.hf.space` or local gateway).

---

## 1. Authentication & Session Security

### 1.1 Login & Cookie Authentication
- **Endpoint:** `POST /api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "password": "YOUR_HERMES_AUTH_KEY"
  }
  ```
- **Response:** `200 OK` with `Set-Cookie: session=...; Path=/; HttpOnly; SameSite=Lax`
- **Client Handling:** Handled via OkHttp `InMemoryCookieJar` or persistent cookie storage; all subsequent HTTP, SSE, and WebSocket requests automatically transmit the session cookie.

### 1.2 Authentication Status Check
- **Endpoint:** `GET /api/auth/status`
- **Response:**
  ```json
  {
    "authenticated": true,
    "user": "admin"
  }
  ```

---

## 2. Application Bootstrap & Model Catalog

### 2.1 App Start Bootstrap
- **Endpoints:** `GET /api/bootstrap`, `GET /bootstrap/{org_id}/app_start`, `GET /account/app_start`
- **Headers:** Session cookie or `Authorization: Bearer <token>`
- **Response:**
  ```json
  {
    "account": {
      "uuid": "usr_0123456789abcdef",
      "email_address": "jishnupg2005@gmail.com",
      "full_name": "Jishnu (Admin Max)",
      "memberships": [
        {
          "organization": {
            "id": "org_0123456789abcdef",
            "name": "Hermes Admin Max Team",
            "capabilities": ["chat", "claude_pro", "claude_max", "artifacts", "projects", "voice", "mcp"]
          },
          "role": "admin"
        }
      ]
    },
    "models": [
      {
        "id": "claude-3-5-sonnet-20241022",
        "name": "Sonnet 3.7",
        "short_name": "Sonnet 3.7 Low"
      }
    ]
  }
  ```

### 2.2 Model Catalog
- **Endpoints:** `GET /api/models`, `GET /v1/models`
- **Response:**
  ```json
  {
    "data": [
      {
        "id": "claude-3-5-sonnet-20241022",
        "name": "Sonnet 3.7",
        "short_name": "Sonnet 3.7 Low",
        "capabilities": {
          "code_execution": true,
          "web_search": true,
          "mm_images": true
        }
      }
    ]
  }
  ```

---

## 3. Conversations & Real-Time Chat Streaming

### 3.1 Sessions Listing
- **Endpoint:** `GET /api/sessions`
- **Response:**
  ```json
  [
    {
      "session_id": "sess_8921f00a",
      "name": "Python String Reversal",
      "created_at": "2026-09-20T09:29:00Z",
      "updated_at": "2026-09-20T09:30:00Z",
      "pinned": false
    }
  ]
  ```

### 3.2 Session Creation
- **Endpoint:** `POST /api/session/new`
- **Request Body:**
  ```json
  {
    "name": "New Chat"
  }
  ```
- **Response:**
  ```json
  {
    "session_id": "sess_9042ab11",
    "name": "New Chat"
  }
  ```

### 3.3 Session Message History
- **Endpoint:** `GET /api/sessions/{session_id}/messages` or `GET /api/session?id={session_id}`
- **Response:**
  ```json
  [
    {
      "id": "msg_001",
      "role": "user",
      "content": "Write a Python function to reverse a string"
    },
    {
      "id": "msg_002",
      "role": "assistant",
      "content": "I am Hermes Agent. Here is a clean, robust...",
      "step_title": "Building script",
      "artifact_title": "script",
      "artifact_type": "Code · PY"
    }
  ]
  ```

### 3.4 SSE Realtime Chat Streaming
- **Endpoint:** `POST /v1/chat/completions`
- **Headers:** `Accept: text/event-stream`, `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "model": "hermes-agent",
    "stream": true,
    "messages": [
      { "role": "user", "content": "Write a Python function" }
    ]
  }
  ```
- **Event Protocol:**
  - `data: {"choices":[{"delta":{"reasoning_content":"Analyzing requirement..."}}]}` -> triggers `ThinkingDelta` & Thinking Starburst animation
  - `data: {"choices":[{"delta":{"content":"Here is the code..."}}]}` -> triggers `TextDelta`
  - `data: [DONE]` -> triggers `MessageCompleted` & final state commit

---

## 4. Tasks & Autonomous Engine

### 4.1 Tasks Listing
- **Endpoint:** `GET /v1/tasks` or `GET /api/tasks`
- **Response:**
  ```json
  [
    {
      "id": "tsk_8921",
      "title": "Isolate Worker Workspaces",
      "status": "RUNNING",
      "progress": 0.65,
      "steps": [
        { "id": "step_1", "name": "Grep files", "status": "COMPLETED" },
        { "id": "step_2", "name": "Write Dockerfile", "status": "RUNNING" }
      ]
    }
  ]
  ```

### 4.2 Task Control Actions
- **Pause:** `POST /v1/tasks/{task_id}/pause`
- **Resume:** `POST /v1/tasks/{task_id}/resume`
- **Cancel:** `POST /v1/tasks/{task_id}/cancel`

---

## 5. Security Vault & Approval Hierarchy

### 5.1 Approvals Listing
- **Endpoint:** `GET /v1/approvals`
- **Response:**
  ```json
  [
    {
      "id": "appr_101",
      "type": "BASH",
      "command": "docker push registry.internal/hermes-worker:latest",
      "reason": "Pushing built worker image to repository",
      "risk_level": "HIGH"
    }
  ]
  ```

### 5.2 Approve / Deny
- **Approve:** `POST /v1/approvals/{approval_id}/approve`
- **Deny:** `POST /v1/approvals/{approval_id}/deny`

---

## 6. Live Terminal & PTY Streaming

### 6.1 PTY WebSocket Stream
- **URL:** `WSS /api/pty` or `WSS /ws/logs`
- **Protocol:** Duplex binary/text frames for stdin input, terminal escape sequences (ANSI), and stdout/stderr broadcasts.

---

## 7. Voice Autonomy Mode

### 7.1 Voice WebSocket Protocol
- **URL:** `WSS /v1/voice/ws` or `WSS /voice`
- **Inbound frames (Mic to Server):** PCM 16kHz 16-bit mono or Opus audio chunks.
- **Outbound frames (Server to Speaker):** TTS audio chunks and transcript state updates (`{"type": "transcript", "role": "assistant", "text": "..."}`).
- **REST Turn Fallback:** `POST /v1/voice/turn` with multipart audio input.
