package com.brujuladelezo.domain.repository

import com.brujuladelezo.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocation(): Flow<GeoPoint>
}
