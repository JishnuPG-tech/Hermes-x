package com.example.hermes.ui.main

import com.example.hermes.data.*
import com.example.hermes.ui.screens.ChatViewModel
import com.example.hermes.ui.screens.TasksViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun chatViewModel_initialMessages() = runTest {
    val fakeRepo = FakeHermesRepository()
    val viewModel = ChatViewModel(fakeRepo)
    backgroundScope.launch(testDispatcher) { viewModel.messages.collect() }
    testScheduler.advanceUntilIdle()
    val messages = viewModel.messages.value
    assertEquals(1, messages.size)
    assertEquals("assistant", messages.first().role)
  }

  @Test
  fun chatViewModel_sendMessage_updatesRepository() = runTest {
    val fakeRepo = FakeHermesRepository()
    val viewModel = ChatViewModel(fakeRepo)
    backgroundScope.launch(testDispatcher) { viewModel.messages.collect() }
    viewModel.sendMessage("Test message")
    testScheduler.advanceUntilIdle()
    assertTrue(fakeRepo.messagesSent.contains("Test message"))
  }

  @Test
  fun tasksViewModel_initialTasksLoaded() = runTest {
    val fakeRepo = FakeHermesRepository()
    val viewModel = TasksViewModel(fakeRepo)
    backgroundScope.launch(testDispatcher) { viewModel.tasks.collect() }
    testScheduler.advanceUntilIdle()
    val tasks = viewModel.tasks.value
    assertEquals(1, tasks.size)
    assertEquals("task-1", tasks.first().id)
  }
}

private class FakeHermesRepository : DataRepository {
  val messagesSent = mutableListOf<String>()

  private val _messages = MutableStateFlow<List<ChatMessage>>(
    listOf(ChatMessage(role = "assistant", content = "Hello!"))
  )
  override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _isStreaming = MutableStateFlow(false)
  override val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

  private val _activeThinking = MutableStateFlow<String?>(null)
  override val activeThinking: StateFlow<String?> = _activeThinking.asStateFlow()

  private val _thinkingPhase = MutableStateFlow(ThinkingPhase.IDLE)
  override val thinkingPhase: StateFlow<ThinkingPhase> = _thinkingPhase.asStateFlow()

  private val _tasks = MutableStateFlow<List<TaskDto>>(
    listOf(TaskDto(id = "task-1", title = "Test Task", status = "completed"))
  )
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

  private val _approvals = MutableStateFlow<List<ApprovalDto>>(emptyList())
  override val approvals: StateFlow<List<ApprovalDto>> = _approvals.asStateFlow()

  private val _hostStatus = MutableStateFlow<HostStatusDto?>(null)
  override val hostStatus: StateFlow<HostStatusDto?> = _hostStatus.asStateFlow()

  private val _knowledgeSources = MutableStateFlow<List<KnowledgeSourceDto>>(emptyList())
  override val knowledgeSources: StateFlow<List<KnowledgeSourceDto>> = _knowledgeSources.asStateFlow()

  private val _directoryServers = MutableStateFlow<List<DirectoryServerItemDto>>(emptyList())
  override val directoryServers: StateFlow<List<DirectoryServerItemDto>> = _directoryServers.asStateFlow()

  private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
  override val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

  override val isLoggedIn: kotlinx.coroutines.flow.Flow<Boolean> = MutableStateFlow(true).asStateFlow()
  override val currentUserId: kotlinx.coroutines.flow.Flow<String> = MutableStateFlow("test-user").asStateFlow()
  override suspend fun logout() { }
  override suspend fun refreshAuthConfig(): Result<AuthConfigResponse> = Result.success(AuthConfigResponse())

  override fun sendMessage(
    content: String,
    model: String,
    attachments: List<ChatAttachment>,
    webSearch: Boolean,
    memory: Boolean
  ) {
    messagesSent.add(content)
  }

  override fun stopGeneration() { }
  override fun fetchTasks() { }
  override fun createNewTask(title: String, prompt: String) { }
  override fun pauseTask(taskId: String) { }
  override fun resumeTask(taskId: String) { }
  override fun cancelTask(taskId: String) { }
  override fun fetchSessions() { }
  override fun loadSession(sessionId: String) { }
  override fun syncActiveSession() { }
  override fun deleteSession(sessionId: String) { }
  override fun updateSessionTitle(sessionId: String, newTitle: String) { }
  override fun fetchProjects() { }
  override fun createNewProject(name: String, description: String) { }
  override fun fetchModels() { }
  override fun clearMessages() {
    _messages.value = emptyList()
  }
  override fun fetchApprovals() { }
  override fun approveRequest(approvalId: String) { }
  override fun denyRequest(approvalId: String) { }
  override fun fetchHostStatus() { }
  override fun fetchKnowledgeSources() { }
  override fun fetchDirectoryServers() { }
  override fun connectTerminalPty() { }
  override fun sendTerminalInput(input: String) { }
  override fun disconnectTerminalPty() { }
  override fun getPreferencesManager(): PreferencesManager? = null

  override suspend fun loginWithGoogle(idToken: String, displayName: String, email: String, avatar: String): Result<VerifyGoogleResponse> {
    return Result.success(VerifyGoogleResponse(success = true))
  }

  override val googleClientId: kotlinx.coroutines.flow.Flow<String> = MutableStateFlow("fake-client-id").asStateFlow()
  override val currentServerUrl: kotlinx.coroutines.flow.Flow<String> = MutableStateFlow("https://test.hermes.space").asStateFlow()
  override suspend fun setGoogleClientId(clientId: String) { }
  override suspend fun setServerBaseUrl(url: String) { }
}
