package com.brujuladelezo.data.repository

import android.hardware.GeomagneticField
import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.repository.GeomagneticRepository

class GeomagneticRepositoryImpl : GeomagneticRepository {
    override fun declinationDegrees(point: GeoPoint): Float {
        val field = GeomagneticField(
            point.latitude.toFloat(),
            point.longitude.toFloat(),
            0f,
            System.currentTimeMillis(),
        )
        return field.declination
    }
}
