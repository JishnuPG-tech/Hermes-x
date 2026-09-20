package com.example.hermes.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

fun Modifier.dashedBorder(
    strokeWidth: Dp = 1.dp,
    color: Color,
    cornerRadius: Dp = 28.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp
) = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokePx / 2f, strokePx / 2f),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
        )
    )
}

@Composable
fun ClaudeHomeComposer(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    selectedModel: String = "Sonnet 5",
    modelTier: String = "Low",
    showProBanner: Boolean = true,
    placeholder: String = if (showProBanner) "Chat with Claude..." else "Reply to Claude...",
    isIncognito: Boolean = false,
    isStreaming: Boolean = false,
    onModelClick: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onSend: () -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    val baseModifier = if (isIncognito) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1B1A18))
            .dashedBorder(strokeWidth = 1.2.dp, color = Color(0xFF4A4843), cornerRadius = 28.dp)
            .padding(10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1B1A18))
            .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
            .padding(10.dp)
    }

    Column(modifier = baseModifier) {
        // Top Pro Upgrade Banner (12-00-55, 12-06-53)
        if (showProBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF242320))
                    .clickable(onClick = onUpgradeClick)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Get more with Claude Pro",
                    style = HermesTypography.bodyMedium.copy(
                        color = Color(0xFFB0AEA5),
                        fontSize = 13.5.sp
                    )
                )
                Text(
                    text = "Upgrade to Pro",
                    style = HermesTypography.bodyMedium.copy(
                        color = Color(0xFFB395F7),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        fontSize = 13.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Chat Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (showProBanner) 4.dp else 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val effectivePlaceholder = if (isStreaming) "Sending..." else placeholder
            if (text.isEmpty()) {
                Text(
                    text = effectivePlaceholder,
                    color = Color(0xFF7E7B74),
                    style = HermesTypography.bodyLarge.copy(fontSize = 16.5.sp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = !isStreaming,
                textStyle = HermesTypography.bodyLarge.copy(
                    color = TextPrimaryWarm,
                    fontSize = 16.5.sp,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(BrandCoral),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Controls Row: [+] , [Model Pill] , [Mic] , [Waveform / Send]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Add Attachment Button & Model Selector Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add (+) icon button (12-00-55)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2926))
                        .clickable(onClick = onAttachClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add files or content",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Model Selector Chip (12-00-55: "Sonnet 5 Low")
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2926))
                        .clickable(onClick = onModelClick)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedModel,
                        style = HermesTypography.bodyMedium.copy(
                            color = TextPrimaryWarm,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modelTier,
                        style = HermesTypography.bodyMedium.copy(
                            color = Color(0xFF8E8B82),
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Right Group: Dictation Mic & Realtime Voice Waveform / Send Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dictation Mic Button (12-00-55)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2926))
                        .clickable(onClick = onVoiceClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice dictation",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // If streaming: Show authentic Stop Generation square button (Screenshot 2)
                // Otherwise: Animated Send Pill (Waveform when empty, Coral Arrow when text typed)
                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262523))
                            .border(1.dp, Color(0xFF383632), CircleShape)
                            .clickable(onClick = onStopGeneration),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PureWhite)
                        )
                    }
                } else {
                    val canSend = text.isNotBlank()
                    AnimatedContent(
                        targetState = canSend,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith
                                    (scaleOut(targetScale = 0.8f) + fadeOut())
                        },
                        label = "SendButtonAnimation"
                    ) { isSending ->
                        if (isSending) {
                            // Solid Coral Circle with White Arrow (21-44-47)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BrandCoral)
                                    .clickable(onClick = onSend),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Solid Pure White Circle with Black Waveform Icon (12-00-55)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite)
                                    .clickable(onClick = onVoiceClick),
                                contentAlignment = Alignment.Center
                            ) {
                                ClaudeWaveformIcon(size = 20.dp, tint = PureBlack)
                            }
                        }
                    }
                }
            }
        }
    }
}
