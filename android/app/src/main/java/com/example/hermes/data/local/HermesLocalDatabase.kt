package com.example.hermes.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.example.hermes.data.FtsSearchResultDto

class HermesLocalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "hermes_cache.db"
        const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: HermesLocalDatabase? = null

        fun getInstance(context: Context): HermesLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HermesLocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var activeUserId: String = "guest"

    private val _sessionsFlow = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessionsFlow: Flow<List<SessionEntity>> = _sessionsFlow.asStateFlow()

    private val _tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasksFlow: Flow<List<TaskEntity>> = _tasksFlow.asStateFlow()

    private val _projectsFlow = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val projectsFlow: Flow<List<ProjectEntity>> = _projectsFlow.asStateFlow()

    private val _artifactsFlow = MutableStateFlow<List<ArtifactEntity>>(emptyList())
    val artifactsFlow: Flow<List<ArtifactEntity>> = _artifactsFlow.asStateFlow()

    init {
        refreshAll()
    }

    fun setActiveUser(userId: String) {
        val normalized = userId.trim().ifBlank { "guest" }
        if (activeUserId != normalized) {
            activeUserId = normalized
            if (normalized != "guest") {
                try {
                    writableDatabase.execSQL("UPDATE sessions SET user_id = ? WHERE user_id = 'guest' OR user_id = ''", arrayOf(normalized))
                    writableDatabase.execSQL("UPDATE messages SET user_id = ? WHERE user_id = 'guest' OR user_id = ''", arrayOf(normalized))
                    writableDatabase.execSQL("UPDATE tasks SET user_id = ? WHERE user_id = 'guest' OR user_id = ''", arrayOf(normalized))
                    writableDatabase.execSQL("UPDATE projects SET user_id = ? WHERE user_id = 'guest' OR user_id = ''", arrayOf(normalized))
                    writableDatabase.execSQL("UPDATE artifacts SET user_id = ? WHERE user_id = 'guest' OR user_id = ''", arrayOf(normalized))
                } catch (_: Exception) {}
            }
            refreshAll()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sessions (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                model TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT 'guest'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id, updated_at DESC)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id TEXT PRIMARY KEY,
                session_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                thinking TEXT,
                timestamp INTEGER NOT NULL,
                is_streaming INTEGER NOT NULL DEFAULT 0,
                artifact_title TEXT,
                artifact_type TEXT,
                artifact_language TEXT,
                artifact_code TEXT,
                step_title TEXT,
                user_id TEXT NOT NULL DEFAULT 'guest'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_user ON messages(user_id, session_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                prompt TEXT NOT NULL,
                status TEXT NOT NULL,
                progress REAL NOT NULL DEFAULT 0,
                agent_name TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT 'guest'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id, updated_at DESC)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT 'guest'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS artifacts (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                language TEXT NOT NULL,
                code TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                user_id TEXT NOT NULL DEFAULT 'guest'
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE sessions ADD COLUMN user_id TEXT NOT NULL DEFAULT 'guest'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE messages ADD COLUMN user_id TEXT NOT NULL DEFAULT 'guest'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE tasks ADD COLUMN user_id TEXT NOT NULL DEFAULT 'guest'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE projects ADD COLUMN user_id TEXT NOT NULL DEFAULT 'guest'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE artifacts ADD COLUMN user_id TEXT NOT NULL DEFAULT 'guest'")
            } catch (_: Exception) {}
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id, updated_at DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_user ON messages(user_id, session_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id, updated_at DESC)")
            } catch (_: Exception) {}
        }
    }

    fun refreshAll() {
        _sessionsFlow.value = querySessionsSync()
        _tasksFlow.value = queryTasksSync()
        _projectsFlow.value = queryProjectsSync()
        _artifactsFlow.value = queryArtifactsSync()
    }

    // --- SESSIONS ---

    private fun querySessionsSync(): List<SessionEntity> {
        val list = mutableListOf<SessionEntity>()
        readableDatabase.query(
            "sessions", null, "user_id = ? OR user_id = 'guest'", arrayOf(activeUserId), null, null, "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toSessionEntity())
            }
        }
        return list
    }

    suspend fun upsertSessions(sessions: List<SessionEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (s in sessions) {
                val uid = if (s.userId.isBlank() || s.userId == "guest") activeUserId else s.userId
                val cv = ContentValues().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("model", s.model)
                    put("updated_at", s.updatedAt)
                    put("user_id", uid)
                }
                db.insertWithOnConflict("sessions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _sessionsFlow.value = querySessionsSync()
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", title)
            put("updated_at", System.currentTimeMillis())
        }
        db.update("sessions", cv, "id = ? AND (user_id = ? OR user_id = 'guest')", arrayOf(sessionId, activeUserId))
        _sessionsFlow.value = querySessionsSync()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("sessions", "id = ? AND (user_id = ? OR user_id = 'guest')", arrayOf(sessionId, activeUserId))
        db.delete("messages", "session_id = ? AND (user_id = ? OR user_id = 'guest')", arrayOf(sessionId, activeUserId))
        _sessionsFlow.value = querySessionsSync()
    }

    // --- MESSAGES ---

    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MessageEntity>()
        readableDatabase.query(
            "messages", null, "session_id = ? AND (user_id = ? OR user_id = 'guest')", arrayOf(sessionId, activeUserId), null, null, "timestamp ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toMessageEntity())
            }
        }
        list
    }

    suspend fun saveMessages(messages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (m in messages) {
                val uid = if (m.userId.isBlank() || m.userId == "guest") activeUserId else m.userId
                val cv = ContentValues().apply {
                    put("id", m.id)
                    put("session_id", m.sessionId)
                    put("role", m.role)
                    put("content", m.content)
                    put("thinking", m.thinking)
                    put("timestamp", m.timestamp)
                    put("is_streaming", if (m.isStreaming) 1 else 0)
                    put("artifact_title", m.artifactTitle)
                    put("artifact_type", m.artifactType)
                    put("artifact_language", m.artifactLanguage)
                    put("artifact_code", m.artifactCode)
                    put("step_title", m.stepTitle)
                    put("user_id", uid)
                }
                db.insertWithOnConflict("messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun searchMessagesFts(query: String): List<FtsSearchResultDto> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val list = mutableListOf<FtsSearchResultDto>()
        val sql = """
            SELECT m.session_id, s.title, m.role, m.content, m.timestamp
            FROM messages m
            LEFT JOIN sessions s ON m.session_id = s.id
            WHERE (m.user_id = ? OR m.user_id = 'guest')
              AND m.content LIKE ?
            ORDER BY m.timestamp DESC
            LIMIT 40
        """.trimIndent()

        readableDatabase.rawQuery(sql, arrayOf(activeUserId, "%$trimmed%")).use { cursor ->
            while (cursor.moveToNext()) {
                val sId = cursor.getString(0) ?: ""
                val sTitle = cursor.getString(1) ?: "Chat"
                val role = cursor.getString(2) ?: "assistant"
                val fullContent = cursor.getString(3) ?: ""
                val ts = cursor.getLong(4)

                val idx = fullContent.indexOf(trimmed, ignoreCase = true)
                val start = (idx - 35).coerceAtLeast(0)
                val end = (idx + trimmed.length + 55).coerceAtMost(fullContent.length)
                val snippet = (if (start > 0) "..." else "") +
                        fullContent.substring(start, end).replace("\n", " ") +
                        (if (end < fullContent.length) "..." else "")

                list.add(FtsSearchResultDto(
                    sessionId = sId,
                    sessionTitle = sTitle,
                    role = role,
                    snippet = snippet,
                    timestamp = ts
                ))
            }
        }
        list
    }

    // --- TASKS ---

    private fun queryTasksSync(): List<TaskEntity> {
        val list = mutableListOf<TaskEntity>()
        readableDatabase.query(
            "tasks", null, "user_id = ? OR user_id = 'guest'", arrayOf(activeUserId), null, null, "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toTaskEntity())
            }
        }
        return list
    }

    suspend fun upsertTasks(tasks: List<TaskEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (t in tasks) {
                val uid = if (t.userId.isBlank() || t.userId == "guest") activeUserId else t.userId
                val cv = ContentValues().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("prompt", t.prompt)
                    put("status", t.status)
                    put("progress", t.progress)
                    put("agent_name", t.agentName)
                    put("created_at", t.createdAt)
                    put("updated_at", t.updatedAt)
                    put("user_id", uid)
                }
                db.insertWithOnConflict("tasks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _tasksFlow.value = queryTasksSync()
    }

    suspend fun updateTaskStatus(taskId: String, status: String, progress: Float? = null) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("status", status)
            put("updated_at", System.currentTimeMillis())
            progress?.let { put("progress", it) }
        }
        writableDatabase.update("tasks", cv, "id = ? AND (user_id = ? OR user_id = 'guest')", arrayOf(taskId, activeUserId))
        _tasksFlow.value = queryTasksSync()
    }

    // --- PROJECTS ---

    private fun queryProjectsSync(): List<ProjectEntity> {
        val list = mutableListOf<ProjectEntity>()
        readableDatabase.query(
            "projects", null, "user_id = ? OR user_id = 'guest'", arrayOf(activeUserId), null, null, "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toProjectEntity())
            }
        }
        return list
    }

    suspend fun upsertProjects(projects: List<ProjectEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (p in projects) {
                val uid = if (p.userId.isBlank() || p.userId == "guest") activeUserId else p.userId
                val cv = ContentValues().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    put("created_at", p.createdAt)
                    put("user_id", uid)
                }
                db.insertWithOnConflict("projects", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _projectsFlow.value = queryProjectsSync()
    }

    // --- ARTIFACTS ---

    private fun queryArtifactsSync(): List<ArtifactEntity> {
        val list = mutableListOf<ArtifactEntity>()
        readableDatabase.query(
            "artifacts", null, "user_id = ? OR user_id = 'guest'", arrayOf(activeUserId), null, null, "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toArtifactEntity())
            }
        }
        return list
    }

    suspend fun upsertArtifacts(artifacts: List<ArtifactEntity>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (a in artifacts) {
                val uid = if (a.userId.isBlank() || a.userId == "guest") activeUserId else a.userId
                val cv = ContentValues().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("type", a.type)
                    put("language", a.language)
                    put("code", a.code)
                    put("created_at", a.createdAt)
                    put("user_id", uid)
                }
                db.insertWithOnConflict("artifacts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _artifactsFlow.value = queryArtifactsSync()
    }

    // --- CURSOR CONVERTERS ---

    private fun Cursor.toSessionEntity() = SessionEntity(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        model = getString(getColumnIndexOrThrow("model")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        userId = getColumnIndex("user_id").takeIf { it >= 0 }?.let { getString(it) } ?: "guest"
    )

    private fun Cursor.toMessageEntity() = MessageEntity(
        id = getString(getColumnIndexOrThrow("id")),
        sessionId = getString(getColumnIndexOrThrow("session_id")),
        role = getString(getColumnIndexOrThrow("role")),
        content = getString(getColumnIndexOrThrow("content")),
        thinking = if (isNull(getColumnIndexOrThrow("thinking"))) null else getString(getColumnIndexOrThrow("thinking")),
        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
        isStreaming = getInt(getColumnIndexOrThrow("is_streaming")) == 1,
        artifactTitle = if (isNull(getColumnIndexOrThrow("artifact_title"))) null else getString(getColumnIndexOrThrow("artifact_title")),
        artifactType = if (isNull(getColumnIndexOrThrow("artifact_type"))) null else getString(getColumnIndexOrThrow("artifact_type")),
        artifactLanguage = if (isNull(getColumnIndexOrThrow("artifact_language"))) null else getString(getColumnIndexOrThrow("artifact_language")),
        artifactCode = if (isNull(getColumnIndexOrThrow("artifact_code"))) null else getString(getColumnIndexOrThrow("artifact_code")),
        stepTitle = if (isNull(getColumnIndexOrThrow("step_title"))) null else getString(getColumnIndexOrThrow("step_title")),
        userId = getColumnIndex("user_id").takeIf { it >= 0 }?.let { getString(it) } ?: "guest"
    )

    private fun Cursor.toTaskEntity() = TaskEntity(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        prompt = getString(getColumnIndexOrThrow("prompt")),
        status = getString(getColumnIndexOrThrow("status")),
        progress = getFloat(getColumnIndexOrThrow("progress")),
        agentName = if (isNull(getColumnIndexOrThrow("agent_name"))) null else getString(getColumnIndexOrThrow("agent_name")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        userId = getColumnIndex("user_id").takeIf { it >= 0 }?.let { getString(it) } ?: "guest"
    )

    private fun Cursor.toProjectEntity() = ProjectEntity(
        id = getString(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        description = getString(getColumnIndexOrThrow("description")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        userId = getColumnIndex("user_id").takeIf { it >= 0 }?.let { getString(it) } ?: "guest"
    )

    private fun Cursor.toArtifactEntity() = ArtifactEntity(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        type = getString(getColumnIndexOrThrow("type")),
        language = getString(getColumnIndexOrThrow("language")),
        code = getString(getColumnIndexOrThrow("code")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        userId = getColumnIndex("user_id").takeIf { it >= 0 }?.let { getString(it) } ?: "guest"
    )
}
