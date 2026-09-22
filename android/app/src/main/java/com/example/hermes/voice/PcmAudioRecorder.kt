package com.example.hermes.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Continuous 16kHz 16-bit Mono PCM audio capture engine.
 *
 * Utilizes AudioSource.VOICE_COMMUNICATION with hardware Acoustic Echo Cancellation (AEC),
 * Noise Suppression (NS), and Automatic Gain Control (AGC) to ensure clean speech recording
 * and prevent loudspeaker feedback from re-triggering speech detection.
 */
class PcmAudioRecorder(
    private val sampleRate: Int = 16000,
    private val chunkDurationMs: Int = 50,
    private val onChunkRecorded: (ByteArray, Float) -> Unit
) {
    companion object {
        private const val TAG = "PcmAudioRecorder"
    }

    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    // Audio effects references to prevent premature GC
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    // 16kHz 16-bit Mono = 32,000 bytes/sec -> 50ms = 1600 bytes (800 samples)
    private val chunkSize = (sampleRate * 2 * chunkDurationMs) / 1000

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording.getAndSet(true)) {
            Log.d(TAG, "Audio recording is already active")
            return
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = maxOf(minBufferSize, chunkSize * 4)

                val sources = listOf(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.MIC
                )

                var recorder: AudioRecord? = null
                for (source in sources) {
                    try {
                        val candidate = AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
                        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                            recorder = candidate
                            Log.i(TAG, "AudioRecord initialized successfully with audio source $source")
                            break
                        } else {
                            candidate.release()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio source $source unavailable: ${e.message}")
                    }
                }

                if (recorder == null) {
                    Log.e(TAG, "Failed to initialize AudioRecord with any audio source (permission or hardware).")
                    isRecording.set(false)
                    return@launch
                }

                audioRecord = recorder
                val sessionId = recorder.audioSessionId

                // Attach Hardware Acoustic Effects
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        aec = AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                            Log.d(TAG, "Hardware AcousticEchoCanceler enabled on session $sessionId")
                        }
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        ns = NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                            Log.d(TAG, "Hardware NoiseSuppressor enabled on session $sessionId")
                        }
                    }
                    if (AutomaticGainControl.isAvailable()) {
                        agc = AutomaticGainControl.create(sessionId)?.apply {
                            enabled = true
                            Log.d(TAG, "Hardware AutomaticGainControl enabled on session $sessionId")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Non-fatal error initializing audio effects: ${e.message}")
                }

                recorder.startRecording()
                Log.i(TAG, "AudioRecord started: ${sampleRate}Hz, chunkSize=$chunkSize bytes (${chunkDurationMs}ms)")

                val buffer = ByteArray(chunkSize)
                while (isActive && isRecording.get()) {
                    var totalRead = 0
                    while (totalRead < chunkSize && isActive && isRecording.get()) {
                        val read = recorder.read(buffer, totalRead, chunkSize - totalRead)
                        if (read > 0) {
                            totalRead += read
                        } else if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e(TAG, "AudioRecord read error: $read")
                            break
                        }
                    }

                    if (totalRead == chunkSize && isRecording.get()) {
                        val chunkCopy = buffer.copyOf(chunkSize)
                        val rms = calculateRms(chunkCopy)
                        onChunkRecorded(chunkCopy, rms)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in audio recording loop", e)
            } finally {
                cleanupInternal()
            }
        }
    }

    fun stop() {
        if (!isRecording.getAndSet(false)) return
        recordingJob?.cancel()
        recordingJob = null
        cleanupInternal()
    }

    private fun cleanupInternal() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        try {
            aec?.release()
            ns?.release()
            agc?.release()
        } catch (_: Exception) {}
        aec = null
        ns = null
        agc = null
        Log.d(TAG, "AudioRecord and effects released")
    }

    /**
     * Compute normalized RMS volume (0.0 to 1.0) from 16-bit PCM little-endian buffer.
     */
    private fun calculateRms(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        var sumSquare = 0.0
        val numSamples = pcmBytes.size / 2
        for (i in 0 until numSamples) {
            val sample = (pcmBytes[i * 2].toInt() and 0xFF) or (pcmBytes[i * 2 + 1].toInt() shl 8)
            val shortVal = sample.toShort()
            sumSquare += (shortVal * shortVal).toDouble()
        }
        val meanSquare = sumSquare / numSamples
        val rms = sqrt(meanSquare)
        // Normalize against max short value (32767)
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }
}
