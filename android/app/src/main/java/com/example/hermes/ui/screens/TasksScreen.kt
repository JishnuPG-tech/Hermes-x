package com.example.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.hermes.data.*
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenDrawer: () -> Unit,
    onNavigateChat: (String) -> Unit,
    onNavigateConfig: () -> Unit = {},
    tasksViewModel: TasksViewModel = viewModel()
) {
    val liveTasks by tasksViewModel.tasks.collectAsStateWithLifecycle()
    val activeTab by tasksViewModel.activeTab.collectAsStateWithLifecycle()
    val expandedTaskId by tasksViewModel.expandedTaskId.collectAsStateWithLifecycle()
    val workforceRoles by tasksViewModel.workforceRoles.collectAsStateWithLifecycle()
    val automations by tasksViewModel.automations.collectAsStateWithLifecycle()
    val isRefreshing by tasksViewModel.isRefreshing.collectAsStateWithLifecycle()

    var showCreateAutomationSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Bar: Hamburger, Serif Tasks title, Avatar
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
                    text = "Autonomous Tasks",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activeTab == TaskTab.AUTOMATIONS) {
                    IconButton(onClick = { showCreateAutomationSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Automation", tint = BrandCoral)
                    }
                } else {
                    IconButton(onClick = { onNavigateChat("Create a new autonomous task.") }) {
                        Icon(Icons.Default.Add, contentDescription = "New Task", tint = TextPrimaryWarm)
                    }
                }
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

        // Segmented Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1B1A18))
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TaskTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                val label = when (tab) {
                    TaskTab.TASKS -> "Tasks"
                    TaskTab.DAG_WORKFORCE -> "DAG & Swarm"
                    TaskTab.AUTOMATIONS -> "24x7 Automations"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isSelected) Color(0xFF2B2A27) else Color.Transparent)
                        .clickable { tasksViewModel.selectTab(tab) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = HermesTypography.labelSmall.copy(
                            color = if (isSelected) TextPrimaryWarm else TextMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeTab) {
            TaskTab.TASKS -> {
                // Tasks List with DAG expansion
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (liveTasks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No Autonomous Tasks", style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tap '+' to spawn an autonomous multi-agent task.", style = HermesTypography.bodySmall.copy(color = TextSubtle))
                                }
                            }
                        }
                    } else {
                        items(liveTasks, key = { it.id }) { task ->
                            val isExpanded = expandedTaskId == task.id
                            TaskCardWithDag(
                                task = task,
                                isExpanded = isExpanded,
                                onToggleExpand = { tasksViewModel.toggleTaskExpand(task.id) },
                                onPause = { tasksViewModel.pauseTask(task.id) },
                                onResume = { tasksViewModel.resumeTask(task.id) },
                                onCancel = { tasksViewModel.cancelTask(task.id) },
                                onOpenChat = { onNavigateChat(task.prompt.ifBlank { task.title }) }
                            )
                        }
                    }
                }
            }

            TaskTab.DAG_WORKFORCE -> {
                // Multi-Agent Workforce Roles
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Specialist Agent Workforce",
                            style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm, fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Coordinated by Hermes TeamCoordinator for concurrent subtask DAG execution.",
                            style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 12.5.sp)
                        )
                    }

                    val defaultRoles = if (workforceRoles.isNotEmpty()) workforceRoles else listOf(
                        WorkforceRoleDto("Orchestrator", "Coordinates task DAG breakdown, subtask delegation, and synthesis.", listOf("read_file", "list_directory"), "Execution plan & synthesis deliverable"),
                        WorkforceRoleDto("Architect", "Designs software architecture, data contracts, and API boundaries.", listOf("read_file", "write_file"), "Technical specification"),
                        WorkforceRoleDto("Developer", "Writes production code, modules, and tests.", listOf("read_file", "write_file", "edit_file", "bash_exec"), "Source code files & diffs"),
                        WorkforceRoleDto("QA Engineer", "Executes unit and integration tests and verifies criteria.", listOf("read_file", "write_file", "bash_exec"), "Verification evidence & test suite"),
                        WorkforceRoleDto("Security Reviewer", "Audits code for vulnerabilities and secret leakage.", listOf("read_file", "list_directory"), "Security assessment report"),
                        WorkforceRoleDto("DevOps", "Configures builds, containers, and deployment health.", listOf("bash_exec", "read_file", "write_file"), "Build configurations & logs")
                    )

                    items(defaultRoles, key = { it.name }) { role ->
                        WorkforceRoleCard(role = role)
                    }
                }
            }

            TaskTab.AUTOMATIONS -> {
                // 24x7 Scheduled Automations
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Scheduled Automations",
                                    style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm, fontSize = 16.sp)
                                )
                                Text(
                                    text = "Hermes 24x7 background cron tasks and watchdog.",
                                    style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 12.5.sp)
                                )
                            }
                        }
                    }

                    if (automations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No automations scheduled. Tap '+' to create one.", style = HermesTypography.bodyMedium.copy(color = TextSubtle))
                            }
                        }
                    } else {
                        items(automations, key = { it.id }) { auto ->
                            AutomationCard(
                                automation = auto,
                                onToggle = { tasksViewModel.toggleAutomation(auto.id) },
                                onRunNow = { tasksViewModel.runAutomationNow(auto.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateAutomationSheet) {
        CreateAutomationBottomSheet(
            onDismiss = { showCreateAutomationSheet = false },
            onCreate = { title, prompt, cron ->
                tasksViewModel.createAutomation(title, prompt, cron)
                showCreateAutomationSheet = false
            }
        )
    }
}

@Composable
private fun TaskCardWithDag(
    task: TaskDto,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpenChat: () -> Unit
) {
    val statusColor = when (task.status.uppercase()) {
        "RUNNING" -> AccentBlue
        "COMPLETED" -> AccentGreen
        "PAUSED" -> AccentWarning
        else -> DestructiveRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1D1B))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = task.title,
                    style = HermesTypography.titleMedium.copy(
                        fontSize = 16.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = task.status.uppercase(),
                    style = HermesTypography.labelSmall.copy(color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        if (task.prompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.prompt,
                style = HermesTypography.bodySmall.copy(color = Color(0xFFC0BAB0), fontSize = 13.sp),
                maxLines = if (isExpanded) 6 else 2
            )
        }

        // Progress bar
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2825))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(task.progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions & DAG Expansion button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrandCoral,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isExpanded) "Hide DAG Subtasks" else "View DAG Subtasks (${task.subtasks.size})",
                    style = HermesTypography.labelSmall.copy(color = BrandCoral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.status.equals("RUNNING", ignoreCase = true)) {
                    IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                } else if (task.status.equals("PAUSED", ignoreCase = true)) {
                    IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = AccentGreen, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onOpenChat, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Open in Chat", tint = TextPrimaryWarm, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Live DAG Subtask Tree Visualizer
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161514))
                    .border(1.dp, Color(0xFF282725), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DAG Subtask Execution Graph",
                    style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                )

                if (task.subtasks.isEmpty()) {
                    Text("No subtasks mapped yet", style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 12.sp))
                } else {
                    task.subtasks.forEachIndexed { index, sub ->
                        DagSubtaskNode(subtask = sub, index = index)
                    }
                }
            }
        }
    }
}

@Composable
private fun DagSubtaskNode(subtask: SubtaskDto, index: Int) {
    val nodeColor = when (subtask.status.lowercase()) {
        "completed" -> AccentGreen
        "running" -> AccentBlue
        else -> TextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1D1B))
            .border(1.dp, Color(0xFF2A2926), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(nodeColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = HermesTypography.labelSmall.copy(color = nodeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subtask.title.ifBlank { "Subtask ${index + 1}" },
                style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm, fontSize = 13.5.sp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Role: ${subtask.role}",
                    style = HermesTypography.bodySmall.copy(color = BrandCoral, fontSize = 11.sp)
                )
                if (subtask.dependencies.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Depends on ${subtask.dependencies.size} task(s)",
                        style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 11.sp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(nodeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = subtask.status.uppercase(),
                style = HermesTypography.labelSmall.copy(color = nodeColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun WorkforceRoleCard(role: WorkforceRoleDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E1D1B))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2B2420)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GroupWork, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = role.name,
                    style = HermesTypography.titleLarge.copy(fontSize = 16.sp, color = TextPrimaryWarm, fontWeight = FontWeight.Medium)
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E281F))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Ready", style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 11.sp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = role.description,
            style = HermesTypography.bodySmall.copy(color = Color(0xFFC0BAB0), fontSize = 13.sp, lineHeight = 18.sp)
        )

        if (role.allowed_tools.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                role.allowed_tools.take(4).forEach { tool ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF262523))
                            .border(1.dp, Color(0xFF383632), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(tool, style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 10.5.sp, fontFamily = JetBrainsMono))
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationCard(
    automation: ScheduledAutomationDto,
    onToggle: () -> Unit,
    onRunNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E1D1B))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = automation.title,
                    style = HermesTypography.titleMedium.copy(fontSize = 15.5.sp, color = TextPrimaryWarm, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Cron: ${automation.cron_expression}",
                        style = HermesTypography.labelSmall.copy(color = BrandCoral, fontFamily = JetBrainsMono, fontSize = 11.5.sp)
                    )
                }
            }

            ClaudeToggle(
                checked = automation.enabled,
                onCheckedChange = { onToggle() }
            )
        }

        if (automation.prompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = automation.prompt,
                style = HermesTypography.bodySmall.copy(color = Color(0xFFC0BAB0), fontSize = 12.5.sp),
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onRunNow,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryWarm),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run Now", style = HermesTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAutomationBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var cron by remember { mutableStateOf("0 * * * *") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141413),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "New Scheduled Automation",
                style = HermesTypography.headlineSmall.copy(
                    fontFamily = AnthropicSerif,
                    color = TextPrimaryWarm,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = TextSubtle) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCoral,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimaryWarm,
                    unfocusedTextColor = TextPrimaryWarm
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt / Objective", color = TextSubtle) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCoral,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimaryWarm,
                    unfocusedTextColor = TextPrimaryWarm
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cron,
                onValueChange = { cron = it },
                label = { Text("Cron Expression (e.g. 0 * * * *)", color = TextSubtle) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandCoral,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimaryWarm,
                    unfocusedTextColor = TextPrimaryWarm
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, prompt, cron)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Schedule Automation", style = HermesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
