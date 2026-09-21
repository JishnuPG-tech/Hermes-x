package com.example.hermes.data

import android.content.Context
import com.example.hermes.data.local.*
import com.example.hermes.stream.StreamSmoothingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID

interface DataRepository {
    val messages: StateFlow<List<ChatMessage>>
    val isStreaming: StateFlow<Boolean>
    val activeThinking: StateFlow<String?>
    val thinkingPhase: StateFlow<ThinkingPhase>
    val tasks: StateFlow<List<TaskDto>>
    val sessions: StateFlow<List<SessionDto>>
    val currentSessionId: StateFlow<String?>
    val projects: StateFlow<List<ProjectDto>>
    val availableModels: StateFlow<List<ModelOptionDto>>
    val allArtifacts: StateFlow<List<ArtifactItemDto>>
    val approvals: StateFlow<List<ApprovalDto>>
    val hostStatus: StateFlow<HostStatusDto?>
    val knowledgeSources: StateFlow<List<KnowledgeSourceDto>>
    val directoryServers: StateFlow<List<DirectoryServerItemDto>>
    val terminalLogs: StateFlow<List<String>>

    fun sendMessage(
        content: String,
        model: String = "hermes-agent",
        attachments: List<ChatAttachment> = emptyList(),
        webSearch: Boolean = true,
        memory: Boolean = true
    )
    fun stopGeneration()
    fun fetchTasks()
    fun createNewTask(title: String, prompt: String)
    fun pauseTask(taskId: String)
    fun resumeTask(taskId: String)
    fun cancelTask(taskId: String)
    fun fetchSessions()
    fun loadSession(sessionId: String)
    fun syncActiveSession()
    fun deleteSession(sessionId: String)
    fun updateSessionTitle(sessionId: String, newTitle: String)
    fun fetchProjects()
    fun createNewProject(name: String, description: String = "")
    fun fetchModels()
    fun clearMessages()
    fun fetchApprovals()
    fun approveRequest(approvalId: String)
    fun denyRequest(approvalId: String)
    fun fetchHostStatus()
    fun fetchKnowledgeSources()
    fun fetchDirectoryServers()
    fun connectTerminalPty()
    fun sendTerminalInput(input: String)
    fun disconnectTerminalPty()
    suspend fun searchKnowledge(query: String, sources: String? = null): List<KnowledgeSearchResultItemDto>
    suspend fun syncKnowledge(connector: String? = null): Boolean
    suspend fun createKnowledgeNote(title: String, content: String, destination: String = "notion"): Boolean
    suspend fun listComputerFiles(path: String? = null): List<ComputerFileItemDto>
    suspend fun getComputerFileContent(path: String): String?
    suspend fun getBrowserStatus(): BrowserStatusDto?
    suspend fun navigateBrowser(url: String): BrowserNavigateResponseDto?
    suspend fun getBrowserScreenshot(): BrowserScreenshotResponseDto?
    suspend fun getWorkforceRoles(): List<WorkforceRoleDto>
    suspend fun getAutomations(): List<ScheduledAutomationDto>
    suspend fun createAutomation(title: String, prompt: String, cronExpression: String = "0 * * * *"): Boolean
    suspend fun toggleAutomation(automationId: String): Boolean
    suspend fun runAutomationNow(automationId: String): Boolean
    suspend fun getChannels(): Result<ChannelsConfigDto>
    suspend fun updateChannels(request: UpdateChannelsRequestDto): Result<Boolean>
    suspend fun testChannel(channel: String, message: String): Result<TestChannelResponseDto>
    suspend fun getOmniRouteTelemetry(): Result<OmniRouteTelemetryDto>
    suspend fun searchMessagesFts(query: String): List<FtsSearchResultDto>

    // Auth and Server Preferences
    val isLoggedIn: kotlinx.coroutines.flow.Flow<Boolean>
    val currentUserId: kotlinx.coroutines.flow.Flow<String>
    suspend fun logout()
    suspend fun loginWithGoogle(idToken: String, displayName: String, email: String, avatar: String): Result<VerifyGoogleResponse>
    suspend fun refreshAuthConfig(): Result<AuthConfigResponse>
    val googleClientId: kotlinx.coroutines.flow.Flow<String>
    val currentServerUrl: kotlinx.coroutines.flow.Flow<String>
    suspend fun setGoogleClientId(clientId: String)
    suspend fun setServerBaseUrl(url: String)

    // DataStore Preferences access via Repository
    fun getPreferencesManager(): PreferencesManager?
}

enum class ThinkingPhase {
    IDLE,
    THOUGHT_PROCESS,
    BUILDING,
    CREATING_FILE,
    FINALIZING,
    COMPLETED
}

fun ThinkingPhase.toDisplayString(): String = when (this) {
    ThinkingPhase.IDLE -> "Thought process"
    ThinkingPhase.THOUGHT_PROCESS -> "Thought process"
    ThinkingPhase.BUILDING -> "Building…"
    ThinkingPhase.CREATING_FILE -> "Creating file…"
    ThinkingPhase.FINALIZING -> "Finalizing…"
    ThinkingPhase.COMPLETED -> "Thought process"
}

class HermesDataRepository(
    private val apiClient: HermesApiClient = HermesApiClient.instance
) : DataRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var smoother: StreamSmoothingEngine? = null
    private var activeStreamJob: kotlinx.coroutines.Job? = null
    private var roomDb: HermesRoomDatabase? = null
    @Volatile
    private var activeUserId: String = "guest"
    private var prefsManager: PreferencesManager? = null
    private var ptyWebSocket: WebSocket? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _activeThinking = MutableStateFlow<String?>(null)
    override val activeThinking: StateFlow<String?> = _activeThinking.asStateFlow()

    private val _thinkingPhase = MutableStateFlow(ThinkingPhase.IDLE)
    override val thinkingPhase: StateFlow<ThinkingPhase> = _thinkingPhase.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    override val tasks: StateFlow<List<TaskDto>> = _tasks.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionDto>>(emptyList())
    override val sessions: StateFlow<List<SessionDto>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    override val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _projects = MutableStateFlow<List<ProjectDto>>(emptyList())
    override val projects: StateFlow<List<ProjectDto>> = _projects.asStateFlow()

    private val _availableModels = MutableStateFlow<List<ModelOptionDto>>(DEFAULT_MODELS)
    override val availableModels: StateFlow<List<ModelOptionDto>> = _availableModels.asStateFlow()

    private val _allArtifacts = MutableStateFlow<List<ArtifactItemDto>>(emptyList())
    override val allArtifacts: StateFlow<List<ArtifactItemDto>> = _allArtifacts.asStateFlow()

    private val _approvals = MutableStateFlow<List<ApprovalDto>>(emptyList())
    override val approvals: StateFlow<List<ApprovalDto>> = _approvals.asStateFlow()

    private val _hostStatus = MutableStateFlow<HostStatusDto?>(null)
    override val hostStatus: StateFlow<HostStatusDto?> = _hostStatus.asStateFlow()

    private val _knowledgeSources = MutableStateFlow<List<KnowledgeSourceDto>>(emptyList())
    override val knowledgeSources: StateFlow<List<KnowledgeSourceDto>> = _knowledgeSources.asStateFlow()

    private val _directoryServers = MutableStateFlow<List<DirectoryServerItemDto>>(emptyList())
    override val directoryServers: StateFlow<List<DirectoryServerItemDto>> = _directoryServers.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    override val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    companion object {
        val DEFAULT_MODELS = listOf(
            ModelOptionDto(
                id = "auto/best-chat",
                name = "Hermes Smart",
                display_name = "Hermes Smart",
                short_name = "Smart",
                description = ModelDescriptionDto("General intelligence, dialogue & creative agent workflows.")
            ),
            ModelOptionDto(
                id = "auto/best-coding",
                name = "Hermes Coding",
                display_name = "Hermes Coding",
                short_name = "Coding",
                description = ModelDescriptionDto("High precision code synthesis, refactoring & review.")
            ),
            ModelOptionDto(
                id = "auto/best-reasoning",
                name = "Hermes Reasoning",
                display_name = "Hermes Reasoning",
                short_name = "Reasoning",
                description = ModelDescriptionDto("Deep reasoning, complex logic & multi-step planning.")
            ),
            ModelOptionDto(
                id = "auto/best-coding-fast",
                name = "Hermes Turbo",
                display_name = "Hermes Turbo",
                short_name = "Turbo",
                description = ModelDescriptionDto("Ultra-low latency inference for rapid prototyping & quick edits.")
            )
        )

        val instance: HermesDataRepository by lazy { HermesDataRepository() }

        private val CODE_BLOCK_REGEX = Regex("```([a-zA-Z0-9_-]+)?\\s*\\n([\\s\\S]*?)(?:```|$)")
        private val ANT_ARTIFACT_REGEX = Regex("<antArtifact\\s+([^>]+)>([\\s\\S]*?)(?:</antArtifact>|$)", RegexOption.IGNORE_CASE)

        fun initialize(context: Context) {
            instance.setContext(context.applicationContext)
        }

        fun syncActiveSession() {
            instance.syncActiveSession()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun setContext(context: Context) {
        val db = HermesRoomDatabase.getInstance(context)
        val prefs = PreferencesManager.getInstance(context)
        roomDb = db
        prefsManager = prefs

        // Listen for active user ID change to partition local Room queries
        repositoryScope.launch(Dispatchers.IO) {
            prefs.googleSub.collect { sub ->
                if (sub.isNotBlank()) {
                    apiClient.updateUserId(sub)
                }
            }
        }

        repositoryScope.launch(Dispatchers.IO) {
            prefs.currentUserId.collect { userId ->
                activeUserId = userId
                val cachedSessions = db.sessionDao().getSessionsList(userId)
                if (cachedSessions.isNotEmpty()) {
                    val localDtos = cachedSessions.map {
                        SessionDto(
                            session_id = it.id,
                            title = it.title,
                            model = it.model,
                            updated_at = it.updatedAt / 1000.0
                        )
                    }
                    val localIds = localDtos.map { it.session_id }.toSet()
                    val remoteOnly = _sessions.value.filter { it.session_id !in localIds }
                    _sessions.value = (localDtos + remoteOnly).sortedByDescending { it.updated_at }
                }
            }
        }

        // Load cached entities from Room reactively
        repositoryScope.launch(Dispatchers.IO) {
            prefs.currentUserId.flatMapLatest { userId ->
                db.sessionDao().getSessions(userId)
            }.collect { cachedSessions ->
                val localDtos = cachedSessions.map {
                    SessionDto(
                        session_id = it.id,
                        title = it.title,
                        model = it.model,
                        updated_at = it.updatedAt / 1000.0
                    )
                }
                val localIds = localDtos.map { it.session_id }.toSet()
                val remoteOnly = _sessions.value.filter { it.session_id !in localIds }
                _sessions.value = (localDtos + remoteOnly).sortedByDescending { it.updated_at }
            }
        }

        repositoryScope.launch(Dispatchers.IO) {
            prefs.currentUserId.flatMapLatest { userId ->
                db.taskDao().getTasks(userId)
            }.collect { cachedTasks ->
                if (_tasks.value.isEmpty() && cachedTasks.isNotEmpty()) {
                    _tasks.value = cachedTasks.map {
                        TaskDto(
                            id = it.id,
                            title = it.title,
                            prompt = it.prompt,
                            status = it.status,
                            progress = it.progress,
                            agent_name = it.agentName
                        )
                    }
                }
            }
        }

        repositoryScope.launch(Dispatchers.IO) {
            prefs.currentUserId.flatMapLatest { userId ->
                db.projectDao().getProjects(userId)
            }.collect { cachedProjects ->
                if (_projects.value.isEmpty() && cachedProjects.isNotEmpty()) {
                    _projects.value = cachedProjects.map {
                        ProjectDto(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            created_at = it.createdAt.toDouble()
                        )
                    }
                }
            }
        }

        repositoryScope.launch(Dispatchers.IO) {
            prefs.currentUserId.flatMapLatest { userId ->
                db.artifactDao().getArtifacts(userId)
            }.collect { cachedArtifacts ->
                if (_allArtifacts.value.isEmpty() && cachedArtifacts.isNotEmpty()) {
                    _allArtifacts.value = cachedArtifacts.map {
                        ArtifactItemDto(
                            id = it.id,
                            title = it.title,
                            type = it.type,
                            language = it.language,
                            code = it.code,
                            timestamp = it.createdAt
                        )
                    }
                }
            }
        }
    }

    override fun getPreferencesManager(): PreferencesManager? = prefsManager

    override val isLoggedIn: kotlinx.coroutines.flow.Flow<Boolean>
        get() = prefsManager?.isLoggedIn ?: kotlinx.coroutines.flow.flowOf(false)

    override val currentUserId: kotlinx.coroutines.flow.Flow<String>
        get() = prefsManager?.currentUserId ?: kotlinx.coroutines.flow.flowOf("guest")

    override val googleClientId: kotlinx.coroutines.flow.Flow<String>
        get() = prefsManager?.googleClientId ?: kotlinx.coroutines.flow.flowOf(PreferencesManager.DEFAULT_GOOGLE_CLIENT_ID)

    override val currentServerUrl: kotlinx.coroutines.flow.Flow<String>
        get() = prefsManager?.connectionBaseUrl ?: kotlinx.coroutines.flow.flowOf(HermesApiClient.DEFAULT_BASE_URL)

    override suspend fun setGoogleClientId(clientId: String) {
        prefsManager?.setGoogleClientId(clientId)
    }

    override suspend fun setServerBaseUrl(url: String) {
        prefsManager?.setConnectionBaseUrl(url)
        apiClient.updateBaseUrl(url)
    }

    override suspend fun logout() {
        stopGeneration()
        prefsManager?.clearAuth()
        activeUserId = "guest"
        apiClient.updateUserId("")
        _messages.value = emptyList()
        _sessions.value = emptyList()
        _tasks.value = emptyList()
        _projects.value = emptyList()
        _allArtifacts.value = emptyList()
        _currentSessionId.value = null
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        displayName: String,
        email: String,
        avatar: String
    ): Result<VerifyGoogleResponse> = withContext(Dispatchers.IO) {
        val name = displayName.ifBlank { email.substringBefore('@').replaceFirstChar { it.uppercase() }.ifBlank { "User" } }

        try {
            // Strictly verify against backend OAuth validator before creating local authenticated state
            val result = apiClient.verifyGoogleToken(idToken)
            if (result.isSuccess) {
                val data = result.getOrNull()
                val serverToken = data?.secret ?: data?.sessionKey ?: ""
                val verifiedEmail = data?.account?.email_address?.ifBlank { email } ?: email
                val verifiedName = data?.account?.display_name?.ifBlank { name } ?: name
                // google_sub is the permanent unique ID — use it as the cloud sync key
                val googleSub = data?.google_sub?.ifBlank { verifiedEmail } ?: verifiedEmail

                // 1. Partition Room queries immediately by verified email
                activeUserId = verifiedEmail

                // 2. Persist verified profile, server auth token, and cloud sync sub
                prefsManager?.setUserProfile(verifiedName, verifiedEmail, avatar, googleSub)
                if (serverToken.isNotBlank()) {
                    prefsManager?.setAuthToken(serverToken)
                }
                prefsManager?.setGoogleSub(googleSub)

                // 3. Wire google_sub into API client for X-User-ID header
                apiClient.updateUserId(googleSub)

                // 4. Trigger initial sync for authenticated account
                repositoryScope.launch {
                    fetchSessions()
                    fetchProjects()
                    fetchTasks()
                    fetchModels()
                }

                Result.success(
                    data ?: VerifyGoogleResponse(
                        success = true,
                        secret = serverToken,
                        account = GoogleAccountDto(
                            email_address = verifiedEmail,
                            full_name = verifiedName,
                            display_name = verifiedName
                        )
                    )
                )
            } else {
                val err = result.exceptionOrNull() ?: Exception("Google OAuth verification rejected by Hermes Gateway")
                Result.failure(err)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshAuthConfig(): Result<AuthConfigResponse> {
        val result = apiClient.getAuthConfig()
        if (result.isSuccess) {
            val cfg = result.getOrNull()
            val clientId = cfg?.google_client_id
            if (!clientId.isNullOrBlank()) {
                prefsManager?.setGoogleClientId(clientId)
            }
        }
        return result
    }

    init {
        // Light async refresh in background without flooding network on startup
        repositoryScope.launch(Dispatchers.IO) {
            refreshAuthConfig()
        }
    }

    override fun stopGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        _isStreaming.value = false
        _activeThinking.value = null
        _thinkingPhase.value = ThinkingPhase.IDLE

        val currentMessages = _messages.value
        if (currentMessages.isNotEmpty() && currentMessages.last().isStreaming) {
            val lastMsg = currentMessages.last()
            _messages.value = currentMessages.dropLast(1) + lastMsg.copy(isStreaming = false)
        }
    }

    override fun sendMessage(
        content: String,
        model: String,
        attachments: List<ChatAttachment>,
        webSearch: Boolean,
        memory: Boolean
    ) {
        stopGeneration()

        val resolvedModel = when (model) {
            "Hermes Smart" -> "hermes-agent"
            "Hermes Coding" -> "auto/best-coding"
            "Hermes Reasoning" -> "auto/best-reasoning"
            "Hermes Turbo" -> "auto/best-coding-fast"
            "hermes-agent" -> "hermes-agent"
            else -> if (model.isBlank()) "hermes-agent" else model
        }

        val effectiveContent = if (attachments.isNotEmpty()) {
            val attInfo = attachments.joinToString("\n") { att ->
                if (att.isImage) "[Attached Image: ${att.name}]" else "[Attached File: ${att.name}]"
            }
            if (content.isNotBlank()) "$attInfo\n\n$content" else attInfo
        } else {
            content
        }

        // Ensure session exists or create a new session
        val isNewChat = _currentSessionId.value == null
        val activeSessionId = _currentSessionId.value ?: ("sess_" + UUID.randomUUID().toString().replace("-", "").take(16))
        _currentSessionId.value = activeSessionId

        val autoTitle = if (content.isNotBlank()) {
            val firstLine = content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            val clean = firstLine.replace(Regex("""^[#*>\s\-]+"""), "").replace(Regex("""\s+"""), " ")
            val words = clean.split(" ")
            val titleCandidate = if (words.size > 5) {
                words.take(5).joinToString(" ")
            } else {
                clean.take(35)
            }
            titleCandidate.ifBlank { "New chat" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            attachments.firstOrNull()?.name?.take(35) ?: "New chat"
        }

        val chatTitle = if (isNewChat) {
            autoTitle
        } else {
            val existing = _sessions.value.firstOrNull { it.session_id == activeSessionId }?.title
            if (existing.isNullOrBlank() || existing == "Chat" || existing == "New chat") {
                updateSessionTitle(activeSessionId, autoTitle)
                autoTitle
            } else {
                existing
            }
        }

        if (isNewChat) {
            val nowSec = System.currentTimeMillis() / 1000.0
            val newSessionDto = SessionDto(
                session_id = activeSessionId,
                title = chatTitle,
                model = resolvedModel,
                created_at = nowSec,
                updated_at = nowSec,
                message_count = 1
            )
            _sessions.value = listOf(newSessionDto) + _sessions.value.filter { it.session_id != activeSessionId }

            // Persist immediately in Room database
            roomDb?.let { db ->
                repositoryScope.launch(Dispatchers.IO) {
                    db.sessionDao().upsertSessions(listOf(SessionEntity(
                        id = activeSessionId,
                        title = chatTitle,
                        model = resolvedModel,
                        updatedAt = System.currentTimeMillis(),
                        userId = activeUserId
                    )))
                }
            }

            // Sync with backend asynchronously
            repositoryScope.launch(Dispatchers.IO) {
                apiClient.createSession(title = chatTitle, model = resolvedModel, sessionId = activeSessionId)
            }
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = effectiveContent,
            attachments = attachments
        )

        val assistantMsgId = UUID.randomUUID().toString()
        val initialAssistantMsg = ChatMessage(
            id = assistantMsgId,
            role = "assistant",
            content = "",
            isStreaming = true,
            stepTitle = "Thought process"
        )

        val updatedList = _messages.value + userMsg + initialAssistantMsg
        _messages.value = updatedList
        _isStreaming.value = true
        _activeThinking.value = "Thought process"
        _thinkingPhase.value = ThinkingPhase.THOUGHT_PROCESS

        val currentSession = _currentSessionId.value ?: activeSessionId
        roomDb?.let { db ->
            repositoryScope.launch(Dispatchers.IO) {
                db.messageDao().insertMessages(listOf(
                    MessageEntity(
                        id = userMsg.id,
                        sessionId = currentSession,
                        role = userMsg.role,
                        content = userMsg.content,
                        timestamp = userMsg.timestamp,
                        userId = activeUserId
                    ),
                    MessageEntity(
                        id = assistantMsgId,
                        sessionId = currentSession,
                        role = "assistant",
                        content = "",
                        thinking = null,
                        timestamp = System.currentTimeMillis(),
                        isStreaming = true,
                        stepTitle = "Thought process",
                        userId = activeUserId
                    )
                ))
            }
        }

        val newSmoother = StreamSmoothingEngine(repositoryScope)
        smoother = newSmoother
        val thoughtsAccumulator = StringBuilder()

        // Smoother tick collector
        repositoryScope.launch {
            newSmoother.renderedText.collect { smoothContent ->
                if (_isStreaming.value) {
                    val artifactMeta = extractArtifactFromText(smoothContent)
                    updateAssistantMessage(
                        id = assistantMsgId,
                        content = smoothContent,
                        thinking = thoughtsAccumulator.toString(),
                        isStreaming = true,
                        artifactTitle = artifactMeta?.title,
                        artifactType = artifactMeta?.type,
                        artifactLanguage = artifactMeta?.language,
                        artifactCode = artifactMeta?.code,
                        stepTitle = artifactMeta?.stepTitle ?: if (thoughtsAccumulator.isNotBlank()) "Thought process" else null
                    )
                }
            }
        }

        // Live SSE stream collector
        activeStreamJob = repositoryScope.launch {
            try {
                apiClient.streamChat(updatedList.dropLast(1), resolvedModel, sessionId = currentSession).collect { event ->
                    when (event) {
                        is StreamEvent.Thinking -> {
                            thoughtsAccumulator.append(event.text)
                            val latestThought = thoughtsAccumulator.toString().lines().lastOrNull { it.isNotBlank() } ?: "Thought process"
                            _activeThinking.value = latestThought
                            _thinkingPhase.value = ThinkingPhase.THOUGHT_PROCESS

                            val currentText = newSmoother.renderedText.value
                            val artifactMeta = extractArtifactFromText(currentText)
                            updateAssistantMessage(
                                id = assistantMsgId,
                                content = currentText,
                                thinking = thoughtsAccumulator.toString(),
                                isStreaming = true,
                                artifactTitle = artifactMeta?.title,
                                artifactType = artifactMeta?.type,
                                artifactLanguage = artifactMeta?.language,
                                artifactCode = artifactMeta?.code,
                                stepTitle = artifactMeta?.stepTitle ?: "Thought process"
                            )
                        }
                        is StreamEvent.Token -> {
                            _thinkingPhase.value = ThinkingPhase.COMPLETED
                            newSmoother.appendToken(event.text)
                        }
                        is StreamEvent.Done -> {
                            _thinkingPhase.value = ThinkingPhase.COMPLETED
                            val rawResponse = event.fullResponse
                            val effectiveResponse = if (rawResponse.isNotBlank()) {
                                rawResponse
                            } else if (newSmoother.renderedText.value.isNotBlank()) {
                                newSmoother.renderedText.value
                            } else if (thoughtsAccumulator.isNotBlank()) {
                                thoughtsAccumulator.toString()
                            } else {
                                "Hermes has completed processing your request."
                            }

                            newSmoother.complete(effectiveResponse)
                            val artifactMeta = extractArtifactFromText(effectiveResponse)

                            if (artifactMeta != null) {
                                val newArtifact = ArtifactItemDto(
                                    title = artifactMeta.title,
                                    type = artifactMeta.type,
                                    language = artifactMeta.language,
                                    code = artifactMeta.code
                                )
                                _allArtifacts.value = listOf(newArtifact) + _allArtifacts.value.filter { it.title != newArtifact.title }
                                roomDb?.let { db ->
                                    repositoryScope.launch(Dispatchers.IO) {
                                        db.artifactDao().upsertArtifacts(listOf(ArtifactEntity(
                                            id = newArtifact.id,
                                            title = newArtifact.title,
                                            type = newArtifact.type,
                                            language = newArtifact.language ?: "",
                                            code = newArtifact.code ?: "",
                                            userId = activeUserId
                                        )))
                                    }
                                }
                            }

                            updateAssistantMessage(
                                id = assistantMsgId,
                                content = effectiveResponse,
                                thinking = thoughtsAccumulator.toString(),
                                isStreaming = false,
                                artifactTitle = artifactMeta?.title,
                                artifactType = artifactMeta?.type,
                                artifactLanguage = artifactMeta?.language,
                                artifactCode = artifactMeta?.code,
                                stepTitle = artifactMeta?.stepTitle
                            )

                            if (rawResponse.isBlank()) {
                                val curSess = currentSession
                                repositoryScope.launch(Dispatchers.IO) {
                                    try {
                                        val sessionDto = apiClient.getSession(curSess)
                                        val lastAsst = sessionDto?.messages?.lastOrNull { it.role == "assistant" }
                                        if (lastAsst != null && lastAsst.content.isNotBlank()) {
                                            updateAssistantMessage(
                                                id = assistantMsgId,
                                                content = lastAsst.content,
                                                thinking = lastAsst.reasoning_content ?: thoughtsAccumulator.toString(),
                                                isStreaming = false
                                            )
                                        }
                                    } catch (_: Exception) {}
                                }
                            }

                            // Save message exchange to local Room cache
                            val currentSession = _currentSessionId.value ?: activeSessionId
                            roomDb?.let { db ->
                                repositoryScope.launch(Dispatchers.IO) {
                                    db.messageDao().insertMessages(listOf(
                                        MessageEntity(
                                            id = userMsg.id,
                                            sessionId = currentSession,
                                            role = userMsg.role,
                                            content = userMsg.content,
                                            timestamp = userMsg.timestamp,
                                            userId = activeUserId
                                        ),
                                        MessageEntity(
                                            id = assistantMsgId,
                                            sessionId = currentSession,
                                            role = "assistant",
                                            content = event.fullResponse,
                                            thinking = thoughtsAccumulator.toString(),
                                            timestamp = System.currentTimeMillis(),
                                            isStreaming = false,
                                            artifactTitle = artifactMeta?.title,
                                            artifactType = artifactMeta?.type,
                                            artifactLanguage = artifactMeta?.language,
                                            artifactCode = artifactMeta?.code,
                                            stepTitle = artifactMeta?.stepTitle,
                                            userId = activeUserId
                                        )
                                    ))

                                    // Update session updated_at in local Room DB
                                    db.sessionDao().upsertSessions(listOf(SessionEntity(
                                        id = currentSession,
                                        title = chatTitle,
                                        model = model,
                                        updatedAt = System.currentTimeMillis(),
                                        userId = activeUserId
                                    )))
                                }
                            }

                            // Update _sessions in-memory with new timestamp & message count
                            _sessions.value = _sessions.value.map { s ->
                                if (s.session_id == currentSession) {
                                    s.copy(
                                        updated_at = System.currentTimeMillis() / 1000.0,
                                        message_count = _messages.value.size
                                    )
                                } else s
                            }.sortedByDescending { it.updated_at }

                            // If this is the 1st or 2nd message exchange, generate a smart AI title using the fast mini model
                            // If title is default or first/second exchange, generate a smart AI title using the fast mini model
                            val currentSessionTitle = _sessions.value.firstOrNull { it.session_id == currentSession }?.title ?: ""
                            val isTitleGeneric = currentSessionTitle.isBlank() ||
                                currentSessionTitle.lowercase() in listOf("chat", "new chat", "untitled") ||
                                currentSessionTitle.startsWith("sess_") ||
                                currentSessionTitle == userMsg.content.lines().firstOrNull()?.trim()?.take(40) ||
                                _messages.value.size in 2..4

                            if (isTitleGeneric) {
                                repositoryScope.launch(Dispatchers.IO) {
                                    try {
                                        val aiTitle = apiClient.generateChatTitle(
                                            userPrompt = userMsg.content,
                                            assistantReply = event.fullResponse
                                        )
                                        if (!aiTitle.isNullOrBlank()) {
                                            updateSessionTitle(currentSession, aiTitle)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        is StreamEvent.Error -> {
                            _thinkingPhase.value = ThinkingPhase.COMPLETED
                            val currentText = newSmoother.renderedText.value
                            val errorText = if (currentText.isBlank()) {
                                "Unable to connect to Hermes server. ${event.throwable.message ?: "Please check connection."}"
                            } else {
                                currentText
                            }
                            newSmoother.complete(errorText)
                            val artifactMeta = extractArtifactFromText(errorText)
                            updateAssistantMessage(
                                id = assistantMsgId,
                                content = errorText,
                                thinking = thoughtsAccumulator.toString(),
                                isStreaming = false,
                                artifactTitle = artifactMeta?.title,
                                artifactType = artifactMeta?.type,
                                artifactLanguage = artifactMeta?.language,
                                artifactCode = artifactMeta?.code,
                                stepTitle = artifactMeta?.stepTitle
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _thinkingPhase.value = ThinkingPhase.COMPLETED
                val currentText = newSmoother.renderedText.value
                val fallback = if (currentText.isBlank()) "Error: ${e.message}" else currentText
                newSmoother.complete(fallback)
                val artifactMeta = extractArtifactFromText(fallback)
                updateAssistantMessage(
                    id = assistantMsgId,
                    content = fallback,
                    thinking = thoughtsAccumulator.toString(),
                    isStreaming = false,
                    artifactTitle = artifactMeta?.title,
                    artifactType = artifactMeta?.type,
                    artifactLanguage = artifactMeta?.language,
                    artifactCode = artifactMeta?.code,
                    stepTitle = artifactMeta?.stepTitle
                )
            } finally {
                _isStreaming.value = false
                _activeThinking.value = null
                _thinkingPhase.value = ThinkingPhase.COMPLETED
            }
        }
    }

    private data class ExtractedArtifact(
        val title: String,
        val type: String,
        val language: String,
        val code: String,
        val stepTitle: String
    )

    private fun extractArtifactFromText(text: String): ExtractedArtifact? {
        if (text.isBlank()) return null

        val antMatch = ANT_ARTIFACT_REGEX.find(text)
        if (antMatch != null) {
            val attrs = antMatch.groupValues[1]
            val content = antMatch.groupValues[2].trim()
            val titleMatch = Regex("""title=["']([^"']+)["']""").find(attrs)
            val title = titleMatch?.groupValues?.get(1) ?: "Document"
            val typeMatch = Regex("""type=["']([^"']+)["']""").find(attrs)
            val rawType = typeMatch?.groupValues?.get(1) ?: "text/markdown"
            val lang = when {
                rawType.contains("python", true) -> "Python"
                rawType.contains("javascript", true) || rawType.contains("js", true) -> "JavaScript"
                rawType.contains("kotlin", true) -> "Kotlin"
                rawType.contains("html", true) -> "HTML"
                else -> "Markdown"
            }
            val displayType = if (lang == "Markdown") "Document · MD" else "Code · ${lang.take(3).uppercase()}"
            return ExtractedArtifact(
                title = title,
                type = displayType,
                language = lang,
                code = content,
                stepTitle = "Creating $title"
            )
        }

        val codeMatch = CODE_BLOCK_REGEX.find(text)
        if (codeMatch != null) {
            val rawLang = (codeMatch.groupValues[1]).trim().lowercase()
            val codeBody = codeMatch.groupValues[2].trim()

            if (codeBody.length > 30) {
                val (langName, ext, title) = when (rawLang) {
                    "python", "py" -> Triple("Python", "PY", "calculator.py".takeIf { codeBody.contains("calc", true) } ?: "script.py")
                    "kotlin", "kt" -> Triple("Kotlin", "KT", "MainActivity.kt")
                    "javascript", "js" -> Triple("JavaScript", "JS", "app.js")
                    "html" -> Triple("HTML", "HTML", "index.html")
                    "bash", "sh" -> Triple("Bash", "SH", "setup.sh")
                    "json" -> Triple("JSON", "JSON", "data.json")
                    "markdown", "md" -> Triple("Markdown", "MD", "README.md")
                    else -> Triple("Code", "CODE", "solution.txt")
                }

                val cleanTitle = when {
                    codeBody.contains("class Calculator", true) -> "Calculator"
                    codeBody.contains("fun main", true) -> "Application"
                    rawLang.contains("md") -> "Implementation Guide"
                    else -> title.substringBeforeLast(".")
                }

                return ExtractedArtifact(
                    title = cleanTitle,
                    type = "Code · $ext",
                    language = langName,
                    code = codeBody,
                    stepTitle = "Building $cleanTitle"
                )
            }
        }

        return null
    }

    private fun updateAssistantMessage(
        id: String,
        content: String,
        thinking: String?,
        isStreaming: Boolean,
        artifactTitle: String? = null,
        artifactType: String? = null,
        artifactLanguage: String? = null,
        artifactCode: String? = null,
        stepTitle: String? = null
    ) {
        val currentList = _messages.value
        val exists = currentList.any { it.id == id }
        if (!exists) {
            _messages.value = currentList + ChatMessage(
                id = id,
                role = "assistant",
                content = content,
                thinking = if (thinking.isNullOrEmpty()) null else thinking,
                isStreaming = isStreaming,
                artifactTitle = artifactTitle,
                artifactType = artifactType,
                artifactLanguage = artifactLanguage,
                artifactCode = artifactCode,
                stepTitle = stepTitle
            )
        } else {
            _messages.value = currentList.map { msg ->
                if (msg.id == id) {
                    msg.copy(
                        content = content,
                        thinking = if (thinking.isNullOrEmpty()) null else thinking,
                        isStreaming = isStreaming,
                        artifactTitle = artifactTitle ?: msg.artifactTitle,
                        artifactType = artifactType ?: msg.artifactType,
                        artifactLanguage = artifactLanguage ?: msg.artifactLanguage,
                        artifactCode = artifactCode ?: msg.artifactCode,
                        stepTitle = stepTitle ?: msg.stepTitle
                    )
                } else {
                    msg
                }
            }
        }
    }

    override fun fetchSessions() {
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val serverSessions = apiClient.getSessions()
                if (serverSessions.isNotEmpty()) {
                    _sessions.value = serverSessions
                    roomDb?.let { db ->
                        db.sessionDao().upsertSessions(serverSessions.map {
                            SessionEntity(
                                id = it.session_id,
                                title = it.title,
                                model = it.model ?: "hermes-agent",
                                updatedAt = (it.updated_at * 1000).toLong(),
                                userId = activeUserId
                            )
                        })
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun loadSession(sessionId: String) {
        repositoryScope.launch(Dispatchers.IO) {
            // Guard: If we are currently actively streaming for this exact session,
            // DO NOT wipe the active stream with stale cache!
            if (_currentSessionId.value == sessionId && _isStreaming.value) {
                return@launch
            }

            _currentSessionId.value = sessionId

            // 1. Immediately load and render from Room cache (0ms delay)
            roomDb?.let { db ->
                val cached = db.messageDao().getMessagesForSession(sessionId, activeUserId)
                if (cached.isNotEmpty() && (!_isStreaming.value || _messages.value.isEmpty())) {
                    _messages.value = cached.map {
                        ChatMessage(
                            id = it.id,
                            role = it.role,
                            content = it.content,
                            thinking = it.thinking,
                            isStreaming = it.isStreaming,
                            timestamp = it.timestamp,
                            artifactTitle = it.artifactTitle,
                            artifactType = it.artifactType,
                            artifactLanguage = it.artifactLanguage,
                            artifactCode = it.artifactCode,
                            stepTitle = it.stepTitle
                        )
                    }
                }
            }

            // 2. Fetch from server and sync
            val detail = apiClient.getSession(sessionId)
            if (_isStreaming.value && _currentSessionId.value == sessionId) {
                return@launch
            }

            if (detail != null) {
                // Update session title if server generated a smart title
                val serverTitle = detail.title
                if (!serverTitle.isNullOrBlank() && serverTitle.lowercase() !in listOf("chat", "new chat", "untitled") && !serverTitle.startsWith("sess_")) {
                    _sessions.value = _sessions.value.map { s ->
                        if (s.session_id == sessionId) s.copy(title = serverTitle) else s
                    }
                    roomDb?.let { db ->
                        db.sessionDao().updateSessionTitle(sessionId, activeUserId, serverTitle)
                    }
                }
            }

            if (detail != null && detail.messages.isNotEmpty()) {
                val mapped = detail.messages.map { sMsg ->
                    val art = extractArtifactFromText(sMsg.content)
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = sMsg.role,
                        content = sMsg.content,
                        thinking = sMsg.reasoning_content,
                        isStreaming = false,
                        timestamp = (sMsg.timestamp?.toLong() ?: (System.currentTimeMillis() / 1000)) * 1000,
                        artifactTitle = art?.title,
                        artifactType = art?.type,
                        artifactLanguage = art?.language,
                        artifactCode = art?.code,
                        stepTitle = art?.stepTitle
                    )
                }
                _messages.value = mapped

                // Update Room cache with remote messages
                roomDb?.let { db ->
                    db.messageDao().insertMessages(mapped.map { m ->
                        MessageEntity(
                            id = m.id,
                            sessionId = sessionId,
                            role = m.role,
                            content = m.content,
                            thinking = m.thinking,
                            timestamp = m.timestamp,
                            isStreaming = false,
                            artifactTitle = m.artifactTitle,
                            artifactType = m.artifactType,
                            artifactLanguage = m.artifactLanguage,
                            artifactCode = m.artifactCode,
                            stepTitle = m.stepTitle,
                            userId = activeUserId
                        )
                    })
                }

                // Check if title is still default/generic and trigger fallback title generation
                val currentSessionTitle = _sessions.value.firstOrNull { it.session_id == sessionId }?.title ?: ""
                val isTitleGeneric = currentSessionTitle.isBlank() ||
                    currentSessionTitle.lowercase() in listOf("chat", "new chat", "untitled") ||
                    currentSessionTitle.startsWith("sess_") ||
                    currentSessionTitle == mapped.firstOrNull { it.role == "user" }?.content?.lines()?.firstOrNull()?.trim()?.take(40)

                if (isTitleGeneric) {
                    val userContent = mapped.firstOrNull { it.role == "user" }?.content
                    val asstContent = mapped.lastOrNull { it.role == "assistant" && it.content.isNotBlank() }?.content
                    if (!userContent.isNullOrBlank() && !asstContent.isNullOrBlank()) {
                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                val aiTitle = apiClient.generateChatTitle(userContent, asstContent)
                                if (!aiTitle.isNullOrBlank()) {
                                    updateSessionTitle(sessionId, aiTitle)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                // If session is still streaming on the server, poll until completed
                if (detail.is_streaming) {
                    _isStreaming.value = true
                    _activeThinking.value = "Hermes is working on your response..."
                    repositoryScope.launch {
                        var pollCount = 0
                        while (_isStreaming.value && pollCount < 60) {
                            kotlinx.coroutines.delay(1500)
                            pollCount++
                            val updatedDetail = apiClient.getSession(sessionId)
                            if (updatedDetail != null && updatedDetail.messages.isNotEmpty()) {
                                val updatedMapped = updatedDetail.messages.map { sMsg ->
                                    val art = extractArtifactFromText(sMsg.content)
                                    ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        role = sMsg.role,
                                        content = sMsg.content,
                                        thinking = sMsg.reasoning_content,
                                        isStreaming = updatedDetail.is_streaming,
                                        timestamp = (sMsg.timestamp?.toLong() ?: (System.currentTimeMillis() / 1000)) * 1000,
                                        artifactTitle = art?.title,
                                        artifactType = art?.type,
                                        artifactLanguage = art?.language,
                                        artifactCode = art?.code,
                                        stepTitle = art?.stepTitle
                                    )
                                }
                                _messages.value = updatedMapped

                                if (!updatedDetail.title.isNullOrBlank() && updatedDetail.title.lowercase() !in listOf("chat", "new chat")) {
                                    updateSessionTitle(sessionId, updatedDetail.title)
                                }

                                if (!updatedDetail.is_streaming) {
                                    _isStreaming.value = false
                                    _activeThinking.value = null
                                    roomDb?.let { db ->
                                        db.messageDao().insertMessages(updatedMapped.map { m ->
                                            MessageEntity(
                                                id = m.id,
                                                sessionId = sessionId,
                                                role = m.role,
                                                content = m.content,
                                                thinking = m.thinking,
                                                timestamp = m.timestamp,
                                                isStreaming = false,
                                                artifactTitle = m.artifactTitle,
                                                artifactType = m.artifactType,
                                                artifactLanguage = m.artifactLanguage,
                                                artifactCode = m.artifactCode,
                                                stepTitle = m.stepTitle,
                                                userId = activeUserId
                                            )
                                        })
                                    }
                                    break
                                }
                            }
                        }
                        _isStreaming.value = false
                        _activeThinking.value = null
                    }
                } else {
                    _isStreaming.value = false
                }
            }
        }
    }

    override fun syncActiveSession() {
        val active = _currentSessionId.value
        if (!active.isNullOrBlank() && !_isStreaming.value) {
            loadSession(active)
        }
        fetchSessions()
    }

    override fun deleteSession(sessionId: String) {
        repositoryScope.launch(Dispatchers.IO) {
            apiClient.deleteSession(sessionId)
            roomDb?.sessionDao()?.deleteSession(sessionId, activeUserId)
            roomDb?.messageDao()?.deleteMessagesForSession(sessionId, activeUserId)
            _sessions.value = _sessions.value.filter { it.session_id != sessionId }
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }

    override fun updateSessionTitle(sessionId: String, newTitle: String) {
        val clean = newTitle.trim().take(60)
        if (clean.isBlank()) return

        // 1. Update in-memory state so UI updates immediately
        _sessions.value = _sessions.value.map { s ->
            if (s.session_id == sessionId) {
                s.copy(title = clean)
            } else s
        }

        // 2. Persist in local Room database
        roomDb?.let { db ->
            repositoryScope.launch(Dispatchers.IO) {
                db.sessionDao().updateSessionTitle(sessionId, activeUserId, clean)
            }
        }

        // 3. Sync with backend server
        repositoryScope.launch(Dispatchers.IO) {
            apiClient.renameSession(sessionId, clean)
        }
    }

    override fun fetchProjects() {
        repositoryScope.launch(Dispatchers.IO) {
            val list = apiClient.getProjects()
            if (list.isNotEmpty()) {
                _projects.value = list
                roomDb?.let { db ->
                    db.projectDao().upsertProjects(list.map {
                        ProjectEntity(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            createdAt = (it.created_at * 1000).toLong(),
                            userId = activeUserId
                        )
                    })
                }
            }
        }
    }

    override fun createNewProject(name: String, description: String) {
        repositoryScope.launch(Dispatchers.IO) {
            val created = apiClient.createProject(name, description)
            if (created != null) {
                _projects.value = listOf(created) + _projects.value
                roomDb?.let { db ->
                    db.projectDao().upsertProjects(listOf(ProjectEntity(
                        id = created.id,
                        name = created.name,
                        description = created.description,
                        userId = activeUserId
                    )))
                }
            }
        }
    }

    override fun fetchModels() {
        repositoryScope.launch {
            val list = apiClient.getModels()
            if (list.isNotEmpty()) {
                _availableModels.value = list
            }
        }
    }

    override fun fetchTasks() {
        repositoryScope.launch(Dispatchers.IO) {
            val fetched = apiClient.getTasks()
            if (fetched.isNotEmpty()) {
                _tasks.value = fetched
                roomDb?.let { db ->
                    db.taskDao().upsertTasks(fetched.map {
                        TaskEntity(
                            id = it.id,
                            title = it.title,
                            prompt = it.prompt,
                            status = it.status,
                            progress = it.progress,
                            agentName = it.agent_name,
                            userId = activeUserId
                        )
                    })
                }
            }
        }
    }

    override fun createNewTask(title: String, prompt: String) {
        repositoryScope.launch(Dispatchers.IO) {
            val created = apiClient.createTask(title, prompt)
            if (created != null) {
                _tasks.value = listOf(created) + _tasks.value
                roomDb?.let { db ->
                    db.taskDao().upsertTasks(listOf(TaskEntity(
                        id = created.id,
                        title = created.title,
                        prompt = created.prompt,
                        status = created.status,
                        progress = created.progress,
                        agentName = created.agent_name,
                        userId = activeUserId
                    )))
                }
            }
        }
    }

    override fun pauseTask(taskId: String) {
        repositoryScope.launch(Dispatchers.IO) {
            apiClient.pauseTask(taskId)
            _tasks.value = _tasks.value.map {
                if (it.id == taskId) it.copy(status = "PAUSED") else it
            }
            roomDb?.taskDao()?.updateTaskStatus(taskId, activeUserId, "PAUSED")
        }
    }

    override fun resumeTask(taskId: String) {
        repositoryScope.launch(Dispatchers.IO) {
            apiClient.resumeTask(taskId)
            _tasks.value = _tasks.value.map {
                if (it.id == taskId) it.copy(status = "RUNNING") else it
            }
            roomDb?.taskDao()?.updateTaskStatus(taskId, activeUserId, "RUNNING")
        }
    }

    override fun cancelTask(taskId: String) {
        repositoryScope.launch(Dispatchers.IO) {
            apiClient.cancelTask(taskId)
            _tasks.value = _tasks.value.map {
                if (it.id == taskId) it.copy(status = "CANCELLED") else it
            }
            roomDb?.taskDao()?.updateTaskStatus(taskId, activeUserId, "CANCELLED")
        }
    }

    override fun fetchApprovals() {
        repositoryScope.launch {
            val list = apiClient.getApprovals()
            _approvals.value = list
        }
    }

    override fun approveRequest(approvalId: String) {
        repositoryScope.launch {
            apiClient.approveRequest(approvalId)
            _approvals.value = _approvals.value.filter { it.id != approvalId }
        }
    }

    override fun denyRequest(approvalId: String) {
        repositoryScope.launch {
            apiClient.denyRequest(approvalId)
            _approvals.value = _approvals.value.filter { it.id != approvalId }
        }
    }

    override fun fetchHostStatus() {
        repositoryScope.launch {
            val status = apiClient.getHostStatus()
            if (status != null) {
                _hostStatus.value = status
            }
        }
    }

    override fun fetchKnowledgeSources() {
        repositoryScope.launch {
            val sources = apiClient.getKnowledgeSources()
            if (sources.isNotEmpty()) {
                _knowledgeSources.value = sources
            }
        }
    }

    override fun fetchDirectoryServers() {
        repositoryScope.launch {
            val servers = apiClient.getDirectoryServers()
            if (servers.isNotEmpty()) {
                _directoryServers.value = servers
            }
        }
    }

    override fun connectTerminalPty() {
        if (ptyWebSocket != null) return
        ptyWebSocket = apiClient.connectPtyWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                repositoryScope.launch {
                    _terminalLogs.value = _terminalLogs.value + "[Connected to Hermes Live PTY]"
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                repositoryScope.launch {
                    val clean = text.replace("\r\n", "\n").replace("\r", "\n")
                    val lines = clean.lines().filter { it.isNotEmpty() }
                    _terminalLogs.value = (_terminalLogs.value + lines).takeLast(200)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                repositoryScope.launch {
                    _terminalLogs.value = _terminalLogs.value + "[PTY Connection Closed: $reason]"
                }
                ptyWebSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                repositoryScope.launch {
                    _terminalLogs.value = _terminalLogs.value + "[PTY Error: ${t.message}]"
                }
                ptyWebSocket = null
            }
        })
    }

    override fun sendTerminalInput(input: String) {
        ptyWebSocket?.send("$input\n")
    }

    override fun disconnectTerminalPty() {
        ptyWebSocket?.close(1000, "User disconnected")
        ptyWebSocket = null
    }

    override suspend fun searchKnowledge(query: String, sources: String?): List<KnowledgeSearchResultItemDto> {
        return apiClient.searchKnowledge(query, sources)
    }

    override suspend fun syncKnowledge(connector: String?): Boolean {
        val ok = apiClient.syncKnowledge(connector)
        fetchKnowledgeSources()
        return ok
    }

    override suspend fun createKnowledgeNote(title: String, content: String, destination: String): Boolean {
        return apiClient.createKnowledgeNote(title, content, destination)
    }

    override suspend fun listComputerFiles(path: String?): List<ComputerFileItemDto> {
        return apiClient.listComputerFiles(path)
    }

    override suspend fun getComputerFileContent(path: String): String? {
        return apiClient.getComputerFileContent(path)
    }

    override suspend fun getBrowserStatus(): BrowserStatusDto? {
        return apiClient.getBrowserStatus()
    }

    override suspend fun navigateBrowser(url: String): BrowserNavigateResponseDto? {
        return apiClient.navigateBrowser(url)
    }

    override suspend fun getBrowserScreenshot(): BrowserScreenshotResponseDto? {
        return apiClient.getBrowserScreenshot()
    }

    override suspend fun getWorkforceRoles(): List<WorkforceRoleDto> {
        return apiClient.getWorkforceRoles()
    }

    override suspend fun getAutomations(): List<ScheduledAutomationDto> {
        return apiClient.getAutomations()
    }

    override suspend fun createAutomation(title: String, prompt: String, cronExpression: String): Boolean {
        return apiClient.createAutomation(title, prompt, cronExpression)
    }

    override suspend fun toggleAutomation(automationId: String): Boolean {
        return apiClient.toggleAutomation(automationId)
    }

    override suspend fun runAutomationNow(automationId: String): Boolean {
        return apiClient.runAutomationNow(automationId)
    }

    override suspend fun getChannels(): Result<ChannelsConfigDto> {
        return apiClient.getChannels()
    }

    override suspend fun updateChannels(request: UpdateChannelsRequestDto): Result<Boolean> {
        return apiClient.updateChannels(request)
    }

    override suspend fun testChannel(channel: String, message: String): Result<TestChannelResponseDto> {
        return apiClient.testChannel(channel, message)
    }

    override suspend fun getOmniRouteTelemetry(): Result<OmniRouteTelemetryDto> {
        return apiClient.getOmniRouteTelemetry()
    }

    override suspend fun searchMessagesFts(query: String): List<FtsSearchResultDto> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val matches = roomDb?.messageDao()?.searchMessages(query, activeUserId) ?: emptyList()
        val sessionsMap = _sessions.value.associateBy { it.session_id }
        matches.map { entity ->
            val session = sessionsMap[entity.sessionId]
            val text = entity.content.ifBlank { entity.thinking ?: "" }
            val snippet = extractSnippet(text, query)
            FtsSearchResultDto(
                sessionId = entity.sessionId,
                sessionTitle = session?.title ?: "Chat",
                role = entity.role,
                snippet = snippet,
                timestamp = entity.timestamp
            )
        }
    }

    private fun extractSnippet(content: String, query: String): String {
        val idx = content.indexOf(query, ignoreCase = true)
        if (idx < 0) return content.take(120)
        val start = (idx - 40).coerceAtLeast(0)
        val end = (idx + query.length + 60).coerceAtMost(content.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < content.length) "…" else ""
        return prefix + content.substring(start, end).replace("\n", " ") + suffix
    }

    override fun clearMessages() {
        stopGeneration()
        _currentSessionId.value = null
        _messages.value = emptyList()
    }
}
