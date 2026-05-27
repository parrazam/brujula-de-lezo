package com.brujuladelezo.domain.usecase

import com.brujuladelezo.domain.math.BearingCalculator
import com.brujuladelezo.domain.model.LondonDirection
import com.brujuladelezo.domain.model.Landmarks
import com.brujuladelezo.domain.repository.GeomagneticRepository
import com.brujuladelezo.domain.repository.LocationRepository
import com.brujuladelezo.domain.repository.OrientationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val POINTING_THRESHOLD_DEGREES = 5f

class ObserveLondonDirectionUseCase(
    private val locationRepository: LocationRepository,
    private val orientationRepository: OrientationRepository,
    private val geomagneticRepository: GeomagneticRepository,
) {
    operator fun invoke(): Flow<LondonDirection> =
        combine(
            locationRepository.observeLocation(),
            orientationRepository.observeOrientation(),
        ) { location, orientation ->
            val declination = geomagneticRepository.declinationDegrees(location)
            val trueAzimuth = BearingCalculator.normalizeDegrees(
                orientation.magneticAzimuthDegrees + declination.toDouble()
            )
            val bearing = BearingCalculator.initialBearing(location, Landmarks.LONDON).toFloat()
            val arrowRotation = BearingCalculator.normalizeDegrees(bearing - trueAzimuth.toDouble())
            val isPointing = Math.abs(
                BearingCalculator.relativeAngle(bearing, trueAzimuth)
            ) < POINTING_THRESHOLD_DEGREES

            LondonDirection(
                arrowRotationDegrees = arrowRotation,
                isPointingAtLondon = isPointing,
                accuracy = orientation.accuracy,
            )
        }
}
