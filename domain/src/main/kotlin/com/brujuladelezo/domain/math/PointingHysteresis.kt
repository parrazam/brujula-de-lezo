package com.brujuladelezo.domain.math

/**
 * Decide si el móvil apunta a Londres con dos umbrales: se entra por debajo de
 * [ENTER_DEGREES] y no se sale hasta superar [EXIT_DEGREES].
 *
 * Con un único umbral, el ruido del magnetómetro (±1–2° incluso con el filtro paso-bajo) hacía
 * que la desviación cruzase los 5° varias veces por segundo al apuntar justo al borde, y cada
 * cruce disparaba otra vibración.
 */
object PointingHysteresis {
    const val ENTER_DEGREES = 5f
    const val EXIT_DEGREES = 8f

    /** @param deviationDegrees desviación absoluta entre el rumbo a Londres y el azimut. */
    fun isPointing(deviationDegrees: Float, wasPointing: Boolean): Boolean =
        if (wasPointing) deviationDegrees <= EXIT_DEGREES else deviationDegrees < ENTER_DEGREES
}
