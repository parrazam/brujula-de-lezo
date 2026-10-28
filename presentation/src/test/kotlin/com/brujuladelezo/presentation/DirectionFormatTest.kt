package com.brujuladelezo.presentation

import com.brujuladelezo.domain.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DirectionFormatTest {

    private val es = Locale.forLanguageTag("es-ES")

    @Test
    fun `distancia en km sin decimales a partir de 10`() {
        assertEquals("1264", formatDistance(1264.4, DistanceUnit.KILOMETERS, es).filter(Char::isDigit))
        assertEquals("34", formatDistance(33.6, DistanceUnit.KILOMETERS, es))
    }

    @Test
    fun `por debajo de 10 un decimal con la coma del idioma`() {
        assertEquals("3,4", formatDistance(3.44, DistanceUnit.KILOMETERS, es))
        assertEquals("3.4", formatDistance(3.44, DistanceUnit.KILOMETERS, Locale.UK))
    }

    @Test
    fun `en millas nauticas convierte antes de redondear`() {
        // 1264 / 1,852 = 682,51
        assertEquals("683", formatDistance(1264.0, DistanceUnit.NAUTICAL_MILES, es))
    }

    @Test
    fun `el giro es entero y sin signo`() {
        assertEquals(34, turnMagnitude(-34.4f))
        assertEquals(35, turnMagnitude(34.5f))
    }

    @Test
    fun `el rumbo redondeado nunca es 360`() {
        assertEquals(0, roundedBearing(359.6f))
        assertEquals(11, roundedBearing(11.36f))
    }
}
