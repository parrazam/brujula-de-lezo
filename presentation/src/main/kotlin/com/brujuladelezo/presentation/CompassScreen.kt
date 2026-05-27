package com.brujuladelezo.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brujuladelezo.designsystem.components.CompassRose
import com.brujuladelezo.designsystem.components.LondonNeedle
import com.brujuladelezo.domain.model.CompassAccuracy

@Composable
fun CompassScreen(viewModel: CompassViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        if (!granted) permissionDeniedPermanently = true
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Vibrate when pointing at London
    LaunchedEffect(uiState.isPointingAtLondon) {
        if (uiState.isPointingAtLondon) {
            vibrateDevice(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !uiState.hasCompassSensor -> NoSensorMessage()
            !uiState.hasLocationPermission -> PermissionMessage(
                permanentlyDenied = permissionDeniedPermanently,
                onRequest = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                onOpenSettings = { context.openAppSettings() },
            )
            uiState.isLoading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
            else -> CompassContent(uiState = uiState)
        }
    }
}

@Composable
private fun CompassContent(uiState: CompassUiState) {
    var accumulatedRotation by remember { mutableFloatStateOf(uiState.arrowRotation) }

    LaunchedEffect(uiState.arrowRotation) {
        val delta = shortestAngleDiff(uiState.arrowRotation, accumulatedRotation % 360f)
        accumulatedRotation += delta
    }

    val animatedRotation by animateFloatAsState(
        targetValue = accumulatedRotation,
        animationSpec = tween(durationMillis = 300),
        label = "needle_rotation",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        // "Pointing at London" message — parte superior
        AnimatedVisibility(visible = uiState.isPointingAtLondon) {
            Text(
                text = "¡Apuntando a la pérfida Albión!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        // Calibration warning
        AnimatedVisibility(
            visible = uiState.accuracy == CompassAccuracy.BAJA || uiState.accuracy == CompassAccuracy.NO_FIABLE
        ) {
            CalibrationBanner()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compass rose + needle overlay
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompassRose(modifier = Modifier.fillMaxSize())
            LondonNeedle(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedRotation),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quote
        Text(
            text = "«Todo buen español deberá mear siempre mirando a Inglaterra»",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "— Blas de Lezo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CalibrationBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "⚠ Brújula sin calibrar — mueve el móvil en forma de 8",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun PermissionMessage(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Text(
            text = "Para apuntar a Londres necesitamos conocer tu ubicación aproximada.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (permanentlyDenied) {
            TextButton(onClick = onOpenSettings) {
                Text("Abrir Ajustes")
            }
        } else {
            Button(onClick = onRequest) {
                Text("Conceder ubicación")
            }
        }
    }
}

@Composable
private fun NoSensorMessage() {
    Text(
        text = "Tu dispositivo no tiene brújula.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(32.dp),
    )
}

private fun shortestAngleDiff(target: Float, current: Float): Float {
    var diff = (target - current) % 360f
    if (diff > 180f) diff -= 360f
    if (diff <= -180f) diff += 360f
    return diff
}

private fun vibrateDevice(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        vibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
