package com.brujuladelezo.domain.repository

import com.brujuladelezo.domain.model.RawOrientation
import kotlinx.coroutines.flow.Flow

interface OrientationRepository {
    fun observeOrientation(): Flow<RawOrientation>
}
