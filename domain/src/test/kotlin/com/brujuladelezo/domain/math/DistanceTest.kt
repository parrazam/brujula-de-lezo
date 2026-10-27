package com.brujuladelezo.domain.math

import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.model.Landmarks
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {

    @Test
    fun `Madrid a Londres son unos 1264 km`() {
        val km = BearingCalculator.distanceKm(GeoPoint(40.4168, -3.7038), Landmarks.LONDON)
        assertEquals(1264.0, km, 5.0)
    }

    @Test
    fun `Cartagena de Indias a Londres son unos 8090 km`() {
        // El otro extremo de la historia de Blas de Lezo.
        val km = BearingCalculator.distanceKm(GeoPoint(10.3910, -75.4794), Landmarks.LONDON)
        // Contrastado con la ley esférica de cosenos (8093 km).
        assertEquals(8090.0, km, 10.0)
    }

    @Test
    fun `en Londres la distancia es cero`() {
        assertEquals(0.0, BearingCalculator.distanceKm(Landmarks.LONDON, Landmarks.LONDON), 1e-9)
    }

    @Test
    fun `en las antipodas no revienta por redondeo`() {
        val antipode = GeoPoint(-51.5074, 179.8722)
        assertEquals(Math.PI * 6371.0088, BearingCalculator.distanceKm(Landmarks.LONDON, antipode), 1.0)
    }
}
