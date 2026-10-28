package com.brujuladelezo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brujuladelezo.domain.model.AppSettings
import com.brujuladelezo.domain.model.DistanceUnit
import com.brujuladelezo.domain.model.ThemePreference
import com.brujuladelezo.domain.usecase.ObserveSettingsUseCase
import com.brujuladelezo.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    /**
     * `null` hasta la primera lectura: MainActivity no pinta nada mientras tanto, para no
     * mostrar un fotograma en claro a quien eligió el tema oscuro. `Eagerly` porque el tema hace
     * falta desde el primer fotograma y leer tres preferencias no cuesta nada.
     */
    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setTheme(theme: ThemePreference) = update { it.copy(theme = theme) }

    fun setVibrationEnabled(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }

    fun setDistanceUnit(unit: DistanceUnit) = update { it.copy(distanceUnit = unit) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }
}
