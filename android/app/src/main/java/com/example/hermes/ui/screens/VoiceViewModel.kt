package com.example.hermes.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.voice.EngineVoiceState
import com.example.hermes.voice.VoiceEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

class VoiceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val engine = VoiceEngine.getInstance(application)

    init {
        engine.start()
    }

    val voiceState: StateFlow<VoiceState> = engine.voiceState
        .map { engineState ->
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoiceState.CONNECTING)

    val statusText: StateFlow<String> = engine.statusText
    val liveRms: StateFlow<Float> = engine.liveRms
    val isMuted: StateFlow<Boolean> = engine.isMuted
    val selectedVoice: StateFlow<String> = engine.selectedVoice
    val selectedModel: StateFlow<String> = engine.selectedModel

    fun setModel(model: String) {
        engine.setModel(model)
    }

    fun setVoice(voiceId: String) {
        engine.setVoice(voiceId)
    }

    fun startListening() {
        engine.interruptAndBargeIn()
    }

    fun stopListening() {
        // Full duplex engine with AEC manages turn boundaries continuously
    }

    fun toggleMute() {
        engine.toggleMute()
    }

    fun interruptAndBargeIn() {
        engine.interruptAndBargeIn()
    }

    fun sendTextInput(text: String) {
        engine.sendTextQuery(text)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
