package com.example.hermes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.theme.*

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val lastUpdated: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onOpenDrawer: () -> Unit,
    onNavigateChat: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var activeProjectDetail by remember { mutableStateOf<ProjectItem?>(null) }

    LaunchedEffect(Unit) {
        HermesDataRepository.instance.fetchProjects()
    }

    val serverProjects by chatViewModel.projects.collectAsStateWithLifecycle()
    val projects = remember(serverProjects) {
        serverProjects.map { p ->
            ProjectItem(
                id = p.id,
                name = p.name,
                description = p.description,
                lastUpdated = if (p.created_at > 0) {
                    java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date((p.created_at * 1000).toLong()))
                } else "Recent"
            )
        }
    }

    if (activeProjectDetail != null) {
        // Project Detail Screen (Image 8 & Image 10)
        ProjectDetailView(
            project = activeProjectDetail!!,
            onBack = { activeProjectDetail = null },
            onNewChat = onNavigateChat
        )
    } else {
        // Projects Main List Screen (Image 6 & Image 11)
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
                // Top App Bar: Hamburger on left, Sliders on right (Image 6)
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

                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Serif Large "Projects" Header (Image 6)
                Text(
                    text = "Projects",
                    style = HermesTypography.displayLarge.copy(
                        fontSize = 32.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar (Image 6)
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
                                        text = "Search projects",
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

                Spacer(modifier = Modifier.height(16.dp))

                val filteredProjects = projects.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                }

                if (filteredProjects.isEmpty()) {
                    // Empty State (Image 29)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            CylinderIllustration(modifier = Modifier.size(80.dp))

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Create a project to organize and customize chats with Hermes around a topic or set of documents.",
                                style = HermesTypography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 23.sp
                                )
                            )
                        }
                    }
                } else {
                    // Project List Rows (Image 6 & Image 11)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredProjects) { project ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeProjectDetail = project }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = project.name,
                                    style = HermesTypography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        color = TextPrimaryWarm,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = project.lastUpdated,
                                    style = HermesTypography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        color = TextSubtle
                                    )
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Floating "+ New project" Button (Image 6 & Image 11)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable(onClick = { showCreateSheet = true })
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PureBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New project",
                    style = HermesTypography.titleLarge.copy(
                        fontSize = 16.sp,
                        color = PureBlack,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Exact Create Project Bottom Sheet (Image 7)
        if (showCreateSheet) {
            CreateProjectBottomSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { name, desc ->
                    chatViewModel.createProject(name, desc)
                    showCreateSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141413),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF383632))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header (Image 7: ✕ on left, "Create a project" in center)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Create a project",
                    style = HermesTypography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryWarm
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Field 1: "What are you working on?"
            Text(
                text = "What are you working on?",
                style = HermesTypography.titleMedium.copy(
                    fontSize = 16.5.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1A18))
                    .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = {
                        Text(
                            text = "Name your project",
                            style = HermesTypography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = TextSubtle
                            )
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimaryWarm,
                        unfocusedTextColor = TextPrimaryWarm
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Field 2: "What are you trying to achieve?"
            Text(
                text = "What are you trying to achieve?",
                style = HermesTypography.titleMedium.copy(
                    fontSize = 16.5.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1A18))
                    .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    placeholder = {
                        Text(
                            text = "Describe your project, goals, subject, etc...",
                            style = HermesTypography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = TextSubtle,
                                lineHeight = 22.sp
                            )
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimaryWarm,
                        unfocusedTextColor = TextPrimaryWarm
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Full-Width "Create project" Button (Image 7)
            Button(
                onClick = { if (nameInput.isNotBlank()) onCreate(nameInput, descInput) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF383632),
                    contentColor = TextPrimaryWarm
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Create project",
                    style = HermesTypography.titleMedium.copy(
                        fontSize = 16.5.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProjectDetailView(
    project: ProjectItem,
    onBack: () -> Unit,
    onNewChat: () -> Unit
) {
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
            // Top App Bar: Back Arrow on left, Overflow on right (Image 8 & Image 10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Serif Project Title (Image 8 & Image 10)
            Text(
                text = project.name,
                style = HermesTypography.displayLarge.copy(
                    fontSize = 32.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Private Badge Chip (Image 8 & Image 10)
            Row(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1F1E1C))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private",
                            style = HermesTypography.labelSmall.copy(
                                fontSize = 13.sp,
                                color = TextPrimaryWarm,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Memory Card (Image 8 & Image 10)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1B1A18))
                    .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Project memory will appear after a few chats.",
                    style = HermesTypography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = TextMuted,
                        lineHeight = 21.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2-Up Cards: Project Knowledge & Custom Instructions (Image 8 & Image 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Card: Project Knowledge
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B1A18))
                        .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Project knowledge",
                        style = HermesTypography.titleMedium.copy(
                            fontSize = 16.sp,
                            color = TextPrimaryWarm,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Add knowledge",
                        style = HermesTypography.labelMedium.copy(
                            fontSize = 14.5.sp,
                            color = BrandCoral,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.clickable { }
                    )
                }

                // Right Card: Custom Instructions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B1A18))
                        .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Custom instructions",
                        style = HermesTypography.titleMedium.copy(
                            fontSize = 16.sp,
                            color = TextPrimaryWarm,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Add instructions",
                        style = HermesTypography.labelMedium.copy(
                            fontSize = 14.5.sp,
                            color = BrandCoral,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.clickable { }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Center Empty Chat Bubble State (Image 8 & Image 10)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Chats you've had with Hermes will show up here.",
                        style = HermesTypography.bodyLarge.copy(
                            fontSize = 16.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }

        // Floating "+ New chat" Button (Image 8 & Image 10)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .clickable(onClick = onNewChat)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PureBlack,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New chat",
                style = HermesTypography.titleLarge.copy(
                    fontSize = 16.sp,
                    color = PureBlack,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun CylinderIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val strokeColor = Color(0xFF8E8B82)
        val strokeWidth = 3f

        // Draw 3 stacked horizontal tray lines matching Image 29
        val line1Y = h * 0.25f
        val line2Y = h * 0.45f
        val trayY = h * 0.65f
        val trayH = h * 0.25f

        // Top line 1
        drawLine(
            color = strokeColor,
            start = Offset(cx - w * 0.25f, line1Y),
            end = Offset(cx + w * 0.25f, line1Y),
            strokeWidth = strokeWidth
        )

        // Middle line 2
        drawLine(
            color = strokeColor,
            start = Offset(cx - w * 0.35f, line2Y),
            end = Offset(cx + w * 0.35f, line2Y),
            strokeWidth = strokeWidth
        )

        // Bottom tray box
        val path = Path().apply {
            moveTo(cx - w * 0.4f, trayY)
            lineTo(cx - w * 0.35f, trayY + trayH)
            lineTo(cx + w * 0.35f, trayY + trayH)
            lineTo(cx + w * 0.4f, trayY)
            close()
        }

        drawPath(path, color = strokeColor, style = Stroke(width = strokeWidth))
    }
}
