package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.HermesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

enum class VoiceState {
    CONNECTING,
    CONNECTED,
    LISTENING,
    SPEAKING,
    MUTED,
    ERROR,
    DISCONNECTED
}

class VoiceViewModel(
    private val apiClient: HermesApiClient = HermesApiClient.instance
) : ViewModel() {

    private val _voiceState = MutableStateFlow(VoiceState.CONNECTING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Hold tight, connecting...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var webSocket: WebSocket? = null

    init {
        connect()
    }

    fun connect() {
        _voiceState.value = VoiceState.CONNECTING
        _statusText.value = "Hold tight, connecting..."

        viewModelScope.launch {
            try {
                webSocket = apiClient.connectVoiceWebSocket(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _voiceState.value = VoiceState.LISTENING
                        _statusText.value = "Listening..."
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        _voiceState.value = VoiceState.SPEAKING
                        _statusText.value = "Hermes speaking..."
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        _voiceState.value = VoiceState.SPEAKING
                        _statusText.value = "Hermes speaking..."
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _voiceState.value = VoiceState.ERROR
                        _statusText.value = "Voice node initializing (ready soon)"
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _voiceState.value = VoiceState.DISCONNECTED
                        _statusText.value = "Disconnected"
                    }
                })
            } catch (e: Exception) {
                _voiceState.value = VoiceState.ERROR
                _statusText.value = "Voice service standby"
            }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            _statusText.value = "Muted"
        } else {
            _statusText.value = if (_voiceState.value == VoiceState.SPEAKING) "Hermes speaking..." else "Listening..."
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            webSocket?.close(1000, "Screen closed")
        } catch (_: Exception) {}
        webSocket = null
    }
}
