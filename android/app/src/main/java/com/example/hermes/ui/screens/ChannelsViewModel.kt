package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelsViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    private val _channelsConfig = MutableStateFlow(ChannelsConfigDto())
    val channelsConfig: StateFlow<ChannelsConfigDto> = _channelsConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getChannels()
            res.onSuccess {
                _channelsConfig.value = it
            }.onFailure {
                _statusMessage.value = "Failed to load channels: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun saveChannels(
        telegramEnabled: Boolean,
        telegramToken: String,
        telegramAllowed: String,
        telegramAdminId: String,
        emailEnabled: Boolean,
        emailAddress: String,
        emailPassword: String,
        emailImap: String,
        emailSmtp: String,
        emailPoll: Int,
        discordEnabled: Boolean,
        discordToken: String,
        discordAllowed: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Saving channel configurations..."
            val req = UpdateChannelsRequestDto(
                telegram = TelegramUpdateRequestDto(
                    enabled = telegramEnabled,
                    token = telegramToken.ifBlank { null },
                    allowed_users = telegramAllowed.ifBlank { null },
                    admin_id = telegramAdminId.ifBlank { null }
                ),
                email = EmailUpdateRequestDto(
                    enabled = emailEnabled,
                    address = emailAddress.ifBlank { null },
                    password = emailPassword.ifBlank { null },
                    imap_host = emailImap.ifBlank { null },
                    smtp_host = emailSmtp.ifBlank { null },
                    poll_interval = emailPoll
                ),
                discord = DiscordUpdateRequestDto(
                    enabled = discordEnabled,
                    token = discordToken.ifBlank { null },
                    allowed_users = discordAllowed.ifBlank { null }
                )
            )
            val res = repository.updateChannels(req)
            res.onSuccess {
                _statusMessage.value = "Channels saved & reloaded successfully!"
                loadChannels()
            }.onFailure {
                _statusMessage.value = "Failed to save: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun testChannel(channel: String) {
        viewModelScope.launch {
            _statusMessage.value = "Sending test message to $channel..."
            val res = repository.testChannel(channel, "🔔 [Hermes Test Alert] Channel connection is verified and operational!")
            res.onSuccess {
                if (it.status == "success") {
                    _statusMessage.value = "✅ $channel test alert sent successfully!"
                } else {
                    _statusMessage.value = "⚠️ $channel test failed: ${it.error}"
                }
            }.onFailure {
                _statusMessage.value = "❌ Error testing $channel: ${it.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
