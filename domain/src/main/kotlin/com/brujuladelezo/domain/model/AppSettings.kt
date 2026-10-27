package com.brujuladelezo.domain.model

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class DistanceUnit(private val kmPerUnit: Double) {
    KILOMETERS(1.0),
    /** La milla náutica internacional: 1852 m exactos. */
    NAUTICAL_MILES(1.852);

    fun fromKm(km: Double): Double = km / kmPerUnit
}

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val vibrationEnabled: Boolean = true,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
) {
    companion object {
        /** Lee un enum guardado por nombre; un valor desconocido (versión futura, fichero roto) cae al defecto. */
        inline fun <reified T : Enum<T>> parse(stored: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == stored } ?: default
    }
}
