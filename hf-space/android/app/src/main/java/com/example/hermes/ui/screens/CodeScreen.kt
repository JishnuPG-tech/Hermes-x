package com.example.hermes.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.ComputerFileItemDto
import com.example.hermes.theme.*
import com.example.hermes.ui.components.*

@Composable
fun CodeScreen(
    onOpenDrawer: () -> Unit,
    onLaunchTerminal: () -> Unit,
    codeViewModel: CodeViewModel = viewModel()
) {
    val selectedTab by codeViewModel.selectedTab.collectAsStateWithLifecycle()
    val approvals by codeViewModel.approvals.collectAsStateWithLifecycle()

    // File Explorer State
    val files by codeViewModel.files.collectAsStateWithLifecycle()
    val currentPath by codeViewModel.currentPath.collectAsStateWithLifecycle()
    val activeFilePath by codeViewModel.activeFilePath.collectAsStateWithLifecycle()
    val activeFileContent by codeViewModel.activeFileContent.collectAsStateWithLifecycle()
    val isLoadingFiles by codeViewModel.isLoadingFiles.collectAsStateWithLifecycle()

    // Browser State
    val browserStatus by codeViewModel.browserStatus.collectAsStateWithLifecycle()
    val browserScreenshot by codeViewModel.browserScreenshot.collectAsStateWithLifecycle()
    val isNavigatingBrowser by codeViewModel.isNavigatingBrowser.collectAsStateWithLifecycle()
    var browserUrlInput by remember(browserStatus?.current_url) {
        mutableStateOf(browserStatus?.current_url ?: "https://google.com")
    }

    // Agent Telemetry State
    val contextUsageTokens by codeViewModel.contextUsageTokens.collectAsStateWithLifecycle()
    val gitBranch by codeViewModel.gitBranch.collectAsStateWithLifecycle()
    val hostStatus by codeViewModel.hostStatus.collectAsStateWithLifecycle()
    val contextMax = codeViewModel.contextMaxTokens
    val fraction = (contextUsageTokens.toFloat() / contextMax).coerceIn(0.01f, 1f)
    val percent = (fraction * 100).toInt()

    val focusManager = LocalFocusManager.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimaryWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hermes Code",
                        style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Terminal Quick Launch Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF242320))
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable(onClick = onLaunchTerminal)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnthropicIcon(AnthropicIcons.CommandLine, size = 15.dp, tint = TextPrimaryWarm)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Terminal", style = HermesTypography.labelSmall.copy(color = TextPrimaryWarm, fontSize = 12.sp))
                        }
                    }
                }
            }

            // Segmented Tab Selector (Agent / Files / Browser)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1A18))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CodeViewTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        CodeViewTab.AGENT -> "Agent Session"
                        CodeViewTab.FILES -> "Workspace Files"
                        CodeViewTab.BROWSER -> "Live Browser"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) Color(0xFF2B2A27) else Color.Transparent)
                            .clickable { codeViewModel.selectTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = HermesTypography.labelSmall.copy(
                                color = if (isSelected) TextPrimaryWarm else TextMuted,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedTab) {
                CodeViewTab.AGENT -> {
                    // Agent Timeline View
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Context Window Usage Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Context window",
                                style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 12.sp)
                            )
                            Text(
                                text = "${contextUsageTokens / 1000}k / ${contextMax / 1000}k ($percent%)",
                                style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2926))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(AccentBlue)
                            )
                        }

                        // Git Branch Strip
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnthropicIcon(AnthropicIcons.Branch, size = 15.dp, tint = TextMuted)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gitBranch,
                                style = HermesTypography.bodyMedium.copy(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E281F))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (hostStatus != null) "Host: ${hostStatus?.storage_root ?: "Hermes Server"}" else "Computer Host Ready",
                                    style = HermesTypography.labelSmall.copy(color = AccentGreen, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Prompt Bubble
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E1D1B))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "Hermes Server Computer Autonomous Workspace",
                                        style = HermesTypography.bodyLarge.copy(fontSize = 14.5.sp, color = TextPrimaryWarm)
                                    )
                                }
                            }
                        }

                        // Interactive Tool Steps
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF1B1A18))
                                    .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(18.dp))
                                    .padding(vertical = 6.dp)
                            ) {
                                ToolStepItem(title = "Computer Status", detail = "Storage mounted on /data (healthy)")
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                                ToolStepItem(title = "Knowledge Engine", detail = "Notion & Obsidian bi-directional sync ready")
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                                ToolStepItem(title = "Browser Runtime", detail = "Playwright headless Chromium connected")
                            }
                        }

                        // Dynamic Action Approvals
                        if (approvals.isNotEmpty()) {
                            approvals.forEach { appr ->
                                item(key = appr.id) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF242017))
                                            .border(1.dp, Color(0xFF4A3E20), RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Shield,
                                                contentDescription = null,
                                                tint = AccentWarning,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Permission Request: ${appr.tool}",
                                                style = HermesTypography.titleMedium.copy(fontSize = 14.sp, color = TextPrimaryWarm, fontWeight = FontWeight.SemiBold)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "Risk: ${appr.risk}",
                                                style = HermesTypography.labelSmall.copy(color = AccentWarning, fontSize = 11.sp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = appr.command.ifBlank { "Executing automated background step" },
                                            style = HermesTypography.bodySmall.copy(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextMuted)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { codeViewModel.approve(appr.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve", style = HermesTypography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                            OutlinedButton(
                                                onClick = { codeViewModel.deny(appr.id) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed),
                                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DestructiveRed)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Deny", style = HermesTypography.labelMedium.copy(fontWeight = FontWeight.Medium))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CodeViewTab.FILES -> {
                    // File Explorer View
                    if (activeFilePath != null && activeFileContent != null) {
                        // File Content Viewer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { codeViewModel.closeActiveFile() }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to files", tint = TextPrimaryWarm, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeFilePath?.substringAfterLast('/') ?: "File",
                                        style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm, fontSize = 14.5.sp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF181715))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = activeFileContent ?: "",
                                    style = HermesTypography.bodyMedium.copy(
                                        fontFamily = JetBrainsMono,
                                        fontSize = 12.5.sp,
                                        color = TextPrimaryWarm,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    } else {
                        // Files Listing
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentPath.isNotBlank()) {
                                        IconButton(onClick = { codeViewModel.navigateUpDirectory() }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Up", tint = TextPrimaryWarm, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (currentPath.isBlank()) "/data/jarvis (Root)" else "/$currentPath",
                                        style = HermesTypography.bodyMedium.copy(fontFamily = JetBrainsMono, color = TextMuted, fontSize = 13.sp)
                                    )
                                }

                                IconButton(onClick = { codeViewModel.fetchFiles(currentPath) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }

                            if (isLoadingFiles) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = BrandCoral, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                            } else if (files.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Directory is empty", style = HermesTypography.bodyMedium.copy(color = TextSubtle))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1F1E1D))
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    items(files, key = { it.path }) { file ->
                                        FileRowItem(file = file, onClick = { codeViewModel.openFile(file) })
                                    }
                                }
                            }
                        }
                    }
                }

                CodeViewTab.BROWSER -> {
                    // Browser Live View
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // URL Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1D1B))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = browserUrlInput,
                                onValueChange = { browserUrlInput = it },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimaryWarm,
                                    unfocusedTextColor = TextPrimaryWarm,
                                    cursorColor = BrandCoral
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    focusManager.clearFocus()
                                    codeViewModel.navigateBrowser(browserUrlInput)
                                }),
                                modifier = Modifier.weight(1f)
                            )
                            if (isNavigatingBrowser) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandCoral, strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        codeViewModel.navigateBrowser(browserUrlInput)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Navigate", tint = TextPrimaryWarm, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Browser Viewport View
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141413))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!browserScreenshot.isNullOrBlank()) {
                                val imageBitmap = remember(browserScreenshot) {
                                    try {
                                        val decoded = Base64.decode(browserScreenshot, Base64.DEFAULT)
                                        val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                        bmp?.asImageBitmap()
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Live browser viewport",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("Viewport rendered", style = HermesTypography.bodyMedium.copy(color = TextMuted))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Laptop, contentDescription = null, tint = TextMuted, modifier = Modifier.size(42.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = browserStatus?.title?.ifBlank { "Playwright Browser Ready" } ?: "Playwright Browser Ready",
                                        style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = browserStatus?.current_url ?: "Enter a URL above to inspect live pages",
                                        style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 12.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRowItem(
    file: ComputerFileItemDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (file.is_dir) Icons.Outlined.Folder else Icons.Outlined.Description,
                contentDescription = null,
                tint = if (file.is_dir) BrandCoral else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = file.name,
                style = HermesTypography.bodyMedium.copy(
                    color = TextPrimaryWarm,
                    fontSize = 14.sp,
                    fontWeight = if (file.is_dir) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }

        if (file.is_dir) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(18.dp))
        } else if (file.size > 0) {
            Text(
                text = "${file.size / 1024} KB",
                style = HermesTypography.bodySmall.copy(color = TextSubtle, fontSize = 11.5.sp)
            )
        }
    }
}

@Composable
private fun ToolStepItem(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = HermesTypography.titleMedium.copy(
                    fontSize = 14.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Text(
            text = detail,
            style = HermesTypography.bodySmall.copy(
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                color = TextMuted
            ),
            maxLines = 1
        )
    }
}
