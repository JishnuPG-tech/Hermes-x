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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.data.SkillItemDto
import com.example.hermes.theme.*
import com.example.hermes.ui.components.AnthropicIcon
import com.example.hermes.ui.components.AnthropicIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val skills by chatViewModel.skills.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedSkill by remember { mutableStateOf<SkillItemDto?>(null) }

    LaunchedEffect(Unit) {
        chatViewModel.fetchSkills()
    }

    val categories = remember(skills) {
        listOf("All", "Active") + skills.map { it.category }.distinct()
    }

    val filteredSkills = remember(skills, selectedCategory, searchQuery) {
        skills.filter { skill ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Active" -> skill.is_active
                else -> skill.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    skill.name.contains(searchQuery, ignoreCase = true) ||
                    skill.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = CanvasNearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Skills",
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
            // Description header
            Text(
                text = "Dynamic system and domain capabilities that empower Hermes Agent.",
                style = HermesTypography.bodyMedium.copy(
                    color = Color(0xFF9E9C96),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BrandCoral else SurfaceDarkElevated)
                            .border(1.dp, if (isSelected) BrandCoral else BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = category,
                            style = HermesTypography.bodyMedium.copy(
                                color = if (isSelected) PureWhite else Color(0xFFD4D2CD),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            // Skills List
            if (filteredSkills.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnthropicIcon(
                            drawableId = AnthropicIcons.Spark,
                            contentDescription = null,
                            tint = Color(0xFF6B6A65),
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No skills found",
                            style = HermesTypography.bodyLarge.copy(color = Color(0xFF9E9C96))
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSkills, key = { it.id }) { skill ->
                        SkillCard(
                            skill = skill,
                            onToggle = { active ->
                                scope.launch {
                                    chatViewModel.toggleSkill(skill.name, active)
                                }
                            },
                            onInspect = { inspectedSkill = skill }
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
    if (inspectedSkill != null) {
        val s = inspectedSkill!!
        ModalBottomSheet(
            onDismissRequest = { inspectedSkill = null },
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
                        text = s.name,
                        style = HermesTypography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (s.is_active) BrandCoral.copy(alpha = 0.2f) else Color(0xFF2C2B28))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (s.is_active) "Active" else "Inactive",
                            style = HermesTypography.labelSmall.copy(
                                color = if (s.is_active) BrandCoral else Color(0xFF9E9C96),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    text = s.description,
                    style = HermesTypography.bodyMedium.copy(
                        color = Color(0xFFD4D2CD),
                        lineHeight = 22.sp
                    )
                )

                HorizontalDivider(color = BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category", style = HermesTypography.bodyMedium.copy(color = Color(0xFF9E9C96)))
                    Text(s.category, style = HermesTypography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.SemiBold))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Author", style = HermesTypography.bodyMedium.copy(color = Color(0xFF9E9C96)))
                    Text(s.author, style = HermesTypography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.SemiBold))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", style = HermesTypography.bodyMedium.copy(color = Color(0xFF9E9C96)))
                    Text(s.version, style = HermesTypography.bodyMedium.copy(color = PureWhite, fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SkillCard(
    skill: SkillItemDto,
    onToggle: (Boolean) -> Unit,
    onInspect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDarkElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onInspect)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (skill.is_active) BrandCoral.copy(alpha = 0.15f) else Color(0xFF262523)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnthropicIcon(
                            drawableId = AnthropicIcons.Spark,
                            contentDescription = null,
                            tint = if (skill.is_active) BrandCoral else Color(0xFF9E9C96),
                            size = 18.dp
                        )
                    }
                    Column {
                        Text(
                            text = skill.name,
                            style = HermesTypography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = skill.category,
                            style = HermesTypography.bodySmall.copy(
                                color = Color(0xFF8E8D8A),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = skill.is_active,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = BrandCoral,
                        uncheckedThumbColor = Color(0xFF8E8D8A),
                        uncheckedTrackColor = Color(0xFF2C2B28)
                    )
                )
            }

            Text(
                text = skill.description,
                style = HermesTypography.bodyMedium.copy(
                    color = Color(0xFFD4D2CD),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
