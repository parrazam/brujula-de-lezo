package com.brujuladelezo

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brujuladelezo.designsystem.theme.BrujulaDeLezoTheme
import com.brujuladelezo.domain.model.ThemePreference
import com.brujuladelezo.presentation.CompassScreen
import com.brujuladelezo.presentation.SettingsViewModel

class MainActivity : ComponentActivity() {

    // El bloqueo está gateado por sw600dp, que es justo lo que la regla de lint persigue: desde
    // API 36 Android ignora la restricción de orientación en pantallas de 600dp o más.
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (resources.getBoolean(R.bool.lock_portrait_orientation)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val container = (application as BrujulaApplication).container
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = container.settingsViewModelFactory)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            // Hasta la primera lectura de ajustes se ve el windowBackground: mejor eso que un
            // fotograma en el tema equivocado.
            val loaded = settings ?: return@setContent

            val darkTheme = when (loaded.theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            // El enableEdgeToEdge() de arriba decide el color de los iconos de las barras por el
            // modo del sistema; con el tema forzado desde Ajustes hay que decírselo.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightNavigationScrim, DarkNavigationScrim) { darkTheme },
                )
                onDispose {}
            }

            BrujulaDeLezoTheme(darkTheme = darkTheme) {
                CompassScreen(
                    viewModel = viewModel(factory = container.compassViewModelFactory),
                    settingsViewModel = settingsViewModel,
                    appVersion = appVersion,
                )
            }
        }
    }

    private companion object {
        // Los mismos que usa enableEdgeToEdge() por defecto para la navegación de tres botones.
        val LightNavigationScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkNavigationScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
