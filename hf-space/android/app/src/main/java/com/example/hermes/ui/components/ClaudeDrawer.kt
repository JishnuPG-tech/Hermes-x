package com.example.hermes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.data.SessionDto
import com.example.hermes.theme.*

@Composable
fun ClaudeDrawerContent(
    sessions: List<SessionDto> = emptyList(),
    userName: String = "User",
    userEmail: String = "",
    onNavigateHome: () -> Unit,
    onNavigateChats: () -> Unit,
    onNavigateVoiceChats: () -> Unit = {},
    onNavigateProjects: () -> Unit,
    onNavigateTasks: () -> Unit = {},
    onNavigateAgents: () -> Unit = {},
    onNavigateSkills: () -> Unit = {},
    onNavigateKnowledge: () -> Unit = {},
    onNavigateArtifacts: () -> Unit,
    onNavigateActivity: () -> Unit = {},
    onNavigateSettings: () -> Unit,
    onNewChat: () -> Unit,
    onOpenRecentChat: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Hermes Serif Wordmark — Exact Claude Reference (Image 3: 36sp serif normal weight)
            Text(
                text = "Hermes",
                style = HermesTypography.displayLarge.copy(
                    fontFamily = AnthropicSerif,
                    fontSize = 36.sp,
                    color = PureWhite,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 20.dp)
                    .clickable(onClick = onNavigateHome)
            )

            // Primary Navigation Menu Links
            // Exact Claude items: Chats, Projects, Skills, Artifacts
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Chats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateChats)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnthropicIcon(
                        drawableId = AnthropicIcons.Chats,
                        contentDescription = null,
                        tint = PureWhite,
                        size = 23.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Chats",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // 2. Projects
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateProjects)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnthropicIcon(
                        drawableId = AnthropicIcons.Projects,
                        contentDescription = null,
                        tint = PureWhite,
                        size = 23.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Projects",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // 3. Skills (100% pure authentic Claude Spark icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateSkills)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnthropicIcon(
                        drawableId = AnthropicIcons.Spark,
                        contentDescription = null,
                        tint = PureWhite,
                        size = 23.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Skills",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // 4. Artifacts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateArtifacts)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnthropicIcon(
                        drawableId = AnthropicIcons.Artifacts,
                        contentDescription = null,
                        tint = PureWhite,
                        size = 23.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Artifacts",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
            Spacer(modifier = Modifier.height(14.dp))

            val pinnedSessions = sessions.filter { it.pinned }
            val recentSessions = sessions.filter { !it.pinned }.take(8)

            // Scrollable Pinned & Recents (Exact Claude Reference: Title Case headers, bold pure white items)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Pinned Header & Items (if any)
                if (pinnedSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pinned",
                            style = HermesTypography.bodyMedium.copy(
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 2.dp, bottom = 12.dp)
                        )
                    }

                    items(pinnedSessions.size) { index ->
                        val item = pinnedSessions[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenRecentChat(item.session_id) }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title.ifBlank { "New chat" },
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                if (recentSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recents",
                            style = HermesTypography.bodyMedium.copy(
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 2.dp, bottom = 12.dp)
                        )
                    }

                    items(recentSessions.size) { index ->
                        val item = recentSessions[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenRecentChat(item.session_id) }
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isVoice) {
                                VoiceWaveMiniBadge(modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(
                                text = item.title.ifBlank { if (item.isVoice) "Voice Chat" else "New chat" },
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        }

        // Floating Bottom Action Footer (12-01-18: Salmon avatar on left, white pill on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Salmon Circle Avatar with User Initial (Exact Claude reference: Image 3)
            val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString()
                ?: userEmail.trim().firstOrNull()?.uppercaseChar()?.toString()
                ?: "J"
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AvatarSalmon)
                    .clickable(onClick = onNavigateSettings),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = HermesTypography.headlineMedium.copy(
                        color = PureWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // New Chat Solid White Pill (12-01-18)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PureBlack,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New chat",
                    style = HermesTypography.titleMedium.copy(
                        color = PureBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// Custom 1:1 Canister / Cylinder icon (12-01-18)
@Composable
fun CanisterIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimaryWarm
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // Top lid handle
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.18f),
            end = Offset(w * 0.65f, h * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Canister Body
        val bodyPath = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.8f, h * 0.35f)
            lineTo(w * 0.74f, h * 0.85f)
            lineTo(w * 0.26f, h * 0.85f)
            close()
        }
        drawPath(
            bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// Custom 1:1 Hermes Agent Terminal Console icon
@Composable
fun HermesAgentMenuIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimaryWarm
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // Console screen rounded outline
        val corner = 3.5.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.14f),
            size = Size(w * 0.84f, h * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(width = stroke)
        )

        // > prompt arrow
        val promptPath = Path().apply {
            moveTo(w * 0.25f, h * 0.38f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.25f, h * 0.62f)
        }
        drawPath(promptPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // _ blinking cursor bar
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.62f),
            end = Offset(w * 0.72f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// Custom 1:1 Code Brackets </> icon (12-01-18)
@Composable
fun CodeBracketsIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimaryWarm
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // < bracket
        val leftPath = Path().apply {
            moveTo(w * 0.36f, h * 0.28f)
            lineTo(w * 0.16f, h * 0.50f)
            lineTo(w * 0.36f, h * 0.72f)
        }
        drawPath(leftPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // > bracket
        val rightPath = Path().apply {
            moveTo(w * 0.64f, h * 0.28f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.72f)
        }
        drawPath(rightPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Custom 1:1 Artifacts Shapes icon (12-01-18)
@Composable
fun ArtifactShapesIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimaryWarm
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // Top circle
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.32f, h * 0.35f),
            style = Stroke(width = stroke)
        )

        // Bottom square
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.52f, h * 0.48f),
            size = Size(w * 0.32f, w * 0.32f),
            style = Stroke(width = stroke)
        )

        // Triangle
        val triPath = Path().apply {
            moveTo(w * 0.22f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.32f, h * 0.62f)
            close()
        }
        drawPath(triPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Custom 1:1 Claude-styled Voice Waveform icon
@Composable
fun VoiceWaveDrawerIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = TextPrimaryWarm
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        val bars = listOf(
            Pair(0.18f, 0.35f),
            Pair(0.38f, 0.80f),
            Pair(0.58f, 0.55f),
            Pair(0.78f, 0.40f)
        )

        bars.forEach { (xFrac, hFrac) ->
            val x = w * xFrac
            val barH = h * hFrac
            val yStart = (h - barH) / 2f
            drawLine(
                color = tint,
                start = Offset(x, yStart),
                end = Offset(x, yStart + barH),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

// Custom miniature audio wave badge for Recents and Chat lists
@Composable
fun VoiceWaveMiniBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        val bars = listOf(0.35f, 0.85f, 0.55f)
        bars.forEachIndexed { i, hFrac ->
            val x = w * (0.22f + i * 0.28f)
            val barH = h * hFrac
            val yStart = (h - barH) / 2f
            drawLine(
                color = BrandCoral,
                start = Offset(x, yStart),
                end = Offset(x, yStart + barH),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
