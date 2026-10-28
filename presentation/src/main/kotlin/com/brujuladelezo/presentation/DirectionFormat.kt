package com.brujuladelezo.presentation

import com.brujuladelezo.domain.model.DistanceUnit
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Distancia en la unidad elegida, con el separador del idioma del móvil: un decimal por debajo
 * de 10 (desde Dover importa), entero a partir de ahí.
 */
internal fun formatDistance(km: Double, unit: DistanceUnit, locale: Locale): String {
    val value = unit.fromKm(km)
    val format = NumberFormat.getNumberInstance(locale).apply {
        val decimals = if (value < 10.0) 1 else 0
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return format.format(value)
}

/** Grados enteros de giro, sin signo; el lado lo pone el texto. */
internal fun turnMagnitude(turnDegrees: Float): Int = abs(turnDegrees).roundToInt()

/** Rumbo entero en `[0, 360)`: 359,6° se redondea a 0°, no a 360°. */
internal fun roundedBearing(bearingDegrees: Float): Int = bearingDegrees.roundToInt().mod(360)
