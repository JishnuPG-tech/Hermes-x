package com.example.hermes.voice

import android.content.Context
import android.util.Log
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

enum class EngineVoiceState {
    CONNECTING,
    CONNECTED,
    LISTENING,
    USER_SPEAKING,
    THINKING,
    SPEAKING,
    MUTED,
    ERROR,
    DISCONNECTED
}

/**
 * VoiceEngine — Central Authoritative Realtime Voice Engine
 *
 * Implements a modern 2026 duplex audio architecture:
 * 1. Continuous 16kHz PCM capture via PcmAudioRecorder with hardware AEC/NS/AGC.
 * 2. Streaming binary PCM transmission over WebSocket to Hermes Gateway.
 * 3. Low-latency sequential audio playback via StreamAudioPlayer.
 * 4. Hardware-assisted sub-150ms acoustic barge-in cancellation.
 * 5. Generation-guarded WebSocket connection management (no duplicate listeners).
 */
class VoiceEngine private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "VoiceEngine"

        @Volatile
        private var instanceRef: VoiceEngine? = null

        fun getInstance(context: Context): VoiceEngine {
            return instanceRef ?: synchronized(this) {
                instanceRef ?: VoiceEngine(context.applicationContext).also { instanceRef = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val apiClient = HermesApiClient.instance
    private val prefs = PreferencesManager.getInstance(appContext)

    // Authoritative state flows
    private val _voiceState = MutableStateFlow(EngineVoiceState.CONNECTING)
    val voiceState: StateFlow<EngineVoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Initializing Hermes...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _liveRms = MutableStateFlow(0f)
    val liveRms: StateFlow<Float> = _liveRms.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _selectedVoice = MutableStateFlow("en-US-ChristopherNeural")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _selectedModel = MutableStateFlow("hermes-agent")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    // Subsystems
    private val player = StreamAudioPlayer(
        context = appContext,
        onPlaybackStarted = {
            if (_voiceState.value != EngineVoiceState.MUTED) {
                _voiceState.value = EngineVoiceState.SPEAKING
            }
        },
        onPlaybackCompleted = {
            if (_voiceState.value != EngineVoiceState.MUTED) {
                _voiceState.value = EngineVoiceState.LISTENING
                _statusText.value = "Listening…"
            }
        }
    )

    private val recorder = PcmAudioRecorder(
        sampleRate = 16000,
        chunkDurationMs = 50,
        onChunkRecorded = { chunk, rms ->
            handlePcmChunk(chunk, rms)
        }
    )

    // Connection ownership & generation counter
    private val connectionGeneration = AtomicInteger(0)
    private var activeWebSocket: WebSocket? = null
    private var isEngineStarted = false
    private var reconnectJob: kotlinx.coroutines.Job? = null
    private var hasSessionGreeted = false
    private var activeTurnId: String? = null

    // Downstream audio framing buffer
    private val audioAccumulator = ByteArrayOutputStream()

    // User metadata cache
    private var currentPersona: String = "Rounded"
    private var currentLanguage: String = "English (United Kingdom)"
    private var currentPace: String = "Normal"
    private var currentUserName: String = "Jishnu"

    fun start() {
        if (isEngineStarted) return
        isEngineStarted = true

        player.start(scope)
        recorder.start(scope)

        scope.launch {
            try {
                currentPersona = prefs.voicePersona.first()
                currentLanguage = prefs.voiceLanguage.first()
                currentPace = prefs.voicePace.first()
                val rawName = prefs.userName.first()
                val firstName = rawName.trim().split("\\s+".toRegex()).firstOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Jishnu"
                currentUserName = firstName

                _selectedVoice.value = when (currentPersona) {
                    "Airy"   -> "en-US-AriaNeural"
                    "Mellow" -> "en-US-GuyNeural"
                    "Glassy" -> "en-US-JennyNeural"
                    "Brass"  -> "en-US-EricNeural"
                    else     -> "en-US-ChristopherNeural"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading voice preferences: ${e.message}")
            }

            connectWebSocket()
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun setVoice(voiceId: String) {
        _selectedVoice.value = voiceId
        sendSessionOpen(activeWebSocket)
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            _voiceState.value = EngineVoiceState.MUTED
            _statusText.value = "Muted"
            player.stopAndFlush()
        } else {
            _voiceState.value = EngineVoiceState.LISTENING
            _statusText.value = "Listening…"
        }
    }

    fun interruptAndBargeIn() {
        Log.i(TAG, "Barge-in triggered: flushing audio player and canceling server turn")
        activeTurnId = null
        player.stopAndFlush()
        audioAccumulator.reset()

        activeWebSocket?.let { ws ->
            val cancelPayload = JSONObject().apply {
                put("type", "command_cancel")
                put("scope", "turn")
            }
            ws.send(cancelPayload.toString())
        }

        if (!_isMuted.value) {
            _voiceState.value = EngineVoiceState.LISTENING
            _statusText.value = "Listening…"
        }
    }

    fun sendTextQuery(text: String) {
        if (text.isBlank()) return
        interruptAndBargeIn()

        _voiceState.value = EngineVoiceState.THINKING
        _statusText.value = "Thinking…"

        activeWebSocket?.let { ws ->
            val payload = JSONObject().apply {
                put("type", "text_input")
                put("text", text.trim())
                put("model", _selectedModel.value)
                put("barge_in", true)
            }
            ws.send(payload.toString())
        }
    }

    /**
     * Handle incoming 16kHz PCM chunk from PcmAudioRecorder.
     */
    private fun handlePcmChunk(chunk: ByteArray, rms: Float) {
        if (_isMuted.value) return

        _liveRms.value = rms

        // Self-listening loop elimination:
        // While assistant is actively playing through the loudspeaker, suppress mic streaming.
        // Only if user voice energy crosses the elevated barge-in threshold (RMS > 0.22f) do we interrupt.
        if (player.isCurrentlyPlaying()) {
            if (rms > 0.22f) {
                Log.i(TAG, "User speech detected over loudspeaker (RMS=$rms). Triggering barge-in.")
                interruptAndBargeIn()
                _voiceState.value = EngineVoiceState.USER_SPEAKING
                _statusText.value = "Listening…"
                activeWebSocket?.send(chunk.toByteString())
            }
            return
        }

        // Normal listening state: stream binary PCM chunk to WebSocket
        activeWebSocket?.send(chunk.toByteString())
    }

    private fun connectWebSocket() {
        val gen = connectionGeneration.incrementAndGet()
        Log.i(TAG, "Connecting Voice WebSocket (generation=$gen)...")

        _voiceState.value = EngineVoiceState.CONNECTING
        _statusText.value = "Connecting to Hermes..."

        try {
            activeWebSocket?.close(1000, "Opening new generation connection")
            activeWebSocket = null

            activeWebSocket = apiClient.connectVoiceWebSocket(
                listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (gen != connectionGeneration.get()) {
                            Log.d(TAG, "Stale WebSocket onOpen (gen=$gen, current=${connectionGeneration.get()}). Closing.")
                            webSocket.close(1000, "Stale")
                            return
                        }
                        Log.i(TAG, "Voice WebSocket connected (gen=$gen)")
                        _voiceState.value = EngineVoiceState.CONNECTED
                        sendSessionOpen(webSocket)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (gen != connectionGeneration.get()) return
                        handleServerJsonMessage(text)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        if (gen != connectionGeneration.get()) return
                        if (activeTurnId == null) {
                            // Turn was cancelled or barge-in occurred; discard obsolete audio bytes
                            return
                        }
                        audioAccumulator.write(bytes.toByteArray())
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (gen != connectionGeneration.get()) return
                        Log.w(TAG, "Voice WebSocket failed (gen=$gen): ${t.message}. Retrying in 2s...")
                        _voiceState.value = EngineVoiceState.ERROR
                        _statusText.value = "Reconnecting..."
                        scheduleReconnect(2000L)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (gen != connectionGeneration.get()) return
                        Log.i(TAG, "Voice WebSocket closed (gen=$gen, code=$code): $reason")
                        _voiceState.value = EngineVoiceState.DISCONNECTED
                        _statusText.value = "Reconnecting..."
                        scheduleReconnect(1500L)
                    }
                },
                persona = currentPersona,
                language = currentLanguage,
                pace = currentPace
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing WebSocket connection", e)
            scheduleReconnect(3000L)
        }
    }

    private fun scheduleReconnect(delayMs: Long) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            kotlinx.coroutines.delay(delayMs)
            if (isEngineStarted && !_isMuted.value) {
                connectWebSocket()
            }
        }
    }

    private fun sendSessionOpen(ws: WebSocket?) {
        val speedValue = when (currentPace) {
            "Slow" -> 0.85
            "Fast" -> 1.25
            else   -> 1.0
        }
        // One-time greeting rule: only greet on the very first connection of this voice session
        val shouldGreet = !hasSessionGreeted
        hasSessionGreeted = true

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
                put("greet", shouldGreet)
            })
        }
        ws?.send(payload.toString())
    }

    private fun handleServerJsonMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "assistant_state" -> {
                    val state = json.optString("state")
                    when (state) {
                        "listening" -> {
                            if (!player.isCurrentlyPlaying() && !_isMuted.value) {
                                _voiceState.value = EngineVoiceState.LISTENING
                                _statusText.value = "Listening…"
                            }
                        }
                        "thinking" -> {
                            _voiceState.value = EngineVoiceState.THINKING
                            _statusText.value = "Thinking…"
                        }
                        "speaking" -> {
                            _voiceState.value = EngineVoiceState.SPEAKING
                        }
                        "idle" -> {
                            if (!player.isCurrentlyPlaying() && !_isMuted.value) {
                                _voiceState.value = EngineVoiceState.LISTENING
                                _statusText.value = "Listening…"
                            }
                        }
                    }
                }
                "assistant_text" -> {
                    val phrase = json.optString("text")
                    if (phrase.isNotBlank()) {
                        _statusText.value = phrase
                    }
                }
                "audio_start" -> {
                    val turnId = json.optString("turn_id").ifBlank { null }
                    activeTurnId = turnId
                    audioAccumulator.reset()
                }
                "audio_end" -> {
                    val turnId = json.optString("turn_id").ifBlank { null }
                    if (turnId != null && activeTurnId != null && turnId != activeTurnId) {
                        Log.d(TAG, "Discarding audio_end for obsolete turn $turnId (active: $activeTurnId)")
                        audioAccumulator.reset()
                        return
                    }
                    val audioBytes = audioAccumulator.toByteArray()
                    if (audioBytes.isNotEmpty() && activeTurnId != null) {
                        player.enqueue(audioBytes)
                    }
                    audioAccumulator.reset()
                }
                "task_update" -> {
                    val summary = json.optString("action_summary")
                    if (summary.isNotBlank()) {
                        _statusText.value = summary
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing server JSON message: ${e.message}")
        }
    }

    fun stop() {
        isEngineStarted = false
        connectionGeneration.incrementAndGet()
        recorder.stop()
        player.release()
        try {
            activeWebSocket?.close(1000, "Engine stopped")
        } catch (_: Exception) {}
        activeWebSocket = null
        _voiceState.value = EngineVoiceState.DISCONNECTED
    }
}
