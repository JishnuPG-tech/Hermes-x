package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.ApprovalDto
import com.example.hermes.data.DataRepository
import com.example.hermes.data.HermesDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CodeViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val approvals: StateFlow<List<ApprovalDto>> = repository.approvals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.fetchApprovals()
    }

    fun approve(id: String) {
        repository.approveRequest(id)
    }

    fun deny(id: String) {
        repository.denyRequest(id)
    }

    fun refresh() {
        repository.fetchApprovals()
    }
}
