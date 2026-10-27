package com.brujuladelezo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CardinalDirectionTest {

    @Test
    fun `los cuatro puntos cardinales`() {
        assertEquals(CardinalDirection.N, CardinalDirection.fromBearing(0.0))
        assertEquals(CardinalDirection.E, CardinalDirection.fromBearing(90.0))
        assertEquals(CardinalDirection.S, CardinalDirection.fromBearing(180.0))
        assertEquals(CardinalDirection.O, CardinalDirection.fromBearing(270.0))
    }

    @Test
    fun `Madrid a Londres es NNE`() {
        assertEquals(CardinalDirection.NNE, CardinalDirection.fromBearing(11.36))
    }

    @Test
    fun `los limites caen en el sector siguiente`() {
        assertEquals(CardinalDirection.N, CardinalDirection.fromBearing(11.24))
        assertEquals(CardinalDirection.NNE, CardinalDirection.fromBearing(11.25))
        assertEquals(CardinalDirection.N, CardinalDirection.fromBearing(348.75))
        assertEquals(CardinalDirection.NNO, CardinalDirection.fromBearing(348.74))
    }

    @Test
    fun `normaliza valores fuera de rango`() {
        assertEquals(CardinalDirection.O, CardinalDirection.fromBearing(-90.0))
        assertEquals(CardinalDirection.E, CardinalDirection.fromBearing(450.0))
    }
}
