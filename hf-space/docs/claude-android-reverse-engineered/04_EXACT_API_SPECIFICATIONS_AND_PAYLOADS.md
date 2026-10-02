# Claude Android — Exact API Specifications & Payloads

---

## 1. Authentication & Security Headers

All authenticated REST requests to `api.claude.ai` supply:
* `Authorization: Bearer <session-token>` (or `sessionKey` cookie)
* `User-Agent: Claude-Android/1.260828.0 (Android 16; Build 26082800)`
* `anthropic-client-version: 1.260828.0`
* `anthropic-client-platform: android`
* `anthropic-device-id: <uuid>`

---

## 2. Complete REST API Specifications

### 2.1 Chat & Conversation Endpoints (`defpackage.e33`)

#### 1. Initiate Streaming Completion
* **Endpoint**: `POST /organizations/{organization_uuid}/chat_conversations/{chat_uuid}/completion2`
* **Headers**: `Accept: text/event-stream`, `Content-Type: application/json`
* **Request Body (`ChatCompletionRequest`)**:
```json
{
  "prompt": "Analyze the codebase and write unit tests",
  "timezone": "America/New_York",
  "model": "claude-sonnet-5-20260620",
  "attachments": [],
  "files": [
    {
      "file_name": "MainActivity.kt",
      "file_size": 4096,
      "file_type": "text/plain",
      "file_uuid": "file_01Abc..."
    }
  ],
  "rendering_mode": "MESSAGES",
  "input_mode": "DIRECT",
  "tools": ["web_search", "artifacts", "code_execution"],
  "parent_message_uuid": "msg_01Abc...",
  "effort": "low",
  "thinking_mode": "adaptive",
  "completion_request_id": "req_01Xyz..."
}
```
* **Response**: `200 OK` (Server-Sent Event Stream).

---

#### 2. Get Conversation Details
* **Endpoint**: `GET /organizations/{organization_uuid}/chat_conversations/{chat_uuid}`
* **Query Parameters**:
  * `rendering_mode`: `"MESSAGES"`
  * `render_all_mobile_tools`: `true`
  * `tools`: `"web_search,artifacts,code_execution"`
  * `include_extracted_content`: `true`
* **Response Body (`ChatConversationWithNestedMessage`)**:
```json
{
  "uuid": "chat_01Abc...",
  "name": "Android Architecture Refactor",
  "summary": "Conversation regarding Jetpack Compose and MVI",
  "created_at": "2026-09-18T10:00:00Z",
  "updated_at": "2026-09-18T10:05:00Z",
  "chat_messages": [
    {
      "uuid": "msg_01...",
      "sender": "human",
      "text": "Hello Claude",
      "created_at": "2026-09-18T10:00:00Z"
    },
    {
      "uuid": "msg_02...",
      "sender": "assistant",
      "text": "Hello! How can I help you today?",
      "content": [
        { "type": "text", "text": "Hello! How can I help you today?" }
      ],
      "created_at": "2026-09-18T10:00:02Z"
    }
  ]
}
```

---

#### 3. Submit Tool Approval
* **Endpoint**: `POST /organizations/{organization_uuid}/chat_conversations/{chat_uuid}/tool_approval`
* **Request Body (`RecordToolApprovalRequest`)**:
```json
{
  "tool_use_id": "toolu_01Abc...",
  "decision": "allow_once",
  "message_uuid": "msg_01Abc..."
}
```
* **Response**: `200 OK` `{ "success": true }`.

---

#### 4. Upload File Attachment
* **Endpoint**: `POST /{organization_uuid}/upload`
* **Headers**: `Content-Type: multipart/form-data`
* **Form Parts**: `file` (Binary file content), `file_name` (String), `content_type` (MIME)
* **Response Body (`MessageFile`)**:
```json
{
  "file_uuid": "file_01Abc...",
  "file_name": "screenshot.png",
  "file_size": 245100,
  "file_type": "image/png",
  "created_at": "2026-09-18T10:12:00Z"
}
```

---

### 2.2 Claude Code Remote (CCR) & Cowork Endpoints (`defpackage.hhi`)

#### 1. Create Remote Session
* **Endpoint**: `POST /api/organizations/{organizationId}/cowork/sessions`
* **Request Body (`CreateCoworkRemoteSessionRequest`)**:
```json
{
  "message": "Fix broken gradle build dependencies",
  "message_uuid": "msg_01Xyz...",
  "model": "claude-sonnet-5",
  "effort_level": "medium",
  "file_attachments": [],
  "user_declared_urls": [],
  "permission_mode": "auto",
  "target_device_id": "dev_laptop_123",
  "memory_mode": "project"
}
```
* **Response Body (`CreateCoworkRemoteSessionResponse`)**:
```json
{
  "session": {
    "id": "sess_01Abc..."
  }
}
```

---

#### 2. Multi-Session Watch Stream
* **Endpoint**: `GET /v1/code/sessions/watch`
* **Headers**: `Accept: text/event-stream`
* **Query Parameters**:
  * `resume_token`: `"tok_01..."`
  * `include_trigger_sessions`: `true`
* **Response**: `200 OK` (Stream of session state changes and execution steps).

---

#### 3. Single-Session Event Stream
* **Endpoint**: `GET /v1/code/sessions/{sessionId}/events/stream`
* **Headers**: `Accept: text/event-stream`
* **Query Parameters**: `from_sequence_num`: `0`
* **Response**: `200 OK` (Realtime terminal chunks, diff events, and tool outputs).

---

#### 4. Move Session to Cloud Runner
* **Endpoint**: `POST /v1/code/sessions/{sessionId}/move-to-cloud`
* **Request Body (`MoveToCloudRequest`)**:
```json
{
  "environment_id": "env_anthropic_cloud_default",
  "git_commit_sha": "31dcb6c04f9812a"
}
```
* **Response Body (`SessionV2Envelope`)**:
```json
{
  "session_id": "sess_01Abc...",
  "status": "RUNNING",
  "environment_state": "CLOUD_PROVISIONED"
}
```

---

### 2.3 Artifacts & Wiggle File Endpoints (`defpackage.cbm`)

* `POST /organizations/{org}/conversations/{chat}/wiggle/upload-file`: Upload files into live artifact workspace.
* `POST /organizations/{org}/conversations/{chat}/wiggle/delete-file`: Remove file from artifact directory.
* `POST /organizations/{org}/conversations/{chat}/wiggle/convert-file-to-artifact`: Promote local file to standalone public artifact.
* `GET /api/organizations/{org}/conversations/{chat}/wiggle/download-file?path={path}`: Download raw artifact file content.

---

### 2.4 Error Response Schemas (`ClaudeApiError`)

When an endpoint fails, the API returns a structured JSON payload:
```json
{
  "error": {
    "type": "rate_limit_error | overloaded_error | authentication_error | invalid_request_error | cloudflare_challenge",
    "message": "You have exceeded your current message limits. Try again in 2 hours.",
    "resets_at": "2026-09-18T14:00:00Z",
    "rate_limit_upsell": "GET_MORE_USAGE"
  }
}
```
