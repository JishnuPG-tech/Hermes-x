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
        const val DEFAULT_API_KEY = "Jishnu2005"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        val instance: HermesApiClient by lazy { HermesApiClient() }
    }

    private val cookieJar = InMemoryCookieJar()

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl.trimEnd('/')
    }

    fun updateApiKey(newKey: String) {
        apiKey = newKey
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

        val urls = listOf(
            "$baseUrl/api/sessions",
            "$baseUrl/v1/sessions",
            "$baseUrl/sessions"
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
    suspend fun createSession(title: String = "Chat", model: String = "hermes-agent"): SessionDto? = withContext(Dispatchers.IO) {
        if (!checkAuthStatus()) {
            login()
        }

        val reqBody = json.encodeToString(NewSessionRequest(title = title, model = model))
        val req = Request.Builder()
            .url("$baseUrl/api/session/new")
            .post(reqBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .build()

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
                    ApiMessage(role = "system", content = "You are a succinct title generator. Output only the title, max 5 words, no punctuation, no quotes."),
                    ApiMessage(role = "user", content = prompt)
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
        val apiMessages = messages.map {
            ApiMessage(role = it.role, content = it.content)
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
                                val token = delta?.content
                                val reasoning = delta?.reasoning_content

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
     * Connects to the full-duplex Voice WebSocket endpoint.
     */
    fun connectVoiceWebSocket(listener: WebSocketListener): WebSocket {
        val wsUrl = if (baseUrl.startsWith("https://")) {
            baseUrl.replace("https://", "wss://") + "/v1/voice/ws"
        } else {
            baseUrl.replace("http://", "ws://") + "/v1/voice/ws"
        }

        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $apiKey")
            .build()

        return okHttpClient.newWebSocket(request, listener)
    }
}
