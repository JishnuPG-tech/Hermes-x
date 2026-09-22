package com.example.hermes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.*
import com.example.hermes.ui.components.ClaudeStarburst
import com.example.hermes.ui.components.NeuralNodesIllustration
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onContinueToApp: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val googleClientId by authViewModel.googleClientId.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

    var isSplashMode by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }

    // Single Opening Splash Screen (1.8 seconds)
    LaunchedEffect(Unit) {
        delay(1800)
        isSplashMode = false
    }

    fun launchGoogleSignIn() {
        authViewModel.clearError()
        coroutineScope.launch {
            try {
                val clientId = googleClientId.trim().ifBlank { "292824298430-113kq16cbpq6i02jin424gb1mk5ebm40.apps.googleusercontent.com" }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    authViewModel.loginWithGoogleIdToken(
                        idToken = googleIdTokenCredential.idToken,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        email = googleIdTokenCredential.id,
                        avatar = googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                        onSuccess = onContinueToApp
                    )
                } else {
                    authViewModel.setError("Received unsupported credential type: ${credential.type}")
                }
            } catch (e: GetCredentialCancellationException) {
                // User intentionally canceled Google sign-in dialog
            } catch (e: Exception) {
                authViewModel.setError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasNearBlack)
            .safeDrawingPadding()
    ) {
        AnimatedContent(
            targetState = isSplashMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
            },
            label = "SplashToLogin"
        ) { splash ->
            if (splash) {
                // Opening / Splash Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isSplashMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    // Center Serif Title "Hermes"
                    Text(
                        text = "Hermes",
                        style = HermesTypography.displayLarge.copy(
                            fontSize = 42.sp,
                            color = TextPrimaryWarm,
                            fontFamily = AnthropicSerif,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    )

                    // Bottom Footer Wordmark "APEX"
                    Text(
                        text = "APEX",
                        style = HermesTypography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            color = Color(0xFF8E8B82),
                            letterSpacing = 4.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AnthropicSans
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp)
                    )
                }
            } else {
                // Exact Login / Onboarding Screen (Image 2)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Header: Centered Hermes Serif Wordmark
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hermes",
                            style = HermesTypography.displayLarge.copy(
                                fontSize = 30.sp,
                                color = TextPrimaryWarm,
                                fontFamily = AnthropicSerif,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Neural Nodes Illustration (Image 2)
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

                    // Hero Tagline (Image 2)
                    Text(
                        text = "The AI for problem solvers",
                        style = HermesTypography.displayLarge.copy(
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            color = TextPrimaryWarm,
                            textAlign = TextAlign.Center,
                            fontFamily = AnthropicSerif
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error message banner if any
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF331414),
                            border = BorderStroke(1.dp, Color(0xFF8B2525))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = HermesTypography.bodySmall.copy(
                                        color = Color(0xFFFF8B8B),
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { authViewModel.clearError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFFFF8B8B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Auth Action Controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Continue with Google Button (Image 2)
                        Button(
                            onClick = { launchGoogleSignIn() },
                            enabled = !isLoading,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack,
                                disabledContainerColor = Color(0xFFCCCCCC),
                                disabledContentColor = Color(0xFF666666)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PureBlack,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "G",
                                    style = HermesTypography.titleLarge.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4285F4)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = HermesTypography.titleMedium.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PureBlack,
                                        fontFamily = AnthropicSans
                                    )
                                )
                            }
                        }

                        // OR Divider (Image 2)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
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
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = AnthropicSans
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = HairlineDivider
                            )
                        }

                        // Email Input Row (Image 2)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1F1E1C))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
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
                                            fontSize = 15.5.sp,
                                            color = TextSubtle,
                                            fontFamily = AnthropicSans
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
                                keyboardActions = KeyboardActions(onDone = {
                                    if (emailInput.isNotBlank()) {
                                        authViewModel.loginAsGuest { onContinueToApp() }
                                    }
                                }),
                                modifier = Modifier.weight(1f)
                            )

                            if (emailInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        authViewModel.loginAsGuest { onContinueToApp() }
                                    },
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

                        // Legal Terms Footer (Image 2)
                        val annotatedDisclaimer = androidx.compose.ui.text.buildAnnotatedString {
                            append("By continuing, you agree to Anthropic's ")
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = TextPrimaryWarm,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append("Consumer Terms")
                            pop()
                            append(" and ")
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = TextPrimaryWarm,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append("Usage Policy")
                            pop()
                            append(", and acknowledge their ")
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    color = TextPrimaryWarm,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append("Privacy Policy")
                            pop()
                            append(".")
                        }

                        Text(
                            text = annotatedDisclaimer,
                            style = HermesTypography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = TextSubtle,
                                lineHeight = 18.sp,
                                fontFamily = AnthropicSans,
                                textAlign = TextAlign.Center
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
