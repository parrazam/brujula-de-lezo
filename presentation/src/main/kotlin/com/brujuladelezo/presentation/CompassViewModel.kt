package com.brujuladelezo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.usecase.ObserveLondonDirectionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompassViewModel(
    private val observeLondonDirection: ObserveLondonDirectionUseCase,
    private val dispatchers: DispatcherProvider,
    hasCompassSensor: Boolean = true,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompassUiState(hasCompassSensor = hasCompassSensor))
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) startTracking()
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            observeLondonDirection()
                .flowOn(dispatchers.default)
                .catch { _uiState.update { s -> s.copy(isLoading = false) } }
                .collect { direction ->
                    _uiState.update { s ->
                        s.copy(
                            arrowRotation = direction.arrowRotationDegrees,
                            isPointingAtLondon = direction.isPointingAtLondon,
                            accuracy = direction.accuracy,
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
