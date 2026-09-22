package com.example.hermes.voice

import android.util.Log
import kotlin.math.abs

/**
 * On-Device, Lightweight Wake Word & Speech Activity Detector.
 *
 * Runs locally on Android device without network connectivity or API tokens.
 * Analyzes short-time energy (STE), zero-crossing rate (ZCR), and syllable cadence
 * to detect "Hey Hermes" keyword triggers and speech turn activity.
 */
class LocalWakeWordDetector(
    private val sampleRate: Int = 16000,
    private val onWakeWordDetected: () -> Unit,
    private val onSpeechActivityChanged: ((Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "LocalWakeWordDetector"
        private const val DEFAULT_ENERGY_THRESHOLD = 0.035f
        private const val ZCR_SPEECH_THRESHOLD = 0.08f
        private const val MIN_SYLLABLE_COUNT = 2
        private const val MAX_SYLLABLE_COUNT = 4
    }

    private var noiseFloor = 0.015f
    private var isCurrentlySpeaking = false
    private var speechStartTimestamp = 0L
    private var syllableCounter = 0
    private var lastPeakTimestamp = 0L
    private var lastTriggerTimestamp = 0L

    /**
     * Process incoming 16-bit PCM chunk (e.g. 50ms = 800 samples).
     */
    fun processChunk(pcmBytes: ByteArray, rms: Float) {
        // Adapt noise floor slowly when quiet
        if (rms < noiseFloor * 1.2f) {
            noiseFloor = (noiseFloor * 0.95f) + (rms * 0.05f)
        }

        val speechThreshold = maxOf(DEFAULT_ENERGY_THRESHOLD, noiseFloor * 2.5f)
        val isSpeechChunk = rms > speechThreshold

        val zcr = computeZeroCrossingRate(pcmBytes)
        val now = System.currentTimeMillis()

        if (isSpeechChunk && zcr > ZCR_SPEECH_THRESHOLD) {
            if (!isCurrentlySpeaking) {
                isCurrentlySpeaking = true
                speechStartTimestamp = now
                syllableCounter = 1
                lastPeakTimestamp = now
                onSpeechActivityChanged?.invoke(true)
            } else {
                // Syllable peak detection (cadence ~150ms-350ms between syllable vowels)
                val diff = now - lastPeakTimestamp
                if (diff in 140..420 && rms > speechThreshold * 1.3f) {
                    syllableCounter++
                    lastPeakTimestamp = now
                }
            }
        } else {
            if (isCurrentlySpeaking && (now - lastPeakTimestamp > 300)) {
                // Speech ended or paused
                val utteranceDuration = now - speechStartTimestamp
                isCurrentlySpeaking = false
                onSpeechActivityChanged?.invoke(false)

                // Check if syllable count and duration matches "Hey Hermes" (~400ms to 1200ms, 2-4 syllables)
                if (utteranceDuration in 400..1400 && syllableCounter in MIN_SYLLABLE_COUNT..MAX_SYLLABLE_COUNT) {
                    if (now - lastTriggerTimestamp > 2000) { // 2s debounce
                        lastTriggerTimestamp = now
                        Log.i(TAG, "Local candidate pattern matched 'Hey Hermes' (duration=${utteranceDuration}ms, syllables=$syllableCounter)")
                        onWakeWordDetected()
                    }
                }
                syllableCounter = 0
            }
        }
    }

    private fun computeZeroCrossingRate(pcmBytes: ByteArray): Float {
        val numSamples = pcmBytes.size / 2
        if (numSamples < 2) return 0f
        var zeroCrossings = 0
        var prevSign = (pcmBytes[0].toInt() and 0xFF) or (pcmBytes[1].toInt() shl 8) >= 0

        for (i in 1 until numSamples) {
            val sample = (pcmBytes[i * 2].toInt() and 0xFF) or (pcmBytes[i * 2 + 1].toInt() shl 8)
            val currSign = sample >= 0
            if (currSign != prevSign) {
                zeroCrossings++
                prevSign = currSign
            }
        }
        return zeroCrossings.toFloat() / numSamples
    }

    fun reset() {
        isCurrentlySpeaking = false
        speechStartTimestamp = 0L
        syllableCounter = 0
        lastPeakTimestamp = 0L
    }
}
