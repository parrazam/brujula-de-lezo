package com.brujuladelezo.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "LocationRepository"
private const val MIN_TIME_MS = 10_000L
private const val MIN_DISTANCE_M = 500f

class LocationRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<GeoPoint> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = LocationListenerCompat { location -> trySend(location.toGeoPoint()) }

        // Emit last known location immediately if available
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        providers.firstNotNullOfOrNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }?.let { trySend(it.toGeoPoint()) }

        val activeProviders = providers.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrElse { false }
        }

        // Con el executor explícito: la sobrecarga sin él exige que el hilo llamante tenga
        // Looper, y este flow corre en dispatchers.io, así que reventaba en silencio dentro del
        // runCatching y las actualizaciones no llegaban a registrarse nunca.
        val request = LocationRequestCompat.Builder(MIN_TIME_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .build()
        val executor = ContextCompat.getMainExecutor(context)

        activeProviders.forEach { provider ->
            runCatching {
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    provider,
                    request,
                    executor,
                    listener,
                )
            }.onFailure { Log.w(TAG, "No se pudo registrar el proveedor $provider", it) }
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.flowOn(dispatchers.io)

    private fun Location.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)
}
