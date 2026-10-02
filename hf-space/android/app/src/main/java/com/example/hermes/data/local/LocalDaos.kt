package com.example.hermes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE user_id = :userId OR user_id = 'guest' OR user_id = '' ORDER BY updated_at DESC")
    fun getSessions(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE user_id = :userId OR user_id = 'guest' OR user_id = '' ORDER BY updated_at DESC")
    suspend fun getSessionsList(userId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '') LIMIT 1")
    suspend fun getSessionById(sessionId: String, userId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<SessionEntity>)

    @Query("UPDATE sessions SET title = :title, updated_at = :updatedAt WHERE id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '')")
    suspend fun updateSessionTitle(sessionId: String, userId: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sessions WHERE id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '')")
    suspend fun deleteSession(sessionId: String, userId: String)

    @Query("DELETE FROM sessions WHERE user_id = :userId")
    suspend fun clearSessionsForUser(userId: String)

    @Query("UPDATE sessions SET user_id = :targetUserId WHERE user_id = 'guest' OR user_id = ''")
    suspend fun migrateGuestSessions(targetUserId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE session_id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '') ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String, userId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE session_id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '') ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String, userId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE session_id = :sessionId AND (user_id = :userId OR user_id = 'guest' OR user_id = '')")
    suspend fun deleteMessagesForSession(sessionId: String, userId: String)

    @Query("DELETE FROM messages WHERE user_id = :userId")
    suspend fun clearMessagesForUser(userId: String)

    @Query("UPDATE messages SET user_id = :targetUserId WHERE user_id = 'guest' OR user_id = ''")
    suspend fun migrateGuestMessages(targetUserId: String)

    @Query("SELECT * FROM messages WHERE (user_id = :userId OR user_id = 'guest' OR user_id = '') AND (content LIKE '%' || :query || '%' OR thinking LIKE '%' || :query || '%') ORDER BY timestamp DESC LIMIT 50")
    suspend fun searchMessages(query: String, userId: String): List<MessageEntity>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY updated_at DESC")
    suspend fun getTasksList(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET status = :status, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    suspend fun updateTaskStatus(taskId: String, userId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :taskId AND user_id = :userId")
    suspend fun deleteTask(taskId: String, userId: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE user_id = :userId ORDER BY created_at DESC")
    fun getProjects(userId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getProjectsList(userId: String): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjects(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :projectId AND user_id = :userId")
    suspend fun deleteProject(projectId: String, userId: String)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE user_id = :userId ORDER BY created_at DESC")
    fun getArtifacts(userId: String): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getArtifactsList(userId: String): List<ArtifactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtifacts(artifacts: List<ArtifactEntity>)

    @Query("DELETE FROM artifacts WHERE id = :artifactId AND user_id = :userId")
    suspend fun deleteArtifact(artifactId: String, userId: String)
}
