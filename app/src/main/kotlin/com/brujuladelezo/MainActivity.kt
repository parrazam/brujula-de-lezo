package com.brujuladelezo

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brujuladelezo.designsystem.theme.BrujulaDeLezoTheme
import com.brujuladelezo.presentation.CompassScreen

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

        val factory = (application as BrujulaApplication).container.compassViewModelFactory

        setContent {
            BrujulaDeLezoTheme {
                CompassScreen(
                    viewModel = viewModel(factory = factory),
                )
            }
        }
    }
}
