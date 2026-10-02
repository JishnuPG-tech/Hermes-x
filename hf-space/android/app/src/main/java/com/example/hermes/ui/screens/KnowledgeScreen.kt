package com.example.hermes.ui.screens

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
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.KnowledgeSearchResultItemDto
import com.example.hermes.data.KnowledgeSummarySourceDto
import com.example.hermes.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val summary by chatViewModel.knowledgeSummary.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<KnowledgeSearchResultItemDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        chatViewModel.fetchKnowledgeSummary()
    }

    Scaffold(
        containerColor = CanvasNearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Knowledge",
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
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val ok = HermesApiClient.instance.syncKnowledge()
                                isSyncing = false
                                syncMessage = if (ok) "Knowledge stores synchronized" else "Sync failed"
                                chatViewModel.fetchKnowledgeSummary()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Sync",
                            tint = if (isSyncing) BrandCoral else PureWhite
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
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.length >= 2) {
                        scope.launch {
                            isSearching = true
                            val res = HermesApiClient.instance.searchKnowledge(it)
                            searchResults = res
                            isSearching = false
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                placeholder = {
                    Text("Search Notion, Obsidian, and Memory...", color = Color(0xFF6B6A65), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF8E8D8A))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkElevated,
                    unfocusedContainerColor = SurfaceDarkElevated,
                    focusedBorderColor = BrandCoral,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                singleLine = true
            )

            if (syncMessage != null) {
                Text(
                    text = syncMessage!!,
                    style = HermesTypography.bodySmall.copy(color = BrandCoral),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (searchQuery.isNotBlank()) {
                // Search Results View
                Text(
                    text = "Search Results (${searchResults.size})",
                    style = HermesTypography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PureWhite),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandCoral, modifier = Modifier.size(28.dp))
                    }
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "No matching knowledge entries found.",
                        style = HermesTypography.bodyMedium.copy(color = Color(0xFF9E9C96))
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceDarkElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = result.title.ifBlank { "Untitled" },
                                            style = HermesTypography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PureWhite,
                                                fontSize = 15.sp
                                            )
                                        )
                                        Text(
                                            text = result.source.uppercase(),
                                            style = HermesTypography.labelSmall.copy(
                                                color = BrandCoral,
                                                fontFamily = JetBrainsMono,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = result.snippet,
                                        style = HermesTypography.bodySmall.copy(
                                            color = Color(0xFFD4D2CD),
                                            lineHeight = 18.sp
                                        ),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Connected Sources Overview
                Text(
                    text = "Connected Stores & Integrations",
                    style = HermesTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val sources = summary?.sources ?: emptyList()
                if (sources.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandCoral)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sources, key = { it.id }) { src ->
                            KnowledgeSourceCard(src)
                        }
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeSourceCard(source: KnowledgeSummarySourceDto) {
    val icon: ImageVector = when {
        source.id.contains("notion", true) -> Icons.Outlined.Article
        source.id.contains("obsidian", true) -> Icons.Outlined.FolderCopy
        else -> Icons.Outlined.Storage
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        text = source.name,
                        style = HermesTypography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "${source.type} · ${source.item_count} items",
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
                    .background(BrandCoral.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = source.status,
                    style = HermesTypography.labelSmall.copy(
                        color = BrandCoral,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
