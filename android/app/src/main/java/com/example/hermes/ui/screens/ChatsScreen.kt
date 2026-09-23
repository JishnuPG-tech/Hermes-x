package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.hermes.data.FtsSearchResultDto
import com.example.hermes.ui.components.VoiceWaveMiniBadge

@Composable
fun ChatsScreen(
    initialFilter: String = "all",
    onOpenDrawer: () -> Unit,
    onNavigateChat: (String?) -> Unit,
    onNewChat: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember(initialFilter) {
        mutableStateOf(
            when (initialFilter.lowercase()) {
                "voice" -> "voice"
                "chat", "chats" -> "chat"
                else -> "all"
            }
        )
    }
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()
    val ftsResults by chatViewModel.ftsSearchResults.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        chatViewModel.fetchSessions()
    }

    LaunchedEffect(searchQuery) {
        chatViewModel.searchMessagesFts(searchQuery)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { chatViewModel.fetchSessions() }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Refresh",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListBulleted,
                            contentDescription = "View",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Serif Large "Chats" Header
            Text(
                text = "Chats",
                style = HermesTypography.displayLarge.copy(
                    fontSize = 32.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1E1C))
                    .border(1.dp, BorderSubtle, CircleShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSubtle,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = HermesTypography.bodyLarge.copy(
                        fontSize = 15.5.sp,
                        color = TextPrimaryWarm
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandCoral),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search Chats & Message Contents",
                                    style = HermesTypography.bodyLarge.copy(
                                        fontSize = 15.5.sp,
                                        color = TextSubtle
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSubtle,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Filter Tabs: All | Chats | Voice
            val totalCount = sessions.size
            val voiceCount = sessions.count { it.isVoice }
            val normalCount = totalCount - voiceCount

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTabPill(
                    label = "All",
                    count = totalCount,
                    isSelected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" }
                )
                FilterTabPill(
                    label = "Chats",
                    count = normalCount,
                    isSelected = selectedFilter == "chat",
                    onClick = { selectedFilter = "chat" }
                )
                FilterTabPill(
                    label = "Voice",
                    count = voiceCount,
                    isSelected = selectedFilter == "voice",
                    onClick = { selectedFilter = "voice" },
                    hasVoiceIcon = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val typeFilteredSessions = when (selectedFilter) {
                "voice" -> sessions.filter { it.isVoice }
                "chat" -> sessions.filter { !it.isVoice }
                else -> sessions
            }

            val filteredSessions = typeFilteredSessions.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }

            val isSearchActive = searchQuery.isNotBlank()
            val noResults = isSearchActive && filteredSessions.isEmpty() && ftsResults.isEmpty()

            if (typeFilteredSessions.isEmpty() && !isSearchActive) {
                val emptyMessage = when (selectedFilter) {
                    "voice" -> "No voice conversations yet. Start a voice chat with Hermes."
                    "chat" -> "No text conversations yet. Start a new chat."
                    else -> "No conversations yet. Start a new chat."
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = HermesTypography.bodyLarge.copy(fontSize = 15.sp, color = TextSubtle)
                    )
                }
            } else if (noResults) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching chats or messages found.",
                        style = HermesTypography.bodyLarge.copy(fontSize = 15.sp, color = TextSubtle)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isSearchActive && filteredSessions.isNotEmpty()) {
                        item {
                            Text(
                                text = "MATCHED SESSIONS (${filteredSessions.size})",
                                style = HermesTypography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    items(filteredSessions, key = { it.session_id }) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateChat(session.session_id)
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (session.isVoice) {
                                        VoiceWaveMiniBadge(modifier = Modifier.padding(end = 7.dp))
                                    }
                                    Text(
                                        text = session.title.ifBlank { if (session.isVoice) "Voice conversation" else "Untitled" },
                                        style = HermesTypography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            color = TextPrimaryWarm,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val timeStr = formatEpochTime(session.updated_at)
                                val subText = if (session.isVoice) {
                                    "Voice · $timeStr · ${session.message_count} messages"
                                } else {
                                    "$timeStr · ${session.message_count} messages"
                                }
                                Text(
                                    text = subText,
                                    style = HermesTypography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = TextSubtle
                                    )
                                )
                            }

                            IconButton(
                                onClick = { chatViewModel.deleteSession(session.session_id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = TextSubtle,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isSearchActive && ftsResults.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MATCHED MESSAGE CONTENTS (${ftsResults.size})",
                                style = HermesTypography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        items(ftsResults) { fts ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1F1E1C))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable { onNavigateChat(fts.sessionId) }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CanvasNearBlack)
                                            .border(1.dp, BorderSubtle, CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = fts.sessionTitle,
                                            style = HermesTypography.labelSmall.copy(
                                                color = BrandCoral,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                    }

                                    Text(
                                        text = if (fts.role == "user") "You" else "Hermes",
                                        style = HermesTypography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = fts.snippet,
                                    style = HermesTypography.bodyMedium.copy(
                                        color = TextPrimaryWarm,
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }


        // Floating Action Button
        FloatingActionButton(
            onClick = {
                chatViewModel.clearMessages()
                onNewChat()
            },
            containerColor = BrandCoral,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Start new chat",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

private fun formatEpochTime(epochSeconds: Double): String {
    if (epochSeconds <= 0.0) return "Recently"
    val diffMillis = System.currentTimeMillis() - (epochSeconds * 1000).toLong()
    val diffMinutes = diffMillis / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minutes ago"
        diffHours < 24 -> "$diffHours hours ago"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        else -> {
            val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            sdf.format(Date((epochSeconds * 1000).toLong()))
        }
    }
}

@Composable
private fun FilterTabPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    hasVoiceIcon: Boolean = false
) {
    val bg = if (isSelected) Color(0xFF2B2824) else Color(0xFF181715)
    val borderCol = if (isSelected) BrandCoral else BorderSubtle
    val textCol = if (isSelected) PureWhite else TextMuted
    val countCol = if (isSelected) BrandCoral else TextSubtle

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasVoiceIcon) {
            VoiceWaveMiniBadge(modifier = Modifier.padding(end = 6.dp))
        }
        Text(
            text = label,
            style = HermesTypography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = textCol,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                style = HermesTypography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    color = countCol,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

