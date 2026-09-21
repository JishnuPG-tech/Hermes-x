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

enum class TaskTab {
    TASKS,
    DAG_WORKFORCE,
    AUTOMATIONS
}

class TasksViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val tasks: StateFlow<List<TaskDto>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTab = MutableStateFlow(TaskTab.TASKS)
    val activeTab: StateFlow<TaskTab> = _activeTab.asStateFlow()

    private val _expandedTaskId = MutableStateFlow<String?>(null)
    val expandedTaskId: StateFlow<String?> = _expandedTaskId.asStateFlow()

    private val _workforceRoles = MutableStateFlow<List<WorkforceRoleDto>>(emptyList())
    val workforceRoles: StateFlow<List<WorkforceRoleDto>> = _workforceRoles.asStateFlow()

    private val _automations = MutableStateFlow<List<ScheduledAutomationDto>>(emptyList())
    val automations: StateFlow<List<ScheduledAutomationDto>> = _automations.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
        fetchWorkforce()
        fetchAutomations()
    }

    fun selectTab(tab: TaskTab) {
        _activeTab.value = tab
        if (tab == TaskTab.DAG_WORKFORCE && _workforceRoles.value.isEmpty()) {
            fetchWorkforce()
        } else if (tab == TaskTab.AUTOMATIONS) {
            fetchAutomations()
        }
    }

    fun toggleTaskExpand(taskId: String) {
        _expandedTaskId.value = if (_expandedTaskId.value == taskId) null else taskId
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchTasks()
                fetchWorkforce()
                fetchAutomations()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun fetchWorkforce() {
        viewModelScope.launch {
            try {
                _workforceRoles.value = repository.getWorkforceRoles()
            } catch (_: Exception) {}
        }
    }

    fun fetchAutomations() {
        viewModelScope.launch {
            try {
                _automations.value = repository.getAutomations()
            } catch (_: Exception) {}
        }
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

    fun toggleAutomation(id: String) {
        viewModelScope.launch {
            try {
                repository.toggleAutomation(id)
                fetchAutomations()
            } catch (_: Exception) {}
        }
    }

    fun runAutomationNow(id: String) {
        viewModelScope.launch {
            try {
                repository.runAutomationNow(id)
                fetchAutomations()
                repository.fetchTasks()
            } catch (_: Exception) {}
        }
    }

    fun createAutomation(title: String, prompt: String, cronExpression: String = "0 * * * *") {
        viewModelScope.launch {
            try {
                repository.createAutomation(title, prompt, cronExpression)
                fetchAutomations()
            } catch (_: Exception) {}
        }
    }
}
