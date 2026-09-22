package com.example.hermes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
fun MainNavigation(
    chatViewModel: ChatViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    startVoice: Boolean = false
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUserName by authViewModel.userName.collectAsStateWithLifecycle()
    val currentUserEmail by authViewModel.userEmail.collectAsStateWithLifecycle()

    var isSplashVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        isSplashVisible = false
    }

    if (isLoggedIn == null || isSplashVisible) {
        SplashScreen()
        return
    }

    val backStack = rememberNavBackStack(if (isLoggedIn == true) NavHome else NavAuth)
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
                    AuthScreen(
                        onContinueToApp = {
                            backStack.clear()
                            backStack.add(NavHome)
                        }
                    )
                }
                entry<NavHome> {
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
                entry<NavChat> { key ->
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
                entry<NavProjects> {
                    ProjectsScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateChat = {
                            chatViewModel.clearMessages()
                            backStack.add(NavChat(prompt = null))
                        }
                    )
                }
                entry<NavCode> {
                    CodeScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onLaunchTerminal = { backStack.add(NavTerminal) }
                    )
                }
                entry<NavArtifacts> {
                    ArtifactsScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenArtifact = { title, type, code, lang ->
                            backStack.add(NavArtifactViewer(artifactTitle = title, artifactType = type, artifactCode = code, artifactLanguage = lang))
                        }
                    )
                }
                entry<NavArtifactViewer> { key ->
                    ArtifactViewerScreen(
                        artifactTitle = key.artifactTitle,
                        artifactType = key.artifactType,
                        artifactCode = key.artifactCode,
                        artifactLanguage = key.artifactLanguage,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavTasks> {
                    TasksScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateChat = { prompt -> backStack.add(NavChat(prompt = prompt)) },
                        onNavigateConfig = { backStack.add(NavConnectors) }
                    )
                }
                entry<NavSettings> {
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
                entry<NavProfile> {
                    ProfileScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavBilling> {
                    BillingScreen(
                        userEmail = currentUserEmail,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavCapabilities> {
                    CapabilitiesScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavConnectors> {
                    ConnectorsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavChannels> {
                    ChannelsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavOmniRoute> {
                    OmniRouteScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavPermissions> {
                    PermissionsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavVoiceSettings> {
                    VoiceSettingsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavNotifications> {
                    NotificationsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavTimeFocus> {
                    TimeFocusScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavPrivacy> {
                    PrivacyScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavSharing> {
                    SharingScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NavTerminal> {
                    TerminalScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}
