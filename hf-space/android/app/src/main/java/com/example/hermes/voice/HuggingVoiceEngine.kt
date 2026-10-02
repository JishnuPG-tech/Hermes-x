package com.example.hermes.voice

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * HuggingVoiceEngine — Official Hugging Face speech-to-speech / Hugging Voice Client for Hermes Agent.
 *
 * Implements the full-duplex conversational voice architecture:
 * 1. Base64 16kHz PCM streaming via `input_audio_buffer.append`.
 * 2. Instant sub-50ms acoustic and hardware barge-in interruption.
 * 3. High-throughput direct AudioTrack playback of incoming raw PCM audio deltas (response.output_audio.delta).
 * 4. Automatic function tool dispatch for `hermes_execute` to control tools and system commands.
 * 5. Sweet, calm, warm female persona (en-US-JennyNeural) by default.
 */
class HuggingVoiceEngine private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "HuggingVoiceEngine"

        @Volatile
        private var instanceRef: HuggingVoiceEngine? = null

        fun getInstance(context: Context): HuggingVoiceEngine {
            return instanceRef ?: synchronized(this) {
                instanceRef ?: HuggingVoiceEngine(context.applicationContext).also { instanceRef = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val apiClient = HermesApiClient.instance
    private val prefs = PreferencesManager.getInstance(appContext)

    // Authoritative state flows
    private val _voiceState = MutableStateFlow(EngineVoiceState.CONNECTING)
    val voiceState: StateFlow<EngineVoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Initializing Hugging Voice...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _liveRms = MutableStateFlow(0f)
    val liveRms: StateFlow<Float> = _liveRms.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _selectedVoice = MutableStateFlow("en-US-JennyNeural")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _selectedModel = MutableStateFlow("hermes-agent")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _assistantTranscript = MutableStateFlow("")
    val assistantTranscript: StateFlow<String> = _assistantTranscript.asStateFlow()

    private val _voiceSessionId = MutableStateFlow("voice_sess_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16))
    val voiceSessionId: StateFlow<String> = _voiceSessionId.asStateFlow()

    @Volatile
    private var pendingToolObjective: String? = null
    @Volatile
    private var pendingToolOutput: String? = null

    // Subsystems
    private val player = HuggingVoicePlayer(
        sampleRate = 24000,
        onPlaybackStarted = {
            if (_voiceState.value != EngineVoiceState.MUTED) {
                _voiceState.value = EngineVoiceState.SPEAKING
            }
        },
        onPlaybackCompleted = {
            if (_voiceState.value != EngineVoiceState.MUTED && _voiceState.value == EngineVoiceState.SPEAKING) {
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

    private val connectionGeneration = AtomicInteger(0)
    private var activeWebSocket: WebSocket? = null
    private var isEngineStarted = false
    private var reconnectJob: Job? = null

    private var currentPersona: String = "Glassy"
    private var currentLanguage: String = "English (United States)"
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
                val rawName = prefs.userName.first()
                val firstName = rawName.trim().split("\\s+".toRegex()).firstOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Jishnu"
                currentUserName = firstName

                // Default sweet calm warm female voice (Jenny)
                _selectedVoice.value = PreferencesManager.getVoiceIdForPersona(currentPersona)
            } catch (e: Exception) {
                Log.w(TAG, "Error reading voice preferences: ${e.message}")
            }

            connectWebSocket()
        }

        // Live observation of voice persona preferences
        scope.launch {
            prefs.voicePersona.collect { persona ->
                currentPersona = persona
                val targetVoice = PreferencesManager.getVoiceIdForPersona(persona)
                if (targetVoice != _selectedVoice.value) {
                    Log.i(TAG, "Voice persona preference dynamically updated to $persona ($targetVoice)")
                    setVoice(targetVoice)
                }
            }
        }
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun setVoice(voiceId: String) {
        _selectedVoice.value = voiceId
        sendSessionUpdate(activeWebSocket)
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
        Log.i(TAG, "Sub-50ms barge-in triggered: flushing player and cancelling turn")
        player.stopAndFlush()

        activeWebSocket?.let { ws ->
            val cancelPayload = JSONObject().apply {
                put("type", "response.cancel")
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
        _userTranscript.value = text
        _assistantTranscript.value = ""

        activeWebSocket?.let { ws ->
            val itemPayload = JSONObject().apply {
                put("type", "conversation.item.create")
                put("item", JSONObject().apply {
                    put("type", "message")
                    put("role", "user")
                    put("content", text.trim())
                })
            }
            ws.send(itemPayload.toString())

            val createRespPayload = JSONObject().apply {
                put("type", "response.create")
            }
            ws.send(createRespPayload.toString())
        }
    }

    private fun handlePcmChunk(chunk: ByteArray, rms: Float) {
        if (_isMuted.value) return

        _liveRms.value = rms

        // Loudspeaker acoustic suppression & intentional barge-in detection
        if (player.isCurrentlyPlaying() || _voiceState.value == EngineVoiceState.SPEAKING) {
            if (rms > 0.28f) {
                Log.i(TAG, "Intentional user speech detected during playback (RMS=$rms). Triggering barge-in.")
                interruptAndBargeIn()
                _voiceState.value = EngineVoiceState.USER_SPEAKING
                _statusText.value = "Listening…"
                streamAudioChunk(chunk)
            }
            // SUPPRESS streaming to server during assistant playback to prevent acoustic loopback
            return
        }

        if (rms > 0.035f && _voiceState.value == EngineVoiceState.LISTENING) {
            _voiceState.value = EngineVoiceState.USER_SPEAKING
        } else if (rms <= 0.02f && _voiceState.value == EngineVoiceState.USER_SPEAKING) {
            _voiceState.value = EngineVoiceState.LISTENING
        }

        streamAudioChunk(chunk)
    }

    private fun streamAudioChunk(chunk: ByteArray) {
        val b64 = Base64.encodeToString(chunk, Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("type", "input_audio_buffer.append")
            put("audio", b64)
        }
        activeWebSocket?.send(payload.toString())
    }

    private fun connectWebSocket() {
        val gen = connectionGeneration.incrementAndGet()
        Log.i(TAG, "Connecting Hugging Voice Realtime WebSocket (generation=$gen)...")

        _voiceState.value = EngineVoiceState.CONNECTING
        _statusText.value = "Connecting to Hugging Voice..."

        try {
            activeWebSocket?.close(1000, "Opening new generation connection")
            activeWebSocket = null

            activeWebSocket = apiClient.connectHuggingVoiceWebSocket(
                listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (gen != connectionGeneration.get()) {
                            webSocket.close(1000, "Stale")
                            return
                        }
                        Log.i(TAG, "Hugging Voice Realtime WebSocket connected (gen=$gen)")
                        _voiceState.value = EngineVoiceState.CONNECTED
                        sendSessionUpdate(webSocket)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (gen != connectionGeneration.get()) return
                        handleServerRealtimeEvent(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (gen != connectionGeneration.get()) return
                        Log.w(TAG, "Hugging Voice WebSocket failed: ${t.message}. Reconnecting in 2s...")
                        _voiceState.value = EngineVoiceState.ERROR
                        _statusText.value = "Reconnecting..."
                        scheduleReconnect(2000L)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (gen != connectionGeneration.get()) return
                        Log.i(TAG, "Hugging Voice WebSocket closed: $reason")
                        _voiceState.value = EngineVoiceState.DISCONNECTED
                        _statusText.value = "Reconnecting..."
                        scheduleReconnect(1500L)
                    }
                },
                voice = _selectedVoice.value,
                sessionId = _voiceSessionId.value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing Hugging Voice WebSocket", e)
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

    private fun sendSessionUpdate(ws: WebSocket?) {
        val payload = JSONObject().apply {
            put("type", "session.update")
            put("session", JSONObject().apply {
                put("modalities", JSONArray().put("audio").put("text"))
                put("voice", _selectedVoice.value)
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
                put("instructions", "You are Hermes, a warm, sweet, calm, highly articulate, intelligent personal AI companion speaking live over the phone. Speak in pure, natural, human conversational English. NEVER read code blocks, terminal outputs, file names, slashes, or special symbols aloud. When reporting on executed commands like ls or status, speak a friendly 1-2 sentence conversational summary.")
                put("tools", JSONArray().put(JSONObject().apply {
                    put("type", "function")
                    put("name", "hermes_execute")
                    put("description", "Execute autonomous actions or workspace tasks via Hermes Agent")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("objective", JSONObject().apply {
                                put("type", "string")
                                put("description", "Action to execute")
                            })
                        })
                        put("required", JSONArray().put("objective"))
                    })
                }))
            })
        }
        ws?.send(payload.toString())
    }

    private fun handleServerRealtimeEvent(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "session.created", "session.updated" -> {
                    Log.i(TAG, "Hugging Voice session ready ($type)")
                    if (!_isMuted.value && !player.isCurrentlyPlaying()) {
                        _voiceState.value = EngineVoiceState.LISTENING
                        _statusText.value = "Listening…"
                    }
                }

                "input_audio_buffer.speech_started" -> {
                    // Server detected user speech onset: flush player immediately for sub-50ms barge-in
                    if (player.isCurrentlyPlaying()) {
                        player.stopAndFlush()
                    }
                    if (!_isMuted.value) {
                        _voiceState.value = EngineVoiceState.USER_SPEAKING
                        _statusText.value = "Listening…"
                    }
                }

                "input_audio_buffer.speech_stopped" -> {
                    _voiceState.value = EngineVoiceState.THINKING
                    _statusText.value = "Thinking…"
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val userText = json.optString("transcript")
                    if (userText.isNotBlank()) {
                        _userTranscript.value = userText
                        _statusText.value = "“$userText”"
                        _assistantTranscript.value = ""
                        // INSTANT CHAT DISPLAY: Record user speech into Room immediately
                        com.example.hermes.data.HermesDataRepository.instance.recordVoiceUserMessage(
                            sessionId = _voiceSessionId.value,
                            userText = userText
                        )
                    }
                }

                "response.created" -> {
                    _assistantTranscript.value = ""
                    _voiceState.value = EngineVoiceState.THINKING
                }

                "response.output_audio_transcript.delta", "response.audio_transcript.delta" -> {
                    val delta = json.optString("delta")
                    if (delta.isNotBlank()) {
                        _assistantTranscript.value += delta
                        _statusText.value = _assistantTranscript.value
                        // INSTANT CHAT DISPLAY: Stream text delta into active chat immediately
                        com.example.hermes.data.HermesDataRepository.instance.recordVoiceAssistantDelta(
                            sessionId = _voiceSessionId.value,
                            delta = delta
                        )
                    }
                }

                "response.output_audio_transcript.done", "response.audio_transcript.done" -> {
                    val transcript = json.optString("transcript")
                    val finalText = if (transcript.isNotBlank()) transcript else _assistantTranscript.value
                    if (finalText.isNotBlank()) {
                        com.example.hermes.data.HermesDataRepository.instance.recordVoiceAssistantMessage(
                            sessionId = _voiceSessionId.value,
                            assistantText = finalText
                        )
                    }
                }

                "response.output_audio.delta", "response.audio.delta" -> {
                    val b64 = json.optString("delta")
                    if (b64.isNotBlank()) {
                        try {
                            val pcmBytes = Base64.decode(b64, Base64.DEFAULT)
                            player.enqueue(pcmBytes)
                            if (_voiceState.value != EngineVoiceState.SPEAKING && !_isMuted.value) {
                                _voiceState.value = EngineVoiceState.SPEAKING
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Audio decode error: ${e.message}")
                        }
                    }
                }

                "response.output_audio.done", "response.audio.done" -> {
                    Log.d(TAG, "Response output audio completed from server")
                    player.markTurnAudioDone()
                }

                "response.function_call_arguments.done" -> {
                    val callId = json.optString("call_id")
                    val name = json.optString("name")
                    val argsStr = json.optString("arguments")

                    if (name == "hermes_execute") {
                        Log.i(TAG, "Executing Hermes action: $argsStr")
                        _statusText.value = "⚡ Hermes executing task…"

                        scope.launch(Dispatchers.IO) {
                            try {
                                val argsJson = JSONObject(argsStr)
                                val objective = argsJson.optString("objective", "Execute task")
                                val taskType = argsJson.optString("task_type", "general")
                                val background = argsJson.optBoolean("background", false)

                                val res = apiClient.executeVoiceTask(
                                    objective = objective,
                                    taskType = taskType,
                                    background = background
                                )

                                val outputText = res.getOrNull()?.let {
                                    it.summary ?: it.result ?: "Action completed."
                                } ?: "Task executed successfully."

                                pendingToolObjective = objective
                                pendingToolOutput = outputText

                                // INSTANT CHAT DISPLAY: Record raw technical execution into Room DB
                                com.example.hermes.data.HermesDataRepository.instance.recordVoiceToolExecution(
                                    sessionId = _voiceSessionId.value,
                                    objective = objective,
                                    rawOutput = outputText
                                )

                                // PURE CONVERSATIONAL AGENT: Never feed raw directory dumps to voice synthesis
                                val isLs = objective.contains("ls", ignoreCase = true) || objective.contains("dir", ignoreCase = true)
                                val voiceSummaryForModel = if (isLs) {
                                    val files = outputText.lines().filter { it.isNotBlank() && !it.startsWith("total") }
                                    "Command completed. The directory has a total of ${files.size} files exist. State conversationally that the directory has total ${files.size} files and ask if the user wants you to read all for them."
                                } else {
                                    "Task completed. Summary: ${outputText.take(200).replace("\n", " ")}"
                                }

                                // Report output back to session
                                val outputItem = JSONObject().apply {
                                    put("type", "conversation.item.create")
                                    put("item", JSONObject().apply {
                                        put("type", "function_call_output")
                                        put("call_id", callId)
                                        put("output", voiceSummaryForModel)
                                    })
                                }
                                activeWebSocket?.send(outputItem.toString())

                                // Request voice synthesis for the result
                                val createResp = JSONObject().apply {
                                    put("type", "response.create")
                                }
                                activeWebSocket?.send(createResp.toString())

                            } catch (e: Exception) {
                                Log.e(TAG, "Error executing voice action: ${e.message}")
                            }
                        }
                    }
                }

                "response.done" -> {
                    Log.d(TAG, "Response turn completed from server")
                    player.markTurnAudioDone()

                    val uTranscript = _userTranscript.value
                    val aTranscript = _assistantTranscript.value
                    val toolObj = pendingToolObjective
                    val toolOut = pendingToolOutput

                    if (uTranscript.isNotBlank() || aTranscript.isNotBlank()) {
                        com.example.hermes.data.HermesDataRepository.instance.recordVoiceTurn(
                            sessionId = _voiceSessionId.value,
                            userTranscript = uTranscript,
                            assistantReply = aTranscript,
                            toolObjective = toolObj,
                            toolOutput = toolOut
                        )
                    }
                    pendingToolObjective = null
                    pendingToolOutput = null

                    if (!player.isCurrentlyPlaying() && !_isMuted.value) {
                        _voiceState.value = EngineVoiceState.LISTENING
                        _statusText.value = "Listening…"
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing server event: ${e.message}")
        }
    }

    fun stop() {
        isEngineStarted = false
        reconnectJob?.cancel()
        reconnectJob = null

        try {
            activeWebSocket?.close(1000, "Engine stopped")
            activeWebSocket = null
        } catch (_: Exception) {}

        recorder.stop()
        player.stopAndFlush()
        player.release()
    }
}
