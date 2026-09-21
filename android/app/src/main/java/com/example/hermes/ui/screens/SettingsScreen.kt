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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onNavigateProfile: () -> Unit = {},
    onNavigateBilling: () -> Unit = {},
    onNavigateCapabilities: () -> Unit = {},
    onNavigateConnectors: () -> Unit = {},
    onNavigatePermissions: () -> Unit = {},
    onNavigateVoiceSettings: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onNavigateTimeFocus: () -> Unit = {},
    onNavigatePrivacy: () -> Unit = {},
    onNavigateSharing: () -> Unit = {},
    onNavigateChannels: () -> Unit = {},
    onNavigateOmniRoute: () -> Unit = {},
    onNavigateAuth: () -> Unit = {},
    onUpgradeClick: () -> Unit = onNavigateBilling,
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }

    val themeMode by settingsViewModel.themeMode.collectAsState()
    val userEmail by settingsViewModel.userEmail.collectAsState()
    val selectedFontStyle by prefs.fontStyle.collectAsState(initial = "Default")
    val voicePersona by prefs.voicePersona.collectAsState(initial = "Rounded")
    val connectorDiscovery by prefs.connectorDiscovery.collectAsState(initial = true)

    val webSearchEnabled by prefs.capWebSearch.collectAsState(initial = true)
    val codeExecEnabled by prefs.capCodeExec.collectAsState(initial = true)
    val inlineVizEnabled by prefs.capInlineViz.collectAsState(initial = true)
    val switchModelsEnabled by prefs.capSwitchModels.collectAsState(initial = true)
    val genMemoryEnabled by prefs.capGenMemory.collectAsState(initial = true)

    val enabledCapsCount = listOf(webSearchEnabled, codeExecEnabled, inlineVizEnabled, switchModelsEnabled, genMemoryEnabled).count { it }
    val capabilitiesSubtitle = "$enabledCapsCount enabled"

    val packageInfo = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "1.0"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 1L
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toLong() ?: 1L
    }
    val appVersionText = "Hermes v$versionName ($versionCode)"

    var showColorModeDialog by remember { mutableStateOf(false) }
    var showFontStyleDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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
            // Top App Bar: Hamburger, Serif Settings Title, Info Icon (12-01-20)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimaryWarm)
                }

                Text(
                    text = "Settings",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )

                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "Info", tint = TextPrimaryWarm)
                }
            }

            // Scrollable settings sections (12-01-20)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Account Pill Card
                item {
                    val isAdmin = com.example.hermes.data.PreferencesManager.isUserAdmin(userEmail)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, if (isAdmin) Color(0xFFE27D60).copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(22.dp))
                            .clickable(onClick = onNavigateProfile)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = userEmail.ifBlank { "hermes-user" },
                                style = HermesTypography.titleLarge.copy(fontSize = 15.sp, color = TextPrimaryWarm)
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Administrator • Full Max Access",
                                    style = HermesTypography.bodySmall.copy(fontSize = 12.sp, color = Color(0xFFE27D60))
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isAdmin) Color(0xFFE27D60) else PureWhite)
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "Admin Max" else "Free",
                                style = HermesTypography.labelSmall.copy(
                                    color = if (isAdmin) PureWhite else TextInk,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Promo Card: "Want more Claude?" (12-01-20) - Hidden for Admin Max
                val isAdmin = com.example.hermes.data.PreferencesManager.isUserAdmin(userEmail)
                if (!isAdmin) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF1F1E1C))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Want more Hermes?",
                                style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Upgrade for more usage and capabilities.",
                                style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onUpgradeClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    text = "Upgrade",
                                    style = HermesTypography.titleLarge.copy(
                                        fontSize = 14.sp,
                                        color = PureBlack,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Group 1: Profile & Billing
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        SettingOptionRow("Profile", icon = Icons.Outlined.AccountCircle, onClick = onNavigateProfile)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow(
                            "Billing",
                            subtitle = if (isAdmin) "Hermes Max (Admin Plan)" else "Free plan",
                            icon = Icons.Outlined.MonetizationOn,
                            onClick = onNavigateBilling
                        )
                    }
                }

                // Group 2: Preferences (Font style, Color mode, Capabilities, Connectors, Permissions, Voice)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        SettingOptionRow(
                            "Font style",
                            subtitle = selectedFontStyle,
                            icon = Icons.Outlined.FormatSize,
                            onClick = { showFontStyleDialog = true }
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow(
                            "Color mode",
                            subtitle = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            icon = Icons.Outlined.DarkMode,
                            onClick = { showColorModeDialog = true }
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Capabilities", subtitle = capabilitiesSubtitle, icon = Icons.Outlined.Tune, onClick = onNavigateCapabilities)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Connectors", subtitle = if (connectorDiscovery) "Active • Auto-Discovery" else "Connected", icon = Icons.Outlined.AttachFile, onClick = onNavigateConnectors)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Channels Hub", subtitle = "Telegram, Email, Discord", icon = Icons.Outlined.Forum, onClick = onNavigateChannels)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("OmniRoute HUD", subtitle = "Model telemetry & fleet", icon = Icons.Outlined.AltRoute, onClick = onNavigateOmniRoute)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Permissions", icon = Icons.Outlined.Security, onClick = onNavigatePermissions)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Voice", subtitle = voicePersona, icon = Icons.Outlined.RecordVoiceOver, onClick = onNavigateVoiceSettings)
                    }
                }

                // Group 3: Controls (Notifications, Time & focus, Data & privacy, Sharing)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        SettingOptionRow("Notifications", icon = Icons.Outlined.Notifications, onClick = onNavigateNotifications)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Time & focus", icon = Icons.Outlined.HourglassEmpty, onClick = onNavigateTimeFocus)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Data & privacy", icon = Icons.Outlined.Lock, onClick = onNavigatePrivacy)
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Sharing", icon = Icons.Outlined.Share, onClick = onNavigateSharing)
                    }
                }

                // Group 4: Support & Legal
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        SettingOptionRow("Help & support", icon = Icons.Outlined.HelpOutline, onClick = { showHelpDialog = true })
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Privacy policy", icon = Icons.Outlined.Description, onClick = { showPrivacyDialog = true })
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                        SettingOptionRow("Terms of service", icon = Icons.Outlined.Gavel, onClick = { showTermsDialog = true })
                    }
                }

                // Group 5: Log Out
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1F1E1C))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    ) {
                        SettingOptionRow(
                            "Log out",
                            icon = Icons.Default.ExitToApp,
                            titleColor = DestructiveRed,
                            showChevron = false,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }

                // App Version Footer
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appVersionText,
                            style = HermesTypography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = TextSubtle
                            )
                        )
                    }
                }
            }
        }

        // Color Mode Dialog (12-01-44)
        if (showColorModeDialog) {
            AlertDialog(
                onDismissRequest = { showColorModeDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Color mode",
                        style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("System", "Dark", "Light").forEach { mode ->
                            val currentStr = themeMode.name.lowercase().replaceFirstChar { it.uppercase() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val tm = when(mode) {
                                            "Light" -> com.example.hermes.data.ThemeMode.LIGHT
                                            "Dark" -> com.example.hermes.data.ThemeMode.DARK
                                            else -> com.example.hermes.data.ThemeMode.SYSTEM
                                        }
                                        settingsViewModel.setThemeMode(tm)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentStr.equals(mode, true),
                                    onClick = {
                                        val tm = when(mode) {
                                            "Light" -> com.example.hermes.data.ThemeMode.LIGHT
                                            "Dark" -> com.example.hermes.data.ThemeMode.DARK
                                            else -> com.example.hermes.data.ThemeMode.SYSTEM
                                        }
                                        settingsViewModel.setThemeMode(tm)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AccentBlue,
                                        unselectedColor = TextSubtle
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = mode,
                                    style = HermesTypography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = TextPrimaryWarm
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorModeDialog = false }) {
                        Text("OK", color = AccentBlue, fontSize = 15.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showColorModeDialog = false }) {
                        Text("Cancel", color = TextMuted, fontSize = 15.sp)
                    }
                }
            )
        }

        // Font Style Dialog (12-01-47)
        if (showFontStyleDialog) {
            AlertDialog(
                onDismissRequest = { showFontStyleDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Font style",
                        style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Default", "Dyslexic-friendly").forEach { font ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { coroutineScope.launch { prefs.setFontStyle(font) } }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFontStyle == font,
                                    onClick = { coroutineScope.launch { prefs.setFontStyle(font) } },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AccentBlue,
                                        unselectedColor = TextSubtle
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = font,
                                    style = HermesTypography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = TextPrimaryWarm
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontStyleDialog = false }) {
                        Text("OK", color = AccentBlue, fontSize = 15.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFontStyleDialog = false }) {
                        Text("Cancel", color = TextMuted, fontSize = 15.sp)
                    }
                }
            )
        }

        // Help & Support Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = { Text("Hermes Help & Support", style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)) },
                text = {
                    Text(
                        "Hermes Agent provides 24x7 autonomous workflow assistance, multi-model intelligence via OmniRoute, and persistent memory.\n\nNeed enterprise support or reporting an issue?\nReach us at support@hermes-agent.io",
                        style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 14.5.sp, lineHeight = 20.sp)
                    )
                },
                confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Close", color = AccentBlue) } }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = { Text("Privacy Policy", style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)) },
                text = {
                    Text(
                        "Hermes respects user sovereignty and local-first memory. Sensitive tokens and memory are isolated per-user in encrypted secure storage, and never shared across guest or other user sessions.",
                        style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 14.5.sp, lineHeight = 20.sp)
                    )
                },
                confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Close", color = AccentBlue) } }
            )
        }

        // Terms of Service Dialog
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = { Text("Terms of Service", style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)) },
                text = {
                    Text(
                        "By using Hermes Agent, you agree to execute local and cloud actions responsibly according to trust-tier configurations and human-in-the-loop policies.",
                        style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 14.5.sp, lineHeight = 20.sp)
                    )
                },
                confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text("Close", color = AccentBlue) } }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(22.dp),
                title = { Text("About Hermes", style = HermesTypography.headlineMedium.copy(fontSize = 19.sp, color = TextPrimaryWarm)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = appVersionText,
                            style = HermesTypography.titleLarge.copy(fontSize = 14.5.sp, color = BrandCoral)
                        )
                        Text(
                            text = "Autonomous AI Agent & Knowledge Space client built with Jetpack Compose, Room persistence, and full-duplex WebSocket protocols.",
                            style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 13.5.sp, lineHeight = 19.sp)
                        )
                        HorizontalDivider(color = BorderSubtle)
                        Text(
                            text = "Connected Account: $userEmail",
                            style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp)
                        )
                        Text(
                            text = "Engine: Antigravity / Gemini-2.5-Flash",
                            style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 12.sp)
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close", color = AccentBlue) } }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Log out?", color = TextPrimaryWarm) },
                text = { Text("Are you sure you want to log out of Hermes?", color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            settingsViewModel.logout()
                            onNavigateAuth()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed)
                    ) {
                        Text("Log out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    titleColor: Color = TextPrimaryWarm,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == DestructiveRed) DestructiveRed else TextPrimaryWarm,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HermesTypography.titleLarge.copy(fontSize = 15.5.sp, color = titleColor)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = HermesTypography.bodyMedium.copy(fontSize = 12.5.sp, color = TextSubtle)
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
