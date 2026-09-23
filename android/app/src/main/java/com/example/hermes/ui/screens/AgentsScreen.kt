package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.hermes.data.AgentRoleDto
import com.example.hermes.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val agents by chatViewModel.agents.collectAsStateWithLifecycle()
    var inspectedAgent by remember { mutableStateOf<AgentRoleDto?>(null) }

    LaunchedEffect(Unit) {
        chatViewModel.fetchAgents()
    }

    Scaffold(
        containerColor = CanvasNearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Agents",
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
            // Coordinator Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDarkElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Workforce Team Coordinator",
                            style = HermesTypography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Autonomous multi-agent task execution and delegation",
                            style = HermesTypography.bodySmall.copy(
                                color = Color(0xFF9E9C96),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandCoral.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = HermesTypography.labelSmall.copy(
                                color = BrandCoral,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Specialist Workforce Roles",
                style = HermesTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (agents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandCoral)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(agents, key = { it.id }) { agent ->
                        AgentCard(
                            agent = agent,
                            onClick = { inspectedAgent = agent }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (inspectedAgent != null) {
        val a = inspectedAgent!!
        ModalBottomSheet(
            onDismissRequest = { inspectedAgent = null },
            containerColor = SurfaceDarkElevated,
            contentColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = a.name,
                        style = HermesTypography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandCoral.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = a.status,
                            style = HermesTypography.labelSmall.copy(
                                color = BrandCoral,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    text = a.description,
                    style = HermesTypography.bodyMedium.copy(
                        color = Color(0xFFD4D2CD),
                        lineHeight = 22.sp
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "System Contract & Instructions",
                    style = HermesTypography.bodyMedium.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141312))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = a.system_prompt,
                        style = HermesTypography.bodySmall.copy(
                            fontFamily = AnthropicSans,
                            color = Color(0xFFB0AEA5),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    )
                }

                if (a.output_contract.isNotBlank()) {
                    Text(
                        text = "Deliverable Contract: ${a.output_contract}",
                        style = HermesTypography.bodySmall.copy(color = Color(0xFF8E8D8A))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AgentCard(
    agent: AgentRoleDto,
    onClick: () -> Unit
) {
    val icon: ImageVector = when {
        agent.name.contains("Orchestrator", true) -> Icons.Outlined.AccountTree
        agent.name.contains("Architect", true) -> Icons.Outlined.DesignServices
        agent.name.contains("Developer", true) -> Icons.Outlined.Code
        agent.name.contains("QA", true) -> Icons.Outlined.FactCheck
        agent.name.contains("Security", true) -> Icons.Outlined.Security
        agent.name.contains("DevOps", true) -> Icons.Outlined.Terminal
        else -> Icons.Outlined.SmartToy
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262523)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BrandCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = agent.name,
                            style = HermesTypography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = agent.tier,
                            style = HermesTypography.bodySmall.copy(
                                color = Color(0xFF8E8D8A),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF262523))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = agent.status,
                        style = HermesTypography.labelSmall.copy(
                            color = Color(0xFFD4D2CD),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Text(
                text = agent.description,
                style = HermesTypography.bodyMedium.copy(
                    color = Color(0xFFD4D2CD),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Allowed tools chips
            if (agent.allowed_tools.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(agent.allowed_tools) { tool ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141312))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tool,
                                style = HermesTypography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0AEA5)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
