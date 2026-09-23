package com.example.hermes.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 120 FPS High-Throughput Stream Smoothing Engine.
 * Fluidly buffers incoming SSE tokens and emits them with zero perceptible lag.
 * Employs dynamic queue-proportional draining:
 * - Natural typing rhythm when tokens trickle in.
 * - Instant acceleration during large bursts so the UI never falls behind the LLM.
 */
class StreamSmoothingEngine(
    private val scope: CoroutineScope,
    private val tickIntervalMs: Long = 16L // 60-120 FPS cadence
) {
    private val tokenQueue = ConcurrentLinkedQueue<String>()
    private val _renderedText = MutableStateFlow("")
    val renderedText: StateFlow<String> = _renderedText.asStateFlow()

    private val accumulatedBuilder = StringBuilder()
    private var isCompleted = false
    private var tickerJob: Job? = null

    init {
        startTicker()
    }

    private fun startTicker() {
        tickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val qSize = tokenQueue.size
                if (qSize > 0) {
                    // Dynamic proportional drain: Catch up immediately if queue grows
                    val drainCount = when {
                        qSize > 25 -> qSize // Immediate catchup on huge bursts
                        qSize > 12 -> 8
                        qSize > 6 -> 4
                        qSize > 2 -> 2
                        else -> 1
                    }

                    var appendedAny = false
                    for (i in 0 until drainCount) {
                        val nextToken = tokenQueue.poll() ?: break
                        accumulatedBuilder.append(nextToken)
                        appendedAny = true
                    }
                    if (appendedAny) {
                        _renderedText.value = accumulatedBuilder.toString()
                    }
                } else if (isCompleted) {
                    _renderedText.value = accumulatedBuilder.toString()
                    break
                }
                delay(tickIntervalMs)
            }
        }
    }

    /**
     * Feed a newly arrived raw token from the SSE stream into the smoother buffer.
     */
    fun appendToken(token: String) {
        if (token.isNotEmpty()) {
            tokenQueue.offer(token)
        }
    }

    /**
     * Mark stream as complete. Flushes all remaining buffered tokens instantly.
     */
    fun complete(fullText: String? = null) {
        isCompleted = true
        if (fullText != null) {
            accumulatedBuilder.clear()
            accumulatedBuilder.append(fullText)
            _renderedText.value = fullText
        } else {
            // Drain anything left in queue immediately
            while (!tokenQueue.isEmpty()) {
                val tok = tokenQueue.poll() ?: break
                accumulatedBuilder.append(tok)
            }
            _renderedText.value = accumulatedBuilder.toString()
        }
        tickerJob?.cancel()
    }

    /**
     * Reset the smoother for a fresh message.
     */
    fun reset() {
        tickerJob?.cancel()
        tokenQueue.clear()
        accumulatedBuilder.clear()
        _renderedText.value = ""
        isCompleted = false
        startTicker()
    }
}
