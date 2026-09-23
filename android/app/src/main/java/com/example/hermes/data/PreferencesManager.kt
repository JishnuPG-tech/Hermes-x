package com.example.hermes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.hermesDataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_user_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_BASE_URL = stringPreferencesKey("connection_base_url")
        val KEY_API_KEY = stringPreferencesKey("connection_api_key")
        val KEY_GOOGLE_CLIENT_ID = stringPreferencesKey("google_client_id")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        val KEY_GOOGLE_SUB = stringPreferencesKey("google_sub")

        val KEY_FONT_STYLE = stringPreferencesKey("font_style")
        val KEY_VOICE_MODE = stringPreferencesKey("voice_engine_mode")
        val KEY_VOICE_PERSONA = stringPreferencesKey("voice_persona")
        val KEY_VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val KEY_VOICE_PACE = stringPreferencesKey("voice_pace")
        val KEY_CONNECTOR_DISCOVERY = booleanPreferencesKey("connector_discovery")

        val KEY_CAP_WEB_SEARCH = booleanPreferencesKey("cap_web_search")
        val KEY_CAP_INLINE_VIZ = booleanPreferencesKey("cap_inline_viz")
        val KEY_CAP_CODE_EXEC = booleanPreferencesKey("cap_code_exec")
        val KEY_CAP_SWITCH_MODELS = booleanPreferencesKey("cap_switch_models")
        val KEY_CAP_GEN_MEMORY = booleanPreferencesKey("cap_gen_memory")
        val KEY_CAP_SENSITIVE_MEM = booleanPreferencesKey("cap_sensitive_mem")
        val KEY_CAP_TOOL_ACCESS = stringPreferencesKey("cap_tool_access")

        const val ADMIN_EMAIL = "jishnupg2005@gmail.com"
        const val DEFAULT_GOOGLE_CLIENT_ID = "292824298430-113kq16cbpq6i02jin424gb1mk5ebm40.apps.googleusercontent.com"
        val KEY_USER_ROLE = stringPreferencesKey("user_role")

        fun isUserAdmin(email: String?, role: String? = null): Boolean {
            if (role?.equals("admin", ignoreCase = true) == true) return true
            val trimmed = email?.trim()
            if (trimmed.isNullOrBlank()) return false
            return trimmed.equals(ADMIN_EMAIL, ignoreCase = true) ||
                   trimmed.equals("jishnu.pg@gmail.com", ignoreCase = true)
        }

        fun getVoiceIdForPersona(persona: String?): String {
            return when (persona?.trim()) {
                "Jenny"                  -> "en-US-JennyNeural"
                "Ava"                    -> "en-US-AvaNeural"
                "Aria"                   -> "en-US-AriaNeural"
                "Chris", "Christopher"   -> "en-US-ChristopherNeural"
                "Guy"                    -> "en-US-GuyNeural"
                "Eric"                   -> "en-US-EricNeural"
                // Legacy persona fallbacks
                "Airy"                   -> "en-US-AriaNeural"
                "Brass"                  -> "en-US-EricNeural"
                "Rounded"                -> "en-US-ChristopherNeural"
                "Glassy"                 -> "en-US-JennyNeural"
                "Mellow"                 -> "en-US-AvaNeural"
                else                     -> "en-US-JennyNeural"
            }
        }

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val themeMode: Flow<ThemeMode> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.DARK.name
            try {
                ThemeMode.valueOf(raw)
            } catch (_: Exception) {
                ThemeMode.DARK
            }
        }

    val selectedModel: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_SELECTED_MODEL] ?: "hermes-agent"
        }

    val hapticsEnabled: Flow<Boolean> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_HAPTICS_ENABLED] ?: true
        }

    val notificationsEnabled: Flow<Boolean> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    val connectionBaseUrl: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_BASE_URL] ?: HermesApiClient.DEFAULT_BASE_URL
        }

    val connectionApiKey: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_API_KEY] ?: HermesApiClient.DEFAULT_API_KEY
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setConnectionBaseUrl(url: String) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = url.trimEnd('/')
        }
    }

    suspend fun setConnectionApiKey(key: String) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key
        }
    }

    val googleClientId: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_GOOGLE_CLIENT_ID] ?: DEFAULT_GOOGLE_CLIENT_ID
        }

    val authToken: Flow<String?> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_AUTH_TOKEN]
        }

    val userName: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_USER_NAME] ?: "Jishnu"
        }

    val userEmail: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_USER_EMAIL] ?: ""
        }

    val userAvatar: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[KEY_USER_AVATAR] ?: ""
        }

    val isLoggedIn: Flow<Boolean> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val token = prefs[KEY_AUTH_TOKEN]
            !token.isNullOrBlank()
        }

    val currentUserId: Flow<String> = context.hermesDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val email = prefs[KEY_USER_EMAIL]?.trim()
            if (!email.isNullOrBlank()) email else "anonymous_user"
        }

    suspend fun clearAuth() {
        context.hermesDataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_AVATAR)
            prefs.remove(KEY_GOOGLE_SUB)
            prefs.remove(KEY_USER_ROLE)
        }
    }

    suspend fun setGoogleClientId(clientId: String) {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_GOOGLE_CLIENT_ID] = clientId.trim()
        }
    }

    suspend fun setAuthToken(token: String?) {
        context.hermesDataStore.edit { prefs ->
            if (token != null) {
                prefs[KEY_AUTH_TOKEN] = token
            } else {
                prefs.remove(KEY_AUTH_TOKEN)
            }
        }
    }

    suspend fun setUserProfile(name: String, email: String, avatar: String = "", googleSub: String = "") {
        context.hermesDataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_AVATAR] = avatar
            if (googleSub.isNotBlank()) prefs[KEY_GOOGLE_SUB] = googleSub
            if (isUserAdmin(email)) {
                prefs[KEY_USER_ROLE] = "admin"
            }
        }
    }

    val googleSub: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_GOOGLE_SUB] ?: "" }

    suspend fun setGoogleSub(sub: String) {
        context.hermesDataStore.edit { it[KEY_GOOGLE_SUB] = sub }
    }

    val fontStyle: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_FONT_STYLE] ?: "Default" }

    suspend fun setFontStyle(style: String) {
        context.hermesDataStore.edit { it[KEY_FONT_STYLE] = style }
    }

    val voiceMode: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { 
            val mode = it[KEY_VOICE_MODE] ?: "hugging_voice"
            if (mode == "apollo") "hugging_voice" else mode
        }

    suspend fun setVoiceMode(mode: String) {
        context.hermesDataStore.edit { it[KEY_VOICE_MODE] = mode }
    }

    val voicePersona: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { 
            val p = it[KEY_VOICE_PERSONA]
            if (p.isNullOrBlank() || p == "Rounded") "Jenny" else p
        }

    val voicePersonaVoiceId: Flow<String> = voicePersona.map { getVoiceIdForPersona(it) }

    suspend fun setVoicePersona(persona: String) {
        context.hermesDataStore.edit { it[KEY_VOICE_PERSONA] = persona }
    }

    val voiceLanguage: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_VOICE_LANGUAGE] ?: "English (United Kingdom)" }

    suspend fun setVoiceLanguage(lang: String) {
        context.hermesDataStore.edit { it[KEY_VOICE_LANGUAGE] = lang }
    }

    val voicePace: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_VOICE_PACE] ?: "Normal" }

    suspend fun setVoicePace(pace: String) {
        context.hermesDataStore.edit { it[KEY_VOICE_PACE] = pace }
    }

    val connectorDiscovery: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CONNECTOR_DISCOVERY] ?: true }

    suspend fun setConnectorDiscovery(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CONNECTOR_DISCOVERY] = enabled }
    }

    val capWebSearch: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_WEB_SEARCH] ?: true }

    suspend fun setCapWebSearch(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_WEB_SEARCH] = enabled }
    }

    val capInlineViz: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_INLINE_VIZ] ?: true }

    suspend fun setCapInlineViz(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_INLINE_VIZ] = enabled }
    }

    val capCodeExec: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_CODE_EXEC] ?: true }

    suspend fun setCapCodeExec(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_CODE_EXEC] = enabled }
    }

    val capSwitchModels: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_SWITCH_MODELS] ?: true }

    suspend fun setCapSwitchModels(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_SWITCH_MODELS] = enabled }
    }

    val capGenMemory: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_GEN_MEMORY] ?: true }

    suspend fun setCapGenMemory(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_GEN_MEMORY] = enabled }
    }

    val capSensitiveMem: Flow<Boolean> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_SENSITIVE_MEM] ?: false }

    suspend fun setCapSensitiveMem(enabled: Boolean) {
        context.hermesDataStore.edit { it[KEY_CAP_SENSITIVE_MEM] = enabled }
    }

    val capToolAccess: Flow<String> = context.hermesDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_CAP_TOOL_ACCESS] ?: "Auto" }

    suspend fun setCapToolAccess(access: String) {
        context.hermesDataStore.edit { it[KEY_CAP_TOOL_ACCESS] = access }
    }

    suspend fun saveCredential(service: String, secret: String) {
        val key = stringPreferencesKey("cred_${service.lowercase().trim()}")
        context.hermesDataStore.edit { it[key] = secret }
    }

    fun getCredential(service: String): Flow<String?> {
        val key = stringPreferencesKey("cred_${service.lowercase().trim()}")
        return context.hermesDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] }
    }
}

