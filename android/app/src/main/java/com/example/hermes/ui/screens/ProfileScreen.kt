package com.example.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { com.example.hermes.data.PreferencesManager.getInstance(context) }
    val savedName by prefs.userName.collectAsState(initial = "")
    val savedEmail by prefs.userEmail.collectAsState(initial = "")
    var fullName by remember(savedName) { mutableStateOf(savedName.ifBlank { "User" }) }
    var preferredName by remember(savedName) { mutableStateOf(savedName.substringBefore(' ').ifBlank { "User" }) }
    var customInstructions by remember {
        mutableStateOf("Senior full-stack engineer and AI systems developer. Prefer concise, production-ready Kotlin Jetpack Compose and modern architecture patterns.")
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWarm,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profile",
                    style = HermesTypography.headlineMedium.copy(
                        fontSize = 20.sp,
                        color = TextPrimaryWarm,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Full name",
                            style = HermesTypography.labelMedium.copy(
                                color = TextSubtle,
                                fontSize = 13.sp
                            )
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1F1E1C),
                                unfocusedContainerColor = Color(0xFF1F1E1C),
                                focusedTextColor = TextPrimaryWarm,
                                unfocusedTextColor = TextPrimaryWarm,
                                focusedBorderColor = BrandCoral,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "What should Hermes call you?",
                            style = HermesTypography.labelMedium.copy(
                                color = TextSubtle,
                                fontSize = 13.sp
                            )
                        )
                        OutlinedTextField(
                            value = preferredName,
                            onValueChange = { preferredName = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1F1E1C),
                                unfocusedContainerColor = Color(0xFF1F1E1C),
                                focusedTextColor = TextPrimaryWarm,
                                unfocusedTextColor = TextPrimaryWarm,
                                focusedBorderColor = BrandCoral,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Custom Instructions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Custom instructions",
                            style = HermesTypography.labelMedium.copy(
                                color = TextSubtle,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "What would you like Hermes to know about you to provide better responses? e.g. preferred coding languages, response length, context about your work or projects.",
                            style = HermesTypography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customInstructions,
                            onValueChange = { customInstructions = it },
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1F1E1C),
                                unfocusedContainerColor = Color(0xFF1F1E1C),
                                focusedTextColor = TextPrimaryWarm,
                                unfocusedTextColor = TextPrimaryWarm,
                                focusedBorderColor = BrandCoral,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.setUserProfile(fullName, savedEmail)
                            }
                            onBack()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Save changes",
                            style = HermesTypography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Danger Zone: Delete Account
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete account",
                        style = HermesTypography.titleMedium.copy(
                            color = DestructiveRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Permanently delete your account and all associated chats, artifacts, and preferences.",
                        style = HermesTypography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DestructiveRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Delete Account", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF1F1E1C),
                shape = RoundedCornerShape(20.dp),
                title = { Text("Delete Account?", color = TextPrimaryWarm) },
                text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.", color = TextMuted) },
                confirmButton = {
                    Button(
                        onClick = { showDeleteConfirm = false; onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}
