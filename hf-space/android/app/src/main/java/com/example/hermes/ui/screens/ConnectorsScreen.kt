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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.KnowledgeSearchResultItemDto
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectorsScreen(
    onBack: () -> Unit,
    viewModel: ConnectorsViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }

    val discoveryEnabled by prefs.connectorDiscovery.collectAsState(initial = true)
    var searchQuery by remember { mutableStateOf("") }
    var showCreateNoteSheet by remember { mutableStateOf(false) }

    val knowledgeSources by viewModel.knowledgeSources.collectAsStateWithLifecycle()
    val directoryServers by viewModel.directoryServers.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Top Navigation Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryWarm,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Knowledge & Connectors",
                style = HermesTypography.headlineMedium.copy(
                    fontSize = 20.sp,
                    color = TextPrimaryWarm,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { showCreateNoteSheet = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Document",
                    tint = BrandCoral,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Search Bar for Knowledge Space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1D1B))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isBlank()) {
                            viewModel.clearSearch()
                        } else {
                            viewModel.searchKnowledge(it)
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search Notion, Obsidian & Vaults...",
                            style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 14.sp)
                        )
                    },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.searchKnowledge(searchQuery)
                    }),
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            viewModel.clearSearch()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Results Mode
            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSearching) "Searching Knowledge..." else "${searchResults.size} Knowledge Results",
                            style = HermesTypography.labelSmall.copy(color = TextMuted, fontSize = 12.sp)
                        )
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = BrandCoral
                            )
                        }
                    }
                }

                if (searchResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No knowledge records match \"$searchQuery\"",
                                style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 14.sp)
                            )
                        }
                    }
                } else {
                    items(searchResults, key = { it.id.ifBlank { it.title } }) { res ->
                        KnowledgeSearchResultCard(result = res)
                    }
                }
            } else {
                // Sync Banner Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1D))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Knowledge Space Sync",
                                style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = syncMessage ?: "Incremental sync with Notion & Obsidian vaults.",
                                style = HermesTypography.bodyMedium.copy(fontSize = 13.sp, color = TextMuted)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSyncing) Color(0xFF2E2D2A) else BrandCoral)
                                .clickable(enabled = !isSyncing) { viewModel.triggerSync() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimaryWarm, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync", style = HermesTypography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }

                // Card 1: Connector Discovery Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1D))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            tint = TextPrimaryWarm,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connector discovery",
                                style = HermesTypography.titleLarge.copy(fontSize = 16.5.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Hermes queries linked knowledge sources and MCP tool endpoints dynamically.",
                                style = HermesTypography.bodyMedium.copy(fontSize = 13.5.sp, color = TextMuted, lineHeight = 18.sp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        ClaudeToggle(
                            checked = discoveryEnabled,
                            onCheckedChange = { coroutineScope.launch { prefs.setConnectorDiscovery(it) } }
                        )
                    }
                }

                // Card 2: Knowledge Sources & MCP Endpoints
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1D))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        val allItems = mutableListOf<ConnectorUiModel>()

                        // Knowledge sources (Notion, Obsidian, Vaults)
                        knowledgeSources.forEach { ks ->
                            allItems.add(
                                ConnectorUiModel(
                                    name = ks.name.replaceFirstChar { it.uppercase() },
                                    subtitle = "Type: ${ks.type} • Status: ${ks.status}",
                                    badge = if (ks.primary) "Primary" else null,
                                    isExternal = !ks.primary
                                )
                            )
                        }

                        // Directory MCP servers
                        directoryServers.forEach { ds ->
                            allItems.add(
                                ConnectorUiModel(
                                    name = ds.name.ifBlank { ds.id },
                                    subtitle = ds.description.ifBlank { "${ds.tools.size} active tools" },
                                    badge = if (ds.tools.isNotEmpty()) "${ds.tools.size}" else null,
                                    isExternal = false
                                )
                            )
                        }

                        if (allItems.isNotEmpty()) {
                            allItems.forEachIndexed { index, item ->
                                if (index > 0) {
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                                }
                                ConnectorRow(item.name, item.subtitle, item.badge, item.isExternal)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("No Connectors Configured", style = HermesTypography.titleMedium.copy(color = TextPrimaryWarm))
                                    Text("Register MCP servers or Notion/Obsidian in config to bind connectors.", style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Sheet: Create Knowledge Note
    if (showCreateNoteSheet) {
        CreateNoteBottomSheet(
            onDismiss = { showCreateNoteSheet = false },
            onCreate = { title, content, dest ->
                viewModel.createNote(title, content, dest) {
                    showCreateNoteSheet = false
                    viewModel.refresh()
                }
            }
        )
    }
}

@Composable
private fun KnowledgeSearchResultCard(result: KnowledgeSearchResultItemDto) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.title.ifBlank { "Untitled Document" },
                style = HermesTypography.titleLarge.copy(
                    fontSize = 16.sp,
                    color = TextPrimaryWarm,
                    fontFamily = AnthropicSerif,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2520))
                    .border(1.dp, Color(0xFF4A3428), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = result.source.uppercase(),
                    style = HermesTypography.labelSmall.copy(color = BrandCoral, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
        if (result.snippet.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.snippet,
                style = HermesTypography.bodyMedium.copy(color = Color(0xFFC0BAB0), fontSize = 13.sp, lineHeight = 18.sp),
                maxLines = 3
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNoteBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("notion") }

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
                text = "New Knowledge Document",
                style = HermesTypography.headlineSmall.copy(
                    fontFamily = AnthropicSerif,
                    color = TextPrimaryWarm,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Destination Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("notion", "obsidian").forEach { dest ->
                    val isSelected = destination == dest
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BrandCoral else Color(0xFF201F1D))
                            .border(1.dp, if (isSelected) BrandCoral else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { destination = dest }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dest.replaceFirstChar { it.uppercase() },
                            style = HermesTypography.labelSmall.copy(
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

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
                value = content,
                onValueChange = { content = it },
                label = { Text("Content (Markdown)", color = TextSubtle) },
                minLines = 4,
                maxLines = 8,
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
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onCreate(title, content, destination)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Document", style = HermesTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private data class ConnectorUiModel(
    val name: String,
    val subtitle: String,
    val badge: String? = null,
    val isExternal: Boolean = false
)

@Composable
private fun ConnectorRow(
    name: String,
    subtitle: String,
    badge: String? = null,
    isExternal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E2D2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = name,
                    style = HermesTypography.titleLarge.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                )
                Text(
                    text = subtitle,
                    style = HermesTypography.bodyMedium.copy(fontSize = 12.sp, color = TextSubtle)
                )
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF233E60))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = HermesTypography.labelSmall.copy(color = AccentBlue, fontSize = 11.sp)
                )
            }
        } else if (isExternal) {
            Icon(
                imageVector = Icons.Outlined.NorthEast,
                contentDescription = "External link",
                tint = TextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
