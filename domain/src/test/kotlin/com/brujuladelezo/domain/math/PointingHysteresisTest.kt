package com.brujuladelezo.domain.math

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointingHysteresisTest {

    @Test
    fun `fuera del blanco se entra por debajo de 5 grados`() {
        assertTrue(PointingHysteresis.isPointing(4.9f, wasPointing = false))
        assertFalse(PointingHysteresis.isPointing(5f, wasPointing = false))
    }

    @Test
    fun `dentro del blanco se sigue apuntando hasta 8 grados`() {
        assertTrue(PointingHysteresis.isPointing(6.5f, wasPointing = true))
        assertTrue(PointingHysteresis.isPointing(8f, wasPointing = true))
    }

    @Test
    fun `dentro del blanco se sale al pasar de 8 grados`() {
        assertFalse(PointingHysteresis.isPointing(8.1f, wasPointing = true))
    }

    @Test
    fun `entre los dos umbrales manda el estado anterior`() {
        assertFalse(PointingHysteresis.isPointing(6.5f, wasPointing = false))
        assertTrue(PointingHysteresis.isPointing(6.5f, wasPointing = true))
    }
}
