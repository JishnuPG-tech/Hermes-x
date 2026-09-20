package com.example.hermes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionSummarySheet(
    onDismiss: () -> Unit,
    onStopGeneration: () -> Unit = {},
    isThinkingActive: Boolean = false,
    promptTopic: String = "Building a Python calculator for a mobile app.",
    artifactName: String? = null,
    artifactType: String? = null,
    thinkingPhase: com.example.hermes.data.ThinkingPhase = com.example.hermes.data.ThinkingPhase.IDLE,
    onOpenArtifact: (String, String) -> Unit = { _, _ -> }
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF191816),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF383632))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Header (✕ on left, "Summary" in Editorial Serif in center - Screenshot 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Summary",
                    style = HermesTypography.headlineMedium.copy(
                        fontSize = 21.sp,
                        color = TextPrimaryWarm,
                        fontFamily = AnthropicSerif,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Stepper Timeline (Exact Replica of Screenshot 4, 5, 6, 7)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Step 1: Prompt Execution (Topic Header)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5A5852))
                        )
                        val hasArtifact = !artifactName.isNullOrBlank() && (thinkingPhase == com.example.hermes.data.ThinkingPhase.CREATING_FILE ||
                                thinkingPhase == com.example.hermes.data.ThinkingPhase.FINALIZING ||
                                thinkingPhase == com.example.hermes.data.ThinkingPhase.COMPLETED ||
                                !isThinkingActive)

                        if (hasArtifact || isThinkingActive) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(60.dp)
                                    .background(Color(0xFF35332E))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = promptTopic,
                            style = HermesTypography.displayLarge.copy(
                                fontSize = 19.sp,
                                color = TextPrimaryWarm,
                                lineHeight = 25.sp,
                                fontFamily = AnthropicSerif,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Step 2: Artifact Creation Step (Screenshots 5 & 6)
                val hasArtifact = !artifactName.isNullOrBlank() && (thinkingPhase == com.example.hermes.data.ThinkingPhase.CREATING_FILE ||
                        thinkingPhase == com.example.hermes.data.ThinkingPhase.FINALIZING ||
                        thinkingPhase == com.example.hermes.data.ThinkingPhase.COMPLETED ||
                        !isThinkingActive)

                if (hasArtifact && !artifactName.isNullOrBlank()) {
                    val safeArtifactName = artifactName
                    val safeArtifactType = artifactType ?: "Document · MD"
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2B2A27)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thinkingPhase == com.example.hermes.data.ThinkingPhase.CREATING_FILE) {
                                    ClaudeNoteAddIcon(size = 13.dp, tint = TextMuted)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = "Artifact",
                                        tint = TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            if (isThinkingActive) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(52.dp)
                                        .background(Color(0xFF35332E))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (thinkingPhase == com.example.hermes.data.ThinkingPhase.CREATING_FILE) {
                                Text(
                                    text = "Creating $safeArtifactName",
                                    style = HermesTypography.displayLarge.copy(
                                        fontSize = 18.sp,
                                        color = TextPrimaryWarm,
                                        fontFamily = AnthropicSerif,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            } else {
                                // File badge card (Screenshot 6: "Created" badge + file name)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF22211F))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            onDismiss()
                                            onOpenArtifact(safeArtifactName, safeArtifactType)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x3338BDF8))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Created",
                                            style = HermesTypography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = safeArtifactName,
                                        style = HermesTypography.bodySmall.copy(
                                            fontSize = 12.5.sp,
                                            fontFamily = JetBrainsMono,
                                            color = TextPrimaryWarm
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Step 3: Active Thinking or Completed Step (Screenshot 4, 5, 6)
                if (isThinkingActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .padding(top = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(BrandCoral.copy(alpha = pulseAlpha))
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Thinking",
                            style = HermesTypography.displayLarge.copy(
                                fontSize = 19.sp,
                                color = TextPrimaryWarm,
                                fontFamily = AnthropicSerif,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bottom Stop Generation Button (Screenshot 4)
            if (isThinkingActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF262523))
                            .border(1.dp, Color(0xFF383632), CircleShape)
                            .clickable {
                                onStopGeneration()
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PureWhite)
                        )
                        Text(
                            text = "Stop generation",
                            style = HermesTypography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
