package com.example.hermes.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.PreferencesManager
import com.example.hermes.voice.EngineVoiceState
import com.example.hermes.voice.HuggingVoiceEngine
import com.example.hermes.voice.VoiceEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VoiceState {
    CONNECTING,
    CONNECTED,
    LISTENING,
    THINKING,
    SPEAKING,
    MUTED,
    ERROR,
    DISCONNECTED
}

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = PreferencesManager.getInstance(application)
    private val classicEngine = VoiceEngine.getInstance(application)
    private val huggingVoiceEngine = HuggingVoiceEngine.getInstance(application)

    private val _voiceMode = MutableStateFlow("hugging_voice")
    val voiceMode: StateFlow<String> = _voiceMode.asStateFlow()

    private fun isHuggingVoice(mode: String): Boolean =
        mode == "hugging_voice" || mode == "apollo"

    init {
        viewModelScope.launch {
            prefs.voiceMode.collect { mode ->
                _voiceMode.value = mode
                if (isHuggingVoice(mode)) {
                    classicEngine.stop()
                    huggingVoiceEngine.start()
                } else {
                    huggingVoiceEngine.stop()
                    classicEngine.start()
                }
            }
        }
    }

    val voiceState: StateFlow<VoiceState> = _voiceMode
        .flatMapLatest { mode ->
            val engineStateFlow = if (isHuggingVoice(mode)) huggingVoiceEngine.voiceState else classicEngine.voiceState
            engineStateFlow.map { engineState ->
                when (engineState) {
                    EngineVoiceState.CONNECTING -> VoiceState.CONNECTING
                    EngineVoiceState.CONNECTED -> VoiceState.CONNECTED
                    EngineVoiceState.LISTENING, EngineVoiceState.USER_SPEAKING -> VoiceState.LISTENING
                    EngineVoiceState.THINKING -> VoiceState.THINKING
                    EngineVoiceState.SPEAKING -> VoiceState.SPEAKING
                    EngineVoiceState.MUTED -> VoiceState.MUTED
                    EngineVoiceState.ERROR -> VoiceState.ERROR
                    EngineVoiceState.DISCONNECTED -> VoiceState.DISCONNECTED
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceState.CONNECTING)

    val statusText: StateFlow<String> = _voiceMode
        .flatMapLatest { mode ->
            if (isHuggingVoice(mode)) huggingVoiceEngine.statusText else classicEngine.statusText
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Initializing Hugging Voice...")

    val liveRms: StateFlow<Float> = _voiceMode
        .flatMapLatest { mode ->
            if (isHuggingVoice(mode)) huggingVoiceEngine.liveRms else classicEngine.liveRms
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    val isMuted: StateFlow<Boolean> = _voiceMode
        .flatMapLatest { mode ->
            if (isHuggingVoice(mode)) huggingVoiceEngine.isMuted else classicEngine.isMuted
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val selectedVoice: StateFlow<String> = _voiceMode
        .flatMapLatest { mode ->
            if (isHuggingVoice(mode)) huggingVoiceEngine.selectedVoice else classicEngine.selectedVoice
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en-US-JennyNeural")

    val selectedModel: StateFlow<String> = _voiceMode
        .flatMapLatest { mode ->
            if (isHuggingVoice(mode)) huggingVoiceEngine.selectedModel else classicEngine.selectedModel
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "hermes-agent")

    fun setModel(model: String) {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.setModel(model) else classicEngine.setModel(model)
    }

    fun setVoice(voiceId: String) {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.setVoice(voiceId) else classicEngine.setVoice(voiceId)
    }

    fun setVoiceMode(mode: String) {
        viewModelScope.launch {
            prefs.setVoiceMode(mode)
        }
    }

    fun startListening() {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.interruptAndBargeIn() else classicEngine.interruptAndBargeIn()
    }

    fun stopListening() {
        // Continuous duplex engine with AEC
    }

    fun toggleMute() {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.toggleMute() else classicEngine.toggleMute()
    }

    fun interruptAndBargeIn() {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.interruptAndBargeIn() else classicEngine.interruptAndBargeIn()
    }

    fun sendTextInput(text: String) {
        if (isHuggingVoice(_voiceMode.value)) huggingVoiceEngine.sendTextQuery(text) else classicEngine.sendTextQuery(text)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
