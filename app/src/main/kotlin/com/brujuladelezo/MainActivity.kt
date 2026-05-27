package com.brujuladelezo

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brujuladelezo.designsystem.theme.BrujulaDeLezoTheme
import com.brujuladelezo.presentation.CompassScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
