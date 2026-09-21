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

/**
 * Hermes Local Cache Database.
 * Enforces strict per-account data isolation:
 * - Account A only accesses Account A data.
 * - Account B only accesses Account B data.
 * - Guest data is strictly scoped to 'guest' without cross-account leakage.
 */
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

    @Volatile
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

    fun getActiveUserId(): String = activeUserId

    /**
     * Switches the active user context.
     * Strictly isolates user data: does NOT overwrite or leak previous guest or user records.
     */
    fun setActiveUser(userId: String) {
        val normalized = userId.trim().ifBlank { "guest" }
        if (activeUserId != normalized) {
            activeUserId = normalized
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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_user_updated ON sessions(user_id, updated_at DESC)")

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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_user_session ON messages(user_id, session_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_session_time ON messages(session_id, timestamp ASC)")

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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_user_updated ON tasks(user_id, updated_at DESC)")

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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_user_created ON projects(user_id, created_at DESC)")

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
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_artifacts_user_created ON artifacts(user_id, created_at DESC)")
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
        }
    }

    fun refreshAll() {
        _sessionsFlow.value = querySessionsSync()
        _tasksFlow.value = queryTasksSync()
        _projectsFlow.value = queryProjectsSync()
        _artifactsFlow.value = queryArtifactsSync()
    }

    // --- SESSIONS (Strictly scoped by activeUserId) ---

    private fun querySessionsSync(): List<SessionEntity> {
        val list = mutableListOf<SessionEntity>()
        readableDatabase.query(
            "sessions", null, "user_id = ?", arrayOf(activeUserId), null, null, "updated_at DESC"
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
                val uid = if (s.userId.isBlank()) activeUserId else s.userId
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
        db.update("sessions", cv, "id = ? AND user_id = ?", arrayOf(sessionId, activeUserId))
        _sessionsFlow.value = querySessionsSync()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("sessions", "id = ? AND user_id = ?", arrayOf(sessionId, activeUserId))
        db.delete("messages", "session_id = ? AND user_id = ?", arrayOf(sessionId, activeUserId))
        _sessionsFlow.value = querySessionsSync()
    }

    // --- MESSAGES (Strictly scoped by activeUserId) ---

    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MessageEntity>()
        readableDatabase.query(
            "messages", null, "session_id = ? AND user_id = ?", arrayOf(sessionId, activeUserId), null, null, "timestamp ASC"
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
                val uid = if (m.userId.isBlank()) activeUserId else m.userId
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
        val list = mutableListOf<FtsSearchResultDto>()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext list

        val sql = """
            SELECT m.session_id, s.title, m.role, m.content, m.timestamp
            FROM messages m
            LEFT JOIN sessions s ON m.session_id = s.id
            WHERE m.user_id = ?
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

    // --- TASKS (Strictly scoped by activeUserId) ---

    private fun queryTasksSync(): List<TaskEntity> {
        val list = mutableListOf<TaskEntity>()
        readableDatabase.query(
            "tasks", null, "user_id = ?", arrayOf(activeUserId), null, null, "updated_at DESC"
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
                val uid = if (t.userId.isBlank()) activeUserId else t.userId
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
        writableDatabase.update("tasks", cv, "id = ? AND user_id = ?", arrayOf(taskId, activeUserId))
        _tasksFlow.value = queryTasksSync()
    }

    // --- PROJECTS (Strictly scoped by activeUserId) ---

    private fun queryProjectsSync(): List<ProjectEntity> {
        val list = mutableListOf<ProjectEntity>()
        readableDatabase.query(
            "projects", null, "user_id = ?", arrayOf(activeUserId), null, null, "created_at DESC"
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
                val uid = if (p.userId.isBlank()) activeUserId else p.userId
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

    // --- ARTIFACTS (Strictly scoped by activeUserId) ---

    private fun queryArtifactsSync(): List<ArtifactEntity> {
        val list = mutableListOf<ArtifactEntity>()
        readableDatabase.query(
            "artifacts", null, "user_id = ?", arrayOf(activeUserId), null, null, "created_at DESC"
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
                val uid = if (a.userId.isBlank()) activeUserId else a.userId
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

    // --- DAO IMPLEMENTATION EXPOSURE ---

    val sessionDao: SessionDao = object : SessionDao {
        override fun getSessions(userId: String): Flow<List<SessionEntity>> = sessionsFlow
        override suspend fun getSessionsList(userId: String): List<SessionEntity> = querySessionsSync()
        override suspend fun getSessionById(sessionId: String, userId: String): SessionEntity? {
            readableDatabase.query(
                "sessions", null, "id = ? AND user_id = ?", arrayOf(sessionId, userId), null, null, null
            ).use { cursor ->
                return if (cursor.moveToFirst()) cursor.toSessionEntity() else null
            }
        }
        override suspend fun upsertSessions(sessions: List<SessionEntity>) = this@HermesLocalDatabase.upsertSessions(sessions)
        override suspend fun updateSessionTitle(sessionId: String, userId: String, title: String, updatedAt: Long) {
            val cv = ContentValues().apply {
                put("title", title)
                put("updated_at", updatedAt)
            }
            writableDatabase.update("sessions", cv, "id = ? AND user_id = ?", arrayOf(sessionId, userId))
            refreshAll()
        }
        override suspend fun deleteSession(sessionId: String, userId: String) {
            writableDatabase.delete("sessions", "id = ? AND user_id = ?", arrayOf(sessionId, userId))
            writableDatabase.delete("messages", "session_id = ? AND user_id = ?", arrayOf(sessionId, userId))
            refreshAll()
        }
        override suspend fun clearSessionsForUser(userId: String) {
            writableDatabase.delete("sessions", "user_id = ?", arrayOf(userId))
            writableDatabase.delete("messages", "user_id = ?", arrayOf(userId))
            refreshAll()
        }
    }

    val messageDao: MessageDao = object : MessageDao {
        override suspend fun getMessagesForSession(sessionId: String, userId: String): List<MessageEntity> {
            val list = mutableListOf<MessageEntity>()
            readableDatabase.query(
                "messages", null, "session_id = ? AND user_id = ?", arrayOf(sessionId, userId), null, null, "timestamp ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.toMessageEntity())
                }
            }
            return list
        }
        override fun getMessagesForSessionFlow(sessionId: String, userId: String): Flow<List<MessageEntity>> {
            val flow = MutableStateFlow<List<MessageEntity>>(emptyList())
            flow.value = kotlinx.coroutines.runBlocking { getMessagesForSession(sessionId, userId) }
            return flow.asStateFlow()
        }
        override suspend fun insertMessages(messages: List<MessageEntity>) = saveMessages(messages)
        override suspend fun deleteMessagesForSession(sessionId: String, userId: String) {
            writableDatabase.delete("messages", "session_id = ? AND user_id = ?", arrayOf(sessionId, userId))
        }
        override suspend fun clearMessagesForUser(userId: String) {
            writableDatabase.delete("messages", "user_id = ?", arrayOf(userId))
        }
        override suspend fun searchMessages(query: String, userId: String): List<MessageEntity> {
            val list = mutableListOf<MessageEntity>()
            val pattern = "%$query%"
            readableDatabase.query(
                "messages", null, "user_id = ? AND (content LIKE ? OR thinking LIKE ?)", arrayOf(userId, pattern, pattern), null, null, "timestamp DESC LIMIT 50"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.toMessageEntity())
                }
            }
            return list
        }
    }

    val taskDao: TaskDao = object : TaskDao {
        override fun getTasks(userId: String): Flow<List<TaskEntity>> = tasksFlow
        override suspend fun getTasksList(userId: String): List<TaskEntity> = queryTasksSync()
        override suspend fun upsertTasks(tasks: List<TaskEntity>) = this@HermesLocalDatabase.upsertTasks(tasks)
        override suspend fun updateTaskStatus(taskId: String, userId: String, status: String, updatedAt: Long) {
            val cv = ContentValues().apply {
                put("status", status)
                put("updated_at", updatedAt)
            }
            writableDatabase.update("tasks", cv, "id = ? AND user_id = ?", arrayOf(taskId, userId))
            refreshAll()
        }
        override suspend fun deleteTask(taskId: String, userId: String) {
            writableDatabase.delete("tasks", "id = ? AND user_id = ?", arrayOf(taskId, userId))
            refreshAll()
        }
    }

    val projectDao: ProjectDao = object : ProjectDao {
        override fun getProjects(userId: String): Flow<List<ProjectEntity>> = projectsFlow
        override suspend fun getProjectsList(userId: String): List<ProjectEntity> = queryProjectsSync()
        override suspend fun upsertProjects(projects: List<ProjectEntity>) = this@HermesLocalDatabase.upsertProjects(projects)
        override suspend fun deleteProject(projectId: String, userId: String) {
            writableDatabase.delete("projects", "id = ? AND user_id = ?", arrayOf(projectId, userId))
            refreshAll()
        }
    }

    val artifactDao: ArtifactDao = object : ArtifactDao {
        override fun getArtifacts(userId: String): Flow<List<ArtifactEntity>> = artifactsFlow
        override suspend fun getArtifactsList(userId: String): List<ArtifactEntity> = queryArtifactsSync()
        override suspend fun upsertArtifacts(artifacts: List<ArtifactEntity>) = this@HermesLocalDatabase.upsertArtifacts(artifacts)
        override suspend fun deleteArtifact(artifactId: String, userId: String) {
            writableDatabase.delete("artifacts", "id = ? AND user_id = ?", arrayOf(artifactId, userId))
            refreshAll()
        }
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
