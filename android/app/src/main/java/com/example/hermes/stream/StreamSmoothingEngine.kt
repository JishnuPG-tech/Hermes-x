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
 * 30 FPS Stream Smoothing Engine.
 * Buffers bursty incoming SSE token chunks and emits them at 33ms intervals
 * to prevent UI layout thrashing during high-velocity LLM completions.
 */
class StreamSmoothingEngine(
    private val scope: CoroutineScope,
    private val tickIntervalMs: Long = 33L, // 30 FPS
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
                    val nextToken = tokenQueue.poll()
                    if (nextToken != null) {
                        accumulatedBuilder.append(nextToken)
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
