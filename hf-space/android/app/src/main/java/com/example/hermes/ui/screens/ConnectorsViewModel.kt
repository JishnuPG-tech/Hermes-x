package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectorsViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val knowledgeSources: StateFlow<List<KnowledgeSourceDto>> = repository.knowledgeSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directoryServers: StateFlow<List<DirectoryServerItemDto>> = repository.directoryServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<KnowledgeSearchResultItemDto>>(emptyList())
    val searchResults: StateFlow<List<KnowledgeSearchResultItemDto>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        repository.fetchKnowledgeSources()
        repository.fetchDirectoryServers()
    }

    fun refresh() {
        repository.fetchKnowledgeSources()
        repository.fetchDirectoryServers()
    }

    fun searchKnowledge(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = repository.searchKnowledge(query)
                _searchResults.value = results
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun triggerSync(connector: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Synchronizing Knowledge Space..."
            try {
                val success = repository.syncKnowledge(connector)
                _syncMessage.value = if (success) "Knowledge Space synchronized" else "Sync completed with warnings"
            } catch (e: Exception) {
                _syncMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun createNote(title: String, content: String, destination: String = "notion", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.createKnowledgeNote(title, content, destination)
                onComplete(success)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }
}
