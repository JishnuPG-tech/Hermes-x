package com.example.hermes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

data class SummaryStage(
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionSummarySheet(
    onDismiss: () -> Unit,
    onStopGeneration: () -> Unit = {},
    isThinkingActive: Boolean = false,
    promptTopic: String = "Thought process",
    thinkingContent: String? = null,
    artifactName: String? = null,
    artifactType: String? = null,
    thinkingPhase: com.example.hermes.data.ThinkingPhase = com.example.hermes.data.ThinkingPhase.IDLE,
    onOpenArtifact: (String, String) -> Unit = { _, _ -> }
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    // Build the 5 chronological stages from the task beginning to final serve
    val cleanPrompt = promptTopic.trim().removePrefix("Thought process").ifBlank { "Task execution" }
    val isDone = !isThinkingActive

    val stages = listOf(
        SummaryStage(
            title = "Analysing the request",
            description = "Deconstructing intent, parameters, and context for $cleanPrompt",
            isCompleted = true,
            isActive = isThinkingActive && thinkingPhase == com.example.hermes.data.ThinkingPhase.THOUGHT_PROCESS
        ),
        SummaryStage(
            title = "Executing command & tools",
            description = if (!artifactName.isNullOrBlank()) "Executing code generation and artifact compilation"
                          else "Executing server operations, live inspections, and autonomous tool logic",
            isCompleted = isDone || thinkingPhase in listOf(
                com.example.hermes.data.ThinkingPhase.CREATING_FILE,
                com.example.hermes.data.ThinkingPhase.FINALIZING,
                com.example.hermes.data.ThinkingPhase.COMPLETED
            ),
            isActive = isThinkingActive && thinkingPhase == com.example.hermes.data.ThinkingPhase.BUILDING
        ),
        SummaryStage(
            title = "Inspecting output & results",
            description = "Validating syntax, return codes, and output accuracy",
            isCompleted = isDone || thinkingPhase in listOf(
                com.example.hermes.data.ThinkingPhase.FINALIZING,
                com.example.hermes.data.ThinkingPhase.COMPLETED
            ),
            isActive = isThinkingActive && thinkingPhase == com.example.hermes.data.ThinkingPhase.CREATING_FILE
        ),
        SummaryStage(
            title = "Organising for user",
            description = "Structuring clear, loyal, and respectful response presentation",
            isCompleted = isDone || thinkingPhase == com.example.hermes.data.ThinkingPhase.COMPLETED,
            isActive = isThinkingActive && thinkingPhase == com.example.hermes.data.ThinkingPhase.FINALIZING
        ),
        SummaryStage(
            title = "Ready to serve",
            description = if (isDone) "Final response generated and served to user" else "Finalizing response output",
            isCompleted = isDone,
            isActive = isThinkingActive && thinkingPhase == com.example.hermes.data.ThinkingPhase.COMPLETED
        )
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
                    .padding(bottom = 20.dp)
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

            // Scrollable Timeline Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                stages.forEachIndexed { index, stage ->
                    val isLast = index == stages.lastIndex

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left Timeline Rail
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(26.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            stage.isActive -> BrandCoral.copy(alpha = pulseAlpha)
                                            stage.isCompleted -> Color(0xFF2E2C28)
                                            else -> Color(0xFF22211F)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            stage.isActive -> BrandCoral
                                            stage.isCompleted -> Color(0xFF5A5852)
                                            else -> Color(0xFF35332E)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stage.isCompleted && !stage.isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TextPrimaryWarm,
                                        modifier = Modifier.size(11.dp)
                                    )
                                } else if (stage.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(PureWhite)
                                    )
                                }
                            }

                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(if (index == 1 && !artifactName.isNullOrBlank()) 90.dp else 48.dp)
                                        .background(
                                            if (stage.isCompleted) Color(0xFF423F3A)
                                            else Color(0xFF2A2825)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Stage Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (isLast) 12.dp else 16.dp)
                        ) {
                            Text(
                                text = stage.title,
                                style = HermesTypography.displayLarge.copy(
                                    fontSize = 17.5.sp,
                                    color = if (stage.isActive || stage.isCompleted) TextPrimaryWarm else TextMuted,
                                    fontFamily = AnthropicSerif,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stage.description,
                                style = HermesTypography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    color = TextMuted,
                                    lineHeight = 17.sp
                                )
                            )

                            // Artifact Card attached under Stage 2
                            if (index == 1 && !artifactName.isNullOrBlank()) {
                                val safeArtifactName = artifactName
                                val safeArtifactType = artifactType ?: "Document · MD"
                                Spacer(modifier = Modifier.height(10.dp))
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
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Stop Generation Button (when thinking is active)
            if (isThinkingActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
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
