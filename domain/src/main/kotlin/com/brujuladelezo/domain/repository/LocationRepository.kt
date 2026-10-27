package com.brujuladelezo.domain.repository

import com.brujuladelezo.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocation(): Flow<GeoPoint>

    /** Si la ubicación del sistema está activada. Emite el valor actual y cada cambio. */
    fun observeLocationEnabled(): Flow<Boolean>
}
