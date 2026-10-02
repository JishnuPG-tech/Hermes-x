package com.example.hermes.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermes.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OmniRouteViewModel(
    private val repository: DataRepository = HermesDataRepository.instance
) : ViewModel() {

    private val _telemetry = MutableStateFlow(OmniRouteTelemetryDto())
    val telemetry: StateFlow<OmniRouteTelemetryDto> = _telemetry.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchTelemetry()
    }

    fun fetchTelemetry() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getOmniRouteTelemetry()
            res.onSuccess {
                _telemetry.value = it
            }
            _isLoading.value = false
        }
    }
}
