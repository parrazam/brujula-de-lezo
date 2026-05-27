package com.brujuladelezo.domain.math

import com.brujuladelezo.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BearingCalculatorTest {

    @Test
    fun `Madrid to London bearing is approximately 11 degrees NNE`() {
        val madrid = GeoPoint(40.4168, -3.7038)
        val london = GeoPoint(51.5074, -0.1278)
        val bearing = BearingCalculator.initialBearing(madrid, london)
        // Madrid->London: ~11.36° NNE
        assertTrue("Bearing should be between 8 and 15 degrees, was $bearing", bearing in 8.0..15.0)
    }

    @Test
    fun `normalizeDegrees wraps negative values`() {
        assertEquals(270f, BearingCalculator.normalizeDegrees(-90.0), 0.01f)
    }

    @Test
    fun `normalizeDegrees wraps values over 360`() {
        assertEquals(10f, BearingCalculator.normalizeDegrees(370.0), 0.01f)
    }

    @Test
    fun `relativeAngle returns shortest path clockwise`() {
        assertEquals(10f, BearingCalculator.relativeAngle(10f, 0f), 0.01f)
    }

    @Test
    fun `relativeAngle wraps around 360`() {
        // From 355 to 5 should be +10 (clockwise), not -350
        assertEquals(10f, BearingCalculator.relativeAngle(5f, 355f), 0.01f)
    }

    @Test
    fun `relativeAngle returns negative for counter-clockwise shorter path`() {
        assertEquals(-10f, BearingCalculator.relativeAngle(350f, 0f), 0.01f)
    }
}
