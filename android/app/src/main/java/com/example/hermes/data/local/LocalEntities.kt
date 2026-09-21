package com.example.hermes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["user_id", "updated_at"], name = "idx_sessions_user_updated")
    ]
)
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "model")
    val model: String = "hermes-agent",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String = "guest"
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["user_id", "session_id"], name = "idx_messages_user_session"),
        Index(value = ["session_id", "timestamp"], name = "idx_messages_session_time")
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "thinking")
    val thinking: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_streaming")
    val isStreaming: Boolean = false,

    @ColumnInfo(name = "artifact_title")
    val artifactTitle: String? = null,

    @ColumnInfo(name = "artifact_type")
    val artifactType: String? = null,

    @ColumnInfo(name = "artifact_language")
    val artifactLanguage: String? = null,

    @ColumnInfo(name = "artifact_code")
    val artifactCode: String? = null,

    @ColumnInfo(name = "step_title")
    val stepTitle: String? = null,

    @ColumnInfo(name = "user_id")
    val userId: String = "guest"
)

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["user_id", "updated_at"], name = "idx_tasks_user_updated")
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "prompt")
    val prompt: String = "",

    @ColumnInfo(name = "status")
    val status: String = "RUNNING",

    @ColumnInfo(name = "progress")
    val progress: Float = 0f,

    @ColumnInfo(name = "agent_name")
    val agentName: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String = "guest"
)

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["user_id", "created_at"], name = "idx_projects_user_created")
    ]
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String = "guest"
)

@Entity(
    tableName = "artifacts",
    indices = [
        Index(value = ["user_id", "created_at"], name = "idx_artifacts_user_created")
    ]
)
data class ArtifactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "language")
    val language: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String = "guest"
)
