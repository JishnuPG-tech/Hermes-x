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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.ui.text.font.FontWeight
import com.example.hermes.data.ChatAttachment
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
        size = Size(this.size.width - strokePx, this.size.height - strokePx),
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
    attachments: List<ChatAttachment> = emptyList(),
    onRemoveAttachment: (String) -> Unit = {},
    selectedModel: String = "Hermes Smart",
    modelTier: String = "Smart",
    showProBanner: Boolean = false,
    placeholder: String = "Reply to Hermes...",
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
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1E1D1B))
            .dashedBorder(strokeWidth = 1.2.dp, color = Color(0xFF4A4843), cornerRadius = 26.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1E1D1B))
            .border(1.dp, Color(0xFF2E2C29), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    }

    Column(modifier = baseModifier) {
        // Attachment Preview Chips
        if (attachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachments, key = { it.id }) { att ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2B2A27))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (att.isImage) Icons.Outlined.Image else Icons.Outlined.InsertDriveFile,
                            contentDescription = null,
                            tint = BrandCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = att.name.take(20),
                            style = HermesTypography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = TextSubtle,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveAttachment(att.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Chat Input Field (generous height, never compressed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp, max = 140.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val effectivePlaceholder = if (isStreaming) "Sending..." else placeholder
            if (text.isEmpty()) {
                Text(
                    text = effectivePlaceholder,
                    color = Color(0xFF8E8B82),
                    style = HermesTypography.bodyLarge.copy(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = !isStreaming,
                textStyle = HermesTypography.bodyLarge.copy(
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    lineHeight = 22.sp
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    autoCorrect = true,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Default
                ),
                cursorBrush = SolidColor(BrandCoral),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Controls Row: [+] [Model Pill] ... [Mic] [Waveform/Send]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Add Attachment Button & Model Selector Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add (+) icon button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2B28))
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

                // Clean Model Selector Chip (Exact Claude pill layout)
                val cleanModelName = remember(selectedModel) {
                    when {
                        selectedModel.contains("Coding", ignoreCase = true) -> "Coding"
                        selectedModel.contains("Reasoning", ignoreCase = true) -> "Reasoning"
                        selectedModel.contains("Turbo", ignoreCase = true) -> "Turbo"
                        selectedModel.contains("Smart", ignoreCase = true) -> "Smart"
                        else -> selectedModel.removePrefix("Hermes ").ifBlank { selectedModel }
                    }
                }

                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2B28))
                        .clickable(onClick = onModelClick)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hermes $cleanModelName",
                        style = HermesTypography.bodyMedium.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right Group: Dictation Mic & Voice Waveform / Send Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dictation Mic Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2B28))
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

                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                    val canSend = text.isNotBlank() || attachments.isNotEmpty()
                    AnimatedContent(
                        targetState = canSend,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith
                                    (scaleOut(targetScale = 0.8f) + fadeOut())
                        },
                        label = "SendButtonAnimation"
                    ) { isSending ->
                        if (isSending) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
