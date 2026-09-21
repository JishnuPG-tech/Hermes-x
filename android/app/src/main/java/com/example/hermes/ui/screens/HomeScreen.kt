package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.ChatAttachment
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

@Composable
fun HomeScreen(
    userName: String = "Jishnu",
    userEmail: String = "",
    availableModels: List<com.example.hermes.data.ModelOptionDto> = emptyList(),
    onOpenDrawer: () -> Unit,
    onNavigateChat: (String?, String) -> Unit,
    onNavigateVoice: () -> Unit,
    onNavigateIncognito: () -> Unit = { onNavigateChat(null, "Hermes Smart") },
    onUpgradeClick: () -> Unit = {},
    onNavigateConnectors: () -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    val isAdmin = remember(userEmail) { com.example.hermes.data.PreferencesManager.isUserAdmin(userEmail) }
    var composerText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<ChatAttachment>>(emptyList()) }
    var selectedModel by remember { mutableStateOf("Hermes Smart") }
    var modelTier by remember { mutableStateOf("Smart") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val webSearchEnabled by chatViewModel.webSearchEnabled.collectAsStateWithLifecycle()
    val memoryEnabled by chatViewModel.memoryEnabled.collectAsStateWithLifecycle()
    val selectedProject by chatViewModel.selectedProject.collectAsStateWithLifecycle()
    val projects by chatViewModel.projects.collectAsStateWithLifecycle()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar: Hamburger on left, Ghost/Incognito on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onNavigateIncognito) {
                    ClaudeGhostIcon(tint = TextMuted, size = 25.dp)
                }
            }

            // Center Hero Greeting Section (Anthropic Starburst + Serif greeting)
            val isKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
            val greetingText = remember(currentHour, userName) {
                when (currentHour) {
                    in 5..11 -> "Good morning, $userName"
                    in 12..16 -> "Good afternoon, $userName"
                    in 17..21 -> "Good evening, $userName"
                    else -> "Back at it, $userName"
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = if (isKeyboardVisible) 0.dp else (-20).dp)
                ) {
                    Text(
                        text = greetingText,
                        style = HermesTypography.displayLarge.copy(
                            fontSize = if (isKeyboardVisible) 24.sp else 32.sp,
                            lineHeight = if (isKeyboardVisible) 28.sp else 38.sp,
                            color = PureWhite
                        )
                    )
                }
            }

            // Sleek Pro Banner (above composer, full width, uncompressed) - Hidden for Admin account
            if (!isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF22211F))
                        .border(1.dp, Color(0xFF33312E), RoundedCornerShape(16.dp))
                        .clickable(onClick = onUpgradeClick)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Get more with Hermes Pro",
                        style = HermesTypography.bodyMedium.copy(
                            color = Color(0xFFC4C2BA),
                            fontSize = 13.5.sp
                        )
                    )
                    Text(
                        text = "Upgrade to Pro",
                        style = HermesTypography.bodyMedium.copy(
                            color = Color(0xFFB395F7),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Docked Bottom Claude Home Composer
            ClaudeHomeComposer(
                text = composerText,
                onTextChange = { composerText = it },
                selectedModel = selectedModel,
                modelTier = modelTier,
                placeholder = "Chat with Hermes...",
                attachments = attachments,
                onRemoveAttachment = { id ->
                    attachments = attachments.filterNot { it.id == id }
                },
                onModelClick = { showModelSheet = true },
                onAttachClick = { showAddSheet = true },
                onVoiceClick = onNavigateVoice,
                onSend = {
                    if (composerText.isNotBlank() || attachments.isNotEmpty()) {
                        val query = composerText.ifBlank { "Attached files" }
                        chatViewModel.setPendingAttachments(attachments)
                        composerText = ""
                        attachments = emptyList()
                        onNavigateChat(query, selectedModel)
                    }
                },
                onUpgradeClick = onUpgradeClick,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Model Select Bottom Sheet
        if (showModelSheet) {
            ModelSelectSheet(
                selectedModel = selectedModel,
                serverModels = availableModels,
                onModelSelected = { model ->
                    selectedModel = model
                    modelTier = when {
                        model.contains("Coding", true) -> "Coding"
                        model.contains("Reasoning", true) -> "Reasoning"
                        model.contains("Turbo", true) -> "Turbo"
                        else -> "Smart"
                    }
                },
                onDismiss = { showModelSheet = false }
            )
        }

        // Add to Chat Bottom Sheet
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
                    photoLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
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
    }
}
