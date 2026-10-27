package com.brujuladelezo.domain.usecase

import com.brujuladelezo.domain.model.AppSettings
import com.brujuladelezo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.observe()
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = repository.update(transform)
}
