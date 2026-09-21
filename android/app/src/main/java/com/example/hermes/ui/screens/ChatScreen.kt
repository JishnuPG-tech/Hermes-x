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
import com.example.hermes.data.ChatAttachment
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
    initialModel: String = "Hermes Smart",
    onOpenDrawer: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateVoice: () -> Unit = {},
    onNavigateArtifacts: () -> Unit = {},
    onNavigateArtifactViewer: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    onNavigateConnectors: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    var composerText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<ChatAttachment>>(emptyList()) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }
    var selectedSummaryMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showChatOverflowMenu by remember { mutableStateOf(false) }
    var showVoiceEndedBanner by remember { mutableStateOf(fromVoice) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(initialModel) }
    var modelTier by remember {
        mutableStateOf(
            when {
                initialModel.contains("Coding", true) -> "Coding"
                initialModel.contains("Reasoning", true) -> "Reasoning"
                initialModel.contains("Turbo", true) -> "Turbo"
                else -> "Smart"
            }
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val webSearchEnabled by chatViewModel.webSearchEnabled.collectAsStateWithLifecycle()
    val memoryEnabled by chatViewModel.memoryEnabled.collectAsStateWithLifecycle()
    val selectedProject by chatViewModel.selectedProject.collectAsStateWithLifecycle()
    val projects by chatViewModel.projects.collectAsStateWithLifecycle()
    val approvals by chatViewModel.approvals.collectAsStateWithLifecycle()
    val pendingApprovals = remember(approvals) { approvals.filter { it.status.equals("PENDING", ignoreCase = true) } }
    var activeApprovalToReview by remember { mutableStateOf<com.example.hermes.data.ApprovalDto?>(null) }

    LaunchedEffect(Unit) {
        chatViewModel.fetchApprovals()
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val att = ChatAttachment(
                id = "cam_" + java.util.UUID.randomUUID().toString().take(8),
                name = "camera_capture.jpg",
                mimeType = "image/jpeg",
                sizeBytes = bytes.size.toLong(),
                base64Data = base64,
                isImage = true
            )
            attachments = attachments + att
        }
    }

    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "photo.jpg"
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val att = ChatAttachment(
                id = "photo_" + java.util.UUID.randomUUID().toString().take(8),
                name = fileName,
                mimeType = mime,
                localUri = uri.toString(),
                isImage = true
            )
            attachments = attachments + att
        }
    }

    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val isImg = mime.startsWith("image/")
            val att = ChatAttachment(
                id = "file_" + java.util.UUID.randomUUID().toString().take(8),
                name = fileName,
                mimeType = mime,
                localUri = uri.toString(),
                isImage = isImg
            )
            attachments = attachments + att
        }
    }

    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by chatViewModel.isStreaming.collectAsStateWithLifecycle()
    val activeThinking by chatViewModel.activeThinking.collectAsStateWithLifecycle()
    val thinkingPhase by chatViewModel.thinkingPhase.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()
    val availableModels by chatViewModel.availableModels.collectAsStateWithLifecycle()

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
            val initialAtts = chatViewModel.consumePendingAttachments()
            chatViewModel.sendMessage(initialPrompt, model = selectedModel, attachments = initialAtts)
        }
    }

    // Auto scroll down when new message or token arrives
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        val targetIndex = messages.size - 1
        if (targetIndex >= 0) {
            coroutineScope.launch {
                try {
                    listState.animateScrollToItem(targetIndex)
                } catch (_: Exception) {
                    // Ignore transient layout scroll index shifts
                }
            }
        }
    }

    val isScrolledUp by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = layoutInfo.totalItemsCount
            messages.isNotEmpty() && (totalItems - 1 - lastVisibleIndex > 1)
        }
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
                            val dynamicChatTitle = sessions.firstOrNull { it.session_id == currentSessionId }?.title
                                ?: messages.firstOrNull { it.role == "user" }?.content?.lines()?.firstOrNull { it.isNotBlank() }?.take(40)
                                ?: "Hermes Chat"

                            Text(
                                text = dynamicChatTitle,
                                style = HermesTypography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8B82),
                                    lineHeight = 17.sp
                                ),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                                    currentSessionId?.let { chatViewModel.deleteSession(it) }
                                    chatViewModel.clearMessages()
                                    showChatOverflowMenu = false
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                // Authentic Claude New Chat Welcome View
                val isKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeGreeting = when (currentHour) {
                    in 5..11 -> "Good morning, Jishnu"
                    in 12..16 -> "Good afternoon, Jishnu"
                    in 17..21 -> "Good evening, Jishnu"
                    else -> "Back at it, Jishnu"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = if (isKeyboardVisible) 0.dp else (-16).dp)
                    ) {
                        ClaudeStarburst(
                            size = if (isKeyboardVisible) 36.dp else 52.dp,
                            color = BrandCoral
                        )
                        Spacer(modifier = Modifier.height(if (isKeyboardVisible) 8.dp else 18.dp))

                        Text(
                            text = timeGreeting,
                            style = HermesTypography.displayLarge.copy(
                                fontSize = if (isKeyboardVisible) 24.sp else 32.sp,
                                lineHeight = if (isKeyboardVisible) 28.sp else 38.sp,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "How can Hermes help you today?",
                            style = HermesTypography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = TextMuted
                            )
                        )

                        if (!isKeyboardVisible) {
                            Spacer(modifier = Modifier.height(28.dp))

                            val prompts = listOf(
                                "Write a script or function",
                                "Analyze and debug code",
                                "Plan an autonomous task",
                                "Explain a technical concept"
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                prompts.forEach { prompt ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1E1D1B))
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                            .clickable {
                                                chatViewModel.sendMessage(prompt)
                                            }
                                            .padding(horizontal = 18.dp, vertical = 13.dp)
                                    ) {
                                        Text(
                                            text = prompt,
                                            style = HermesTypography.bodyMedium.copy(
                                                fontSize = 14.5.sp,
                                                color = TextPrimaryWarm,
                                                fontWeight = FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
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
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Hermes is AI and can make mistakes.",
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

            // Pending Approval Banner
            if (pendingApprovals.isNotEmpty()) {
                val firstPending = pendingApprovals.first()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF261E1A))
                        .border(1.dp, Color(0xFF5A3122), RoundedCornerShape(16.dp))
                        .clickable { activeApprovalToReview = firstPending }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3D2319)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = BrandCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pendingApprovals.size == 1) "Action Approval Required" else "${pendingApprovals.size} Actions Require Approval",
                            style = HermesTypography.bodyMedium.copy(
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        )
                        Text(
                            text = "${firstPending.tool.ifBlank { "System action" }} · Tap to review",
                            style = HermesTypography.bodySmall.copy(
                                color = Color(0xFFC0BAB0),
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandCoral)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Review",
                            style = HermesTypography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Docked Composer in Chat (Screenshot 2, 3, 4)
            ClaudeHomeComposer(
                text = composerText,
                onTextChange = { composerText = it },
                attachments = attachments,
                onRemoveAttachment = { removeId ->
                    attachments = attachments.filter { it.id != removeId }
                },
                selectedModel = selectedModel,
                modelTier = modelTier,
                showProBanner = false,
                placeholder = "Reply to Hermes...",
                isIncognito = isIncognito,
                isStreaming = isStreaming,
                onStopGeneration = { chatViewModel.stopGeneration() },
                onModelClick = { showModelSheet = true },
                onAttachClick = { showAddSheet = true },
                onVoiceClick = onNavigateVoice,
                onSend = {
                    if (composerText.isNotBlank() || attachments.isNotEmpty()) {
                        val textToSend = composerText
                        val currentAtts = attachments
                        composerText = ""
                        attachments = emptyList()
                        chatViewModel.sendMessage(textToSend, model = selectedModel, attachments = currentAtts)
                    }
                }
            )
        }

        // Floating Scroll-To-Bottom Button — only visible when user scrolled up
        AnimatedVisibility(
            visible = isScrolledUp,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2826))
                    .border(1.dp, Color(0xFF3A3834), CircleShape)
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
                    contentDescription = "Scroll to bottom",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Model Select Sheet
        if (showModelSheet) {
            ModelSelectSheet(
                selectedModel = selectedModel,
                serverModels = availableModels,
                onModelSelected = {
                    selectedModel = it
                    modelTier = when {
                        it.contains("Coding", true) -> "Coding"
                        it.contains("Reasoning", true) -> "Reasoning"
                        it.contains("Turbo", true) -> "Turbo"
                        else -> "Smart"
                    }
                    showModelSheet = false
                },
                onDismiss = { showModelSheet = false }
            )
        }

        // Add To Chat Sheet
        if (showAddSheet) {
            AddToChatSheet(
                onDismiss = { showAddSheet = false },
                webSearchEnabled = webSearchEnabled,
                onWebSearchChange = { chatViewModel.setWebSearchEnabled(it) },
                memoryEnabled = memoryEnabled,
                onMemoryChange = { chatViewModel.setMemoryEnabled(it) },
                selectedProjectName = selectedProject?.name,
                onCameraClick = {
                    showAddSheet = false
                    cameraLauncher.launch(null)
                },
                onPhotosClick = {
                    showAddSheet = false
                    photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onFilesClick = {
                    showAddSheet = false
                    fileLauncher.launch(arrayOf("*/*"))
                },
                onProjectClick = {
                    showAddSheet = false
                    showProjectDialog = true
                },
                onToolAccessClick = {
                    showAddSheet = false
                },
                onConnectorsClick = {
                    showAddSheet = false
                    onNavigateConnectors()
                }
            )
        }

        // Project Selection Dialog
        if (showProjectDialog) {
            AlertDialog(
                onDismissRequest = { showProjectDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Select Project", color = TextPrimaryWarm) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    chatViewModel.setSelectedProject(null)
                                    showProjectDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProject == null,
                                onClick = {
                                    chatViewModel.setSelectedProject(null)
                                    showProjectDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("None (Standalone chat)", color = TextPrimaryWarm)
                        }
                        projects.forEach { prj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        chatViewModel.setSelectedProject(prj)
                                        showProjectDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProject?.id == prj.id,
                                    onClick = {
                                        chatViewModel.setSelectedProject(prj)
                                        showProjectDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(prj.name, color = TextPrimaryWarm)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProjectDialog = false }) {
                        Text("Done", color = AccentBlue)
                    }
                }
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

        // Human-in-the-Loop Policy Approval Sheet
        activeApprovalToReview?.let { approval ->
            ApprovalActionSheet(
                approval = approval,
                onApprove = { approvalId ->
                    chatViewModel.approveRequest(approvalId)
                    activeApprovalToReview = null
                },
                onDeny = { approvalId, _ ->
                    chatViewModel.denyRequest(approvalId)
                    activeApprovalToReview = null
                },
                onDismiss = { activeApprovalToReview = null }
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
