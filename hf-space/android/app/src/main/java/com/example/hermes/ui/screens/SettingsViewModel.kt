package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    private val prefs = repository.getPreferencesManager()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _baseUrl = MutableStateFlow(HermesApiClient.DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        viewModelScope.launch {
            prefs?.themeMode?.collect { _themeMode.value = it }
        }
        viewModelScope.launch {
            prefs?.hapticsEnabled?.collect { _hapticsEnabled.value = it }
        }
        viewModelScope.launch {
            prefs?.notificationsEnabled?.collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            prefs?.connectionBaseUrl?.collect { _baseUrl.value = it }
        }
        viewModelScope.launch {
            prefs?.userEmail?.collect { _userEmail.value = it }
        }
        viewModelScope.launch {
            prefs?.userName?.collect { _userName.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            prefs?.setThemeMode(mode)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        viewModelScope.launch {
            prefs?.setHapticsEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        viewModelScope.launch {
            prefs?.setNotificationsEnabled(enabled)
        }
    }

    fun setBaseUrl(url: String) {
        _baseUrl.value = url
        viewModelScope.launch {
            prefs?.setConnectionBaseUrl(url)
            HermesApiClient.instance.updateBaseUrl(url)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
