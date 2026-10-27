package com.brujuladelezo.domain.repository

import com.brujuladelezo.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emite los ajustes actuales y cada cambio. */
    fun observe(): Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
