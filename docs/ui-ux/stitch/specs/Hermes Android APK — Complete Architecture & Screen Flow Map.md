# Hermes Android APK — Complete Architecture & Screen Flow Map

## Overview
This document connects every screen, dialog, sheet, and background state into a unified, production-grade Android Navigation Flow for the Hermes Android APK (`com.nousresearch.hermes`), matching Claude's calm editorial baseline (`#141413`, Copernicus/Newsreader serif, `#c96442` terracotta accent) upgraded with Hermes autonomous agent capabilities.

---

## 1. Complete Screen Inventory & Routes

| Step / Route | Screen Title | Artifact ID | Key Function & Transition |
|---|---|---|---|
| **0. Splash** | `Hermes Android - Splash Screen` | `{{DATA:SCREEN:SCREEN_2}}` | Cold start entry; pure dark `#141413`, white Hermes logo + serif wordmark, "NOUS RESEARCH" base credit. Auto-advances (1200ms) or on token check. |
| **1. Auth** | `Hermes Android - Welcome & Sign In` | `{{DATA:SCREEN:SCREEN_3}}` | Sign in / Onboarding: Neural graph illustration, "The AI for problem solvers", Google SSO / Email login, Consumer terms. |
| **2. Home / Greeting** | `Hermes Android - Home & Greeting` | `{{DATA:SCREEN:SCREEN_13}}` / `{{DATA:SCREEN:SCREEN_18}}` | Primary idle hub: Top bar (Drawer trigger ☰, Ghost incognito mode), central pure white Hermes vector logo, serif greeting *"Good afternoon, Jishnu"*, docked composer with model chip (`Sonnet 5 Low`), dictation mic, voice trigger. |
| **3. Nav Drawer** | `Hermes - Navigation Drawer` | `{{DATA:SCREEN:SCREEN_37}}` / `{{DATA:SCREEN:SCREEN_28}}` | Modal navigation drawer sliding from left: Chats, Projects, Tasks, Code, Server Terminal, Artifacts, Pinned conversations, Recents history, User avatar `J`, "+ New chat" white pill button. |
| **4. Attach Sheet** | `Claude Android - Add to Chat Sheet` | `{{DATA:SCREEN:SCREEN_45}}` | Bottom sheet from composer `+`: 3-up icons (Camera, Photos, Files), Web search toggle, Memory toggle, Add to Project dropdown selector. |
| **5. Model Switcher** | `Claude Android - Select Model Sheet` | `{{DATA:SCREEN:SCREEN_46}}` | Bottom sheet from model chip: Model hierarchy (Fable 5.1 Pro/Max, Opus 5 Pro, Sonnet 5 Default, Haiku 4.5 Fast), checkmark selection with accent blue `#3898ec` indicator. |
| **6. Active Chat** | `Hermes Android - Active Chat & Execution Sheet` | `{{DATA:SCREEN:SCREEN_17}}` / `{{DATA:SCREEN:SCREEN_22}}` | Live session: User prompt card, tool execution card (`Creating file >`), animated streaming status indicator (`Still working on it...`), bottom peek Summary sheet showing autonomous step breakdown. |
| **7. Summary Sheet** | `Hermes Android - Full-Screen Summary Sheet` | `{{DATA:SCREEN:SCREEN_16}}` / `{{DATA:SCREEN:SCREEN_21}}` | Full-screen expanded sheet: Multi-step reasoning pipeline (Drafting Android guide, Creating files, Thinking/Sleuthing), token telemetry, execution checkpoint controls. |
| **8. Autonomous Tasks** | `Hermes - Autonomous Tasks` | `{{DATA:SCREEN:SCREEN_35}}` | Background task monitor: Active autonomous jobs, subtask graph, worker delegation (Backend, QA, DevOps agents), verification gates, pause/cancel/retry. |
| **9. Server Terminal** | `Hermes - Server Terminal & Files` | `{{DATA:SCREEN:SCREEN_33}}` | Server computer interface: Interactive shell execution, streaming logs, workspace directory explorer (`/data/jarvis/projects`), git diff inspection. |
| **10. Live Voice Call** | `Hermes Android - Live Voice Call` | `{{DATA:SCREEN:SCREEN_15}}` / `{{DATA:SCREEN:SCREEN_44}}` | Full-screen ambient voice HUD: Pulsing Hermes mark, *"Hold tight, connecting..."*, green system mic indicator, audio waveform, circular mic mute, end-call `✕` button. |
| **11. Voice Settings** | `Claude Android - Voice Settings` | `{{DATA:SCREEN:SCREEN_43}}` | Configuration for voice: Horizontal carousel of voice personas (e.g. "Rounded", warm feminine tone), language picker (English UK BETA), speaking pace (Normal/Fast). |
| **12. Connectors & Vault** | `Hermes - Connectors & Vault` | `{{DATA:SCREEN:SCREEN_31}}` / `{{DATA:SCREEN:SCREEN_29}}` | Integrations & secrets: Connector discovery toggle, Hugging Face, GitHub, Google Drive, Notion connections with secure server-side vault reference. |
| **13. Capabilities** | `Claude Android - Capabilities` | `{{DATA:SCREEN:SCREEN_42}}` | Feature switches: Web search, Artifacts (locked ON dependency), Inline visualizations, Code execution and file creation. |
| **14. Settings / Account** | `Claude Android - Settings Account` | `{{DATA:SCREEN:SCREEN_41}}` | Account profile, subscription tier, billing, system permissions, appearance (Color mode, Font style), notifications, logout. |

---

## 2. Jetpack Compose Navigation Architecture (Kotlin)

```kotlin
// AppNavigation.kt
@Composable
fun HermesAppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        // Cold Start / Auth
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isAuthenticated ->
                    val destination = if (isAuthenticated) Screen.Home.route else Screen.Welcome.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Welcome.route) {
            WelcomeSignInScreen(
                onGoogleSignIn = { navController.navigate(Screen.Home.route) },
                onEmailSignIn = { navController.navigate(Screen.Home.route) }
            )
        }

        // Primary Surface
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenDrawer = { /* Open Drawer via DrawerState */ },
                onOpenVoice = { navController.navigate(Screen.VoiceCall.route) },
                onOpenChatSession = { sessionId -> navController.navigate(Screen.Chat.createRoute(sessionId)) },
                onOpenTasks = { navController.navigate(Screen.Tasks.route) },
                onOpenTerminal = { navController.navigate(Screen.Terminal.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Active Chat & Execution
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onOpenSummary = { navController.navigate(Screen.SummarySheet.route) },
                onOpenVoice = { navController.navigate(Screen.VoiceCall.route) }
            )
        }

        // Full-screen Modal Views
        composable(Screen.VoiceCall.route) {
            LiveVoiceCallScreen(
                onEndCall = { navController.popBackStack() },
                onOpenVoiceSettings = { navController.navigate(Screen.VoiceSettings.route) }
            )
        }
        composable(Screen.SummarySheet.route) {
            FullSummarySheetScreen(
                onDismiss = { navController.popBackStack() }
            )
        }

        // Core Management Surfaces
        composable(Screen.Tasks.route) {
            AutonomousTasksScreen(
                onBack = { navController.popBackStack() },
                onTaskSelected = { taskId -> /* drill-in */ }
            )
        }
        composable(Screen.Terminal.route) {
            ServerTerminalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Connectors.route) {
            ConnectorsVaultScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Capabilities.route) {
            CapabilitiesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VoiceSettings.route) {
            VoiceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsAccountScreen(
                onBack = { navController.popBackStack() },
                onNavigateToConnectors = { navController.navigate(Screen.Connectors.route) },
                onNavigateToCapabilities = { navController.navigate(Screen.Capabilities.route) },
                onNavigateToVoiceSettings = { navController.navigate(Screen.VoiceSettings.route) },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
```

---

## 3. End-to-End User Journeys

### Flow A: Launch & Prompt Execution
1. **Splash (`SCREEN_2`)** launches with pure `#141413` backdrop and crisp white Hermes vector emblem →
2. **Home (`SCREEN_13`)** displays greeting *"Good afternoon, Jishnu"*, user types request in docked composer →
3. Tapping `+` opens **Attach Sheet (`SCREEN_45`)** to toggle Web search / Memory or attach local files →
4. Tapping Model chip opens **Select Model Sheet (`SCREEN_46`)** to pick inference tier (`Sonnet 5`) →
5. Hitting send navigates to **Active Chat (`SCREEN_17`)** with streaming markdown, tool indicator `Creating file >`, and progress spinner →
6. User taps summary pill to expand **Full-Screen Summary Sheet (`SCREEN_16`)** tracking live agent steps and token traces.

### Flow B: Voice Session with Barge-in
1. User taps the white circular voice button on Home or Chat composer →
2. Modal takeover transitions into **Live Voice Call (`SCREEN_15`)** with system mic indicator (`#22c55e`) and status *"Hold tight, connecting..."* →
3. Real-time streaming STT/TTS connects to Kokoro-82M / Edge TTS; Hermes audio waveform responds with barge-in support →
4. Tapping top-right gear opens **Voice Settings (`SCREEN_43`)** carousel to adjust tone or pace →
5. Tapping `✕` ends call and inserts inline receipt card *"Voice chat ended · 2s"* into the active feed.

### Flow C: Autonomous Server & Tasks Supervision
1. User taps ☰ hamburger menu on Home to open **Navigation Drawer (`SCREEN_37`)** →
2. Selects **Autonomous Tasks (`SCREEN_35`)** to monitor long-running background tasks, agent delegation, and safety checkpoints →
3. Selects **Server Terminal (`SCREEN_33`)** to monitor server shell, docker containers, and workspace files at `/data/jarvis/projects` →
4. Selects **Connectors (`SCREEN_31`)** to manage Hugging Face, GitHub, and Notion OAuth connections with zero client secret exposure.
