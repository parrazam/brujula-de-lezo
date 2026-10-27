package com.brujuladelezo.domain.math

import com.brujuladelezo.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BearingCalculator {

    /** Radio medio de la Tierra (IUGG), en km. */
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Distancia de círculo máximo (haversine) entre dos puntos, en km. */
    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun initialBearing(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val x = sin(deltaLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)

        return normalizeDegrees(Math.toDegrees(atan2(x, y))).toDouble()
    }

    fun normalizeDegrees(deg: Double): Float {
        return ((deg % 360.0 + 360.0) % 360.0).toFloat()
    }

    /**
     * Returns the shortest angular difference from [current] to [target], in (-180, 180].
     * Positive = clockwise, negative = counter-clockwise.
     */
    fun relativeAngle(target: Float, current: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff <= -180f) diff += 360f
        return diff
    }
}
