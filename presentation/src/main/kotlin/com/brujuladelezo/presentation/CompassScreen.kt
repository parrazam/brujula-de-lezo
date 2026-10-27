package com.brujuladelezo.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brujuladelezo.designsystem.components.CompassRose
import com.brujuladelezo.designsystem.components.LondonNeedle
import com.brujuladelezo.designsystem.theme.BrujulaDeLezoTheme
import com.brujuladelezo.domain.model.CompassAccuracy

private val ContentPadding = 24.dp
private val WideGap = 32.dp

private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION

@Composable
fun CompassScreen(viewModel: CompassViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Guardados para que girar la pantalla no vuelva a lanzar el diálogo ni olvide la negativa.
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var permissionDeniedPermanently by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        // Tras una negativa, el sistema deja de ofrecer la explicación cuando ya no va a volver a
        // mostrar el diálogo. Antes se daba por definitiva la primera negativa.
        permissionDeniedPermanently =
            !granted && activity?.shouldShowRequestPermissionRationale(LOCATION_PERMISSION) == false
    }

    // En cada vuelta a primer plano: si el permiso se concedió desde Ajustes, se nota aquí.
    LifecycleResumeEffect(Unit) {
        if (context.hasLocationPermission()) {
            permissionDeniedPermanently = false
            viewModel.onPermissionResult(true)
        } else if (!askedOnce) {
            askedOnce = true
            permissionLauncher.launch(LOCATION_PERMISSION)
        }
        onPauseOrDispose { }
    }

    // Vibrate when pointing at London
    LaunchedEffect(uiState.isPointingAtLondon) {
        if (uiState.isPointingAtLondon) {
            vibrateDevice(context)
        }
    }

    // El fondo sí pinta bajo las barras del sistema (edge-to-edge es obligatorio desde
    // targetSdk 36); el contenido se queda dentro del área segura.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            when {
                !uiState.hasCompassSensor -> NoSensorMessage()
                !uiState.hasLocationPermission -> PermissionMessage(
                    permanentlyDenied = permissionDeniedPermanently,
                    onRequest = { permissionLauncher.launch(LOCATION_PERMISSION) },
                    onOpenSettings = { context.openAppSettings() },
                )
                uiState.showLocationDisabled -> StatusMessage(
                    title = stringResource(R.string.location_disabled_title),
                    body = stringResource(R.string.location_disabled_body),
                    action = stringResource(R.string.location_disabled_action),
                    onAction = { context.openLocationSettings() },
                )
                uiState.trackingFailed -> StatusMessage(
                    title = stringResource(R.string.tracking_failed_title),
                    body = stringResource(R.string.tracking_failed_body),
                    action = stringResource(R.string.tracking_failed_action),
                    onAction = viewModel::retry,
                )
                uiState.isLoading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.compass_locating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                else -> CompassContent(uiState = uiState)
            }
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

    val needsCalibration = uiState.accuracy == CompassAccuracy.BAJA ||
        uiState.accuracy == CompassAccuracy.NO_FIABLE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Desde targetSdk 36 la app puede acabar en apaisado o en multiventana en cualquier
        // pantalla de 600dp o más. Se exige además que la ventana sea apaisada: en una tablet
        // en vertical las dos columnas quedarían estrechas y con media pantalla vacía.
        val isWide = maxWidth >= 600.dp && maxWidth > maxHeight
        val scrollState = rememberScrollState()

        if (isWide) {
            // Mitad del ancho útil (sin el padding ni el hueco entre columnas), o el alto útil.
            val dialSize = minOf((maxWidth - ContentPadding * 2 - WideGap) / 2, maxHeight - ContentPadding * 2)
                .times(0.9f)
                .coerceIn(200.dp, 420.dp)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContentPadding),
                horizontalArrangement = Arrangement.spacedBy(WideGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompassDial(rotation = animatedRotation, size = dialSize)
                Column(
                    // Sin acotar, en una tablet de 1280dp el texto quedaría perdido en medio
                    // de un vacío enorme y la rosa pegada al borde.
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 520.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PointingMessage(visible = uiState.isPointingAtLondon)
                    CalibrationBanner(visible = needsCalibration)
                    Spacer(modifier = Modifier.height(16.dp))
                    Quote()
                }
            }
        } else {
            // Del alto se reserva hueco para el mensaje de apuntado, el banner, la cita y la firma.
            // En tablet en vertical se deja crecer más: con el tope de móvil la rosa quedaba
            // diminuta en medio de la pantalla.
            val maxDial = if (maxWidth >= 600.dp) 420.dp else 320.dp
            val dialSize = minOf(maxWidth - ContentPadding * 2, maxHeight - 240.dp)
                .coerceIn(160.dp, maxDial)
            // Red de seguridad para apaisado de móvil y tamaños de fuente muy grandes.
            val needsScroll = maxHeight < 520.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (needsScroll) Arrangement.Top else Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (needsScroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(ContentPadding),
            ) {
                PointingMessage(visible = uiState.isPointingAtLondon)
                CalibrationBanner(visible = needsCalibration)

                Spacer(modifier = Modifier.height(16.dp))
                CompassDial(rotation = animatedRotation, size = dialSize)
                Spacer(modifier = Modifier.height(24.dp))

                Quote()
            }
        }
    }
}

/** Rosa de los vientos con la aguja superpuesta. */
@Composable
private fun CompassDial(rotation: Float, size: Dp) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        CompassRose(modifier = Modifier.fillMaxSize())
        LondonNeedle(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation),
        )
    }
}

@Composable
private fun PointingMessage(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        Text(
            text = stringResource(R.string.compass_pointing),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Quote() {
    Text(
        text = stringResource(R.string.compass_quote),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.compass_quote_author),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CalibrationBanner(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        CalibrationCard()
    }
}

@Composable
private fun CalibrationCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.compass_calibration),
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
    if (permanentlyDenied) {
        StatusMessage(
            title = null,
            body = stringResource(R.string.permission_denied_forever),
            action = stringResource(R.string.permission_open_settings),
            onAction = onOpenSettings,
        )
    } else {
        StatusMessage(
            title = null,
            body = stringResource(R.string.permission_rationale),
            action = stringResource(R.string.permission_grant),
            onAction = onRequest,
        )
    }
}

/** Mensaje a pantalla completa con una única acción para salir del atasco. */
@Composable
private fun StatusMessage(
    title: String?,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 480.dp)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAction) {
            Text(action)
        }
    }
}

@Composable
private fun NoSensorMessage() {
    Text(
        text = stringResource(R.string.no_compass_sensor),
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

@SuppressLint("MissingPermission") // VIBRATE declarado en :app/AndroidManifest.xml
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

// --- Previews: los tamaños cubren los casos que Android 16 puede imponer a la app ---

private val previewState = CompassUiState(
    arrowRotation = 42f,
    isPointingAtLondon = true,
    accuracy = CompassAccuracy.BAJA,
    hasLocationPermission = true,
)

@Preview(name = "Móvil vertical", widthDp = 360, heightDp = 800)
@Preview(name = "Móvil apaisado", widthDp = 800, heightDp = 360)
@Preview(name = "Tablet apaisada", widthDp = 1280, heightDp = 800)
@Preview(name = "Tablet vertical", widthDp = 800, heightDp = 1280)
@Preview(name = "Fuente x2", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun CompassContentPreview() {
    BrujulaDeLezoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CompassContent(uiState = previewState)
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
