package com.example.hermes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.R
import com.example.hermes.data.HermesApiClient
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.theme.*
import com.example.hermes.ui.components.NeuralNodesIllustration
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onContinueToApp: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val proceed = {
        coroutineScope.launch {
            try {
                HermesApiClient.instance.login()
                HermesDataRepository.instance.fetchSessions()
                HermesDataRepository.instance.fetchProjects()
                HermesDataRepository.instance.fetchTasks()
                HermesDataRepository.instance.fetchModels()
            } catch (_: Exception) {}
            onContinueToApp()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        // Login / Onboarding Mode (OS Splash handles initial opening screen)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                    // Top Official Claude Horizontal Vector
                    Image(
                        painter = painterResource(id = R.drawable.logo_claude_horizontal),
                        contentDescription = "Claude",
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .height(30.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Neural Nodes Illustration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        NeuralNodesIllustration(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Tagline
                    Text(
                        text = "The AI for problem solvers",
                        style = HermesTypography.displayLarge.copy(
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            color = TextPrimaryWarm,
                            textAlign = TextAlign.Center,
                            fontFamily = AnthropicSerif
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Auth Action Controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Continue with Google Button
                        Button(
                            onClick = { proceed() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "G",
                                style = HermesTypography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = HermesTypography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PureBlack
                                )
                            )
                        }

                        // OR Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = HairlineDivider
                            )
                            Text(
                                text = "OR",
                                style = HermesTypography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = HairlineDivider
                            )
                        }

                        // Email Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1F1E1C))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                placeholder = {
                                    Text(
                                        text = "Enter your email",
                                        style = HermesTypography.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            color = TextSubtle
                                        )
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimaryWarm,
                                    unfocusedTextColor = TextPrimaryWarm
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { proceed() }),
                                modifier = Modifier.weight(1f)
                            )

                            if (emailInput.isNotBlank()) {
                                IconButton(
                                    onClick = { proceed() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandCoral)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Submit",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Continue as Guest Option
                        TextButton(
                            onClick = { proceed() },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Continue as Guest",
                                style = HermesTypography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        // Legal Terms Footer
                        Text(
                            text = "By continuing, you agree to Anthropic's Terms of Service and Privacy Policy.",
                            style = HermesTypography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                color = TextSubtle,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
    }
}
