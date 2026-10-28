package com.brujuladelezo.presentation

import app.cash.turbine.test
import com.brujuladelezo.domain.model.AppSettings
import com.brujuladelezo.domain.model.DistanceUnit
import com.brujuladelezo.domain.model.ThemePreference
import com.brujuladelezo.domain.repository.SettingsRepository
import com.brujuladelezo.domain.usecase.ObserveSettingsUseCase
import com.brujuladelezo.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val stored = MutableStateFlow(AppSettings())
    private val repository = object : SettingsRepository {
        override fun observe() = stored
        override suspend fun update(transform: (AppSettings) -> AppSettings) = stored.update(transform)
    }

    private fun buildViewModel() =
        SettingsViewModel(ObserveSettingsUseCase(repository), UpdateSettingsUseCase(repository))

    @Test
    fun `cada setter cambia solo su ajuste`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setTheme(ThemePreference.DARK)
        vm.setDistanceUnit(DistanceUnit.NAUTICAL_MILES)
        vm.setVibrationEnabled(false)

        val settings = stored.value
        assertEquals(ThemePreference.DARK, settings.theme)
        assertEquals(DistanceUnit.NAUTICAL_MILES, settings.distanceUnit)
        assertFalse(settings.vibrationEnabled)
    }

    @Test
    fun `expone lo guardado y sus cambios`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.settings.test {
            assertEquals(AppSettings(), awaitItem())
            stored.value = AppSettings(theme = ThemePreference.LIGHT)
            assertEquals(ThemePreference.LIGHT, awaitItem()?.theme)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
