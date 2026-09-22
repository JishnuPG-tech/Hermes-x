package com.example.hermes.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Hermes High-Fidelity Micro-Sound & Haptic Feedback Engine.
 *
 * Generates ultra-low-latency synthesized micro-tones (pure sine wave PCM)
 * and native UI audio clicks to give Hermes an authentic JARVIS-level tactical feel.
 * Optimized for 60-120Hz devices with zero external asset overhead.
 */
object HermesAudioFeedback {

    private val soundScope = CoroutineScope(Dispatchers.Default)

    // Pre-synthesized micro PCM buffers for instant playback
    private val sentToneBuffer: ByteArray by lazy {
        synthesizeTone(
            frequencies = doubleArrayOf(1046.50, 1318.51), // C6 -> E6 crisp high chime
            durationMs = 45,
            sampleRate = 44100,
            volume = 0.28
        )
    }

    private val receivedToneBuffer: ByteArray by lazy {
        synthesizeTone(
            frequencies = doubleArrayOf(880.0, 1174.66, 1567.98), // A5 -> D6 -> G6 harmonic completion
            durationMs = 90,
            sampleRate = 44100,
            volume = 0.22
        )
    }

    private val stepTickBuffer: ByteArray by lazy {
        synthesizeTone(
            frequencies = doubleArrayOf(1760.0), // High subtle micro-tick
            durationMs = 20,
            sampleRate = 44100,
            volume = 0.16
        )
    }

    /**
     * Synthesizes smooth pure-sine PCM 16-bit audio buffer with exponential fade-out envelope.
     */
    private fun synthesizeTone(
        frequencies: DoubleArray,
        durationMs: Int,
        sampleRate: Int = 44100,
        volume: Double = 0.25
    ): ByteArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ByteArray(numSamples * 2)
        val stageDuration = numSamples.toDouble() / frequencies.size

        for (i in 0 until numSamples) {
            val stageIndex = (i / stageDuration).toInt().coerceIn(0, frequencies.size - 1)
            val freq = frequencies[stageIndex]
            val t = i.toDouble() / sampleRate
            // Exponential decay envelope to avoid speaker clicks
            val envelope = (1.0 - (i.toDouble() / numSamples)) * volume
            val sampleValue = (sin(2.0 * PI * freq * t) * envelope * 32767.0).toInt().coerceIn(-32768, 32767)

            buffer[i * 2] = (sampleValue and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sampleValue shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    private fun playPcm(buffer: ByteArray, sampleRate: Int = 44100) {
        soundScope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val durationMs = (buffer.size.toLong() * 1000L) / (sampleRate * 2L) + 25L
                kotlinx.coroutines.delay(durationMs)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Ignore audio track initialization errors on restricted devices
            }
        }
    }

    /**
     * Play message sent sound effect: crisp, high-tech micro tap.
     */
    fun playMessageSent(context: Context) {
        try {
            playPcm(sentToneBuffer)
            vibrateSubtle(context, 18)
        } catch (_: Exception) {}
    }

    /**
     * Play response completed chime: sweet, warm harmonic double-tone.
     */
    fun playMessageReceived(context: Context) {
        try {
            playPcm(receivedToneBuffer)
            vibrateSubtle(context, 25)
        } catch (_: Exception) {}
    }

    /**
     * Play thinking step progress tick: delicate micro-tick.
     */
    fun playStepTransition(context: Context) {
        try {
            playPcm(stepTickBuffer)
            vibrateSubtle(context, 10)
        } catch (_: Exception) {}
    }

    /**
     * Play action click (button, menu item, chip tap).
     */
    fun playActionClick(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f)
            vibrateSubtle(context, 12)
        } catch (_: Exception) {}
    }

    private fun vibrateSubtle(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, 50))
            }
        } catch (_: Exception) {}
    }
}
