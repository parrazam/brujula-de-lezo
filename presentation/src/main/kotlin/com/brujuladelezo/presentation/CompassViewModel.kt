package com.brujuladelezo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.LondonDirection
import com.brujuladelezo.domain.usecase.ObserveLocationEnabledUseCase
import com.brujuladelezo.domain.usecase.ObserveLondonDirectionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Margen para que un giro de pantalla no apague y vuelva a encender los sensores. */
internal const val STOP_TIMEOUT_MILLIS = 5_000L

class CompassViewModel(
    private val observeLondonDirection: ObserveLondonDirectionUseCase,
    private val observeLocationEnabled: ObserveLocationEnabledUseCase,
    private val dispatchers: DispatcherProvider,
    private val hasCompassSensor: Boolean = true,
) : ViewModel() {

    private data class Request(val granted: Boolean, val attempt: Int)

    private val request = MutableStateFlow(Request(granted = false, attempt = 0))

    private val idleState = CompassUiState(hasCompassSensor = hasCompassSensor)

    /**
     * Los sensores y la ubicación solo están registrados mientras alguien observa el estado.
     * Antes el flujo se recogía en viewModelScope sin más, y seguían activos con la app en
     * segundo plano hasta que se destruía el ViewModel.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CompassUiState> = request
        .flatMapLatest { (granted, _) ->
            when {
                !hasCompassSensor -> flowOf(idleState)
                !granted -> flowOf(idleState)
                else -> trackingStates()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), idleState)

    private sealed interface Tracking {
        data object Waiting : Tracking
        data class Ready(val direction: LondonDirection) : Tracking
        data object Failed : Tracking
    }

    private fun trackingStates(): Flow<CompassUiState> {
        val tracking: Flow<Tracking> = observeLondonDirection()
            .map<LondonDirection, Tracking> { Tracking.Ready(it) }
            .onStart { emit(Tracking.Waiting) }
            .catch { emit(Tracking.Failed) }
            .flowOn(dispatchers.default)

        val locationEnabled = observeLocationEnabled()
            .onStart { emit(true) }
            .catch { emit(true) }

        return combine(tracking, locationEnabled) { state, enabled ->
            val base = idleState.copy(hasLocationPermission = true, isLocationEnabled = enabled)
            when (state) {
                Tracking.Waiting -> base.copy(isLoading = true)
                Tracking.Failed -> base.copy(trackingFailed = true)
                is Tracking.Ready -> base.copy(
                    arrowRotation = state.direction.arrowRotationDegrees,
                    isPointingAtLondon = state.direction.isPointingAtLondon,
                    accuracy = state.direction.accuracy,
                    bearingDegrees = state.direction.bearingDegrees,
                    cardinal = state.direction.cardinal,
                    distanceKm = state.direction.distanceKm,
                    turnDegrees = state.direction.turnDegrees,
                )
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        request.update { if (it.granted == granted) it else it.copy(granted = granted) }
    }

    /** Vuelve a suscribirse a los sensores tras un fallo. */
    fun retry() {
        request.update { it.copy(attempt = it.attempt + 1) }
    }
}
