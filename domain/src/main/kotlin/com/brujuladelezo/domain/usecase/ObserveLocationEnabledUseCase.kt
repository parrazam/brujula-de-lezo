package com.brujuladelezo.domain.usecase

import com.brujuladelezo.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveLocationEnabledUseCase(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<Boolean> = locationRepository.observeLocationEnabled().distinctUntilChanged()
}
