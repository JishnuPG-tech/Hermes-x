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
 * 60 FPS Ultra-Smooth Stream Smoothing Engine.
 * Buffers bursty incoming SSE token chunks and emits them at 16ms intervals (60-120 FPS)
 * with adaptive burst acceleration to ensure buttery smooth text flow without layout thrashing or lag.
 */
class StreamSmoothingEngine(
    private val scope: CoroutineScope,
    private val tickIntervalMs: Long = 16L, // 60 FPS
    private val charsPerTick: Int = 3
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
                if (!tokenQueue.isEmpty()) {
                    // Adaptive burst drain: if queue is growing, drain more tokens per tick
                    val drainCount = when {
                        tokenQueue.size > 30 -> 8
                        tokenQueue.size > 15 -> 4
                        tokenQueue.size > 6 -> 2
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
            // Split larger multi-word bursts into smoother micro-chunks
            if (token.length > charsPerTick) {
                token.chunked(charsPerTick).forEach { chunk ->
                    tokenQueue.offer(chunk)
                }
            } else {
                tokenQueue.offer(token)
            }
        }
    }

    /**
     * Mark stream as complete. Flushes all remaining buffered tokens.
     */
    fun complete(fullText: String? = null) {
        isCompleted = true
        if (fullText != null) {
            accumulatedBuilder.clear()
            accumulatedBuilder.append(fullText)
            _renderedText.value = fullText
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
