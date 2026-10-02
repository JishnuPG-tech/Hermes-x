package com.example.hermes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class Thinking(val text: String) : StreamEvent()
    data class Done(val fullResponse: String) : StreamEvent()
    data class Error(val throwable: Throwable) : StreamEvent()
}

class InMemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val currentCookies = cookieStore.getOrPut(host) { mutableListOf() }
        cookies.forEach { newCookie ->
            currentCookies.removeAll { it.name == newCookie.name }
            currentCookies.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: emptyList()
        val now = System.currentTimeMillis()
        return cookies.filter { it.expiresAt > now }
    }

    fun clear() {
        cookieStore.clear()
    }
}

class HermesApiClient(
    private var baseUrl: String = DEFAULT_BASE_URL,
    private var apiKey: String = DEFAULT_API_KEY
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://jishnupg-hermes.hf.space"
        const val DEFAULT_API_KEY = ""
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val instance: HermesApiClient by lazy { HermesApiClient() }
    }

    private val cookieJar = InMemoryCookieJar()

    val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var userId: String = ""

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl.trimEnd('/')
    }

    fun updateApiKey(newKey: String) {
        apiKey = newKey
    }

    fun updateUserId(id: String) {
        userId = id
    }

    fun getBaseUrl(): String = baseUrl

    /**
     * Authenticate with the server using the WebUI password or API key.
     */
    suspend fun login(password: String = apiKey): Boolean = withContext(Dispatchers.IO) {
        val reqBody = json.encodeToString(LoginRequest(password))
        val req = Request.Builder()
            .url("$baseUrl/api/auth/login")
            .post(reqBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val bodyStr = resp.body?.string() ?: return@withContext false
                val res = json.decodeFromString<LoginResponse>(bodyStr)
                res.ok || res.authenticated
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Authenticate with Google ID Token via backend /api/auth/verify_google_mobile
     */
    suspend fun verifyGoogleToken(idToken: String): Result<VerifyGoogleResponse> = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(GoogleAuthRequestDto(id_token = idToken, token = idToken))
        val endpoints = listOf(
            "$baseUrl/api/auth/verify_google_mobile",
            "$baseUrl/api/v1/auth/google",
            "$baseUrl/auth/verify_google_mobile"
        )

        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                val req = Request.Builder()
                    .url(endpoint)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val authRes = json.decodeFromString<VerifyGoogleResponse>(bodyStr)
                        if (authRes.success || !authRes.secret.isNullOrBlank() || !authRes.sessionKey.isNullOrBlank()) {
                            val token = authRes.secret ?: authRes.sessionKey
                            if (!token.isNullOrBlank()) {
                                updateApiKey(token)
                            }
                            return@withContext Result.success(authRes)
                        }
                    } else if (resp.code in 400..499) {
                        return@withContext Result.failure(IOException("Authentication error (${resp.code}): $bodyStr"))
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IOException("Failed to verify Google token with server"))
    }

    /**
     * Fetch OAuth and server configuration dynamically from the backend.
     */
    suspend fun getAuthConfig(): Result<AuthConfigResponse> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "$baseUrl/api/auth/config",
            "$baseUrl/auth/config",
            "$baseUrl/api/v1/auth/config",
            "$baseUrl/hermes/api/auth/config"
        )
        for (endpoint in endpoints) {
            try {
                val req = Request.Builder()
                    .url(endpoint)
                    .get()
                    .header("Accept", "application/json")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful && bodyStr.isNotBlank()) {
                        val cfg = json.decodeFromString<AuthConfigResponse>(bodyStr)
                        return@withContext Result.success(cfg)
                    }
                }
            } catch (_: Exception) {
            }
        }
        Result.failure(IOException("Failed to fetch auth configuration from server"))
    }


    /**
     * Check if currently authenticated with the server.
     */
    suspend fun checkAuthStatus(): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/api/auth/status")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val bodyStr = resp.body?.string() ?: return@withContext false
                val res = json.decodeFromString<AuthStatusResponse>(bodyStr)
                res.logged_in
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ensure active session authentication with backend.
     */
    suspend fun ensureAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        if (checkAuthStatus()) return@withContext true
        login()
    }

    /**
     * Fetch all chat sessions from the server.
     */
    suspend fun getSessions(): List<SessionDto> = withContext(Dispatchers.IO) {
        ensureAuthenticated()

        // Append user_id filter if we have a known google_sub
        val userFilter = if (userId.isNotBlank()) "?user_id=${userId}" else ""
        val urls = listOf(
            "$baseUrl/api/sessions$userFilter",
            "$baseUrl/v1/sessions$userFilter",
            "$baseUrl/sessions$userFilter"
        )
        for (url in urls) {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                if (userId.isNotBlank()) reqBuilder.header("X-User-ID", userId)
                val req = reqBuilder.build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    try {
                        val res = json.decodeFromString<SessionsResponse>(bodyStr)
                        if (res.sessions.isNotEmpty()) return@withContext res.sessions
                    } catch (_: Exception) {}
                    try {
                        val directList = json.decodeFromString<List<SessionDto>>(bodyStr)
                        if (directList.isNotEmpty()) return@withContext directList
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    /**
     * Get details and messages for a specific session.
     */
    suspend fun getSession(sessionId: String): SessionDetailDto? = withContext(Dispatchers.IO) {
        ensureAuthenticated()

        val urls = listOf(
            "$baseUrl/api/session?session_id=$sessionId",
            "$baseUrl/v1/sessions/$sessionId",
            "$baseUrl/sessions/$sessionId",
            "$baseUrl/hermes/v1/sessions/$sessionId"
        )
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    try {
                        val res = json.decodeFromString<SessionDetailResponse>(bodyStr)
                        if (res.session != null) return@withContext res.session
                    } catch (_: Exception) {}
                    try {
                        val direct = json.decodeFromString<SessionDetailDto>(bodyStr)
                        if (direct.session_id.isNotBlank() || direct.messages.isNotEmpty()) return@withContext direct
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Get the real-time execution status of a session (e.g. is background task still generating).
     */
    suspend fun getSessionStatus(sessionId: String): SessionStatusDto? = withContext(Dispatchers.IO) {
        val urls = listOf(
            "$baseUrl/v1/sessions/$sessionId/status",
            "$baseUrl/api/session/status?session_id=$sessionId"
        )
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    try {
                        return@withContext json.decodeFromString<SessionStatusDto>(bodyStr)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Create a new session on the server.
     */
    suspend fun createSession(title: String = "Chat", model: String = "hermes-agent", sessionId: String? = null): SessionDto? = withContext(Dispatchers.IO) {
        if (!checkAuthStatus()) {
            login()
        }

        val reqBody = json.encodeToString(
            NewSessionRequest(
                title = title,
                model = model,
                id = sessionId,
                session_id = sessionId,
                user_id = if (userId.isNotBlank()) userId else null
            )
        )
        val reqBuilder = Request.Builder()
            .url("$baseUrl/api/session/new")
            .post(reqBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
        if (userId.isNotBlank()) {
            reqBuilder.header("X-User-ID", userId)
        }
        val req = reqBuilder.build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                try {
                    val res = json.decodeFromString<NewSessionResponse>(bodyStr)
                    res.session
                } catch (_: Exception) {
                    try {
                        json.decodeFromString<SessionDto>(bodyStr)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Delete a session on the server.
     */
    suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val reqBody = """{"session_id":"$sessionId"}""".toRequestBody(JSON_MEDIA_TYPE)
        val req = Request.Builder()
            .url("$baseUrl/api/session/delete")
            .post(reqBody)
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Rename a session on the server.
     */
    suspend fun renameSession(sessionId: String, title: String): Boolean = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        val escapedTitle = title.replace("\\", "\\\\").replace("\"", "\\\"")
        val reqBody = """{"session_id":"$sessionId","title":"$escapedTitle"}""".toRequestBody(JSON_MEDIA_TYPE)
        val urls = listOf(
            "$baseUrl/api/session/rename",
            "$baseUrl/v1/sessions/$sessionId/rename",
            "$baseUrl/sessions/$sessionId/rename"
        )
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return@withContext true
                }
            } catch (_: Exception) {}
        }
        false
    }

    /**
     * Use a fast mini model to analyze the conversation messages and generate a concise 3-5 word title.
     */
    suspend fun generateChatTitle(userPrompt: String, assistantReply: String): String? = withContext(Dispatchers.IO) {
        val prompt = "Generate a short, concise, high quality title (3 to 5 words maximum) for this conversation. Output ONLY the plain text title without quotes or punctuation.\n\nUser: ${userPrompt.take(250)}\n\nAssistant: ${assistantReply.take(250)}"
        val modelsToTry = listOf(
            "auto/best-fast",
            "antigravity/gemini-2.5-flash",
            "groq/llama-3.3-70b-versatile",
            "claude-3-5-haiku-20241022",
            "auto/smart"
        )

        for (modelName in modelsToTry) {
            val reqBody = ChatCompletionRequest(
                model = modelName,
                messages = listOf(
                    ApiMessage(role = "system", textContent = "You are a succinct title generator. Output only the title, max 5 words, no punctuation, no quotes."),
                    ApiMessage(role = "user", textContent = prompt)
                ),
                stream = false,
                max_tokens = 20
            )

            try {
                val jsonString = json.encodeToString(reqBody)
                val req = Request.Builder()
                    .url("$baseUrl/v1/chat/completions")
                    .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    val parsed = json.decodeFromString<ChatCompletionResponse>(bodyStr)
                    val rawTitle = parsed.choices.firstOrNull()?.message?.content?.trim()
                    if (!rawTitle.isNullOrBlank()) {
                        val cleanTitle = rawTitle.lines().firstOrNull { it.isNotBlank() }
                            ?.replace(Regex("""^["'`*#]+|["'`*#.]+$"""), "")
                            ?.replace(Regex("""^Title:\s*""", RegexOption.IGNORE_CASE), "")
                            ?.trim()
                            ?.take(50)
                        if (!cleanTitle.isNullOrBlank() && cleanTitle.length in 3..55) {
                            return@withContext cleanTitle
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Non-streaming fallback for voice responses or quick queries.
     */
    suspend fun sendChatMessageFallback(
        prompt: String,
        model: String = "hermes-agent"
    ): String = withContext(Dispatchers.IO) {
        val resolvedModel = when (model) {
            "Hermes Smart" -> "hermes-agent"
            "Hermes Coding" -> "auto/best-coding"
            "Hermes Reasoning" -> "auto/best-reasoning"
            "Hermes Turbo" -> "auto/best-coding-fast"
            "hermes-agent" -> "hermes-agent"
            else -> if (model.isBlank()) "hermes-agent" else model
        }
        val reqBody = ChatCompletionRequest(
            model = resolvedModel,
            messages = listOf(
                ApiMessage(
                    role = "system",
                    textContent = "You are Hermes, a helpful, brilliant AI companion. Provide a concise, clear, natural spoken answer in 1-3 sentences without markdown formatting, code blocks, or bullet points."
                ),
                ApiMessage(role = "user", textContent = prompt)
            ),
            stream = false,
            max_tokens = 250
        )

        try {
            val jsonString = json.encodeToString(reqBody)
            val req = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use ""
                val bodyStr = resp.body?.string() ?: return@use ""
                val parsed = json.decodeFromString<ChatCompletionResponse>(bodyStr)
                parsed.choices.firstOrNull()?.message?.content?.trim() ?: ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Fetch available models catalog from the server.
     */
    suspend fun getModels(): List<ModelOptionDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/models")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<ModelsResponse>(bodyStr)
                res.data
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch projects from the server.
     */
    suspend fun getProjects(): List<ProjectDto> = withContext(Dispatchers.IO) {
        if (!checkAuthStatus()) {
            login()
        }

        val req = Request.Builder()
            .url("$baseUrl/api/projects")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<ProjectsResponse>(bodyStr)
                res.projects
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Create a new project on the server.
     */
    suspend fun createProject(name: String, description: String = ""): ProjectDto? = withContext(Dispatchers.IO) {
        val reqBody = json.encodeToString(CreateProjectRequest(name, description))
        val req = Request.Builder()
            .url("$baseUrl/api/projects")
            .post(reqBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<ProjectDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetch all active tasks from the server.
     */
    suspend fun getTasks(): List<TaskDto> = withContext(Dispatchers.IO) {
        if (!checkAuthStatus()) {
            login()
        }

        val req = Request.Builder()
            .url("$baseUrl/api/tasks")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<TaskDto>>(bodyStr)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Create a new task on the server.
     */
    suspend fun createTask(title: String, prompt: String): TaskDto? = withContext(Dispatchers.IO) {
        val reqBody = json.encodeToString(CreateTaskRequest(title, prompt))
        val req = Request.Builder()
            .url("$baseUrl/api/tasks")
            .post(reqBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<TaskDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pause a running task.
     */
    suspend fun pauseTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/tasks/$taskId/pause")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resume a paused task.
     */
    suspend fun resumeTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/tasks/$taskId/resume")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Cancel a task.
     */
    suspend fun cancelTask(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/tasks/$taskId/cancel")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fetch pending approvals.
     */
    suspend fun getApprovals(): List<ApprovalDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/approvals")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<ApprovalsResponse>(bodyStr)
                res.approvals
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Approve an action.
     */
    suspend fun approveRequest(approvalId: String, reason: String = "Approved by Android client"): Boolean = withContext(Dispatchers.IO) {
        val body = """{"reason":"$reason"}""".toRequestBody(JSON_MEDIA_TYPE)
        val req = Request.Builder()
            .url("$baseUrl/v1/approvals/$approvalId/approve")
            .post(body)
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deny an action.
     */
    suspend fun denyRequest(approvalId: String, reason: String = "Denied by Android client"): Boolean = withContext(Dispatchers.IO) {
        val body = """{"reason":"$reason"}""".toRequestBody(JSON_MEDIA_TYPE)
        val req = Request.Builder()
            .url("$baseUrl/v1/approvals/$approvalId/deny")
            .post(body)
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fetch server computer runtime status & storage metrics.
     */
    suspend fun getHostStatus(): HostStatusDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/computer/status")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<HostStatusDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetch knowledge sources and vaults.
     */
    suspend fun getKnowledgeSources(): List<KnowledgeSourceDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/knowledge/sources")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<KnowledgeSourcesResponse>(bodyStr)
                res.sources
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch MCP directory servers.
     */
    suspend fun getDirectoryServers(): List<DirectoryServerItemDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/directory/servers")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<DirectoryServersResponse>(bodyStr)
                if (res.servers.isNotEmpty()) res.servers else res.data
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Unified hybrid search across Notion and Obsidian.
     */
    suspend fun searchKnowledge(query: String, sources: String? = null): List<KnowledgeSearchResultItemDto> = withContext(Dispatchers.IO) {
        val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
        val urlBuilder = "$baseUrl/v1/knowledge/search?q=$encodedQ" + (if (!sources.isNullOrBlank()) "&sources=$sources" else "")
        val req = Request.Builder()
            .url(urlBuilder)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<KnowledgeSearchResponseDto>(bodyStr)
                res.results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Trigger synchronization of Knowledge Space (Notion/Obsidian).
     */
    suspend fun syncKnowledge(connector: String? = null): Boolean = withContext(Dispatchers.IO) {
        val payload = if (connector != null) """{"connector":"$connector"}""" else "{}"
        val req = Request.Builder()
            .url("$baseUrl/v1/knowledge/sync")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Create note or document in Notion or Obsidian.
     */
    suspend fun createKnowledgeNote(title: String, content: String, destination: String = "notion"): Boolean = withContext(Dispatchers.IO) {
        val obj = org.json.JSONObject().apply {
            put("title", title)
            put("content", content)
            put("destination", destination)
        }
        val req = Request.Builder()
            .url("$baseUrl/v1/knowledge/notes")
            .post(obj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * List files in the server computer filesystem.
     */
    suspend fun listComputerFiles(path: String? = null): List<ComputerFileItemDto> = withContext(Dispatchers.IO) {
        val url = if (!path.isNullOrBlank()) "$baseUrl/v1/computer/files?path=${java.net.URLEncoder.encode(path, "UTF-8")}" else "$baseUrl/v1/computer/files"
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<ComputerFilesResponseDto>(bodyStr)
                res.files
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Get content of a file on the server computer.
     */
    suspend fun getComputerFileContent(path: String): String? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/v1/computer/file/content?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                val res = json.decodeFromString<ComputerFileContentResponseDto>(bodyStr)
                res.content
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get browser live status.
     */
    suspend fun getBrowserStatus(): BrowserStatusDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/browser/status")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<BrowserStatusDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Navigate server headless browser.
     */
    suspend fun navigateBrowser(url: String): BrowserNavigateResponseDto? = withContext(Dispatchers.IO) {
        val payload = org.json.JSONObject().apply { put("url", url) }
        val req = Request.Builder()
            .url("$baseUrl/v1/browser/navigate")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<BrowserNavigateResponseDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Capture browser screenshot.
     */
    suspend fun getBrowserScreenshot(): BrowserScreenshotResponseDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/browser/screenshot")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                json.decodeFromString<BrowserScreenshotResponseDto>(bodyStr)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetch multi-agent workforce roles.
     */
    suspend fun getWorkforceRoles(): List<WorkforceRoleDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/workforce/roles")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<WorkforceRolesResponseDto>(bodyStr)
                res.roles
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch scheduled automations (24x7 cron jobs).
     */
    suspend fun getAutomations(): List<ScheduledAutomationDto> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/automations")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<AutomationsResponseDto>(bodyStr)
                res.automations
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Create a scheduled automation.
     */
    suspend fun createAutomation(title: String, prompt: String, cronExpression: String = "0 * * * *"): Boolean = withContext(Dispatchers.IO) {
        val obj = org.json.JSONObject().apply {
            put("title", title)
            put("prompt", prompt)
            put("cron_expression", cronExpression)
        }
        val req = Request.Builder()
            .url("$baseUrl/v1/automations")
            .post(obj.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Toggle automation active/paused state.
     */
    suspend fun toggleAutomation(automationId: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/automations/$automationId/toggle")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Trigger execution of automation now.
     */
    suspend fun runAutomationNow(automationId: String): Boolean = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/v1/automations/$automationId/run_now")
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()
        try {
            okHttpClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Connects to the interactive PTY WebSocket endpoint.
     */
    fun connectPtyWebSocket(listener: WebSocketListener): WebSocket {
        val wsUrl = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://") + "/api/pty"
        } else {
            baseUrl.replace("http://", "ws://") + "/api/pty"
        }
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $apiKey")
            .build()
        return okHttpClient.newWebSocket(request, listener)
    }
    fun streamChat(
        messages: List<ChatMessage>,
        model: String = "hermes-agent",
        sessionId: String? = null
    ): Flow<StreamEvent> = callbackFlow {
        val apiMessages = messages.map { msg ->
            val hasImages = msg.attachments.any { it.isImage && !it.base64Data.isNullOrBlank() }
            val hasFiles = msg.attachments.any { !it.isImage && !it.extractedText.isNullOrBlank() }

            if (hasImages) {
                val contentArray = kotlinx.serialization.json.buildJsonArray {
                    val textBuilder = StringBuilder()
                    if (hasFiles) {
                        msg.attachments.filter { !it.isImage && !it.extractedText.isNullOrBlank() }.forEach { att ->
                            textBuilder.append("[Attached File: ${att.name}]\n${att.extractedText}\n\n")
                        }
                    }
                    val userText = msg.content.ifBlank { "Please inspect and analyze this attached image." }
                    textBuilder.append(userText)

                    add(kotlinx.serialization.json.buildJsonObject {
                        put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                        put("text", kotlinx.serialization.json.JsonPrimitive(textBuilder.toString().trim()))
                    })

                    msg.attachments.filter { it.isImage && !it.base64Data.isNullOrBlank() }.forEach { att ->
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("image_url"))
                            put("image_url", kotlinx.serialization.json.buildJsonObject {
                                put("url", kotlinx.serialization.json.JsonPrimitive("data:${att.mimeType};base64,${att.base64Data}"))
                            })
                        })
                    }
                }
                ApiMessage(role = msg.role, content = contentArray)
            } else if (hasFiles) {
                val textBuilder = StringBuilder()
                msg.attachments.filter { !it.isImage && !it.extractedText.isNullOrBlank() }.forEach { att ->
                    textBuilder.append("[Attached File: ${att.name}]\n${att.extractedText}\n\n")
                }
                textBuilder.append(msg.content)
                ApiMessage(role = msg.role, textContent = textBuilder.toString().trim())
            } else {
                ApiMessage(role = msg.role, textContent = msg.content)
            }
        }
        val requestBody = ChatCompletionRequest(
            model = model,
            messages = apiMessages,
            stream = true,
            sessionId = sessionId
        )

        val jsonString = json.encodeToString(requestBody)
        val reqBuilder = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")

        if (!sessionId.isNullOrBlank()) {
            reqBuilder.header("X-Session-Id", sessionId)
        }

        val httpReq = reqBuilder.build()

        val call = okHttpClient.newCall(httpReq)
        val fullAccumulated = StringBuilder()

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(StreamEvent.Error(e))
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val err = IOException("HTTP ${response.code}: ${response.message}")
                    trySend(StreamEvent.Error(err))
                    close(err)
                    return
                }

                val body = response.body
                if (body == null) {
                    val err = IOException("Empty response body from server")
                    trySend(StreamEvent.Error(err))
                    close(err)
                    return
                }

                try {
                    val source: BufferedSource = body.source()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith(":")) {
                            continue
                        }
                        if (trimmed.startsWith("data:")) {
                            val dataContent = trimmed.substring(5).trim()
                            if (dataContent == "[DONE]") {
                                trySend(StreamEvent.Done(fullAccumulated.toString()))
                                break
                            }
                            try {
                                val chunk = json.decodeFromString<ChatCompletionChunk>(dataContent)
                                val delta = chunk.choices.firstOrNull()?.delta
                                val token = delta?.content ?: delta?.text
                                val reasoning = delta?.reasoning_content ?: delta?.reasoning

                                if (!reasoning.isNullOrEmpty()) {
                                    trySend(StreamEvent.Thinking(reasoning))
                                }
                                if (!token.isNullOrEmpty()) {
                                    fullAccumulated.append(token)
                                    trySend(StreamEvent.Token(token))
                                }
                            } catch (_: Exception) {
                                // Ignore unparseable SSE keep-alive or ping lines
                            }
                        }
                    }
                    trySend(StreamEvent.Done(fullAccumulated.toString()))
                    close()
                } catch (e: Exception) {
                    trySend(StreamEvent.Error(e))
                    close(e)
                } finally {
                    body.close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Connects to the Hugging Voice Realtime WebSocket endpoint.
     */
    fun connectHuggingVoiceWebSocket(
        listener: WebSocketListener,
        voice: String? = null,
        sessionId: String? = null
    ): WebSocket {
        val baseWs = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://") + "/hugging-voice/ws"
        } else {
            baseUrl.replace("http://", "ws://") + "/hugging-voice/ws"
        }

        val queryParams = mutableListOf<String>()
        if (!voice.isNullOrBlank()) {
            queryParams.add("voice=" + java.net.URLEncoder.encode(voice, "UTF-8"))
        }
        if (!sessionId.isNullOrBlank()) {
            queryParams.add("session_id=" + java.net.URLEncoder.encode(sessionId, "UTF-8"))
        }

        val url = if (queryParams.isNotEmpty()) {
            "$baseWs?" + queryParams.joinToString("&")
        } else {
            baseWs
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .build()

        return okHttpClient.newWebSocket(request, listener)
    }

    /**
     * Backward-compatibility alias for Apollo / OpenAI Realtime WebSocket connection.
     */
    fun connectApolloRealtimeWebSocket(
        listener: WebSocketListener,
        voice: String? = null,
        sessionId: String? = null
    ): WebSocket = connectHuggingVoiceWebSocket(listener, voice, sessionId)

    /**
     * Execute autonomous action or background task via Hermes voice execution layer.
     */
    suspend fun executeVoiceTask(
        objective: String,
        taskType: String = "general",
        background: Boolean = false,
        chatId: String = "hugging_voice"
    ): Result<VoiceTaskExecutionResult> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = org.json.JSONObject().apply {
                put("objective", objective)
                put("task_type", taskType)
                put("background", background)
                put("chat_id", chatId)
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/voice/hermes-execute")
                .post(body)
                .header("Authorization", "Bearer $apiKey")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            val respStr = resp.body?.string() ?: "{}"
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${resp.code}: $respStr"))
            }

            val json = org.json.JSONObject(respStr)
            val resultObj = VoiceTaskExecutionResult(
                status = json.optString("status", "completed"),
                result = json.optString("result").takeIf { it.isNotEmpty() },
                summary = json.optString("summary").takeIf { it.isNotEmpty() },
                taskId = json.optString("task_id").takeIf { it.isNotEmpty() },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
            Result.success(resultObj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connects to the full-duplex Voice WebSocket endpoint.
     */
    fun connectVoiceWebSocket(
        listener: WebSocketListener,
        persona: String? = null,
        language: String? = null,
        pace: String? = null
    ): WebSocket {
        val baseWs = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://") + "/v1/voice/ws"
        } else {
            baseUrl.replace("http://", "ws://") + "/v1/voice/ws"
        }

        val urlBuilder = StringBuilder(baseWs)
        val params = mutableListOf<String>()
        if (!persona.isNullOrBlank()) params.add("persona=" + java.net.URLEncoder.encode(persona, "UTF-8"))
        if (!language.isNullOrBlank()) params.add("language=" + java.net.URLEncoder.encode(language, "UTF-8"))
        if (!pace.isNullOrBlank()) params.add("pace=" + java.net.URLEncoder.encode(pace, "UTF-8"))
        if (params.isNotEmpty()) {
            urlBuilder.append("?").append(params.joinToString("&"))
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .header("Authorization", "Bearer $apiKey")
            .build()

        return okHttpClient.newWebSocket(request, listener)
    }

    /**
     * Get channels configuration and status.
     */
    suspend fun getChannels(): Result<ChannelsConfigDto> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/v1/channels")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<ChannelsConfigDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update channels configuration.
     */
    suspend fun updateChannels(request: UpdateChannelsRequestDto): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = json.encodeToString(request)
            val req = Request.Builder()
                .url("$baseUrl/v1/channels")
                .post(jsonStr.toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Test channel message delivery.
     */
    suspend fun testChannel(channel: String, message: String): Result<TestChannelResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = json.encodeToString(TestChannelRequestDto(channel = channel, message = message))
            val req = Request.Builder()
                .url("$baseUrl/v1/channels/test")
                .post(jsonStr.toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(bodyStr.ifBlank { "HTTP ${resp.code}" }))
                val res = json.decodeFromString<TestChannelResponseDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get OmniRoute routing telemetry and traces.
     */
    suspend fun getOmniRouteTelemetry(): Result<OmniRouteTelemetryDto> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/v1/omniroute/telemetry")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<OmniRouteTelemetryDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Securely store service credentials into the encrypted vault on backend.
     */
    suspend fun saveIntegrationCredentials(service: String, credentials: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonObject = org.json.JSONObject().apply {
                put("service", service)
                val cObj = org.json.JSONObject()
                credentials.forEach { (k, v) -> cObj.put(k, v) }
                put("credentials", cObj)
            }
            val req = Request.Builder()
                .url("$baseUrl/api/vault/credentials")
                .post(jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fetch skills catalog from backend.
     */
    suspend fun getSkills(sessionId: String = "global"): Result<List<SkillItemDto>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/skills?session_id=$sessionId")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<SkillsResponseDto>(bodyStr)
                Result.success(res.skills)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activate or deactivate a skill on backend.
     */
    suspend fun toggleSkill(skillName: String, active: Boolean, sessionId: String = "global"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(SkillToggleRequestDto(skill_name = skillName, active = active, session_id = sessionId))
            val req = Request.Builder()
                .url("$baseUrl/api/v1/skills/activate")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                Result.success(resp.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch workforce agent roles and capabilities from backend.
     */
    suspend fun getAgents(): Result<List<AgentRoleDto>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/agents")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<AgentsResponseDto>(bodyStr)
                Result.success(res.agents)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch knowledge summary metrics and connected sources.
     */
    suspend fun getKnowledgeSummary(): Result<KnowledgeSummaryResponseDto> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/knowledge/summary")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<KnowledgeSummaryResponseDto>(bodyStr)
                Result.success(res)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch system and task execution activity timeline.
     */
    suspend fun getActivity(limit: Int = 40): Result<List<ActivityEventDto>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/activity?limit=$limit")
                .get()
                .header("Authorization", "Bearer $apiKey")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                val bodyStr = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<ActivityResponseDto>(bodyStr)
                Result.success(res.events)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class VoiceTaskExecutionResult(
    val status: String,
    val result: String? = null,
    val summary: String? = null,
    val taskId: String? = null,
    val error: String? = null
)


