package com.brujuladelezo.domain.repository

import com.brujuladelezo.domain.model.GeoPoint

interface GeomagneticRepository {
    fun declinationDegrees(point: GeoPoint): Float
}
