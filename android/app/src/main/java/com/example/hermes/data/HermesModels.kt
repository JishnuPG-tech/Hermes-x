package com.example.hermes.data

import kotlinx.serialization.Serializable
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
    val stepTitle: String? = null
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String = "hermes-3-llama-3.1-8b",
    val messages: List<ApiMessage>,
    val stream: Boolean = true
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
    val reasoning_content: String? = null
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val status: String = "pending", // "pending", "in_progress", "completed", "failed"
    val progress: Float = 0f,
    val subtasks: List<SubtaskDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class SubtaskDto(
    val id: String,
    val title: String,
    val status: String = "pending"
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val prompt: String
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
    val archived: Boolean = false
)

@Serializable
data class SessionsResponse(
    val sessions: List<SessionDto> = emptyList()
)

@Serializable
data class NewSessionRequest(
    val title: String? = null,
    val model: String? = null,
    val workspace: String? = null
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
    val messages: List<SessionMessageDto> = emptyList()
)

@Serializable
data class SessionDetailResponse(
    val session: SessionDetailDto? = null
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

