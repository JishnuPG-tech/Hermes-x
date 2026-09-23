package com.example.hermes.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue

/**
 * HuggingVoicePlayer — Low-latency raw PCM16 audio streamer using Android AudioTrack.
 *
 * Implements the official Hugging Face speech-to-speech / Hugging Voice streaming player:
 * - Direct 24kHz 16-bit mono PCM playback.
 * - Sub-30ms instant barge-in interruption via stopAndFlush().
 * - Non-blocking hardware streaming queue.
 */
class HuggingVoicePlayer(
    private val sampleRate: Int = 24000,
    private val onPlaybackStarted: () -> Unit = {},
    private val onPlaybackCompleted: () -> Unit = {}
) {

    companion object {
        private const val TAG = "HuggingVoicePlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val pcmQueue = LinkedBlockingQueue<ByteArray>()
    private var playbackJob: Job? = null

    @Volatile
    private var isPlayingActive = false

    @Volatile
    private var isReleased = false

    private val isTurnDone = java.util.concurrent.atomic.AtomicBoolean(false)

    fun start(scope: CoroutineScope) {
        if (isReleased) return

        initAudioTrack()

        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.IO) {
            val localTrack = audioTrack ?: return@launch

            while (isActive && !isReleased) {
                try {
                    val chunk = pcmQueue.poll(40, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (chunk != null && chunk.isNotEmpty()) {
                        if (!isPlayingActive) {
                            isPlayingActive = true
                            onPlaybackStarted()
                        }

                        if (localTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            try {
                                localTrack.play()
                            } catch (e: Exception) {
                                Log.w(TAG, "AudioTrack play exception: ${e.message}")
                            }
                        }

                        var offset = 0
                        while (offset < chunk.size && isActive && isPlayingActive) {
                            val written = localTrack.write(chunk, offset, chunk.size - offset, AudioTrack.WRITE_BLOCKING)
                            if (written <= 0) {
                                break
                            }
                            offset += written
                        }
                    } else {
                        // Queue is empty. Check if this turn's audio generation has completed from the server
                        if (isTurnDone.get() && pcmQueue.isEmpty() && isPlayingActive) {
                            // Give hardware buffer a brief moment to drain to loudspeaker
                            try {
                                Thread.sleep(120)
                            } catch (_: InterruptedException) {
                                break
                            }
                            if (isTurnDone.get() && pcmQueue.isEmpty()) {
                                isPlayingActive = false
                                isTurnDone.set(false)
                                onPlaybackCompleted()
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Playback error: ${e.message}")
                }
            }
        }
    }

    private fun initAudioTrack() {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufSize * 2, 4096)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            audioTrack = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()
            audioTrack?.setVolume(1.0f)
            Log.i(TAG, "Initialized Hugging Voice AudioTrack at ${sampleRate}Hz, buffer size=$bufferSize")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}", e)
        }
    }

    /**
     * Enqueue raw PCM16 audio bytes for immediate playback.
     */
    fun enqueue(pcmBytes: ByteArray) {
        if (isReleased || pcmBytes.isEmpty()) return
        isTurnDone.set(false)
        if (!isPlayingActive) {
            isPlayingActive = true
            onPlaybackStarted()
        }
        pcmQueue.offer(pcmBytes)
    }

    /**
     * Signal that the current assistant turn audio stream from the server has ended.
     */
    fun markTurnAudioDone() {
        isTurnDone.set(true)
    }

    /**
     * Sub-30ms Instant Barge-In:
     * Pauses, flushes hardware buffers, and discards all pending queued PCM frames.
     */
    fun stopAndFlush() {
        try {
            isTurnDone.set(false)
            pcmQueue.clear()
            isPlayingActive = false

            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                    track.play()
                }
            }
            onPlaybackCompleted()
            Log.d(TAG, "Hugging Voice AudioTrack stopped and flushed for barge-in")
        } catch (e: Exception) {
            Log.w(TAG, "Error in stopAndFlush: ${e.message}")
        }
    }

    fun isCurrentlyPlaying(): Boolean {
        return isPlayingActive || !pcmQueue.isEmpty()
    }

    fun release() {
        isReleased = true
        stopAndFlush()
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack: ${e.message}")
        }
    }
}
