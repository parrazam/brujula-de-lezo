package com.brujuladelezo.domain.usecase

import com.brujuladelezo.domain.math.BearingCalculator
import com.brujuladelezo.domain.math.PointingHysteresis
import com.brujuladelezo.domain.model.LondonDirection
import com.brujuladelezo.domain.model.Landmarks
import com.brujuladelezo.domain.repository.GeomagneticRepository
import com.brujuladelezo.domain.repository.LocationRepository
import com.brujuladelezo.domain.repository.OrientationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class ObserveLondonDirectionUseCase(
    private val locationRepository: LocationRepository,
    private val orientationRepository: OrientationRepository,
    private val geomagneticRepository: GeomagneticRepository,
) {
    // El estado de la histéresis vive dentro del flow, así que cada colector tiene el suyo.
    operator fun invoke(): Flow<LondonDirection> = flow {
        var pointing = false
        combine(
            locationRepository.observeLocation(),
            orientationRepository.observeOrientation(),
        ) { location, orientation -> location to orientation }
            .collect { (location, orientation) ->
                val declination = geomagneticRepository.declinationDegrees(location)
                val trueAzimuth = BearingCalculator.normalizeDegrees(
                    orientation.magneticAzimuthDegrees + declination.toDouble()
                )
                val bearing = BearingCalculator.initialBearing(location, Landmarks.LONDON).toFloat()
                val arrowRotation = BearingCalculator.normalizeDegrees(bearing - trueAzimuth.toDouble())
                val deviation = abs(BearingCalculator.relativeAngle(bearing, trueAzimuth))
                pointing = PointingHysteresis.isPointing(deviation, wasPointing = pointing)

                emit(
                    LondonDirection(
                        arrowRotationDegrees = arrowRotation,
                        isPointingAtLondon = pointing,
                        accuracy = orientation.accuracy,
                    )
                )
            }
    }
}
