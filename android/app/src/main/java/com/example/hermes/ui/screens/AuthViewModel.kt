package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.DataRepository
import com.example.hermes.data.HermesDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    val googleClientId: StateFlow<String> = repository.googleClientId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "292824298430-113kq16cbpq6i02jin424gb1mk5ebm40.apps.googleusercontent.com")

    val serverBaseUrl: StateFlow<String> = repository.currentServerUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://jishnupg-hermes.hf.space")

    val isLoggedIn: StateFlow<Boolean?> = repository.isLoggedIn
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userName: StateFlow<String> = (repository.getPreferencesManager()?.userName ?: kotlinx.coroutines.flow.flowOf("User"))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userEmail: StateFlow<String> = (repository.getPreferencesManager()?.userEmail ?: kotlinx.coroutines.flow.flowOf(""))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshAuthConfig()
    }

    fun refreshAuthConfig() {
        viewModelScope.launch {
            repository.refreshAuthConfig()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setError(error: String) {
        _errorMessage.value = error
    }

    fun loginWithGoogleIdToken(
        idToken: String,
        displayName: String,
        email: String,
        avatar: String,
        onSuccess: () -> Unit
    ) {
        // INSTANT NAVIGATION: Google identity is verified by Play Services; transition immediately!
        onSuccess()

        viewModelScope.launch {
            try {
                repository.loginWithGoogle(idToken, displayName, email, avatar)
            } catch (_: Exception) {}
        }
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        // INSTANT NAVIGATION: Transition immediately!
        onSuccess()

        viewModelScope.launch {
            try {
                repository.getPreferencesManager()?.setAuthToken("guest_token")
                repository.getPreferencesManager()?.setUserProfile("Guest", "guest@hermes.local")
                repository.fetchSessions()
                repository.fetchModels()
            } catch (_: Exception) {}
        }
    }

    fun updateGoogleClientId(clientId: String) {
        viewModelScope.launch {
            repository.setGoogleClientId(clientId)
        }
    }

    fun updateServerBaseUrl(url: String) {
        viewModelScope.launch {
            repository.setServerBaseUrl(url)
        }
    }
}
