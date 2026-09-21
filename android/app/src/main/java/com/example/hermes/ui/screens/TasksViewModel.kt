package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.DataRepository
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.data.TaskDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TasksViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val tasks: StateFlow<List<TaskDto>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        repository.fetchTasks()
    }

    fun createTask(title: String, prompt: String) {
        if (title.isBlank()) return
        repository.createNewTask(title.trim(), prompt.trim())
    }

    fun pauseTask(taskId: String) {
        repository.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) {
        repository.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) {
        repository.cancelTask(taskId)
    }
}
