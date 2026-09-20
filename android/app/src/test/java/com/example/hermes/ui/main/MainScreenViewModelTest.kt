package com.example.hermes.ui.main

import com.example.hermes.data.ChatMessage
import com.example.hermes.data.DataRepository
import com.example.hermes.data.TaskDto
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

  private val _tasks = MutableStateFlow<List<TaskDto>>(
    listOf(TaskDto(id = "task-1", title = "Test Task", status = "completed"))
  )
  override val tasks: StateFlow<List<TaskDto>> = _tasks.asStateFlow()

  override fun sendMessage(content: String, model: String) {
    messagesSent.add(content)
  }

  override fun fetchTasks() { }

  override fun createNewTask(title: String, prompt: String) { }

  override fun clearMessages() {
    _messages.value = emptyList()
  }
}
