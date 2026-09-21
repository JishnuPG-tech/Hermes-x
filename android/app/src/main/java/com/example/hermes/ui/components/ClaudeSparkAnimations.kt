package com.example.hermes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hermes.R
import com.example.hermes.theme.BrandCoral
import kotlinx.coroutines.delay

/**
 * Static authentic Claude / Hermes Starburst Vector Spark.
 */
@Composable
fun ClaudeSpark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = BrandCoral
) {
    Image(
        painter = painterResource(id = R.drawable.claude_spark_icon),
        contentDescription = "Hermes Spark",
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

/**
 * 8-Frame Sequential Vector Thinking Animation (125ms per frame, 1000ms loop).
 * Used during extended reasoning & thinking streams.
 */
@Composable
fun ClaudeSparkThinkingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = BrandCoral
) {
    val frames = listOf(
        R.drawable.claude_spark_thinking1,
        R.drawable.claude_spark_thinking2,
        R.drawable.claude_spark_thinking3,
        R.drawable.claude_spark_thinking4,
        R.drawable.claude_spark_thinking5,
        R.drawable.claude_spark_thinking6,
        R.drawable.claude_spark_thinking7,
        R.drawable.claude_spark_thinking8
    )

    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(125L)
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = frames[currentFrameIndex]),
            contentDescription = "Thinking Animation",
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * 8-Frame Sequential Vector Writing Animation (125ms per frame, 1000ms loop).
 * Used during active token and code generation.
 */
@Composable
fun ClaudeSparkWritingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = BrandCoral
) {
    val frames = listOf(
        R.drawable.claude_spark_writing1,
        R.drawable.claude_spark_writing2,
        R.drawable.claude_spark_writing3,
        R.drawable.claude_spark_writing4,
        R.drawable.claude_spark_writing5,
        R.drawable.claude_spark_writing6,
        R.drawable.claude_spark_writing7,
        R.drawable.claude_spark_writing8
    )

    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(125L)
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = frames[currentFrameIndex]),
            contentDescription = "Writing Animation",
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * 15-Frame Sequential Vector Shimmer Animation (125ms per frame, 1875ms cycle).
 * Used during ambient idle and ready states.
 */
@Composable
fun ClaudeSparkShimmerAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = BrandCoral
) {
    val frames = listOf(
        R.drawable.claude_spark_shimmer1,
        R.drawable.claude_spark_shimmer2,
        R.drawable.claude_spark_shimmer3,
        R.drawable.claude_spark_shimmer4,
        R.drawable.claude_spark_shimmer5,
        R.drawable.claude_spark_shimmer6,
        R.drawable.claude_spark_shimmer7,
        R.drawable.claude_spark_shimmer8,
        R.drawable.claude_spark_shimmer9,
        R.drawable.claude_spark_shimmer10,
        R.drawable.claude_spark_shimmer11,
        R.drawable.claude_spark_shimmer12,
        R.drawable.claude_spark_shimmer13,
        R.drawable.claude_spark_shimmer14,
        R.drawable.claude_spark_shimmer15
    )

    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(125L)
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = frames[currentFrameIndex]),
            contentDescription = "Shimmer Animation",
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Pulsing coral dot — Claude-style loading/thinking indicator.
 * Replaces the starburst logo in all chat loading states.
 * Animation: scale pulses 0.55 → 1.0 → 0.55 in a 700ms loop.
 */
@Composable
fun HermesThinkingDot(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = BrandCoral
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hermes_dot_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
        )
    }
}
