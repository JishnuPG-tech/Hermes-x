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
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
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

    companion object {
        private const val TAG = "VoiceViewModel"
    }

    private val apiClient: HermesApiClient = HermesApiClient.instance
    private val prefs: PreferencesManager = PreferencesManager.getInstance(application)

    private val _voiceState = MutableStateFlow(VoiceState.SPEAKING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Hi Jishnu! How can I help you today?")
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
    private var currentUserName: String = "Jishnu"

    // ---------- WebSocket (primary mode) ----------
    private var webSocket: WebSocket? = null
    private var wsConnected = false

    // Audio playback pipeline (WS mode)
    private var currentAudioBuffer = java.io.ByteArrayOutputStream()
    private val audioQueue: Queue<ByteArray> = LinkedList()
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false

    // ---------- Native Android (fallback & instant greeting) ----------
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var hasSpokenInitialGreeting = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _liveRms = MutableStateFlow(0f)
    val liveRms: StateFlow<Float> = _liveRms.asStateFlow()

    private var lastAssistantPhrase: String? = null
    private var hasReceivedAudioForTurn: Boolean = false

    // ---------- Init ----------
    init {
        viewModelScope.launch {
            try {
                currentPersona = prefs.voicePersona.first()
                currentLanguage = prefs.voiceLanguage.first()
                currentPace = prefs.voicePace.first()
                val rawName = prefs.userName.first()
                val firstName = rawName.trim().split("\\s+".toRegex()).firstOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Jishnu"
                currentUserName = firstName

                val greeting = "Hi $firstName! How can I help you today?"
                _statusText.value = greeting
                _voiceState.value = VoiceState.SPEAKING

                _selectedVoice.value = when (currentPersona) {
                    "Airy"   -> "en-US-AriaNeural"
                    "Mellow" -> "en-US-GuyNeural"
                    "Glassy" -> "en-US-JennyNeural"
                    "Brass"  -> "en-US-EricNeural"
                    else     -> "en-US-ChristopherNeural"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading voice preferences", e)
            }

            initTts()
            initSpeechRecognizer()
            connect()
        }
    }

    // ---------- TextToSpeech (native instant greeting & fallback) ----------
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
                        stopListening()
                    }
                    override fun onDone(utteranceId: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            if (!_isMuted.value) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
                                startListening()
                            }
                        }
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            if (!_isMuted.value) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
                                startListening()
                            }
                        }
                    }
                })
                ttsReady = true
                triggerInitialGreeting()
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status $status")
                // If TTS fails, transition to listening directly
                _voiceState.value = VoiceState.LISTENING
                _statusText.value = "Listening…"
                startListening()
            }
        }
    }

    private fun triggerInitialGreeting() {
        if (hasSpokenInitialGreeting || !ttsReady) return
        hasSpokenInitialGreeting = true
        val greeting = "Hi $currentUserName! How can I help you today?"
        _statusText.value = greeting
        speakWithTts(greeting)
    }

    private fun speakWithTts(text: String) {
        if (!ttsReady || text.isBlank()) return
        stopListening()
        _voiceState.value = VoiceState.SPEAKING
        _statusText.value = text
        val utteranceId = "hermes_${UUID.randomUUID()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // ---------- SpeechRecognizer (Real-time Streaming & Captions) ----------
    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {
                                if (_voiceState.value == VoiceState.LISTENING) {
                                    _statusText.value = "Listening…"
                                }
                            }
                            override fun onBeginningOfSpeech() {
                                if (isPlayingAudio || _voiceState.value == VoiceState.SPEAKING) {
                                    interruptAndBargeIn()
                                }
                            }
                            override fun onRmsChanged(rmsdB: Float) {
                                if (_voiceState.value == VoiceState.LISTENING) {
                                    _liveRms.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                                }
                            }
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}

                            override fun onError(error: Int) {
                                Log.d(TAG, "SpeechRecognizer onError: $error")
                                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                    try { speechRecognizer?.cancel() } catch (_: Exception) {}
                                }
                                if (!_isMuted.value && !isPlayingAudio && _voiceState.value == VoiceState.LISTENING) {
                                    val retryDelay = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) 300L else 1000L
                                    mainHandler.postDelayed({
                                        if (!_isMuted.value && !isPlayingAudio && _voiceState.value == VoiceState.LISTENING) {
                                            startListening()
                                        }
                                    }, retryDelay)
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                val spoken = results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                if (!spoken.isNullOrBlank() && !_isMuted.value) {
                                    _statusText.value = spoken
                                    sendTextInput(spoken)
                                } else if (!_isMuted.value && !isPlayingAudio && _voiceState.value == VoiceState.LISTENING) {
                                    mainHandler.postDelayed({
                                        if (!_isMuted.value && !isPlayingAudio && _voiceState.value == VoiceState.LISTENING) {
                                            startListening()
                                        }
                                    }, 300)
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val partial = partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                if (!partial.isNullOrBlank() && _voiceState.value == VoiceState.LISTENING) {
                                    _statusText.value = partial
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
            }
        }
    }

    fun startListening() {
        if (_isMuted.value || isPlayingAudio || _voiceState.value == VoiceState.SPEAKING || _voiceState.value == VoiceState.THINKING) return
        _voiceState.value = VoiceState.LISTENING
        _statusText.value = "Listening…"

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication<Application>().packageName)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
            }
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
                put("user_name", currentUserName)
                put("greet", false) // Local instant greeting already handled
            })
        }
        webSocket?.send(payload.toString())
    }

    fun connect() {
        viewModelScope.launch {
            try {
                webSocket = apiClient.connectVoiceWebSocket(
                    listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            wsConnected = true
                            Log.i(TAG, "Voice WebSocket connected successfully")
                            sendSessionOpen()
                            if (hasSpokenInitialGreeting && !isPlayingAudio && _voiceState.value != VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
                                startListening()
                            }
                        }
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            handleTextMessage(text)
                        }
                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            handleBinaryAudio(bytes.toByteArray())
                        }
                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            wsConnected = false
                            Log.w(TAG, "Voice WebSocket failed: ${t.message}. Operating in native voice fallback mode.")
                            if (hasSpokenInitialGreeting && !isPlayingAudio && _voiceState.value != VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.LISTENING
                                _statusText.value = "Listening…"
                                mainHandler.post { startListening() }
                            }
                        }
                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            wsConnected = false
                            Log.i(TAG, "Voice WebSocket closed: $reason")
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
                Log.w(TAG, "Error connecting Voice WebSocket: ${e.message}")
                if (hasSpokenInitialGreeting && !isPlayingAudio && _voiceState.value != VoiceState.SPEAKING) {
                    _voiceState.value = VoiceState.LISTENING
                    _statusText.value = "Listening…"
                    mainHandler.post { startListening() }
                }
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
                            if (!isPlayingAudio && audioQueue.isEmpty() && !_isMuted.value && _voiceState.value != VoiceState.SPEAKING) {
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
                            if (!isPlayingAudio && audioQueue.isEmpty() && !_isMuted.value && _voiceState.value != VoiceState.SPEAKING) {
                                val phrase = lastAssistantPhrase
                                if (!hasReceivedAudioForTurn && !phrase.isNullOrBlank()) {
                                    lastAssistantPhrase = null
                                    speakWithTts(phrase)
                                } else {
                                    _voiceState.value = VoiceState.LISTENING
                                    _statusText.value = "Listening…"
                                    startListening()
                                }
                            }
                        }
                    }
                }
                "assistant_text" -> {
                    val phrase = json.optString("text")
                    if (phrase.isNotBlank()) {
                        _statusText.value = phrase
                        lastAssistantPhrase = phrase
                        if (!wsConnected) speakWithTts(phrase)
                    }
                }
                "audio_start" -> {
                    currentAudioBuffer.reset()
                    hasReceivedAudioForTurn = true
                }
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
            if (!_isMuted.value) {
                _voiceState.value = VoiceState.LISTENING
                _statusText.value = "Listening…"
                startListening()
            } else {
                _voiceState.value = VoiceState.MUTED
                _statusText.value = "Muted"
            }
            return
        }

        isPlayingAudio = true
        _voiceState.value = VoiceState.SPEAKING
        stopListening()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isWav = nextChunk.size >= 4 &&
                    nextChunk[0] == 'R'.code.toByte() &&
                    nextChunk[1] == 'I'.code.toByte() &&
                    nextChunk[2] == 'F'.code.toByte() &&
                    nextChunk[3] == 'F'.code.toByte()
                val ext = if (isWav) ".wav" else ".mp3"
                val tempFile = File.createTempFile("hermes_tts_", ext, getApplication<Application>().cacheDir)
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

    // ---------- sendTextInput — routes to WS or native HTTP fallback ----------
    fun sendTextInput(text: String) {
        if (text.isBlank()) return
        stopListening()
        val wasPlaying = isPlayingAudio || _voiceState.value == VoiceState.SPEAKING
        if (wasPlaying) {
            interruptAndBargeIn()
        }

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
        stopListening()
        hasReceivedAudioForTurn = false
        lastAssistantPhrase = null

        if (wsConnected && webSocket != null) {
            val payload = JSONObject().apply {
                put("type", "text_input")
                put("text", query)
                put("model", "hermes-agent")
                put("barge_in", wasPlaying)
            }
            webSocket?.send(payload.toString())
        } else {
            viewModelScope.launch {
                try {
                    val response = apiClient.sendChatMessageFallback(query, "hermes-agent")
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
        val wasPlaying = isPlayingAudio || _voiceState.value == VoiceState.SPEAKING
        viewModelScope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            audioQueue.clear()
            currentAudioBuffer.reset()
            isPlayingAudio = false
            hasReceivedAudioForTurn = false
            lastAssistantPhrase = null
            tts?.stop()

            if (wsConnected && wasPlaying) {
                val cancelPayload = JSONObject().apply {
                    put("type", "command_cancel")
                    put("scope", "current")
                }
                webSocket?.send(cancelPayload.toString())
            }

            if (!_isMuted.value) {
                _voiceState.value = VoiceState.LISTENING
                _statusText.value = "Listening…"
                startListening()
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
            interruptAndBargeIn()
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
        mainHandler.post {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
            speechRecognizer = null
        }
    }
}
