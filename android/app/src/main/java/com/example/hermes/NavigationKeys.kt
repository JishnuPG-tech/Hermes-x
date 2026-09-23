package com.example.hermes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object NavAuth : NavKey
@Serializable data object NavHome : NavKey
@Serializable data class NavChat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String? = null,
    val sessionId: String? = null,
    val isIncognito: Boolean = false,
    val fromVoice: Boolean = false,
    val initialModel: String = "Hermes Smart"
) : NavKey
@Serializable data object NavVoice : NavKey
@Serializable data class NavChats(val filterType: String = "all") : NavKey
@Serializable data object NavProjects : NavKey
@Serializable data object NavCode : NavKey
@Serializable data object NavArtifacts : NavKey
@Serializable data class NavArtifactViewer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val artifactTitle: String = "Theme Showcase",
    val artifactType: String = "Application",
    val artifactCode: String? = null,
    val artifactLanguage: String? = null
) : NavKey
@Serializable data object NavTasks : NavKey
@Serializable data object NavSkills : NavKey
@Serializable data object NavAgents : NavKey
@Serializable data object NavKnowledge : NavKey
@Serializable data object NavActivity : NavKey
@Serializable data object NavSettings : NavKey
@Serializable data object NavProfile : NavKey
@Serializable data object NavBilling : NavKey
@Serializable data object NavCapabilities : NavKey
@Serializable data object NavConnectors : NavKey
@Serializable data object NavPermissions : NavKey
@Serializable data object NavVoiceSettings : NavKey
@Serializable data object NavNotifications : NavKey
@Serializable data object NavTimeFocus : NavKey
@Serializable data object NavPrivacy : NavKey
@Serializable data object NavSharing : NavKey
@Serializable data object NavTerminal : NavKey
@Serializable data object NavChannels : NavKey
@Serializable data object NavOmniRoute : NavKey
