package com.example.hermes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hermes.theme.CanvasNearBlack
import com.example.hermes.theme.SurfaceDarkElevated
import com.example.hermes.ui.components.ClaudeDrawerContent
import com.example.hermes.ui.screens.*
import kotlinx.coroutines.launch

@Composable
private fun ScreenTransitionWrapper(content: @Composable () -> Unit) {
    val state = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) { fullWidth -> fullWidth / 14 },
        exit = fadeOut(
            animationSpec = tween(durationMillis = 160)
        )
    ) {
        content()
    }
}

@Composable
fun MainNavigation(
    chatViewModel: ChatViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    startVoice: Boolean = false
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUserName by authViewModel.userName.collectAsStateWithLifecycle()
    val currentUserEmail by authViewModel.userEmail.collectAsStateWithLifecycle()

    var isSplashVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!startVoice) }

    LaunchedEffect(Unit) {
        if (!startVoice) {
            kotlinx.coroutines.delay(1000)
        }
        isSplashVisible = false
    }

    if (isLoggedIn == null || isSplashVisible) {
        SplashScreen()
        return
    }

    val initialRoute = when {
        isLoggedIn != true -> NavAuth
        startVoice -> NavVoice
        else -> NavHome
    }
    val backStack = rememberNavBackStack(initialRoute)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()

    LaunchedEffect(startVoice) {
        if (startVoice && isLoggedIn == true) {
            if (backStack.lastOrNull() != NavVoice) {
                backStack.add(NavVoice)
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) {
            backStack.clear()
            backStack.add(NavAuth)
        } else if (isLoggedIn == true) {
            chatViewModel.fetchSessions()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDarkElevated
            ) {
                ClaudeDrawerContent(
                    sessions = sessions,
                    userName = currentUserName,
                    userEmail = currentUserEmail,
                    onNavigateHome = {
                        scope.launch { drawerState.close() }
                        if (backStack.lastOrNull() != NavHome) {
                            backStack.clear()
                            backStack.add(NavHome)
                        }
                    },
                    onNavigateChats = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavChats)
                    },
                    onNavigateProjects = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavProjects)
                    },
                    onNavigateCode = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavCode)
                    },
                    onNavigateArtifacts = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavArtifacts)
                    },
                    onNavigateTasks = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavTasks)
                    },
                    onNavigateSettings = {
                        scope.launch { drawerState.close() }
                        backStack.add(NavSettings)
                    },
                    onNewChat = {
                        scope.launch { drawerState.close() }
                        chatViewModel.clearMessages()
                        backStack.add(NavChat(prompt = null))
                    },
                    onOpenRecentChat = { sessionId ->
                        scope.launch { drawerState.close() }
                        backStack.add(NavChat(sessionId = sessionId))
                    }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                } else {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider {
                entry<NavAuth> {
                    ScreenTransitionWrapper {
                        AuthScreen(
                            onContinueToApp = {
                                backStack.clear()
                                backStack.add(NavHome)
                            }
                        )
                    }
                }
                entry<NavHome> {
                    ScreenTransitionWrapper {
                        val availableModels by chatViewModel.availableModels.collectAsStateWithLifecycle()
                        HomeScreen(
                            userName = currentUserName.substringBefore(' ').ifBlank { "User" },
                            userEmail = currentUserEmail,
                            availableModels = availableModels,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateChat = { prompt, model ->
                                chatViewModel.clearMessages()
                                backStack.add(NavChat(prompt = prompt, initialModel = model))
                            },
                            onNavigateVoice = { backStack.add(NavVoice) },
                            onNavigateIncognito = {
                                chatViewModel.clearMessages()
                                backStack.add(NavChat(prompt = null, isIncognito = true))
                            },
                            onUpgradeClick = { backStack.add(NavBilling) },
                            onNavigateConnectors = { backStack.add(NavConnectors) },
                            chatViewModel = chatViewModel
                        )
                    }
                }
                entry<NavChat> { key ->
                    ScreenTransitionWrapper {
                        ChatScreen(
                            initialPrompt = key.prompt,
                            sessionId = key.sessionId,
                            isIncognito = key.isIncognito,
                            fromVoice = key.fromVoice,
                            initialModel = key.initialModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onBack = { backStack.removeLastOrNull() },
                            onNavigateVoice = { backStack.add(NavVoice) },
                            onNavigateArtifacts = { backStack.add(NavArtifacts) },
                            onNavigateArtifactViewer = { title, type, code, lang ->
                                backStack.add(NavArtifactViewer(artifactTitle = title, artifactType = type, artifactCode = code, artifactLanguage = lang))
                            },
                            onNavigateConnectors = { backStack.add(NavConnectors) },
                            chatViewModel = chatViewModel
                        )
                    }
                }
                entry<NavVoice> {
                    VoiceScreen(
                        onClose = {
                            backStack.removeLastOrNull()
                            chatViewModel.clearMessages()
                            backStack.add(NavChat(prompt = null, fromVoice = true))
                        },
                        onOpenVoiceSettings = { backStack.add(NavVoiceSettings) }
                    )
                }
                entry<NavChats> {
                    ScreenTransitionWrapper {
                        ChatsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateChat = { sessionId ->
                                backStack.add(NavChat(sessionId = sessionId))
                            },
                            onNewChat = {
                                chatViewModel.clearMessages()
                                backStack.add(NavChat(prompt = null))
                            }
                        )
                    }
                }
                entry<NavProjects> {
                    ScreenTransitionWrapper {
                        ProjectsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateChat = {
                                chatViewModel.clearMessages()
                                backStack.add(NavChat(prompt = null))
                            }
                        )
                    }
                }
                entry<NavCode> {
                    ScreenTransitionWrapper {
                        CodeScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onLaunchTerminal = { backStack.add(NavTerminal) }
                        )
                    }
                }
                entry<NavArtifacts> {
                    ScreenTransitionWrapper {
                        ArtifactsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenArtifact = { title, type, code, lang ->
                                backStack.add(NavArtifactViewer(artifactTitle = title, artifactType = type, artifactCode = code, artifactLanguage = lang))
                            }
                        )
                    }
                }
                entry<NavArtifactViewer> { key ->
                    ScreenTransitionWrapper {
                        ArtifactViewerScreen(
                            artifactTitle = key.artifactTitle,
                            artifactType = key.artifactType,
                            artifactCode = key.artifactCode,
                            artifactLanguage = key.artifactLanguage,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavTasks> {
                    ScreenTransitionWrapper {
                        TasksScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateChat = { prompt -> backStack.add(NavChat(prompt = prompt)) },
                            onNavigateConfig = { backStack.add(NavConnectors) }
                        )
                    }
                }
                entry<NavSettings> {
                    ScreenTransitionWrapper {
                        SettingsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateProfile = { backStack.add(NavProfile) },
                            onNavigateBilling = { backStack.add(NavBilling) },
                            onNavigateCapabilities = { backStack.add(NavCapabilities) },
                            onNavigateConnectors = { backStack.add(NavConnectors) },
                            onNavigatePermissions = { backStack.add(NavPermissions) },
                            onNavigateVoiceSettings = { backStack.add(NavVoiceSettings) },
                            onNavigateNotifications = { backStack.add(NavNotifications) },
                            onNavigateTimeFocus = { backStack.add(NavTimeFocus) },
                            onNavigatePrivacy = { backStack.add(NavPrivacy) },
                            onNavigateSharing = { backStack.add(NavSharing) },
                            onNavigateChannels = { backStack.add(NavChannels) },
                            onNavigateOmniRoute = { backStack.add(NavOmniRoute) },
                            onNavigateAuth = {
                                backStack.clear()
                                backStack.add(NavAuth)
                            },
                            onUpgradeClick = { backStack.add(NavBilling) }
                        )
                    }
                }
                entry<NavProfile> {
                    ScreenTransitionWrapper {
                        ProfileScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavBilling> {
                    ScreenTransitionWrapper {
                        BillingScreen(
                            userEmail = currentUserEmail,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavCapabilities> {
                    ScreenTransitionWrapper {
                        CapabilitiesScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavConnectors> {
                    ScreenTransitionWrapper {
                        ConnectorsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavChannels> {
                    ScreenTransitionWrapper {
                        ChannelsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavOmniRoute> {
                    ScreenTransitionWrapper {
                        OmniRouteScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavPermissions> {
                    ScreenTransitionWrapper {
                        PermissionsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavVoiceSettings> {
                    ScreenTransitionWrapper {
                        VoiceSettingsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavNotifications> {
                    ScreenTransitionWrapper {
                        NotificationsScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavTimeFocus> {
                    ScreenTransitionWrapper {
                        TimeFocusScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavPrivacy> {
                    ScreenTransitionWrapper {
                        PrivacyScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavSharing> {
                    ScreenTransitionWrapper {
                        SharingScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<NavTerminal> {
                    ScreenTransitionWrapper {
                        TerminalScreen(
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            }
        )
    }
}
