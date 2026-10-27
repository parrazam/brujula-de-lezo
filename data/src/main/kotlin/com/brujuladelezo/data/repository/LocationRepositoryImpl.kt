package com.brujuladelezo.data.repository

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Build
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

/**
 * Proveedores compatibles con `ACCESS_COARSE_LOCATION`, de más a menos útil. `GPS_PROVIDER` exige
 * ubicación precisa. El fusionado es el de AOSP (API 31+), no el de Play Services: cubre los
 * móviles sin `NETWORK_PROVIDER`, donde antes la app se quedaba esperando una posición que nunca
 * llegaba. Las constantes son `static final String`, así que esto se puede testear en la JVM.
 */
@SuppressLint("InlinedApi") // FUSED_PROVIDER solo se añade con sdkInt >= S; lint no sigue el parámetro
internal fun locationProviders(sdkInt: Int): List<String> = buildList {
    if (sdkInt >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
    add(LocationManager.NETWORK_PROVIDER)
    add(LocationManager.PASSIVE_PROVIDER)
}

class LocationRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : LocationRepository {

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<GeoPoint> = callbackFlow {
        val locationManager = locationManager
        val listener = LocationListenerCompat { location -> trySend(location.toGeoPoint()) }
        val providers = locationProviders(Build.VERSION.SDK_INT)

        // La más reciente de todas, no la del primer proveedor que tenga alguna: la del pasivo
        // puede tener días mientras el fusionado tiene una de hace minutos.
        providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { trySend(it.toGeoPoint()) }

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

    override fun observeLocationEnabled(): Flow<Boolean> = callbackFlow {
        val locationManager = locationManager
        fun current() = LocationManagerCompat.isLocationEnabled(locationManager)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current())
            }
        }
        val filter = IntentFilter().apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        // Son broadcasts protegidos del sistema: llegan aunque el receptor no esté exportado.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(current())

        awaitClose { context.unregisterReceiver(receiver) }
    }.flowOn(dispatchers.io)

    private fun Location.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)
}
