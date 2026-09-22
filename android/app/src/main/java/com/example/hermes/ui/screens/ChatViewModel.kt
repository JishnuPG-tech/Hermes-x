package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = repository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isStreaming: StateFlow<Boolean> = repository.isStreaming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeThinking: StateFlow<String?> = repository.activeThinking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val thinkingPhase: StateFlow<ThinkingPhase> = repository.thinkingPhase
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThinkingPhase.IDLE)

    val sessions: StateFlow<List<SessionDto>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessionId: StateFlow<String?> = repository.currentSessionId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projects: StateFlow<List<ProjectDto>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableModels: StateFlow<List<ModelOptionDto>> = repository.availableModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArtifacts: StateFlow<List<ArtifactItemDto>> = repository.allArtifacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskDto>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val webSearchEnabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    val memoryEnabled = kotlinx.coroutines.flow.MutableStateFlow(true)
    val selectedProject = kotlinx.coroutines.flow.MutableStateFlow<ProjectDto?>(null)

    fun setWebSearchEnabled(enabled: Boolean) {
        webSearchEnabled.value = enabled
    }

    fun setMemoryEnabled(enabled: Boolean) {
        memoryEnabled.value = enabled
    }

    fun setSelectedProject(project: ProjectDto?) {
        selectedProject.value = project
    }

    private val _pendingAttachments = kotlinx.coroutines.flow.MutableStateFlow<List<ChatAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments

    fun setPendingAttachments(list: List<ChatAttachment>) {
        _pendingAttachments.value = list
    }

    fun consumePendingAttachments(): List<ChatAttachment> {
        val list = _pendingAttachments.value
        _pendingAttachments.value = emptyList()
        return list
    }

    fun sendMessage(
        content: String,
        model: String = "hermes-agent",
        attachments: List<ChatAttachment> = emptyList()
    ) {
        if (content.isBlank() && attachments.isEmpty()) return
        repository.sendMessage(
            content = content.trim(),
            model = model,
            attachments = attachments,
            webSearch = webSearchEnabled.value,
            memory = memoryEnabled.value
        )
    }

    fun stopGeneration() {
        repository.stopGeneration()
    }

    fun clearMessages() {
        repository.clearMessages()
    }

    fun loadSession(sessionId: String) {
        repository.loadSession(sessionId)
    }

    fun deleteSession(sessionId: String) {
        repository.deleteSession(sessionId)
    }

    fun updateSessionTitle(sessionId: String, newTitle: String) {
        repository.updateSessionTitle(sessionId, newTitle)
    }

    fun renameSession(sessionId: String, title: String) {
        repository.renameSession(sessionId, title)
    }

    fun togglePinSession(sessionId: String) {
        repository.togglePinSession(sessionId)
    }

    fun assignSessionToProject(sessionId: String, projectId: String?) {
        repository.assignSessionToProject(sessionId, projectId)
    }

    fun fetchSessions() {
        repository.fetchSessions()
    }

    fun createProject(name: String, description: String = "") {
        repository.createNewProject(name, description)
    }

    fun createTask(title: String, prompt: String) {
        repository.createNewTask(title, prompt)
    }

    val approvals: StateFlow<List<ApprovalDto>> = repository.approvals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun fetchApprovals() {
        repository.fetchApprovals()
    }

    fun approveRequest(approvalId: String) {
        repository.approveRequest(approvalId)
    }

    fun denyRequest(approvalId: String) {
        repository.denyRequest(approvalId)
    }

    val ftsSearchResults = kotlinx.coroutines.flow.MutableStateFlow<List<FtsSearchResultDto>>(emptyList())

    fun searchMessagesFts(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                ftsSearchResults.value = emptyList()
            } else {
                ftsSearchResults.value = repository.searchMessagesFts(query)
            }
        }
    }
}


