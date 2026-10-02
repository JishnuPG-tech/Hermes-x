package com.example.hermes.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.hermes.data.HermesApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * PersonaDemoPlayer — plays a short (~6s) Edge-TTS demo clip for a given voice model.
 *
 * On persona swipe/select → call [playDemo]. Any active demo is cancelled first.
 * Audio is fetched from /v1/voice/synthesize (returns MP3), cached per voice ID
 * in the app's cache directory, then played via MediaPlayer with audio focus.
 */
object PersonaDemoPlayer {

    private const val TAG = "PersonaDemoPlayer"

    // Demo sentences per voice — natural, conversational, ~6 seconds each
    private val DEMO_TEXTS = mapOf(
        "en-US-JennyNeural"       to "Hi, I'm Jenny. Warm, friendly, and always here to help you.",
        "en-US-AvaNeural"         to "Hello, I'm Ava. Clear, poised, and ready to assist you today.",
        "en-US-AriaNeural"        to "Hey there, I'm Aria. Expressive, lively, and happy to chat!",
        "en-US-ChristopherNeural" to "Good day. I'm Christopher. Thoughtful, calm, and here to serve.",
        "en-US-GuyNeural"         to "Hey, I'm Guy. Casual, warm, and always up for a great conversation.",
        "en-US-EricNeural"        to "Hello, I'm Eric. Authoritative, clear, and fully at your service."
    )

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var currentPlayer: MediaPlayer? = null

    fun playDemo(context: Context, voiceModelId: String) {
        // Cancel previous demo immediately
        cancel()

        val demoText = DEMO_TEXTS[voiceModelId]
            ?: "Hello, I'm your AI voice assistant. How can I help you today?"

        currentJob = scope.launch {
            try {
                val mp3Bytes = fetchOrCache(context, voiceModelId, demoText) ?: return@launch
                playMp3Bytes(context, mp3Bytes)
            } catch (e: Exception) {
                Log.w(TAG, "Demo playback error for $voiceModelId: ${e.message}")
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        currentPlayer?.runCatching { stop(); release() }
        currentPlayer = null
    }

    // ── Network + Cache ──────────────────────────────────────────────────────

    private suspend fun fetchOrCache(context: Context, voiceId: String, text: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "persona_demo_${voiceId.replace("-", "_")}.mp3")
            if (cacheFile.exists() && cacheFile.length() > 1024) {
                Log.d(TAG, "Cache hit for $voiceId")
                return@withContext cacheFile.readBytes()
            }

            val baseUrl = HermesApiClient.instance.getBaseUrl().trimEnd('/')
            val url = "$baseUrl/v1/voice/synthesize"
            val payload = """{"text":${escapeJson(text)},"voice":"$voiceId","format":"mp3","speed":1.0}"""

            val req = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer ${HermesApiClient.DEFAULT_API_KEY}")
                .build()

            return@withContext try {
                HermesApiClient.instance.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "Synthesize request failed: ${resp.code}")
                        return@use null
                    }
                    val bytes = resp.body?.bytes() ?: return@use null
                    if (bytes.size > 1024) {
                        cacheFile.writeBytes(bytes)
                        Log.d(TAG, "Cached demo for $voiceId (${bytes.size} bytes)")
                    }
                    bytes
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error fetching demo: ${e.message}")
                null
            }
        }

    // ── Playback ─────────────────────────────────────────────────────────────

    private suspend fun playMp3Bytes(context: Context, bytes: ByteArray) = withContext(Dispatchers.Main) {
        val tmpFile = withContext(Dispatchers.IO) {
            File.createTempFile("demo_play_", ".mp3", context.cacheDir).also { it.writeBytes(bytes) }
        }

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(tmpFile.absolutePath)
            prepare()
            setVolume(1f, 1f)
            setOnCompletionListener {
                it.release()
                tmpFile.delete()
                if (currentPlayer == it) currentPlayer = null
            }
            start()
        }
        currentPlayer = mp
        Log.d(TAG, "Playing persona demo (${bytes.size} bytes)")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun escapeJson(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"'  -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
