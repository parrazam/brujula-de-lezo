package com.brujuladelezo.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.GeoPoint
import com.brujuladelezo.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

private const val MIN_TIME_MS = 10_000L
private const val MIN_DISTANCE_M = 500f

class LocationRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<GeoPoint> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = LocationListener { location -> trySend(location.toGeoPoint()) }

        // Emit last known location immediately if available
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        providers.firstNotNullOfOrNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }?.let { trySend(it.toGeoPoint()) }

        val activeProviders = providers.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrElse { false }
        }

        activeProviders.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, listener)
            }
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.flowOn(dispatchers.io)

    private fun Location.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)
}
