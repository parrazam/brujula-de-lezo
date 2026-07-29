package com.brujuladelezo.data.repository

import android.hardware.SensorManager
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hasta targetSdk 36 la app estaba bloqueada en retrato, así que las ramas de rotación nunca
 * llegaban a ejecutarse. Ahora que Android 16 ignora esa restricción en pantallas >= 600dp, sí
 * se ejecutan: un mapeo incorrecto se traduce en 90º de error en la aguja.
 */
class AxisRemapTest {

    @Test
    fun `rotacion 0 deja los ejes del sensor sin remapear`() {
        assertEquals(
            SensorManager.AXIS_X to SensorManager.AXIS_Y,
            axesForRotation(Surface.ROTATION_0),
        )
    }

    @Test
    fun `rotacion 90 gira los ejes un cuarto en sentido antihorario`() {
        assertEquals(
            SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X,
            axesForRotation(Surface.ROTATION_90),
        )
    }

    @Test
    fun `rotacion 180 invierte ambos ejes`() {
        assertEquals(
            SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y,
            axesForRotation(Surface.ROTATION_180),
        )
    }

    @Test
    fun `rotacion 270 gira los ejes un cuarto en sentido horario`() {
        assertEquals(
            SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X,
            axesForRotation(Surface.ROTATION_270),
        )
    }

    @Test
    fun `un valor de rotacion desconocido cae en el mapeo de retrato`() {
        assertEquals(
            SensorManager.AXIS_X to SensorManager.AXIS_Y,
            axesForRotation(rotation = 42),
        )
    }
}
