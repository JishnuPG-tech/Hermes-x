package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

@Composable
fun TasksScreen(
    onOpenDrawer: () -> Unit,
    onNavigateChat: (String) -> Unit,
    onNavigateConfig: () -> Unit = {},
    tasksViewModel: TasksViewModel = viewModel()
) {
    var isPaused by remember { mutableStateOf(false) }
    val liveTasks by tasksViewModel.tasks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Bar: Hamburger, Serif Tasks title, Edit & Avatar J
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimaryWarm)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tasks",
                    style = HermesTypography.headlineMedium.copy(fontSize = 22.sp, color = TextPrimaryWarm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.EditNote, contentDescription = "New Task", tint = TextMuted, modifier = Modifier.size(24.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AvatarSalmon),
                    contentAlignment = Alignment.Center
                ) {
                    Text("J", style = HermesTypography.titleLarge.copy(fontSize = 15.sp, color = PureWhite))
                }
            }
        }

        // Subheader: Back, Autonomous Tasks, Engine v2.4 Live, Filter, Add
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Autonomous Tasks", style = HermesTypography.headlineMedium.copy(fontSize = 18.sp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hermes Engine v2.4 · Live", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp))
                }
            }
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onNavigateChat("Create a new autonomous task.") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ACTIVE AGENT SWARM CARD
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1B19))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE AGENT SWARM",
                                style = HermesTypography.labelSmall.copy(fontSize = 12.sp, color = TextPrimaryWarm, letterSpacing = 1.sp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1E2838))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("4 Online", style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 11.sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Agents Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SwarmAgentItem("Backend", "Idle", Icons.Default.Layers, Color(0xFF8E8B82), Modifier.weight(1f))
                        SwarmAgentItem("UI / UX", "Active", Icons.Default.DesignServices, AccentBlue, Modifier.weight(1f))
                        SwarmAgentItem("QA Lead", "Running", Icons.Default.FactCheck, AccentGreen, Modifier.weight(1f))
                        SwarmAgentItem("Security", "Guarding", Icons.Default.Security, AccentWarning, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Swarm Footer: OS Health and Load
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agent OS: Healthy · 0 memory leaks", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.5.sp))
                        }
                        Text("Load: 28%", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.5.sp))
                    }
                }
            }

            // IN FLIGHT EXECUTION CARD
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1B19))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip("RUNNING", color = AccentGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("#TSK-8921", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 12.sp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Android Client Integration & Verification", style = HermesTypography.titleLarge.copy(fontSize = 17.sp))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supervised by: ", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
                        Text("Hermes Supervisor", style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 12.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assigned to: ", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
                        Text("QA Agent & Backend Agent", style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 12.sp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("AUTONOMOUS DIRECTIVE", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Compile Compose UI, execute visual regression tests, verify remote state on Hugging Face space.",
                        style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Step 3 of 5", style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 13.sp))
                        }
                        Text("78%", style = HermesTypography.bodyMedium.copy(color = AccentBlue, fontSize = 13.sp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { 0.78f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = AccentBlue,
                        trackColor = Color(0xFF2A2926)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Command Output
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141413))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$ ./gradlew connectedAn...", style = HermesTypography.labelSmall.copy(color = TextPrimaryWarm, fontSize = 11.5.sp))
                        Text("(14/18 passed)", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 11.5.sp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Last checkpoint: cp-android-v1.4 (safe rollback...)", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: Pause, Live Logs, Folder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isPaused = !isPaused },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262522)),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = TextPrimaryWarm, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPaused) "Resume" else "Pause", style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 13.sp))
                        }

                        Button(
                            onClick = { onNavigateChat("Show live execution logs.") },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262522)),
                            modifier = Modifier.weight(1.3f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Dvr, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Logs", style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 13.sp))
                        }

                        Button(
                            onClick = {},
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262522)),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // DELEGATED QUEUE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delegated Queue", style = HermesTypography.headlineMedium.copy(fontSize = 18.sp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF23221F))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("3", style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp))
                            }
                        }
                        Text("View History", style = HermesTypography.bodyMedium.copy(color = AccentBlue, fontSize = 13.sp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Queue items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1C1B19))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (liveTasks.isNotEmpty()) {
                            liveTasks.forEachIndexed { i, t ->
                                if (i > 0) {
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                                }
                                QueueItem(
                                    icon = if (t.status == "completed") Icons.Default.Check else Icons.Default.Schedule,
                                    iconBg = if (t.status == "completed") AccentGreen.copy(alpha = 0.15f) else AccentBlue.copy(alpha = 0.15f),
                                    iconTint = if (t.status == "completed") AccentGreen else AccentBlue,
                                    tag = "${t.status.uppercase()} • Server Task",
                                    title = t.title,
                                    subtitle = "Status: ${t.status} · ${(t.progress * 100).toInt()}% completed"
                                )
                            }
                        } else {
                            QueueItem(
                                icon = Icons.Default.Check,
                                iconBg = AccentGreen.copy(alpha = 0.15f),
                                iconTint = AccentGreen,
                                tag = "LIVE • Connected",
                                title = "Hermes Agent Server Ready",
                                subtitle = "https://jishnupg-hermes.hf.space"
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                        // Review item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AccentWarning.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentWarning, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF332A15))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text("⚠ APPROVAL REQUIRED", style = HermesTypography.labelSmall.copy(color = AccentWarning, fontSize = 10.sp))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Security Policy Audit & Port R...", style = HermesTypography.titleLarge.copy(fontSize = 14.sp))
                                    Text("Requires elevated root key confir...", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
                                }
                            }

                            Button(
                                onClick = { onNavigateChat("Review security policy audit.") },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentWarning, contentColor = PureBlack),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("Review", style = HermesTypography.bodyMedium.copy(color = PureBlack, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontSize = 13.sp))
                            }
                        }
                    }
                }
            }

            // Autonomous Supervisor Note
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1D1A))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Autonomous Supervisor Note", style = HermesTypography.titleLarge.copy(fontSize = 13.sp, color = TextPrimaryWarm))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Android test harness runs 14% faster than yesterday due to cached Gradle build transforms on local NVMe.",
                            style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar: Chat, Tasks, Artifacts, Config
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141413))
                .border(1.dp, BorderSubtle)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("Chat", Icons.Default.ChatBubbleOutline, selected = false, onClick = { onNavigateChat("") })
            BottomNavItem("Tasks", Icons.Default.Checklist, selected = true, onClick = {})
            BottomNavItem("Artifacts", Icons.Default.Category, selected = false, onClick = onNavigateConfig)
            BottomNavItem("Config", Icons.Default.Tune, selected = false, onClick = onNavigateConfig)
        }
    }
}

@Composable
private fun SwarmAgentItem(
    name: String,
    status: String,
    icon: ImageVector,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF23221F))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E2D29)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimaryWarm, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(name, style = HermesTypography.bodyLarge.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(statusColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(status, style = HermesTypography.labelSmall.copy(color = statusColor, fontSize = 10.sp))
        }
    }
}

@Composable
private fun QueueItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    tag: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(tag, style = HermesTypography.labelSmall.copy(color = TextSubtle, fontSize = 10.5.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(title, style = HermesTypography.titleLarge.copy(fontSize = 14.sp))
                Text(subtitle, style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) BrandCoral else TextSubtle,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = HermesTypography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (selected) BrandCoral else TextSubtle
            )
        )
    }
}
