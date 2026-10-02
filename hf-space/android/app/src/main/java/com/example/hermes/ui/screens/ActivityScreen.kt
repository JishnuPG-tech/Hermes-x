package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.ActivityEventDto
import com.example.hermes.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val events by chatViewModel.activityEvents.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        chatViewModel.fetchActivity()
    }

    Scaffold(
        containerColor = CanvasNearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity",
                        style = HermesTypography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { chatViewModel.fetchActivity() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CanvasNearBlack
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Live timeline of background tasks, tool executions, and system events.",
                style = HermesTypography.bodyMedium.copy(
                    color = Color(0xFF9E9C96),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = Color(0xFF6B6A65),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recent activity recorded",
                            style = HermesTypography.bodyLarge.copy(color = Color(0xFF9E9C96))
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(events, key = { it.id }) { event ->
                        ActivityEventCard(event)
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityEventCard(event: ActivityEventDto) {
    val icon: ImageVector = when (event.type.lowercase()) {
        "system" -> Icons.Outlined.Dns
        "knowledge" -> Icons.Outlined.AutoStories
        "tool" -> Icons.Outlined.Build
        else -> Icons.Outlined.TaskAlt
    }

    val statusColor = when (event.status.lowercase()) {
        "completed", "succeeded", "done" -> Color(0xFF4CAF50)
        "running", "in_progress" -> BrandCoral
        "failed", "error" -> Color(0xFFE57373)
        else -> Color(0xFFB0AEA5)
    }

    val formattedTime = remember(event.timestamp) {
        if (event.timestamp > 0) {
            val date = Date((event.timestamp * 1000).toLong())
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        } else "Recent"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262523)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BrandCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = event.type,
                            style = HermesTypography.labelSmall.copy(
                                color = BrandCoral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                        Text(
                            text = formattedTime,
                            style = HermesTypography.bodySmall.copy(
                                color = Color(0xFF8E8D8A),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.status,
                        style = HermesTypography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(
                text = event.title,
                style = HermesTypography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )

            if (event.details.isNotBlank()) {
                Text(
                    text = event.details,
                    style = HermesTypography.bodySmall.copy(
                        color = Color(0xFFD4D2CD),
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Worker: ${event.worker}",
                style = HermesTypography.bodySmall.copy(
                    color = Color(0xFF8E8D8A),
                    fontSize = 12.sp
                )
            )
        }
    }
}
