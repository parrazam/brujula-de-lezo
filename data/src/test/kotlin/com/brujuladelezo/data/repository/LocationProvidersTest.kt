package com.brujuladelezo.data.repository

import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationProvidersTest {

    @Test
    fun `en API 31 o superior el fusionado va primero`() {
        assertEquals(
            listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            locationProviders(sdkInt = 31),
        )
    }

    @Test
    fun `por debajo de API 31 no hay fusionado`() {
        assertEquals(
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            locationProviders(sdkInt = 30),
        )
    }

    @Test
    fun `nunca se pide GPS, que exige ubicacion precisa`() {
        assertEquals(false, LocationManager.GPS_PROVIDER in locationProviders(sdkInt = 36))
    }
}
