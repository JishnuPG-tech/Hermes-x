package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.DataRepository
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.data.HostStatusDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TerminalViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val hostStatus: StateFlow<HostStatusDto?> = repository.hostStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val terminalLogs: StateFlow<List<String>> = repository.terminalLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Connecting to Hermes Live PTY..."))

    init {
        repository.fetchHostStatus()
        repository.connectTerminalPty()
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        repository.sendTerminalInput(command.trim())
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectTerminalPty()
    }
}
