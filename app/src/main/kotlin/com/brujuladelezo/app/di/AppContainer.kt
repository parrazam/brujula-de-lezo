package com.brujuladelezo.app.di

import android.content.Context
import android.hardware.SensorManager
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.brujuladelezo.core.DefaultDispatcherProvider
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.data.repository.GeomagneticRepositoryImpl
import com.brujuladelezo.data.repository.LocationRepositoryImpl
import com.brujuladelezo.data.repository.OrientationRepositoryImpl
import com.brujuladelezo.domain.usecase.ObserveLondonDirectionUseCase
import com.brujuladelezo.presentation.CompassViewModel

class AppContainer(context: Context) {

    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val hasCompassSensor: Boolean =
        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR) != null ||
            sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) != null

    private val locationRepository = LocationRepositoryImpl(context, dispatchers)
    private val orientationRepository = OrientationRepositoryImpl(sensorManager, windowManager, dispatchers)
    private val geomagneticRepository = GeomagneticRepositoryImpl()

    private val observeLondonDirection = ObserveLondonDirectionUseCase(
        locationRepository = locationRepository,
        orientationRepository = orientationRepository,
        geomagneticRepository = geomagneticRepository,
    )

    val compassViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CompassViewModel(
                observeLondonDirection = observeLondonDirection,
                dispatchers = dispatchers,
                hasCompassSensor = hasCompassSensor,
            ) as T
    }
}
