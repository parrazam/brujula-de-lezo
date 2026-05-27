package com.brujuladelezo.domain.math

import com.brujuladelezo.domain.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BearingCalculator {

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
