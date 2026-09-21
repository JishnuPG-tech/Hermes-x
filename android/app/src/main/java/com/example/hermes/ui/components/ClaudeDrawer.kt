package com.example.hermes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
    onNavigateProjects: () -> Unit,
    onNavigateCode: () -> Unit,
    onNavigateArtifacts: () -> Unit,
    onNavigateTasks: () -> Unit,
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
            // Hermes Serif Wordmark (12-01-18)
            Text(
                text = "Hermes",
                style = HermesTypography.displayLarge.copy(
                    fontSize = 40.sp,
                    color = TextPrimaryWarm,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 28.dp)
                    .clickable(onClick = onNavigateHome)
            )

            // Primary Navigation Menu Links (12-01-18: Chats, Projects, Code, Artifacts)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Chats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateChats)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Chats",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 20.sp,
                            color = TextPrimaryWarm
                        )
                    )
                }

                // Projects (Canister icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateProjects)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanisterIcon(size = 22.dp, tint = TextPrimaryWarm)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Projects",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 20.sp,
                            color = TextPrimaryWarm
                        )
                    )
                }

                // Code (</> brackets icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateCode)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodeBracketsIcon(size = 22.dp, tint = TextPrimaryWarm)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Code",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 20.sp,
                            color = TextPrimaryWarm
                        )
                    )
                }

                // Artifacts (Geometric shapes icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateArtifacts)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtifactShapesIcon(size = 22.dp, tint = TextPrimaryWarm)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Artifacts",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 20.sp,
                            color = TextPrimaryWarm
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
            Spacer(modifier = Modifier.height(14.dp))

            val pinnedSessions = sessions.filter { it.pinned }
            val recentSessions = sessions.filter { !it.pinned }.take(8)

            // Scrollable Pinned & Recents
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
                                color = TextSubtle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(pinnedSessions.size) { index ->
                        val item = pinnedSessions[index]
                        Text(
                            text = item.title.ifBlank { "Untitled" },
                            style = HermesTypography.bodyLarge.copy(
                                fontSize = 16.5.sp,
                                color = TextPrimaryWarm,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRecentChat(item.session_id) }
                                .padding(vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                if (recentSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recents",
                            style = HermesTypography.bodyMedium.copy(
                                color = TextSubtle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(recentSessions.size) { index ->
                        val item = recentSessions[index]
                        Text(
                            text = item.title.ifBlank { "Untitled" },
                            style = HermesTypography.bodyLarge.copy(
                                fontSize = 16.5.sp,
                                color = TextPrimaryWarm,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRecentChat(item.session_id) }
                                .padding(vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
            // Salmon Circle Avatar with User Initial
            val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString()
                ?: userEmail.trim().firstOrNull()?.uppercaseChar()?.toString()
                ?: "U"
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
                        fontSize = 20.sp,
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
                        fontWeight = FontWeight.Medium
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
