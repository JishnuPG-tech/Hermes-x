package com.example.hermes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeToggle

@Composable
fun ChannelsScreen(
    onBack: () -> Unit,
    viewModel: ChannelsViewModel = viewModel()
) {
    val config by viewModel.channelsConfig.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    var telegramEnabled by remember(config) { mutableStateOf(config.telegram.enabled) }
    var telegramToken by remember(config) { mutableStateOf("") }
    var telegramAllowed by remember(config) { mutableStateOf(config.telegram.allowed_users) }
    var telegramAdminId by remember(config) { mutableStateOf(config.telegram.admin_id) }

    var emailEnabled by remember(config) { mutableStateOf(config.email.enabled) }
    var emailAddress by remember(config) { mutableStateOf(config.email.address) }
    var emailPassword by remember(config) { mutableStateOf("") }
    var emailImap by remember(config) { mutableStateOf(config.email.imap_host) }
    var emailSmtp by remember(config) { mutableStateOf(config.email.smtp_host) }
    var emailPoll by remember(config) { mutableStateOf(config.email.poll_interval.toString()) }

    var discordEnabled by remember(config) { mutableStateOf(config.discord.enabled) }
    var discordToken by remember(config) { mutableStateOf("") }
    var discordAllowed by remember(config) { mutableStateOf(config.discord.allowed_users) }

    var showTelegramToken by remember { mutableStateOf(false) }
    var showEmailPassword by remember { mutableStateOf(false) }
    var showDiscordToken by remember { mutableStateOf(false) }

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
            // Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Channels Hub",
                    style = HermesTypography.headlineMedium.copy(fontSize = 20.sp, color = TextPrimaryWarm)
                )

                IconButton(onClick = { viewModel.loadChannels() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Status Banner (if active)
            AnimatedVisibility(visible = statusMsg != null) {
                statusMsg?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (msg.startsWith("✅") || msg.contains("success")) Color(0xFF1E3A29) else Color(0xFF33201D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (msg.startsWith("✅") || msg.contains("success")) Color(0xFF4E9F6E) else BrandCoral)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = HermesTypography.bodyMedium.copy(color = TextPrimaryWarm, fontSize = 13.sp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearStatusMessage() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSubtle, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header description
                item {
                    Text(
                        text = "Connect Hermes to external chat, messaging, and email channels for 24/7 background alerts, notifications, and interactive tasks.",
                        style = HermesTypography.bodyMedium.copy(color = TextMuted, fontSize = 13.5.sp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Channel 1: Telegram Bot
                item {
                    ChannelCard(
                        title = "Telegram Bot",
                        subtitle = if (config.telegram.status == "connected") "Connected • Active webhook" else "Disconnected",
                        icon = Icons.Outlined.Send,
                        isEnabled = telegramEnabled,
                        onToggle = { telegramEnabled = it },
                        statusColor = if (config.telegram.status == "connected") Color(0xFF4E9F6E) else TextSubtle
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClaudeInputField(
                                label = "Bot Token",
                                value = telegramToken,
                                onValueChange = { telegramToken = it },
                                placeholder = if (config.telegram.token_masked.isNotBlank()) config.telegram.token_masked else "123456789:ABCdefGHI...",
                                isPassword = !showTelegramToken,
                                trailingIcon = {
                                    IconButton(onClick = { showTelegramToken = !showTelegramToken }) {
                                        Icon(
                                            if (showTelegramToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle",
                                            tint = TextSubtle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            ClaudeInputField(
                                label = "Whitelisted Usernames / IDs",
                                value = telegramAllowed,
                                onValueChange = { telegramAllowed = it },
                                placeholder = "* (all) or username1, username2"
                            )

                            ClaudeInputField(
                                label = "Admin Chat ID (for test alerts)",
                                value = telegramAdminId,
                                onValueChange = { telegramAdminId = it },
                                placeholder = "e.g. 108293847"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.testChannel("telegram") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandCoral),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Telegram Alert", style = HermesTypography.labelSmall.copy(fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }

                // Channel 2: Email & Gmail
                item {
                    ChannelCard(
                        title = "Email / Gmail",
                        subtitle = if (config.email.status == "active") "Active • ${config.email.address}" else "Standby",
                        icon = Icons.Outlined.Email,
                        isEnabled = emailEnabled,
                        onToggle = { emailEnabled = it },
                        statusColor = if (config.email.status == "active") Color(0xFF4E9F6E) else TextSubtle
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClaudeInputField(
                                label = "Email Address",
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                placeholder = "jishnupg2005@gmail.com"
                            )

                            ClaudeInputField(
                                label = "App Password",
                                value = emailPassword,
                                onValueChange = { emailPassword = it },
                                placeholder = if (config.email.has_password) "••••••••••••••••" else "Google 16-char App Password",
                                isPassword = !showEmailPassword,
                                trailingIcon = {
                                    IconButton(onClick = { showEmailPassword = !showEmailPassword }) {
                                        Icon(
                                            if (showEmailPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle",
                                            tint = TextSubtle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ClaudeInputField(
                                        label = "IMAP Host",
                                        value = emailImap,
                                        onValueChange = { emailImap = it },
                                        placeholder = "imap.gmail.com"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ClaudeInputField(
                                        label = "SMTP Host",
                                        value = emailSmtp,
                                        onValueChange = { emailSmtp = it },
                                        placeholder = "smtp.gmail.com"
                                    )
                                }
                            }

                            ClaudeInputField(
                                label = "Poll Interval (seconds)",
                                value = emailPoll,
                                onValueChange = { emailPoll = it },
                                placeholder = "15",
                                keyboardType = KeyboardType.Number
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.testChannel("email") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandCoral),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Test Email", style = HermesTypography.labelSmall.copy(fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }

                // Channel 3: Discord Bot
                item {
                    ChannelCard(
                        title = "Discord Bot",
                        subtitle = if (config.discord.status == "connected") "Connected" else "Disconnected",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        isEnabled = discordEnabled,
                        onToggle = { discordEnabled = it },
                        statusColor = if (config.discord.status == "connected") Color(0xFF4E9F6E) else TextSubtle
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClaudeInputField(
                                label = "Bot Token",
                                value = discordToken,
                                onValueChange = { discordToken = it },
                                placeholder = if (config.discord.token_masked.isNotBlank()) config.discord.token_masked else "Discord Bot Token",
                                isPassword = !showDiscordToken,
                                trailingIcon = {
                                    IconButton(onClick = { showDiscordToken = !showDiscordToken }) {
                                        Icon(
                                            if (showDiscordToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle",
                                            tint = TextSubtle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            ClaudeInputField(
                                label = "Whitelisted User IDs",
                                value = discordAllowed,
                                onValueChange = { discordAllowed = it },
                                placeholder = "* (all) or user_id1, user_id2"
                            )
                        }
                    }
                }

                // Channel 4: Webhooks
                item {
                    ChannelCard(
                        title = "HTTP Webhooks",
                        subtitle = "Endpoint ready for GitHub, Stripe & CI/CD",
                        icon = Icons.Outlined.Webhook,
                        isEnabled = config.webhooks.enabled,
                        onToggle = {},
                        statusColor = Color(0xFF4E9F6E)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Webhook Ingest Endpoint:",
                                style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CanvasNearBlack)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = config.webhooks.endpoint,
                                    style = HermesTypography.bodyMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = BrandCoral,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sticky Bottom Save Button
            Button(
                onClick = {
                    viewModel.saveChannels(
                        telegramEnabled = telegramEnabled,
                        telegramToken = telegramToken,
                        telegramAllowed = telegramAllowed,
                        telegramAdminId = telegramAdminId,
                        emailEnabled = emailEnabled,
                        emailAddress = emailAddress,
                        emailPassword = emailPassword,
                        emailImap = emailImap,
                        emailSmtp = emailSmtp,
                        emailPoll = emailPoll.toIntOrNull() ?: 15,
                        discordEnabled = discordEnabled,
                        discordToken = discordToken,
                        discordAllowed = discordAllowed
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 2.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureBlack, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Save & Reload Channels",
                        style = HermesTypography.titleLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureBlack
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    statusColor: Color,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(isEnabled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F1E1C))
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CanvasNearBlack)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimaryWarm, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = HermesTypography.titleLarge.copy(fontSize = 16.sp, color = TextPrimaryWarm)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtitle,
                            style = HermesTypography.bodySmall.copy(fontSize = 12.sp, color = TextMuted)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ClaudeToggle(checked = isEnabled, onCheckedChange = onToggle)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSubtle,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle)
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun ClaudeInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = HermesTypography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = HermesTypography.bodyMedium.copy(color = TextSubtle, fontSize = 13.5.sp)
                )
            },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CanvasNearBlack,
                unfocusedContainerColor = CanvasNearBlack,
                focusedBorderColor = BrandCoral,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimaryWarm,
                unfocusedTextColor = TextPrimaryWarm,
                cursorColor = BrandCoral
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
