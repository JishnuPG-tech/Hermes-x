package com.example.hermes.data.local

data class SessionEntity(
    val id: String,
    val title: String,
    val model: String = "hermes-agent",
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)

data class MessageEntity(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val thinking: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val artifactTitle: String? = null,
    val artifactType: String? = null,
    val artifactLanguage: String? = null,
    val artifactCode: String? = null,
    val stepTitle: String? = null,
    val userId: String = ""
)

data class TaskEntity(
    val id: String,
    val title: String,
    val prompt: String = "",
    val status: String = "RUNNING",
    val progress: Float = 0f,
    val agentName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)

data class ProjectEntity(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)

data class ArtifactEntity(
    val id: String,
    val title: String,
    val type: String,
    val language: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)


