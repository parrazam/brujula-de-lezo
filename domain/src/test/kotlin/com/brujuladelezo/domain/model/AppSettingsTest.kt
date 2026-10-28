package com.brujuladelezo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `una milla nautica son 1852 metros`() {
        assertEquals(682.5, DistanceUnit.NAUTICAL_MILES.fromKm(1264.0), 0.1)
        assertEquals(1264.0, DistanceUnit.KILOMETERS.fromKm(1264.0), 0.0)
    }

    @Test
    fun `un valor guardado desconocido cae al defecto`() {
        assertEquals(ThemePreference.SYSTEM, AppSettings.parse("SEPIA", ThemePreference.SYSTEM))
        assertEquals(ThemePreference.SYSTEM, AppSettings.parse(null, ThemePreference.SYSTEM))
        assertEquals(ThemePreference.DARK, AppSettings.parse("DARK", ThemePreference.SYSTEM))
    }
}
