package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import com.example.hermes.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToChatSheet(
    onDismiss: () -> Unit,
    webSearchEnabled: Boolean = true,
    onWebSearchChange: (Boolean) -> Unit = {},
    memoryEnabled: Boolean = true,
    onMemoryChange: (Boolean) -> Unit = {},
    selectedProjectName: String? = null,
    toolAccessDescription: String = "Auto",
    onCameraClick: () -> Unit = {},
    onPhotosClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onProjectClick: () -> Unit = {},
    onToolAccessClick: () -> Unit = {},
    onConnectorsClick: () -> Unit = {}
) {
    var localWebSearch by remember(webSearchEnabled) { mutableStateOf(webSearchEnabled) }
    var localMemory by remember(memoryEnabled) { mutableStateOf(memoryEnabled) }

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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Header (12-01-02: ✕ on left, bold Sans "Add to chat" in center)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Add to chat",
                    style = HermesTypography.titleLarge.copy(
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryWarm
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 3-up Action Tiles (Camera, Photos, Files) (12-01-02)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionSquareTile(
                    label = "Camera",
                    icon = Icons.Outlined.PhotoCamera,
                    modifier = Modifier.weight(1f),
                    onClick = onCameraClick
                )
                ActionSquareTile(
                    label = "Photos",
                    icon = Icons.Outlined.Image,
                    modifier = Modifier.weight(1f),
                    onClick = onPhotosClick
                )
                ActionSquareTile(
                    label = "Files",
                    icon = Icons.Outlined.FileUpload,
                    modifier = Modifier.weight(1f),
                    onClick = onFilesClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 1: Web search & Memory (12-01-02)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1C))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                // Web Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newVal = !localWebSearch
                            localWebSearch = newVal
                            onWebSearchChange(newVal)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2A27)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = TextPrimaryWarm,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Web search",
                            style = HermesTypography.titleMedium.copy(
                                fontSize = 16.sp,
                                color = TextPrimaryWarm
                            )
                        )
                    }
                    Switch(
                        checked = localWebSearch,
                        onCheckedChange = {
                            localWebSearch = it
                            onWebSearchChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = Color(0xFF8E8B82),
                            uncheckedTrackColor = Color(0xFF2E2D2A)
                        )
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Memory
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newVal = !localMemory
                            localMemory = newVal
                            onMemoryChange(newVal)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2A27)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = TextPrimaryWarm,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Memory",
                            style = HermesTypography.titleMedium.copy(
                                fontSize = 16.sp,
                                color = TextPrimaryWarm
                            )
                        )
                    }
                    Switch(
                        checked = localMemory,
                        onCheckedChange = {
                            localMemory = it
                            onMemoryChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = Color(0xFF8E8B82),
                            uncheckedTrackColor = Color(0xFF2E2D2A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 2: Add to project, Tool access, Connectors (12-01-02)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1C))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                // Add to project
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onProjectClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2A27)),
                            contentAlignment = Alignment.Center
                        ) {
                            CanisterIcon(size = 20.dp, tint = TextPrimaryWarm)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Add to project",
                                style = HermesTypography.titleMedium.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedProjectName ?: "None",
                                style = HermesTypography.bodySmall.copy(fontSize = 12.5.sp, color = Color(0xFF8E8B82))
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8B82), modifier = Modifier.size(18.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Tool access
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToolAccessClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2A27)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BusinessCenter,
                                contentDescription = null,
                                tint = TextPrimaryWarm,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Tool access",
                                style = HermesTypography.titleMedium.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = toolAccessDescription,
                                style = HermesTypography.bodySmall.copy(fontSize = 12.5.sp, color = Color(0xFF8E8B82))
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8B82), modifier = Modifier.size(18.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                // Connectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConnectorsClick)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2B2A27)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = TextPrimaryWarm,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Connectors",
                            style = HermesTypography.titleMedium.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8B82), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActionSquareTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F1E1C))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF2B2A27)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimaryWarm,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = HermesTypography.titleSmall.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryWarm
            )
        )
    }
}
