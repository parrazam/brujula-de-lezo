package com.brujuladelezo.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.AppSettings
import com.brujuladelezo.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Ajustes de la app en `SharedPreferences`, del propio framework: tres valores no justifican
 * DataStore, y la app sigue sin dependencias de persistencia. Aquí solo se guardan preferencias
 * de la interfaz; la ubicación nunca se escribe en disco.
 */
class SettingsRepositoryImpl(
    context: Context,
    private val dispatchers: DispatcherProvider,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun observe(): Flow<AppSettings> = callbackFlow {
        // SharedPreferences guarda el listener con una referencia débil: se mantiene aquí.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().flowOn(dispatchers.io)

    override suspend fun update(transform: (AppSettings) -> AppSettings) = withContext(dispatchers.io) {
        val updated = transform(read())
        prefs.edit {
            putString(KEY_THEME, updated.theme.name)
            putBoolean(KEY_VIBRATION, updated.vibrationEnabled)
            putString(KEY_DISTANCE_UNIT, updated.distanceUnit.name)
        }
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = AppSettings.parse(prefs.getString(KEY_THEME, null), defaults.theme),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, defaults.vibrationEnabled),
            distanceUnit = AppSettings.parse(prefs.getString(KEY_DISTANCE_UNIT, null), defaults.distanceUnit),
        )
    }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_THEME = "theme"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_DISTANCE_UNIT = "distance_unit"
    }
}
