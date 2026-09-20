package com.example.hermes.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

enum class VoiceCallState {
    CONNECTING, LISTENING, THINKING, SPEAKING
}

@Composable
fun VoiceScreen(
    onClose: () -> Unit,
    onOpenVoiceSettings: () -> Unit = {}
) {
    var isMuted by remember { mutableStateOf(false) }
    var voiceState by remember { mutableStateOf(VoiceCallState.LISTENING) }
    var captionText by remember { mutableStateOf("Hold tight, connecting…") }
    var activeWordIndex by remember { mutableIntStateOf(0) }

    // Rhythmic breathing pulse for starburst avatar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Bar: Time, Active Mic status pill, and Circular Settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Green Mic Active Capsule
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1B261D))
                    .border(1.dp, Color(0xFF2E4432), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live",
                    style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }

            // Right: Circular Settings Button (12-01-09)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF201F1D))
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable(onClick = onOpenVoiceSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Voice Settings",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Center Content: Starburst Mark + Realtime Serif Caption
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-30).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.scale(if (voiceState == VoiceCallState.SPEAKING) pulseScale else 1.0f),
                contentAlignment = Alignment.Center
            ) {
                if (voiceState == VoiceCallState.THINKING) {
                    ClaudeSparkThinkingAnimation(size = 54.dp, tint = BrandCoral)
                } else {
                    ClaudeSpark(size = 54.dp, tint = BrandCoral)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = captionText,
                style = HermesTypography.displayLarge.copy(
                    fontSize = 26.sp,
                    color = TextPrimaryWarm,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Bottom Control HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elevated Microphone Center Button
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1B19))
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable {
                        isMuted = !isMuted
                        captionText = if (isMuted) "Microphone muted" else "Listening…"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Microphone",
                    tint = if (isMuted) DestructiveRed else TextPrimaryWarm,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Horizontal Bar: [+] , [Sonnet ⬍] , [Hang Up ✕]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Add Context (+)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1E1C))
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable { /* Add context */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add context",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Model Switcher Pill: "Sonnet ⬍"
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF201F1D))
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable { /* Switch Model */ }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sonnet",
                        style = HermesTypography.titleLarge.copy(
                            fontSize = 15.sp,
                            color = TextPrimaryWarm,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Hang Up / Dismiss White Circular Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Call",
                        tint = PureBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
