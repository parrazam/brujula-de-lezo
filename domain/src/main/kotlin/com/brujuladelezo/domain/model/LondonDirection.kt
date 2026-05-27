package com.brujuladelezo.domain.model

data class LondonDirection(
    val arrowRotationDegrees: Float,
    val isPointingAtLondon: Boolean,
    val accuracy: CompassAccuracy,
)
