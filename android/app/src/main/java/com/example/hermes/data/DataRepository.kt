package com.example.hermes.data

import com.example.hermes.stream.StreamSmoothingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun sendMessage(content: String, model: String = "hermes-agent")
    fun stopGeneration()
    fun fetchTasks()
    fun createNewTask(title: String, prompt: String)
    fun fetchSessions()
    fun loadSession(sessionId: String)
    fun deleteSession(sessionId: String)
    fun fetchProjects()
    fun createNewProject(name: String, description: String = "")
    fun fetchModels()
    fun clearMessages()
}

enum class ThinkingPhase {
    IDLE,
    THOUGHT_PROCESS,
    BUILDING,
    CREATING_FILE,
    FINALIZING,
    COMPLETED
}

class HermesDataRepository(
    private val apiClient: HermesApiClient = HermesApiClient.instance
) : DataRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var smoother: StreamSmoothingEngine? = null
    private var activeStreamJob: kotlinx.coroutines.Job? = null

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

    private val _availableModels = MutableStateFlow<List<ModelOptionDto>>(emptyList())
    override val availableModels: StateFlow<List<ModelOptionDto>> = _availableModels.asStateFlow()

    private val _allArtifacts = MutableStateFlow<List<ArtifactItemDto>>(emptyList())
    override val allArtifacts: StateFlow<List<ArtifactItemDto>> = _allArtifacts.asStateFlow()

    companion object {
        val instance: HermesDataRepository by lazy { HermesDataRepository() }

        private val CODE_BLOCK_REGEX = Regex("```([a-zA-Z0-9_-]+)?\\s*\\n([\\s\\S]*?)(?:```|$)")
        private val ANT_ARTIFACT_REGEX = Regex("<antArtifact\\s+([^>]+)>([\\s\\S]*?)(?:</antArtifact>|$)", RegexOption.IGNORE_CASE)
    }

    init {
        // Initial async sync with server
        fetchSessions()
        fetchProjects()
        fetchTasks()
        fetchModels()
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

    override fun sendMessage(content: String, model: String) {
        stopGeneration()

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = content
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
                apiClient.streamChat(updatedList.dropLast(1), model).collect { event ->
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
                            newSmoother.complete(event.fullResponse)
                            val artifactMeta = extractArtifactFromText(event.fullResponse)

                            if (artifactMeta != null) {
                                // Record in allArtifacts
                                val newArtifact = ArtifactItemDto(
                                    title = artifactMeta.title,
                                    type = artifactMeta.type,
                                    language = artifactMeta.language,
                                    code = artifactMeta.code
                                )
                                _allArtifacts.value = listOf(newArtifact) + _allArtifacts.value.filter { it.title != newArtifact.title }
                            }

                            updateAssistantMessage(
                                id = assistantMsgId,
                                content = event.fullResponse,
                                thinking = thoughtsAccumulator.toString(),
                                isStreaming = false,
                                artifactTitle = artifactMeta?.title,
                                artifactType = artifactMeta?.type,
                                artifactLanguage = artifactMeta?.language,
                                artifactCode = artifactMeta?.code,
                                stepTitle = artifactMeta?.stepTitle
                            )
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

        // 1. Check for explicit <antArtifact> tags
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

        // 2. Check for fenced code blocks with substantive content (> 30 characters)
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
        _messages.value = _messages.value.map { msg ->
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

    override fun fetchSessions() {
        repositoryScope.launch {
            val serverSessions = apiClient.getSessions()
            if (serverSessions.isNotEmpty()) {
                _sessions.value = serverSessions
            }
        }
    }

    override fun loadSession(sessionId: String) {
        repositoryScope.launch {
            _currentSessionId.value = sessionId
            val detail = apiClient.getSession(sessionId)
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
            } else {
                _messages.value = emptyList()
            }
        }
    }

    override fun deleteSession(sessionId: String) {
        repositoryScope.launch {
            apiClient.deleteSession(sessionId)
            _sessions.value = _sessions.value.filter { it.session_id != sessionId }
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }

    override fun fetchProjects() {
        repositoryScope.launch {
            val list = apiClient.getProjects()
            _projects.value = list
        }
    }

    override fun createNewProject(name: String, description: String) {
        repositoryScope.launch {
            val created = apiClient.createProject(name, description)
            if (created != null) {
                _projects.value = listOf(created) + _projects.value
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
        repositoryScope.launch {
            val fetched = apiClient.getTasks()
            _tasks.value = fetched
        }
    }

    override fun createNewTask(title: String, prompt: String) {
        repositoryScope.launch {
            val created = apiClient.createTask(title, prompt)
            if (created != null) {
                _tasks.value = listOf(created) + _tasks.value
            }
        }
    }

    override fun clearMessages() {
        _currentSessionId.value = null
        _messages.value = emptyList()
    }
}
