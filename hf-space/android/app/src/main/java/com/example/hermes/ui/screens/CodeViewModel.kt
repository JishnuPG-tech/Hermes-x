package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CodeViewTab {
    AGENT,
    FILES,
    BROWSER
}

class CodeViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val approvals: StateFlow<List<ApprovalDto>> = repository.approvals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hostStatus: StateFlow<HostStatusDto?> = repository.hostStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contextMaxTokens: Int = 200_000

    val contextUsageTokens: StateFlow<Int> = repository.messages
        .map { msgList ->
            val chars = msgList.sumOf { it.content.length + (it.thinking?.length ?: 0) }
            (chars / 4).coerceAtLeast(1450)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1450)

    val gitBranch: StateFlow<String> = kotlinx.coroutines.flow.combine(
        repository.hostStatus,
        repository.projects
    ) { status, projects ->
        val activeProj = projects.firstOrNull()?.workspace?.ifBlank { null }
            ?: projects.firstOrNull()?.name?.ifBlank { null }
        when {
            activeProj != null -> activeProj.lowercase().replace(" ", "-")
            status != null && status.storage_root.isNotBlank() && status.storage_root != "/" ->
                status.storage_root.trimEnd('/').substringAfterLast("/").ifBlank { "main" }
            else -> "main"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "main")

    private val _selectedTab = MutableStateFlow(CodeViewTab.AGENT)
    val selectedTab: StateFlow<CodeViewTab> = _selectedTab.asStateFlow()

    // File Explorer
    private val _files = MutableStateFlow<List<ComputerFileItemDto>>(emptyList())
    val files: StateFlow<List<ComputerFileItemDto>> = _files.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _activeFileContent = MutableStateFlow<String?>(null)
    val activeFileContent: StateFlow<String?> = _activeFileContent.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String?>(null)
    val activeFilePath: StateFlow<String?> = _activeFilePath.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    // Headless Browser Live View
    private val _browserStatus = MutableStateFlow<BrowserStatusDto?>(null)
    val browserStatus: StateFlow<BrowserStatusDto?> = _browserStatus.asStateFlow()

    private val _browserScreenshot = MutableStateFlow<String?>(null)
    val browserScreenshot: StateFlow<String?> = _browserScreenshot.asStateFlow()

    private val _isNavigatingBrowser = MutableStateFlow(false)
    val isNavigatingBrowser: StateFlow<Boolean> = _isNavigatingBrowser.asStateFlow()

    init {
        repository.fetchApprovals()
        repository.fetchHostStatus()
        repository.fetchProjects()
        fetchFiles("")
        refreshBrowserStatus()
    }

    fun selectTab(tab: CodeViewTab) {
        _selectedTab.value = tab
        if (tab == CodeViewTab.FILES && _files.value.isEmpty()) {
            fetchFiles(_currentPath.value)
        } else if (tab == CodeViewTab.BROWSER) {
            refreshBrowserStatus()
            refreshBrowserScreenshot()
        }
    }

    fun fetchFiles(path: String = "") {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            try {
                val list = repository.listComputerFiles(path.ifBlank { null })
                _files.value = list
                _currentPath.value = path
            } catch (_: Exception) {
                _files.value = emptyList()
            } finally {
                _isLoadingFiles.value = false
            }
        }
    }

    fun openFile(file: ComputerFileItemDto) {
        if (file.is_dir) {
            fetchFiles(file.path)
        } else {
            viewModelScope.launch {
                _activeFilePath.value = file.path
                _activeFileContent.value = repository.getComputerFileContent(file.path)
            }
        }
    }

    fun closeActiveFile() {
        _activeFilePath.value = null
        _activeFileContent.value = null
    }

    fun navigateUpDirectory() {
        val path = _currentPath.value
        if (path.contains("/")) {
            val parent = path.substringBeforeLast("/")
            fetchFiles(parent)
        } else if (path.isNotBlank()) {
            fetchFiles("")
        }
    }

    fun refreshBrowserStatus() {
        viewModelScope.launch {
            try {
                val status = repository.getBrowserStatus()
                _browserStatus.value = status
            } catch (_: Exception) {}
        }
    }

    fun refreshBrowserScreenshot() {
        viewModelScope.launch {
            try {
                val res = repository.getBrowserScreenshot()
                if (res != null && res.screenshot_base64.isNotBlank()) {
                    _browserScreenshot.value = res.screenshot_base64
                }
            } catch (_: Exception) {}
        }
    }

    fun navigateBrowser(url: String) {
        viewModelScope.launch {
            _isNavigatingBrowser.value = true
            try {
                val res = repository.navigateBrowser(url)
                if (res != null) {
                    _browserStatus.value = BrowserStatusDto(
                        status = "ok",
                        current_url = res.url,
                        title = res.title,
                        is_active = true
                    )
                    refreshBrowserScreenshot()
                }
            } catch (_: Exception) {
            } finally {
                _isNavigatingBrowser.value = false
            }
        }
    }

    fun approve(id: String) {
        repository.approveRequest(id)
    }

    fun deny(id: String) {
        repository.denyRequest(id)
    }

    fun refresh() {
        repository.fetchApprovals()
        repository.fetchHostStatus()
        repository.fetchProjects()
        fetchFiles(_currentPath.value)
        refreshBrowserStatus()
    }
}
