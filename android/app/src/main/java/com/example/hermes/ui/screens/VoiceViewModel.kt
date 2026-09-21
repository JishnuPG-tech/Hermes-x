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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import java.util.Locale
import java.util.Queue
import java.util.UUID

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

    private val _statusText = MutableStateFlow("Hold tight, connecting…")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _selectedVoice = MutableStateFlow("en-US-ChristopherNeural")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _selectedModel = MutableStateFlow("Hermes Smart")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    private var currentPersona: String = "Rounded"
    private var currentLanguage: String = "English (United Kingdom)"
    private var currentPace: String = "Normal"

    // ---------- WebSocket (primary mode) ----------
    private var webSocket: WebSocket? = null
    private var wsConnected = false

    // Audio playback pipeline (WS mode)
    private var currentAudioBuffer = java.io.ByteArrayOutputStream()
    private val audioQueue: Queue<ByteArray> = LinkedList()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false

    // ---------- Native Android (fallback mode) ----------
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // ---------- Init ----------
    init {
        viewModelScope.launch {
            try {
                currentPersona = prefs.voicePersona.first()
                currentLanguage = prefs.voiceLanguage.first()
                currentPace = prefs.voicePace.first()
                _selectedVoice.value = when (currentPersona) {
                    "Airy"   -> "en-US-AriaNeural"
                    "Mellow" -> "en-US-GuyNeural"
                    "Glassy" -> "en-US-JennyNeural"
                    "Brass"  -> "en-US-EricNeural"
                    else     -> "en-US-ChristopherNeural"
                }
            } catch (_: Exception) {}
            initTts()
            initSpeechRecognizer()
            connect()
        }
    }

    // ---------- TextToSpeech (native fallback) ----------
    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(when (currentPace) {
                    "Slow" -> 0.85f
                    "Fast" -> 1.25f
                    else   -> 1.0f
                })
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        if (!_isMuted.value) {
                            _voiceState.value = VoiceState.LISTENING
                            _statusText.value = "Listening…"
                            mainHandler.post { startListening() }
                        }
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        if (!_isMuted.value) {
                            _voiceState.value = VoiceState.LISTENING
                            _statusText.value = "Listening…"
                            mainHandler.post { startListening() }
                        }
                    }
                })
                ttsReady = true
            }
        }
    }

    private fun speakWithTts(text: String) {
        if (!ttsReady || text.isBlank()) return
        _voiceState.value = VoiceState.SPEAKING
        _statusText.value = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes_${UUID.randomUUID()}")
    }

    // ---------- SpeechRecognizer ----------
    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
                            }
                            override fun onBeginningOfSpeech() {
                                if (isPlayingAudio) interruptAndBargeIn()
                            }
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            private var errorCount = 0

                            override fun onError(error: Int) {
                                // 7 = ERROR_NO_MATCH, 6 = ERROR_SPEECH_TIMEOUT
                                // 4 = ERROR_SERVER, 5 = ERROR_CLIENT
                                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                    errorCount = 0
                                    if (!_isMuted.value && !isPlayingAudio) {
                                        mainHandler.postDelayed({ startListening() }, 600)
                                    }
                                } else {
                                    errorCount++
                                    if (errorCount > 2) {
                                        // Stop retrying and require manual tap
                                        errorCount = 0
                                        _voiceState.value = VoiceState.IDLE
                                        _statusText.value = "Tap to speak"
                                    } else {
                                        if (!_isMuted.value && !isPlayingAudio) {
                                            mainHandler.postDelayed({ startListening() }, 600)
                                        }
                                    }
                                }
                            }
                            override fun onResults(results: Bundle?) {
                                errorCount = 0
                                val spoken = results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                if (!spoken.isNullOrBlank() && !_isMuted.value) {
                                    sendTextInput(spoken)
                                } else if (!_isMuted.value && !isPlayingAudio) {
                                    startListening()
                                }
                            }
                            override fun onPartialResults(partialResults: Bundle?) {
                                val partial = partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                if (!partial.isNullOrBlank()) {
                                    if (isPlayingAudio) interruptAndBargeIn()
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
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
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

    // ---------- WebSocket (primary path) ----------
    private fun sendSessionOpen() {
        val speedValue = when (currentPace) {
            "Slow" -> 0.85
            "Fast" -> 1.25
            else   -> 1.0
        }
        val payload = JSONObject().apply {
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
        webSocket?.send(payload.toString())
    }

    fun connect() {
        _voiceState.value = VoiceState.CONNECTING
        _statusText.value = "Hold tight, connecting…"

        viewModelScope.launch {
            try {
                webSocket = apiClient.connectVoiceWebSocket(
                    listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            wsConnected = true
                            _voiceState.value = VoiceState.LISTENING
                            _statusText.value = "Listening…"
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
                            wsConnected = false
                            // Fall back to native Android voice (STT + TTS + HTTP)
                            _voiceState.value = VoiceState.LISTENING
                            _statusText.value = "Listening…"
                            mainHandler.post { startListening() }
                        }
                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            wsConnected = false
                            _voiceState.value = VoiceState.DISCONNECTED
                            _statusText.value = "Disconnected"
                        }
                    },
                    persona = currentPersona,
                    language = currentLanguage,
                    pace = currentPace
                )
            } catch (e: Exception) {
                wsConnected = false
                // Graceful fallback — use native Android voice
                _voiceState.value = VoiceState.LISTENING
                _statusText.value = "Listening…"
                mainHandler.post { startListening() }
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
                                if (_statusText.value.startsWith("Thinking") ||
                                    _statusText.value.startsWith("Hold tight")) {
                                    _statusText.value = "Listening…"
                                }
                                startListening()
                            }
                        }
                        "thinking" -> {
                            _voiceState.value = VoiceState.THINKING
                            _statusText.value = "Thinking…"
                            stopListening()
                        }
                        "speaking" -> {
                            _voiceState.value = VoiceState.SPEAKING
                            stopListening()
                        }
                        "idle" -> {
                            if (!isPlayingAudio) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
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
                        // If WS audio doesn't arrive, TTS will speak it
                        if (!wsConnected) speakWithTts(phrase)
                    }
                }
                "audio_start" -> { currentAudioBuffer.reset() }
                "audio_end" -> {
                    val audioData = currentAudioBuffer.toByteArray()
                    if (audioData.isNotEmpty()) enqueueAudio(audioData)
                    currentAudioBuffer.reset()
                }
                "task_update" -> {
                    val summary = json.optString("action_summary")
                    if (summary.isNotBlank()) _statusText.value = summary
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
        if (!isPlayingAudio) playNextAudio()
    }

    @Synchronized
    private fun playNextAudio() {
        val nextChunk = audioQueue.poll()
        if (nextChunk == null) {
            isPlayingAudio = false
            _voiceState.value = VoiceState.LISTENING
            _statusText.value = "Listening…"
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
                withContext(Dispatchers.Main) { playNextAudio() }
            }
        }
    }

    // ---------- sendTextInput — routes to WS or native HTTP ----------
    fun sendTextInput(text: String) {
        if (text.isBlank()) return
        interruptAndBargeIn()

        val trimmed = text.trim()
        val wakeWordPattern = Regex("""^(?:hey\s+)?hermes[,\s]*(.*)""", RegexOption.IGNORE_CASE)
        val match = wakeWordPattern.find(trimmed)
        val query = if (match != null) {
            val rest = match.groupValues[1].trim()
            if (rest.isBlank()) {
                speakWithTts("Yes, I'm listening! What's on your mind?")
                return
            }
            rest
        } else {
            trimmed
        }

        _voiceState.value = VoiceState.THINKING
        _statusText.value = "Thinking…"

        if (wsConnected && webSocket != null) {
            // Primary: send to WS
            val payload = JSONObject().apply {
                put("type", "text_input")
                put("text", query)
                put("model", _selectedModel.value)
                put("barge_in", true)
            }
            webSocket?.send(payload.toString())
        } else {
            // Fallback: send to HTTP chat API and read response via TTS
            viewModelScope.launch {
                try {
                    val response = apiClient.sendChatMessageFallback(query, _selectedModel.value)
                    if (response.isNotBlank()) {
                        speakWithTts(response)
                    } else {
                        _voiceState.value = VoiceState.LISTENING
                        _statusText.value = "Listening…"
                        mainHandler.post { startListening() }
                    }
                } catch (e: Exception) {
                    _voiceState.value = VoiceState.LISTENING
                    _statusText.value = "Listening…"
                    mainHandler.post { startListening() }
                }
            }
        }
    }

    fun setVoice(voiceId: String) {
        _selectedVoice.value = voiceId
        if (wsConnected) sendSessionOpen()
    }

    fun interruptAndBargeIn() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            audioQueue.clear()
            currentAudioBuffer.reset()
            isPlayingAudio = false
            tts?.stop()

            if (wsConnected) {
                val cancelPayload = JSONObject().apply {
                    put("type", "command_cancel")
                    put("scope", "current")
                }
                webSocket?.send(cancelPayload.toString())
            }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            _voiceState.value = VoiceState.MUTED
            _statusText.value = "Muted"
            stopListening()
            tts?.stop()
        } else {
            _voiceState.value = VoiceState.LISTENING
            _statusText.value = "Listening…"
            if (!isPlayingAudio) startListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { webSocket?.close(1000, "Screen closed") } catch (_: Exception) {}
        webSocket = null
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        tts?.shutdown()
        tts = null
        mainHandler.post {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
