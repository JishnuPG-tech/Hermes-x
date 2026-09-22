package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

@Composable
fun ClaudeStarburst(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 54.dp,
    color: Color = BrandCoral
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val spokeWidth = this.size.width * 0.065f
        val spokeHeight = this.size.height * 0.23f
        val cornerRadius = CornerRadius(spokeWidth / 2f, spokeWidth / 2f)
        val dist = this.size.height * 0.24f
        for (i in 0 until 14) {
            val angle = i * (360f / 14f)
            withTransform({
                rotate(angle, Offset(cx, cy))
            }) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - spokeWidth / 2f, cy - dist - spokeHeight),
                    size = Size(spokeWidth, spokeHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

/**
 * Authentic Claude 3-bar hamburger icon:
 * - Top bar: full width (20dp)
 * - Middle bar: full width (20dp)
 * - Bottom bar: 60% short width (12dp), aligned to the left
 */
@Composable
fun ClaudeHamburgerIcon(
    modifier: Modifier = Modifier,
    color: Color = PureWhite
) {
    Column(
        modifier = modifier.size(width = 20.dp, height = 15.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
}

@Composable
fun ClaudeWaveformIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    tint: Color = PureBlack
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * (2f / 24f)
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val bars = listOf(
            Pair(4f / 24f, 6f / 24f),
            Pair(8f / 24f, 12f / 24f),
            Pair(12f / 24f, 16f / 24f),
            Pair(16f / 24f, 10f / 24f),
            Pair(20f / 24f, 4f / 24f)
        )
        for ((xRatio, hRatio) in bars) {
            val barH = h * hRatio
            val barX = w * xRatio
            val barY = (h - barH) / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun ClaudeGhostIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.375f, h * 0.79f)
            cubicTo(w * 0.31f, h * 0.85f, w * 0.25f, h * 0.83f, w * 0.25f, h * 0.83f)
            cubicTo(w * 0.19f, h * 0.81f, w * 0.19f, h * 0.71f, w * 0.19f, h * 0.71f)
            lineTo(w * 0.19f, h * 0.375f)
            cubicTo(w * 0.19f, h * 0.21f, w * 0.33f, h * 0.08f, w * 0.5f, h * 0.08f)
            cubicTo(w * 0.67f, h * 0.08f, w * 0.81f, h * 0.21f, w * 0.81f, h * 0.375f)
            lineTo(w * 0.81f, h * 0.71f)
            cubicTo(w * 0.81f, h * 0.81f, w * 0.75f, h * 0.83f, w * 0.75f, h * 0.83f)
            cubicTo(w * 0.75f, h * 0.83f, w * 0.69f, h * 0.85f, w * 0.625f, h * 0.79f)
            cubicTo(w * 0.58f, h * 0.75f, w * 0.54f, h * 0.77f, w * 0.5f, h * 0.75f)
            cubicTo(w * 0.46f, h * 0.77f, w * 0.42f, h * 0.75f, w * 0.375f, h * 0.79f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.395f, h * 0.395f))
        drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.605f, h * 0.395f))
    }
}

@Composable
fun ClaudeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = PureWhite,
            checkedTrackColor = AccentBlue,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = SurfacePill,
            disabledCheckedTrackColor = AccentBlue.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun HermesCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceDarkElevated,
    borderColor: Color = HairlineDivider,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun ActionPill(
    label: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (primary) BrandTerracotta else Color.Transparent)
            .border(1.dp, if (primary) BrandTerracotta else HairlineDivider, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (primary) PureWhite else TextPrimaryWarm,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = label,
            color = if (primary) PureWhite else TextPrimaryWarm,
            style = HermesTypography.bodyMedium.copy(color = if (primary) PureWhite else TextPrimaryWarm, fontSize = 13.sp)
        )
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color = AccentGreen
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            style = HermesTypography.labelSmall.copy(color = color, fontSize = 11.sp)
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HermesTypography.titleLarge.copy(fontSize = 15.sp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = HermesTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSubtle,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun VoiceEndedBanner(
    modifier: Modifier = Modifier,
    durationText: String = "4s",
    onDismiss: () -> Unit,
    onThumbsUp: () -> Unit = {},
    onThumbsDown: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, HairlineDivider, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClaudeWaveformIcon(size = 20.dp, tint = TextPrimaryWarm)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Voice chat ended",
                style = HermesTypography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            )
            Text(
                durationText,
                style = HermesTypography.bodyMedium.copy(fontSize = 12.sp, color = TextMuted)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Thumbs Up",
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onThumbsUp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Default.ThumbDown,
                contentDescription = "Thumbs Down",
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onThumbsDown)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(HairlineDivider)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}

@Composable
fun ScrollToBottomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SurfaceDarkElevated)
            .border(1.dp, HairlineDivider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Scroll to bottom",
            tint = TextPrimaryWarm,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun NeuralNodesIllustration(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 180.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // White flowing ribbon
        val ribbonPath = Path().apply {
            moveTo(w * 0.28f, h * 0.35f)
            cubicTo(w * 0.22f, h * 0.45f, w * 0.30f, h * 0.55f, w * 0.35f, h * 0.52f)
            cubicTo(w * 0.40f, h * 0.50f, w * 0.42f, h * 0.65f, w * 0.48f, h * 0.60f)
            cubicTo(w * 0.52f, h * 0.55f, w * 0.58f, h * 0.75f, w * 0.65f, h * 0.68f)
            cubicTo(w * 0.72f, h * 0.60f, w * 0.68f, h * 0.45f, w * 0.58f, h * 0.42f)
            cubicTo(w * 0.48f, h * 0.38f, w * 0.52f, h * 0.28f, w * 0.62f, h * 0.25f)
        }
        drawPath(
            ribbonPath,
            color = PureWhite,
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Coral neural connections
        val nodes = listOf(
            Offset(w * 0.30f, h * 0.28f),
            Offset(w * 0.42f, h * 0.25f),
            Offset(w * 0.45f, h * 0.38f),
            Offset(w * 0.55f, h * 0.36f),
            Offset(w * 0.68f, h * 0.36f),
            Offset(w * 0.38f, h * 0.44f)
        )
        val connections = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4), Pair(2, 5)
        )
        for ((start, end) in connections) {
            drawLine(
                color = BrandCoral,
                start = nodes[start],
                end = nodes[end],
                strokeWidth = w * 0.018f,
                cap = StrokeCap.Round
            )
        }
        for (node in nodes) {
            drawCircle(
                color = BrandCoral,
                radius = w * 0.045f,
                center = node
            )
        }
    }
}

@Composable
fun ClaudeNoteAddIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 18.dp,
    tint: Color = Color(0xFF8E8B82)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            lineTo(w * 0.55f, h * 0.1f)
            lineTo(w * 0.8f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.9f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val fold = Path().apply {
            moveTo(w * 0.55f, h * 0.1f)
            lineTo(w * 0.55f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.35f)
        }
        drawPath(fold, color = tint, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tint, start = Offset(w * 0.35f, h * 0.62f), end = Offset(w * 0.65f, h * 0.62f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.47f), end = Offset(w * 0.5f, h * 0.77f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}
