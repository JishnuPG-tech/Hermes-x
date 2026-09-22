package com.example.hermes.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val thinking: String? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val artifactTitle: String? = null,
    val artifactType: String? = null,
    val artifactLanguage: String? = null,
    val artifactCode: String? = null,
    val stepTitle: String? = null,
    val attachments: List<ChatAttachment> = emptyList()
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: kotlinx.serialization.json.JsonElement
) {
    constructor(role: String, textContent: String) : this(
        role = role,
        content = kotlinx.serialization.json.JsonPrimitive(textContent)
    )
}

@Serializable
data class ChatCompletionRequest(
    val model: String = "hermes-3-llama-3.1-8b",
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val max_tokens: Int? = null,
    @SerialName("session_id")
    val sessionId: String? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatCompletionChoice> = emptyList()
)

@Serializable
data class ChatCompletionChoice(
    val index: Int? = null,
    val message: ChatCompletionMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatCompletionMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val index: Int? = null,
    val delta: StreamDelta? = null,
    val finish_reason: String? = null
)

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    val reasoning: String? = null,
    val text: String? = null
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val prompt: String = "",
    val status: String = "RUNNING", // "RUNNING", "COMPLETED", "PAUSED", "FAILED"
    val progress: Float = 0f,
    val agent_name: String? = null,
    val supervisor: String? = "Hermes Supervisor",
    val subtasks: List<SubtaskDto> = emptyList(),
    val error: String? = null,
    val created_at: Double = 0.0,
    val updated_at: Double = 0.0
)

@Serializable
data class SubtaskDto(
    val id: String = "",
    val subtask_id: String = "",
    val title: String = "",
    val description: String = "",
    val role: String = "Developer",
    val status: String = "pending",
    val dependencies: List<String> = emptyList(),
    val acceptance_criteria: List<String> = emptyList(),
    val result: String? = null
)

@Serializable
data class WorkforceRoleDto(
    val name: String = "",
    val description: String = "",
    val allowed_tools: List<String> = emptyList(),
    val output_contract: String = ""
)

@Serializable
data class WorkforceRolesResponseDto(
    val roles: List<WorkforceRoleDto> = emptyList()
)

@Serializable
data class ScheduledAutomationDto(
    val id: String = "",
    val title: String = "",
    val cron_expression: String = "0 * * * *",
    val prompt: String = "",
    val enabled: Boolean = true,
    val last_run: Double = 0.0,
    val next_run: Double = 0.0,
    val status: String = "active"
)

@Serializable
data class AutomationsResponseDto(
    val automations: List<ScheduledAutomationDto> = emptyList()
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val prompt: String
)

@Serializable
data class ApprovalDto(
    val id: String,
    val task_id: String? = null,
    val tool: String = "bash",
    val command: String = "",
    val risk: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val status: String = "PENDING", // "PENDING", "APPROVED", "DENIED"
    val reason: String? = null,
    val created_at: Double = 0.0
)

@Serializable
data class ApprovalsResponse(
    val approvals: List<ApprovalDto> = emptyList()
)

@Serializable
data class HostStatusDto(
    val status: String = "healthy",
    val storage_root: String = "/data",
    val writable: Boolean = true,
    val disk_free_gb: Double = 0.0,
    val disk_total_gb: Double = 0.0,
    val active_projects_count: Int = 0,
    val active_workspaces_count: Int = 0
)

@Serializable
data class KnowledgeSourceDto(
    val name: String,
    val type: String,
    val primary: Boolean = false,
    val status: String = "unknown",
    val message: String = "",
    val capabilities: List<String> = emptyList()
)

@Serializable
data class KnowledgeSourcesResponse(
    val status: String = "ok",
    val default_source: String = "notion",
    val sources: List<KnowledgeSourceDto> = emptyList()
)

@Serializable
data class KnowledgeSearchResultItemDto(
    val id: String = "",
    val source: String = "notion",
    val title: String = "",
    val snippet: String = "",
    val score: Double = 0.0,
    val url: String? = null,
    val updated_at: Double = 0.0
)

@Serializable
data class KnowledgeSearchResponseDto(
    val status: String = "ok",
    val query: String = "",
    val results_count: Int = 0,
    val results: List<KnowledgeSearchResultItemDto> = emptyList()
)

@Serializable
data class ComputerFileItemDto(
    val name: String = "",
    val path: String = "",
    val is_dir: Boolean = false,
    val size: Long = 0L,
    val modified: Double = 0.0
)

@Serializable
data class ComputerFilesResponseDto(
    val status: String = "ok",
    val path: String = "",
    val files: List<ComputerFileItemDto> = emptyList()
)

@Serializable
data class ComputerFileContentResponseDto(
    val status: String = "ok",
    val path: String = "",
    val content: String = ""
)

@Serializable
data class BrowserStatusDto(
    val status: String = "ok",
    val current_url: String = "about:blank",
    val title: String = "Ready",
    val is_active: Boolean = false
)

@Serializable
data class BrowserNavigateResponseDto(
    val status: String = "ok",
    val url: String = "",
    val title: String = "",
    val status_code: Int = 200,
    val content_snippet: String = ""
)

@Serializable
data class BrowserScreenshotResponseDto(
    val status: String = "ok",
    val screenshot_base64: String = "",
    val url: String = "",
    val message: String? = null
)

@Serializable
data class DirectoryServerItemDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "ready",
    val tools: List<String> = emptyList()
)

@Serializable
data class DirectoryServersResponse(
    val servers: List<DirectoryServerItemDto> = emptyList(),
    val data: List<DirectoryServerItemDto> = emptyList()
)

@Serializable
data class VoiceWsMessage(
    val type: String, // "audio", "transcript", "status", "error"
    val data: String? = null,
    val state: String? = null
)

@Serializable
data class McpServerDto(
    val id: String,
    val name: String,
    val serverUrl: String,
    val transport: String = "SHTTP", // "STDIO", "SSE", "SHTTP"
    val isConnected: Boolean = true,
    val toolCount: Int = 0
)

@Serializable
data class McpProbeDto(
    val serverUrl: String,
    val authType: String = "none",
    val status: String = "ready"
)

@Serializable
data class LoginRequest(
    val password: String
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val authenticated: Boolean = false
)

@Serializable
data class AuthStatusResponse(
    val auth_enabled: Boolean = false,
    val password_auth_enabled: Boolean = false,
    val logged_in: Boolean = false
)

@Serializable
data class SessionDto(
    val session_id: String,
    val title: String = "Chat",
    val workspace: String = "",
    val model: String? = null,
    val message_count: Int = 0,
    val created_at: Double = 0.0,
    val updated_at: Double = 0.0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val project_id: String? = null
)

@Serializable
data class SessionsResponse(
    val sessions: List<SessionDto> = emptyList()
)

@Serializable
data class NewSessionRequest(
    val title: String? = null,
    val model: String? = null,
    val workspace: String? = null,
    val id: String? = null,
    val session_id: String? = null,
    val user_id: String? = null
)

@Serializable
data class NewSessionResponse(
    val ok: Boolean = false,
    val session: SessionDto? = null
)

@Serializable
data class SessionMessageDto(
    val role: String,
    val content: String = "",
    val reasoning_content: String? = null,
    val timestamp: Double? = null
)

@Serializable
data class SessionDetailDto(
    val session_id: String,
    val title: String = "Chat",
    val workspace: String = "",
    val model: String? = null,
    val message_count: Int = 0,
    val messages: List<SessionMessageDto> = emptyList(),
    val is_streaming: Boolean = false,
    val status: String = "idle"
)

@Serializable
data class SessionDetailResponse(
    val session: SessionDetailDto? = null
)

@Serializable
data class SessionStatusDto(
    val session_id: String? = null,
    val is_streaming: Boolean = false,
    val status: String = "idle",
    val current_text: String? = null,
    val has_active_run: Boolean = false
)

@Serializable
data class ProjectDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val workspace: String = "",
    val created_at: Double = 0.0
)

@Serializable
data class ProjectsResponse(
    val projects: List<ProjectDto> = emptyList()
)

@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String = ""
)

@Serializable
data class ModelDescriptionDto(
    val text: String? = null
)

@Serializable
data class ModelCapabilitiesDto(
    val mm_images: Boolean = false,
    val web_search: Boolean = false,
    val code_execution: Boolean = false
)

@Serializable
data class ModelOptionDto(
    val id: String,
    val name: String,
    val display_name: String? = null,
    val short_name: String? = null,
    val description: ModelDescriptionDto? = null,
    val capabilities: ModelCapabilitiesDto? = null
)

@Serializable
data class ModelsResponse(
    val data: List<ModelOptionDto> = emptyList()
)

@Serializable
data class ArtifactItemDto(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String,
    val language: String? = null,
    val code: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class GoogleAuthRequestDto(
    val id_token: String? = null,
    val token: String? = null,
    val source: String = "google_mobile"
)

@Serializable
data class GoogleAccountDto(
    val uuid: String = "",
    val email_address: String = "",
    val full_name: String = "",
    val display_name: String = ""
)

@Serializable
data class VerifyGoogleResponse(
    val success: Boolean = false,
    val secret: String? = null,
    val sessionKey: String? = null,
    val account: GoogleAccountDto? = null,
    val state: String? = null,
    val google_sub: String? = null
)

@Serializable
data class AuthConfigResponse(
    val google_client_id: String? = null,
    val auth_methods: List<String> = emptyList()
)

// ── Channels Hub DTOs ──────────────────────────────────────────
@Serializable
data class TelegramChannelDto(
    val enabled: Boolean = false,
    val token_masked: String = "",
    val allowed_users: String = "*",
    val admin_id: String = "",
    val status: String = "disconnected"
)

@Serializable
data class EmailChannelDto(
    val enabled: Boolean = false,
    val address: String = "jishnupg2005@gmail.com",
    val has_password: Boolean = false,
    val imap_host: String = "imap.gmail.com",
    val smtp_host: String = "smtp.gmail.com",
    val imap_port: Int = 993,
    val smtp_port: Int = 587,
    val poll_interval: Int = 15,
    val status: String = "standby"
)

@Serializable
data class DiscordChannelDto(
    val enabled: Boolean = false,
    val token_masked: String = "",
    val allowed_users: String = "*",
    val status: String = "disconnected"
)

@Serializable
data class WebhooksChannelDto(
    val enabled: Boolean = true,
    val endpoint: String = "/v1/channels/webhook",
    val status: String = "ready"
)

@Serializable
data class ChannelsConfigDto(
    val telegram: TelegramChannelDto = TelegramChannelDto(),
    val email: EmailChannelDto = EmailChannelDto(),
    val discord: DiscordChannelDto = DiscordChannelDto(),
    val webhooks: WebhooksChannelDto = WebhooksChannelDto()
)

@Serializable
data class TelegramUpdateRequestDto(
    val enabled: Boolean? = null,
    val token: String? = null,
    val allowed_users: String? = null,
    val admin_id: String? = null
)

@Serializable
data class EmailUpdateRequestDto(
    val enabled: Boolean? = null,
    val address: String? = null,
    val password: String? = null,
    val imap_host: String? = null,
    val smtp_host: String? = null,
    val poll_interval: Int? = null
)

@Serializable
data class DiscordUpdateRequestDto(
    val enabled: Boolean? = null,
    val token: String? = null,
    val allowed_users: String? = null
)

@Serializable
data class UpdateChannelsRequestDto(
    val telegram: TelegramUpdateRequestDto? = null,
    val email: EmailUpdateRequestDto? = null,
    val discord: DiscordUpdateRequestDto? = null
)

@Serializable
data class TestChannelRequestDto(
    val channel: String = "telegram",
    val message: String = "🔔 [Hermes Test Alert] Channel connection is verified and operational!"
)

@Serializable
data class TestChannelResponseDto(
    val status: String = "success",
    val channel: String = "",
    val error: String = ""
)

// ── OmniRoute Telemetry DTOs ─────────────────────────────────────
@Serializable
data class ModelTelemetryItemDto(
    val id: String = "",
    val provider: String = "",
    val latency_ms: Int = 0,
    val uptime_pct: Double = 99.9,
    val status: String = "standby",
    val cost_per_1m: String = "$0.00"
)

@Serializable
data class TraceTelemetryItemDto(
    val timestamp: Long = 0L,
    val model: String = "",
    val provider: String = "",
    val tokens_in: Int = 0,
    val tokens_out: Int = 0,
    val latency_ms: Int = 0,
    val status: Int = 200
)

@Serializable
data class OmniRouteTelemetryDto(
    val status: String = "operational",
    val active_upstream: String = "",
    val active_model: String = "",
    val total_requests: Int = 0,
    val avg_latency_ms: Int = 0,
    val cache_hit_rate: String = "0%",
    val failover_mode: String = "automatic",
    val models: List<ModelTelemetryItemDto> = emptyList(),
    val recent_traces: List<TraceTelemetryItemDto> = emptyList()
)

data class FtsSearchResultDto(
    val sessionId: String,
    val sessionTitle: String,
    val role: String,
    val snippet: String,
    val timestamp: Long
)


