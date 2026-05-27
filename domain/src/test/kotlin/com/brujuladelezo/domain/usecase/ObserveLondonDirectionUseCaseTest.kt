package com.brujuladelezo.domain.usecase

import app.cash.turbine.test
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.model.RawOrientation
import com.brujuladelezo.domain.repository.GeomagneticRepository
import com.brujuladelezo.domain.repository.LocationRepository
import com.brujuladelezo.domain.repository.OrientationRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveLondonDirectionUseCaseTest {

    private val madrid = GeoPoint(40.4168, -3.7038)

    private fun makeUseCase(
        location: GeoPoint = madrid,
        azimuth: Float = 0f,
        declination: Float = 0f,
    ): ObserveLondonDirectionUseCase {
        val locationRepo = object : LocationRepository {
            override fun observeLocation() = flowOf(location)
        }
        val orientationRepo = object : OrientationRepository {
            override fun observeOrientation() = flowOf(RawOrientation(azimuth, CompassAccuracy.ALTA))
        }
        val geomagneticRepo = object : GeomagneticRepository {
            override fun declinationDegrees(point: GeoPoint) = declination
        }
        return ObserveLondonDirectionUseCase(locationRepo, orientationRepo, geomagneticRepo)
    }

    @Test
    fun `arrowRotation is calculated from Madrid with zero azimuth`() = runTest {
        val useCase = makeUseCase(azimuth = 0f, declination = 0f)
        useCase().test {
            val direction = awaitItem()
            // Bearing Madrid->London ≈ 11.36° NNE; azimuth=0 → arrowRotation ≈ 11°
            assertTrue(
                "Arrow rotation should be ~11°, was ${direction.arrowRotationDegrees}",
                direction.arrowRotationDegrees in 8f..15f
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isPointingAtLondon is true when azimuth matches bearing`() = runTest {
        // Bearing Madrid->London ≈ 11.36°; if azimuth ≈ 11°, arrow ≈ 0° → pointing
        val useCase = makeUseCase(azimuth = 11f, declination = 0f)
        useCase().test {
            val direction = awaitItem()
            assertTrue("Should be pointing at London, rotation=${direction.arrowRotationDegrees}", direction.isPointingAtLondon)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isPointingAtLondon is false when azimuth is far from bearing`() = runTest {
        val useCase = makeUseCase(azimuth = 180f, declination = 0f)
        useCase().test {
            val direction = awaitItem()
            assertFalse("Should not be pointing at London", direction.isPointingAtLondon)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
