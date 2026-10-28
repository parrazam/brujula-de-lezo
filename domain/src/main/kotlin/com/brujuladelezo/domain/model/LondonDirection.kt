package com.brujuladelezo.domain.model

data class LondonDirection(
    /** Giro de la aguja respecto a la parte superior del móvil, en `[0, 360)`. */
    val arrowRotationDegrees: Float,
    val isPointingAtLondon: Boolean,
    val accuracy: CompassAccuracy,
    /** Rumbo verdadero a Londres desde tu posición, en `[0, 360)`. */
    val bearingDegrees: Float = 0f,
    val distanceKm: Double = 0.0,
    /** Cuánto girar el móvil hacia Londres, en `(-180, 180]`; positivo = a la derecha. */
    val turnDegrees: Float = 0f,
) {
    val cardinal: CardinalDirection get() = CardinalDirection.fromBearing(bearingDegrees.toDouble())
}
