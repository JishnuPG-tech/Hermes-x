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
     * Fetch all chat sessions from the server.
     */
    suspend fun getSessions(): List<SessionDto> = withContext(Dispatchers.IO) {
        // Auto-login if session expired
        if (!checkAuthStatus()) {
            login()
        }

        val req = Request.Builder()
            .url("$baseUrl/api/sessions")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val bodyStr = resp.body?.string() ?: return@withContext emptyList()
                val res = json.decodeFromString<SessionsResponse>(bodyStr)
                res.sessions
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Get details and messages for a specific session.
     */
    suspend fun getSession(sessionId: String): SessionDetailDto? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/api/session?session_id=$sessionId")
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()

        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bodyStr = resp.body?.string() ?: return@withContext null
                val res = json.decodeFromString<SessionDetailResponse>(bodyStr)
                res.session
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Create a new session on the server.
     */
    suspend fun createSession(title: String = "Chat", model: String = "hermes-agent"): SessionDto? = withContext(Dispatchers.IO) {
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
                json.decodeFromString<SessionDto>(bodyStr)
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
     * Streams chat completions via Server-Sent Events (SSE) from the backend.
     */
    fun streamChat(
        messages: List<ChatMessage>,
        model: String = "hermes-agent"
    ): Flow<StreamEvent> = callbackFlow {
        val apiMessages = messages.map {
            ApiMessage(role = it.role, content = it.content)
        }
        val requestBody = ChatCompletionRequest(
            model = model,
            messages = apiMessages,
            stream = true
        )

        val jsonString = json.encodeToString(requestBody)
        val httpReq = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .build()

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
