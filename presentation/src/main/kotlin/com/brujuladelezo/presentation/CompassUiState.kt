package com.brujuladelezo.presentation

import com.brujuladelezo.domain.model.CompassAccuracy

data class CompassUiState(
    val arrowRotation: Float = 0f,
    val isPointingAtLondon: Boolean = false,
    val accuracy: CompassAccuracy = CompassAccuracy.BAJA,
    val hasLocationPermission: Boolean = false,
    val isLoading: Boolean = false,
    val hasCompassSensor: Boolean = true,
)
