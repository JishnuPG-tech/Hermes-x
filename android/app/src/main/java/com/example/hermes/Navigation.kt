package com.example.hermes

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.hermes.data.HermesDataRepository
import com.example.hermes.theme.SurfaceDarkElevated
import com.example.hermes.ui.components.ClaudeDrawerContent
import com.example.hermes.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(NavHome)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDarkElevated
            ) {
                ClaudeDrawerContent(
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
                        HermesDataRepository.instance.clearMessages()
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
                    HomeScreen(
                        userName = "Jishnu",
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateChat = { prompt ->
                            HermesDataRepository.instance.clearMessages()
                            backStack.add(NavChat(prompt = prompt))
                        },
                        onNavigateVoice = { backStack.add(NavVoice) },
                        onNavigateIncognito = {
                            HermesDataRepository.instance.clearMessages()
                            backStack.add(NavChat(prompt = null, isIncognito = true))
                        },
                        onUpgradeClick = { backStack.add(NavBilling) }
                    )
                }
                entry<NavChat> { key ->
                    ChatScreen(
                        initialPrompt = key.prompt,
                        sessionId = key.sessionId,
                        isIncognito = key.isIncognito,
                        fromVoice = key.fromVoice,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateVoice = { backStack.add(NavVoice) },
                        onNavigateArtifacts = { backStack.add(NavArtifacts) },
                        onNavigateArtifactViewer = { title, type, code, lang ->
                            backStack.add(NavArtifactViewer(title, type, code, lang))
                        }
                    )
                }
                entry<NavVoice> {
                    VoiceScreen(
                        onClose = {
                            backStack.removeLastOrNull()
                            HermesDataRepository.instance.clearMessages()
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
                            HermesDataRepository.instance.clearMessages()
                            backStack.add(NavChat(prompt = null))
                        }
                    )
                }
                entry<NavProjects> {
                    ProjectsScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateChat = {
                            HermesDataRepository.instance.clearMessages()
                            backStack.add(NavChat(null))
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
                            backStack.add(NavArtifactViewer(title, type, code, lang))
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
