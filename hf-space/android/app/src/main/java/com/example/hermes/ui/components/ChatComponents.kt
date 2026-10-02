package com.example.hermes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

@Composable
fun ExtendedThinkingBox(
    thinkingText: String,
    isThinking: Boolean = false,
    durationSeconds: Int = 4,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isThinking) {
                    HermesThinkingDot(size = 10.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thinking…",
                        style = HermesTypography.bodyMedium.copy(
                            color = BrandCoral,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thought for $durationSeconds seconds",
                        style = HermesTypography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HairlineDivider)
                        .padding(bottom = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thinkingText,
                    style = HermesTypography.bodyMedium.copy(
                        fontFamily = JetBrainsMono,
                        color = TextSubtle,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String = "kotlin",
    onCopy: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181715))
            .border(1.dp, Color(0xFF282724), RoundedCornerShape(12.dp))
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF201F1D))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.lowercase(),
                style = HermesTypography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Row(
                modifier = Modifier.clickable(onClick = onCopy),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Copy",
                    style = HermesTypography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Code Content
        Text(
            text = code,
            style = HermesTypography.bodyMedium.copy(
                fontFamily = JetBrainsMono,
                color = TextPrimaryWarm,
                fontSize = 12.5.sp,
                lineHeight = 19.sp
            ),
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun ToolExecutionCard(
    toolName: String,
    status: String = "Completed",
    output: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, HairlineDivider, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = BrandCoral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = toolName,
                    style = HermesTypography.titleLarge.copy(fontSize = 13.5.sp, color = TextPrimaryWarm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(status, color = if (status == "Running") BrandCoral else AccentGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (expanded && output != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = output,
                style = HermesTypography.bodyMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            )
        }
    }
}

@Composable
fun ChatActionRow(
    onCopy: () -> Unit = {},
    onThumbsUp: () -> Unit = {},
    onThumbsDown: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onThumbsUp, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.ThumbUp, contentDescription = "Good response", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onThumbsDown, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.ThumbDown, contentDescription = "Bad response", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Retry", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color = AccentGreen,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = HermesTypography.labelSmall.copy(color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun HermesCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}
