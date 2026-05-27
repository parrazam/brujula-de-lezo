package com.brujuladelezo.data.repository

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.RawOrientation
import com.brujuladelezo.domain.repository.OrientationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

private const val LOW_PASS_ALPHA = 0.12f

class OrientationRepositoryImpl(
    private val sensorManager: SensorManager,
    private val windowManager: WindowManager,
    private val dispatchers: DispatcherProvider,
) : OrientationRepository {

    override fun observeOrientation(): Flow<RawOrientation> = callbackFlow {
        var currentAccuracy = CompassAccuracy.BAJA
        var smoothedAzimuth = 0f
        var isFirst = true

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                currentAccuracy = accuracy.toCompassAccuracy()
            }

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val (axisX, axisY) = when (windowManager.defaultDisplay.rotation) {
                    Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
                    Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
                    Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
                    else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y) // ROTATION_0
                }

                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                SensorManager.getOrientation(remappedMatrix, orientation)

                val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val normalized = ((rawAzimuth % 360f) + 360f) % 360f

                // Low-pass filter — handles wrap-around via sin/cos averaging
                if (isFirst) {
                    smoothedAzimuth = normalized
                    isFirst = false
                } else {
                    val deltaAngle = shortestAngleDiff(normalized, smoothedAzimuth)
                    smoothedAzimuth = ((smoothedAzimuth + LOW_PASS_ALPHA * deltaAngle) + 360f) % 360f
                }

                trySend(RawOrientation(smoothedAzimuth, currentAccuracy))
            }
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.flowOn(dispatchers.default)

    private fun shortestAngleDiff(target: Float, current: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff <= -180f) diff += 360f
        return diff
    }

    private fun Int.toCompassAccuracy(): CompassAccuracy = when (this) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.ALTA
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIA
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.BAJA
        else -> CompassAccuracy.NO_FIABLE
    }
}
