package com.brujuladelezo.presentation

import com.brujuladelezo.domain.model.CardinalDirection
import com.brujuladelezo.domain.model.CompassAccuracy

data class CompassUiState(
    val arrowRotation: Float = 0f,
    val isPointingAtLondon: Boolean = false,
    val accuracy: CompassAccuracy = CompassAccuracy.BAJA,
    val hasLocationPermission: Boolean = false,
    /** Con permiso, todavía sin el primer rumbo. */
    val isLoading: Boolean = false,
    val hasCompassSensor: Boolean = true,
    val isLocationEnabled: Boolean = true,
    /** El flujo de sensores falló; la aguja no es fiable y no se muestra. */
    val trackingFailed: Boolean = false,
    val bearingDegrees: Float = 0f,
    val cardinal: CardinalDirection = CardinalDirection.N,
    val distanceKm: Double = 0.0,
    /** Positivo = gira a la derecha. */
    val turnDegrees: Float = 0f,
) {
    /**
     * Sin ubicación del sistema solo bloquea mientras no haya un primer rumbo: si se apaga
     * después, la última posición sigue sirviendo (Londres no se mueve y tú, poco).
     */
    val showLocationDisabled: Boolean get() = isLoading && !isLocationEnabled
}
