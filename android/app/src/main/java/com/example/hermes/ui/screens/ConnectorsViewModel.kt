package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ConnectorsViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val knowledgeSources: StateFlow<List<KnowledgeSourceDto>> = repository.knowledgeSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directoryServers: StateFlow<List<DirectoryServerItemDto>> = repository.directoryServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.fetchKnowledgeSources()
        repository.fetchDirectoryServers()
    }

    fun refresh() {
        repository.fetchKnowledgeSources()
        repository.fetchDirectoryServers()
    }
}
