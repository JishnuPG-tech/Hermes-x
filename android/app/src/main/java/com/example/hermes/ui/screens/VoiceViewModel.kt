package com.example.hermes.ui.screens

import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.HermesApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedList
import java.util.Queue

import com.example.hermes.data.PreferencesManager
import kotlinx.coroutines.flow.first

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

    private val apiClient: HermesApiClient = HermesApiClient.instance
    private val prefs: PreferencesManager = PreferencesManager.getInstance(application)

    private val _voiceState = MutableStateFlow(VoiceState.CONNECTING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Hold tight, connecting...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _selectedVoice = MutableStateFlow("en-US-ChristopherNeural")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private var currentPersona: String = "Rounded"
    private var currentLanguage: String = "English (United Kingdom)"
    private var currentPace: String = "Normal"

    private var webSocket: WebSocket? = null

    // Audio Playback Pipeline
    private var currentAudioBuffer = java.io.ByteArrayOutputStream()
    private val audioQueue: Queue<ByteArray> = LinkedList()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false

    // Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        viewModelScope.launch {
            try {
                currentPersona = prefs.voicePersona.first()
                currentLanguage = prefs.voiceLanguage.first()
                currentPace = prefs.voicePace.first()

                // Map persona to neural voice profile
                _selectedVoice.value = when (currentPersona) {
                    "Airy" -> "en-US-AriaNeural"
                    "Mellow" -> "en-US-GuyNeural"
                    "Glassy" -> "en-US-JennyNeural"
                    "Brass" -> "en-US-EricNeural"
                    else -> "en-US-ChristopherNeural"
                }
            } catch (_: Exception) {}
            connect()
        }
        initSpeechRecognizer()
    }

    fun setVoice(voiceId: String) {
        _selectedVoice.value = voiceId
        sendSessionOpen()
    }

    private fun sendSessionOpen() {
        val speedValue = when (currentPace) {
            "Slow" -> 0.85
            "Fast" -> 1.25
            else -> 1.0
        }
        val openPayload = JSONObject().apply {
            put("type", "session_open")
            put("voice", _selectedVoice.value)
            put("speed", speedValue)
            put("sample_rate", 24000)
            put("client_metadata", JSONObject().apply {
                put("persona", currentPersona)
                put("language", currentLanguage)
                put("pace", currentPace)
            })
        }
        webSocket?.send(openPayload.toString())
    }

    fun connect() {
        _voiceState.value = VoiceState.CONNECTING
        _statusText.value = "Hold tight, connecting..."

        viewModelScope.launch {
            try {
                webSocket = apiClient.connectVoiceWebSocket(
                    listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            _voiceState.value = VoiceState.LISTENING
                            _statusText.value = "Listening..."
                            sendSessionOpen()
                            startListening()
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            handleTextMessage(text)
                        }

                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            handleBinaryAudio(bytes.toByteArray())
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            _voiceState.value = VoiceState.ERROR
                            _statusText.value = "Voice service standby"
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            _voiceState.value = VoiceState.DISCONNECTED
                            _statusText.value = "Disconnected"
                        }
                    },
                    persona = currentPersona,
                    language = currentLanguage,
                    pace = currentPace
                )
            } catch (e: Exception) {
                _voiceState.value = VoiceState.ERROR
                _statusText.value = "Voice service standby"
            }
        }
    }

    private fun handleTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "assistant_state" -> {
                    val state = json.optString("state")
                    when (state) {
                        "listening" -> {
                            if (!isPlayingAudio) {
                                _voiceState.value = VoiceState.LISTENING
                                if (_statusText.value.startsWith("Thinking") || _statusText.value.startsWith("Hold tight")) {
                                    _statusText.value = "Listening..."
                                }
                                startListening()
                            }
                        }
                        "thinking" -> {
                            _voiceState.value = VoiceState.THINKING
                            _statusText.value = "Thinking..."
                            stopListening()
                        }
                        "speaking" -> {
                            _voiceState.value = VoiceState.SPEAKING
                            stopListening()
                        }
                        "idle" -> {
                            if (!isPlayingAudio) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening..."
                                startListening()
                            }
                        }
                    }
                }
                "assistant_text" -> {
                    val phrase = json.optString("text")
                    if (phrase.isNotBlank()) {
                        _voiceState.value = VoiceState.SPEAKING
                        _statusText.value = phrase
                    }
                }
                "audio_start" -> {
                    currentAudioBuffer.reset()
                }
                "audio_end" -> {
                    val audioData = currentAudioBuffer.toByteArray()
                    if (audioData.isNotEmpty()) {
                        enqueueAudio(audioData)
                    }
                    currentAudioBuffer.reset()
                }
                "task_update" -> {
                    val summary = json.optString("action_summary")
                    if (summary.isNotBlank()) {
                        _statusText.value = summary
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleBinaryAudio(bytes: ByteArray) {
        currentAudioBuffer.write(bytes)
    }

    @Synchronized
    private fun enqueueAudio(audioData: ByteArray) {
        audioQueue.add(audioData)
        if (!isPlayingAudio) {
            playNextAudio()
        }
    }

    @Synchronized
    private fun playNextAudio() {
        val nextChunk = audioQueue.poll()
        if (nextChunk == null) {
            isPlayingAudio = false
            _voiceState.value = VoiceState.LISTENING
            _statusText.value = "Listening..."
            startListening()
            return
        }

        isPlayingAudio = true
        _voiceState.value = VoiceState.SPEAKING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File.createTempFile("hermes_tts_", ".mp3", getApplication<Application>().cacheDir)
                FileOutputStream(tempFile).use { it.write(nextChunk) }

                withContext(Dispatchers.Main) {
                    try {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(tempFile.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                tempFile.delete()
                                playNextAudio()
                            }
                            setOnErrorListener { _, _, _ ->
                                tempFile.delete()
                                playNextAudio()
                                true
                            }
                            start()
                        }
                    } catch (e: Exception) {
                        tempFile.delete()
                        playNextAudio()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    playNextAudio()
                }
            }
        }
    }

    fun interruptAndBargeIn() {
        // Barge-in: stop playback immediately within <300ms SLA
        viewModelScope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            audioQueue.clear()
            currentAudioBuffer.reset()
            isPlayingAudio = false
            _voiceState.value = VoiceState.LISTENING
            _statusText.value = "Listening..."

            val cancelPayload = JSONObject().apply {
                put("type", "command_cancel")
                put("scope", "current")
            }
            webSocket?.send(cancelPayload.toString())
            startListening()
        }
    }

    fun sendTextInput(text: String) {
        if (text.isBlank()) return
        interruptAndBargeIn()
        _voiceState.value = VoiceState.THINKING
        _statusText.value = "Thinking..."

        val inputPayload = JSONObject().apply {
            put("type", "text_input")
            put("text", text)
            put("barge_in", true)
        }
        webSocket?.send(inputPayload.toString())
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            _statusText.value = "Muted"
            stopListening()
        } else {
            _statusText.value = if (_voiceState.value == VoiceState.SPEAKING) "Hermes speaking..." else "Listening..."
            if (!isPlayingAudio) {
                startListening()
            }
        }
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {
                                if (isPlayingAudio) {
                                    interruptAndBargeIn()
                                }
                            }
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                if (!_isMuted.value && !isPlayingAudio) {
                                    mainHandler.postDelayed({ startListening() }, 1000)
                                }
                            }
                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val spoken = matches?.firstOrNull()
                                if (!spoken.isNullOrBlank() && !_isMuted.value) {
                                    sendTextInput(spoken)
                                } else if (!_isMuted.value && !isPlayingAudio) {
                                    startListening()
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                val partial = matches?.firstOrNull()
                                if (!partial.isNullOrBlank()) {
                                    if (isPlayingAudio) {
                                        interruptAndBargeIn()
                                    }
                                    _statusText.value = partial
                                }
                            }
                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun startListening() {
        if (_isMuted.value || isPlayingAudio) return
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {}
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            webSocket?.close(1000, "Screen closed")
        } catch (_: Exception) {}
        webSocket = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
