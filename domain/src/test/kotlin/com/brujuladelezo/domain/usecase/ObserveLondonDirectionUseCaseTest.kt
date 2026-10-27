package com.brujuladelezo.domain.usecase

import app.cash.turbine.test
import com.brujuladelezo.domain.math.BearingCalculator
import com.brujuladelezo.domain.model.CardinalDirection
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.model.Landmarks
import com.brujuladelezo.domain.model.RawOrientation
import com.brujuladelezo.domain.repository.GeomagneticRepository
import com.brujuladelezo.domain.repository.LocationRepository
import com.brujuladelezo.domain.repository.OrientationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        return makeUseCase(location, listOf(azimuth), declination)
    }

    private fun makeUseCase(
        location: GeoPoint,
        azimuths: List<Float>,
        declination: Float,
    ): ObserveLondonDirectionUseCase {
        val locationRepo = object : LocationRepository {
            override fun observeLocation() = flowOf(location)
            override fun observeLocationEnabled() = flowOf(true)
        }
        val orientationRepo = object : OrientationRepository {
            // Espaciadas en tiempo virtual: combine puede saltarse valores intermedios si llegan
            // varios antes de que procese el primero.
            override fun observeOrientation() = flow {
                azimuths.forEach { delay(10); emit(RawOrientation(it, CompassAccuracy.ALTA)) }
            }
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

    @Test
    fun `el ruido alrededor de 5 grados no hace entrar y salir del blanco`() = runTest {
        val bearing = BearingCalculator.initialBearing(madrid, Landmarks.LONDON).toFloat()
        // Desviaciones: 5,5° (fuera), 1° (entra), 6,5° y 7,5° (siguen dentro), 9° (sale).
        val azimuths = listOf(-5.5f, -1f, -6.5f, 7.5f, 9f).map { bearing + it }
        val useCase = makeUseCase(madrid, azimuths, declination = 0f)
        val pointing = useCase().toList().map { it.isPointingAtLondon }
        assertEquals(listOf(false, true, true, true, false), pointing)
    }

    @Test
    fun `emite rumbo, distancia y cuanto girar`() = runTest {
        // Mirando al este (90°) desde Madrid: Londres queda ~79° a la izquierda.
        val direction = makeUseCase(azimuth = 90f, declination = 0f)().toList().single()
        assertEquals(11.36f, direction.bearingDegrees, 0.1f)
        assertEquals(CardinalDirection.NNE, direction.cardinal)
        assertEquals(1264.0, direction.distanceKm, 5.0)
        assertEquals(-78.64f, direction.turnDegrees, 0.1f)
    }

    @Test
    fun `la declinacion se aplica antes de calcular el giro`() = runTest {
        // Sensor magnético a 0° con declinación +11,36°: el norte verdadero ya mira a Londres.
        val bearing = BearingCalculator.initialBearing(madrid, Landmarks.LONDON).toFloat()
        val direction = makeUseCase(azimuth = 0f, declination = bearing)().toList().single()
        assertEquals(0f, direction.turnDegrees, 0.01f)
        assertTrue(direction.isPointingAtLondon)
    }
}
