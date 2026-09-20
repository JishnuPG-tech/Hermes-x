package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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

    fun sendMessage(content: String, model: String = "hermes-agent") {
        if (content.isBlank()) return
        repository.sendMessage(content.trim(), model)
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

    fun fetchSessions() {
        repository.fetchSessions()
    }

    fun createProject(name: String, description: String = "") {
        repository.createNewProject(name, description)
    }

    fun createTask(title: String, prompt: String) {
        repository.createNewTask(title, prompt)
    }
}
