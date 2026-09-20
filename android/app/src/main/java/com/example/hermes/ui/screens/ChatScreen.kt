package com.example.hermes.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.ChatMessage
import com.example.hermes.theme.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.hermes.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    initialPrompt: String? = null,
    sessionId: String? = null,
    isIncognito: Boolean = false,
    fromVoice: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateVoice: () -> Unit = {},
    onNavigateArtifacts: () -> Unit = {},
    onNavigateArtifactViewer: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    chatViewModel: ChatViewModel = viewModel()
) {
    var composerText by remember { mutableStateOf("") }
    var showSummarySheet by remember { mutableStateOf(false) }
    var selectedSummaryMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showChatOverflowMenu by remember { mutableStateOf(false) }
    var showVoiceEndedBanner by remember { mutableStateOf(fromVoice) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("Sonnet 3.7") }
    var modelTier by remember { mutableStateOf("Low") }

    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by chatViewModel.isStreaming.collectAsStateWithLifecycle()
    val activeThinking by chatViewModel.activeThinking.collectAsStateWithLifecycle()
    val thinkingPhase by chatViewModel.thinkingPhase.collectAsStateWithLifecycle()

    val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""
    val isCalcPrompt = lastUserMessage.contains("calc", ignoreCase = true) || lastUserMessage.contains("math", ignoreCase = true)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasSentInitialPrompt by rememberSaveable(initialPrompt, sessionId) { mutableStateOf(false) }

    // Load session if sessionId is provided
    LaunchedEffect(sessionId) {
        if (!sessionId.isNullOrBlank()) {
            chatViewModel.loadSession(sessionId)
        }
    }

    // Auto-send initial prompt if provided and no session is being loaded (guarded to prevent replay on back navigation)
    LaunchedEffect(initialPrompt, sessionId) {
        if (!initialPrompt.isNullOrBlank() && sessionId.isNullOrBlank() && !hasSentInitialPrompt) {
            hasSentInitialPrompt = true
            chatViewModel.sendMessage(initialPrompt)
        }
    }

    // Auto scroll down when new message or token arrives
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val isScrolledUp by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
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
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Top App Bar (Matches Screenshots 1, 2, 3, 4, 7)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Hamburger Menu
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: Dynamic Floating Pill (Screenshot 7: "1 Artifact")
                val artifactsInChat = messages.count { !it.artifactTitle.isNullOrBlank() }
                if (artifactsInChat > 0) {
                    val artifactCountText = "$artifactsInChat Artifact${if (artifactsInChat > 1) "s" else ""}"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF242320))
                            .border(1.dp, Color(0xFF383632), CircleShape)
                            .clickable(onClick = onNavigateArtifacts)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artifactCountText,
                            style = HermesTypography.labelMedium.copy(
                                fontSize = 13.5.sp,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Right: New Chat (+) and Overflow (⋮)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { chatViewModel.clearMessages() }) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "New chat",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showChatOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimaryWarm,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showChatOverflowMenu,
                            onDismissRequest = { showChatOverflowMenu = false },
                            modifier = Modifier
                                .width(270.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1E1D1B))
                                .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Claude Android UI/UX design kit documentation",
                                style = HermesTypography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8B82),
                                    lineHeight = 17.sp
                                ),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )

                            ChatMenuItem(
                                title = "Share",
                                icon = Icons.Outlined.Share,
                                onClick = { showChatOverflowMenu = false }
                            )
                            ChatMenuItem(
                                title = "Rename",
                                icon = Icons.Outlined.Edit,
                                onClick = { showChatOverflowMenu = false }
                            )
                            ChatMenuItem(
                                title = if (isPinned) "Unpin" else "Pin",
                                icon = Icons.Outlined.PushPin,
                                onClick = {
                                    isPinned = !isPinned
                                    showChatOverflowMenu = false
                                }
                            )
                            ChatMenuItem(
                                title = "Add to project",
                                customIcon = { CanisterIcon(size = 20.dp, tint = TextPrimaryWarm) },
                                onClick = { showChatOverflowMenu = false }
                            )
                            ChatMenuItem(
                                title = "Add to home",
                                icon = Icons.Outlined.Home,
                                onClick = { showChatOverflowMenu = false }
                            )
                            ChatMenuItem(
                                title = "Delete",
                                icon = Icons.Outlined.DeleteOutline,
                                titleColor = DestructiveRed,
                                onClick = {
                                    showChatOverflowMenu = false
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }

            // Scrollable Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(messages) { index, message ->
                    if (message.role == "user") {
                        // User message bubble (Matches Screenshots 2 & 3)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF262523))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    style = HermesTypography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = TextPrimaryWarm,
                                        lineHeight = 23.sp
                                    )
                                )
                            }
                        }
                    } else {
                        // Assistant Response Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isMessageStreaming = message.isStreaming || (index == messages.lastIndex && isStreaming)
                            val hasContent = message.content.isNotBlank()

                            // Thinking state lifecycle (Screenshots 3, 4, 5, 6, 7):
                            // 1. When loading and waiting for first tokens:
                            //    Show the discreet status line + 8-frame sequential coral starburst thinking animation!
                            // 2. Once response starts, hide the thinking animation and show artifact card + clean response!
                            if (isMessageStreaming && !hasContent) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Stepper Line
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedSummaryMessage = message
                                                showSummarySheet = true
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when (thinkingPhase) {
                                             com.example.hermes.data.ThinkingPhase.CREATING_FILE -> {
                                                ClaudeNoteAddIcon(
                                                    size = 18.dp,
                                                    tint = Color(0xFF8E8B82)
                                                )
                                            }
                                            com.example.hermes.data.ThinkingPhase.FINALIZING -> {
                                                Icon(
                                                    imageVector = Icons.Outlined.Schedule,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8E8B82),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            else -> {
                                                // Solid Coral Dot (Screenshot 3 & 4)
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandCoral)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = activeThinking ?: "Thought process",
                                            style = HermesTypography.bodyMedium.copy(
                                                fontSize = 14.5.sp,
                                                color = Color(0xFF8E8B82)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFF8E8B82),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Authentic 8-Frame Sequential Coral Starburst Animation
                                    ClaudeSparkThinkingAnimation(
                                        size = 28.dp,
                                        tint = BrandCoral,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            } else {
                                // Response ready / streamed -> Discreet single-line stepper
                                if (!message.stepTitle.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedSummaryMessage = message
                                                showSummarySheet = true
                                            }
                                            .padding(vertical = 4.dp, horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF8E8B82),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message.stepTitle ?: "Thought process",
                                            style = HermesTypography.bodyMedium.copy(
                                                fontSize = 14.5.sp,
                                                color = Color(0xFF8E8B82)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFF8E8B82),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Embedded Artifact Card (Screenshots 2, 4, 7) - ONLY if artifact was created
                                if (!message.artifactTitle.isNullOrBlank()) {
                                    val artTitle = message.artifactTitle ?: "Artifact"
                                    val artType = message.artifactType ?: "Document · MD"
                                    val isCodeArtifact = artType.contains("Code", ignoreCase = true) ||
                                            artType.contains("PY", ignoreCase = true) ||
                                            artType.contains("JS", ignoreCase = true) ||
                                            artType.contains("Kotlin", ignoreCase = true)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF1B1A18))
                                            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                                            .clickable {
                                                onNavigateArtifactViewer(artTitle, artType, message.artifactCode, message.artifactLanguage)
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF262523)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCodeArtifact) Icons.Outlined.Code else Icons.Outlined.Description,
                                                contentDescription = null,
                                                tint = TextPrimaryWarm,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = artTitle,
                                                style = HermesTypography.titleMedium.copy(
                                                    fontSize = 16.5.sp,
                                                    color = TextPrimaryWarm,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = artType,
                                                style = HermesTypography.bodySmall.copy(
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF8E8B82)
                                                )
                                            )
                                        }
                                    }
                                }

                                // Assistant Formatted Content using ClaudeMarkdownView (Zero raw **, ##, || leak!)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ClaudeMarkdownView(
                                        content = message.content,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Assistant Action Row (Copy, Share, Listen, Thumbs Up, Thumbs Down, Regenerate)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(19.dp).clickable { }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(19.dp).clickable { }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = "Listen",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(20.dp).clickable { }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ThumbUp,
                                        contentDescription = "Good",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(19.dp).clickable { }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ThumbDown,
                                        contentDescription = "Bad",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(19.dp).clickable { }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = Color(0xFF8E8B82),
                                        modifier = Modifier.size(19.dp).clickable { }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bottom Disclaimer Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ClaudeStarburst(size = 20.dp, color = BrandCoral)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Claude is AI and can make mistakes.",
                                            style = HermesTypography.labelSmall.copy(
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF8E8B82)
                                            )
                                        )
                                        Text(
                                            text = "Please double-check responses.",
                                            style = HermesTypography.labelSmall.copy(
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF8E8B82)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Post-Voice Banner
            if (showVoiceEndedBanner) {
                VoiceEndedBanner(
                    durationText = "4s",
                    onDismiss = { showVoiceEndedBanner = false },
                    onThumbsUp = { showVoiceEndedBanner = false },
                    onThumbsDown = { showVoiceEndedBanner = false },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Docked Composer in Chat (Screenshot 2, 3, 4)
            ClaudeHomeComposer(
                text = composerText,
                onTextChange = { composerText = it },
                selectedModel = selectedModel,
                modelTier = modelTier,
                showProBanner = false,
                placeholder = "Reply to Claude...",
                isIncognito = isIncognito,
                isStreaming = isStreaming,
                onStopGeneration = { chatViewModel.stopGeneration() },
                onModelClick = { showModelSheet = true },
                onAttachClick = { showAddSheet = true },
                onVoiceClick = onNavigateVoice,
                onSend = {
                    if (composerText.isNotBlank()) {
                        val textToSend = composerText
                        composerText = ""
                        chatViewModel.sendMessage(textToSend)
                    }
                }
            )
        }

        // Floating Scroll-To-Bottom Button (Screenshot 4)
        if (isScrolledUp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 82.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262523))
                    .border(1.dp, Color(0xFF383632), CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll down",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Model Select Sheet
        if (showModelSheet) {
            ModelSelectSheet(
                selectedModel = selectedModel,
                onModelSelected = {
                    selectedModel = it
                    showModelSheet = false
                },
                onDismiss = { showModelSheet = false }
            )
        }

        // Add To Chat Sheet
        if (showAddSheet) {
            AddToChatSheet(
                onDismiss = { showAddSheet = false },
                onCameraClick = { showAddSheet = false },
                onPhotosClick = { showAddSheet = false },
                onFilesClick = { showAddSheet = false },
                onProjectClick = { showAddSheet = false },
                onToolAccessClick = { showAddSheet = false },
                onConnectorsClick = { showAddSheet = false }
            )
        }

        // Reasoning Stepper Summary Sheet (Screenshots 4, 5, 6, 7)
        if (showSummarySheet) {
            val targetMsg = selectedSummaryMessage ?: messages.lastOrNull { it.role == "assistant" }
            val promptTopic = targetMsg?.stepTitle ?: lastUserMessage.ifBlank { "Thought process" }

            ExecutionSummarySheet(
                onDismiss = { showSummarySheet = false },
                onStopGeneration = {
                    chatViewModel.stopGeneration()
                    showSummarySheet = false
                },
                isThinkingActive = isStreaming && (targetMsg == null || targetMsg.id == messages.lastOrNull()?.id),
                promptTopic = promptTopic,
                artifactName = targetMsg?.artifactTitle,
                artifactType = targetMsg?.artifactType,
                thinkingPhase = thinkingPhase,
                onOpenArtifact = { title, type ->
                    showSummarySheet = false
                    onNavigateArtifactViewer(title, type, targetMsg?.artifactCode, targetMsg?.artifactLanguage)
                }
            )
        }
    }
}

@Composable
private fun ChatMenuItem(
    title: String,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    titleColor: Color = TextPrimaryWarm,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = HermesTypography.bodyLarge.copy(
                fontSize = 16.sp,
                color = titleColor,
                fontWeight = FontWeight.Normal
            )
        )
    }
}
