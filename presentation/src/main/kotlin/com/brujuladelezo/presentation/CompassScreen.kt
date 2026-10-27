package com.brujuladelezo.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brujuladelezo.designsystem.components.CompassDial
import com.brujuladelezo.designsystem.components.HeroCard
import com.brujuladelezo.designsystem.components.StatusPill
import com.brujuladelezo.designsystem.icons.BrujulaIcons
import com.brujuladelezo.designsystem.theme.BrujulaDeLezoTheme
import com.brujuladelezo.designsystem.theme.BrujulaTheme
import com.brujuladelezo.designsystem.theme.QuoteTextStyle
import com.brujuladelezo.domain.math.BearingCalculator
import com.brujuladelezo.domain.model.AppSettings
import com.brujuladelezo.domain.model.CardinalDirection
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.DistanceUnit
import kotlin.math.abs

private val ContentPadding = 24.dp
private val WideGap = 32.dp

/** compass_face.png es 1024×1024: hasta ~420dp no se reescala. */
private val MaxDialSize = 420.dp
private val MinDialSize = 200.dp
private val MaxCardWidth = 520.dp
private val TallGap = 12.dp

private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    settingsViewModel: SettingsViewModel,
    appVersion: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val currentSettings = settings ?: AppSettings()
    var showSettings by rememberSaveable { mutableStateOf(false) }
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
        if (uiState.isPointingAtLondon && currentSettings.vibrationEnabled) {
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
                else -> CompassContent(
                    uiState = uiState,
                    distanceUnit = currentSettings.distanceUnit,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }

    val privacyUrl = stringResource(R.string.settings_privacy_url)
    if (showSettings) {
        SettingsSheet(
            settings = currentSettings,
            appVersion = appVersion,
            onThemeChange = settingsViewModel::setTheme,
            onDistanceUnitChange = settingsViewModel::setDistanceUnit,
            onVibrationChange = settingsViewModel::setVibrationEnabled,
            onOpenPrivacy = { context.openUrl(privacyUrl) },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
internal fun CompassContent(
    uiState: CompassUiState,
    distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    onOpenSettings: () -> Unit = {},
) {
    val needleRotation = rememberNeedleRotation(uiState.arrowRotation)
    val status = uiState.status()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Desde targetSdk 36 la app puede acabar en apaisado o en multiventana en cualquier
        // pantalla de 600dp o más. Se exige además que la ventana sea apaisada: en una tablet
        // en vertical las dos columnas quedarían estrechas y con media pantalla vacía.
        val isWide = maxWidth >= 600.dp && maxWidth > maxHeight
        if (isWide) {
            WideLayout(uiState, needleRotation, status, distanceUnit, onOpenSettings, maxWidth, maxHeight)
        } else {
            TallLayout(uiState, needleRotation, status, distanceUnit, onOpenSettings, viewportHeight = maxHeight)
        }
    }
}

/**
 * Cabecera, hueco de estado, tarjeta y cita miden lo que miden; la rosa se queda con el resto, así
 * que no puede invadir ni la pastilla de "apuntando" ni la tarjeta (era lo que pasaba con una rosa de
 * tamaño fijo y el mensaje apareciendo encima). Si lo que queda no llega a [MinDialSize] (letra
 * muy grande en una pantalla baja), la rosa se queda en ese mínimo, todo se apila y la pantalla
 * se desplaza en vez de encoger la rosa hasta hacerla inútil.
 */
@Composable
private fun TallLayout(
    uiState: CompassUiState,
    needleRotation: Float,
    status: CompassStatus,
    distanceUnit: DistanceUnit,
    onOpenSettings: () -> Unit,
    viewportHeight: Dp,
) {
    val verticalPadding = 16.dp
    Layout(
        content = {
            Header(onOpenSettings)
            StatusSlot(status)
            Dial(uiState, needleRotation, Modifier.fillMaxSize())
            DirectionCard(uiState, distanceUnit)
            Quote()
        },
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ContentPadding, vertical = verticalPadding),
    ) { measurables, constraints ->
        val gap = TallGap.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val header = measurables[0].measure(loose)
        val slot = measurables[1].measure(loose)
        // En tablet en vertical, a todo el ancho la tarjeta quedaba como una franja vacía.
        val card = measurables[3].measure(Constraints(maxWidth = minOf(width, MaxCardWidth.roundToPx())))
        val quote = measurables[4].measure(loose)

        // verticalScroll da alto infinito: el disponible es el de la ventana menos el padding.
        val available = (viewportHeight - verticalPadding * 2).roundToPx()
        val fixed = header.height + slot.height + card.height + quote.height + gap * 4
        val dialSide = minOf(width, available - fixed, MaxDialSize.roundToPx())
            .coerceAtLeast(minOf(width, MinDialSize.roundToPx()))
        val dial = measurables[2].measure(Constraints.fixed(dialSide, dialSide))

        val height = maxOf(available, fixed + dialSide)
        layout(width, height) {
            var y = 0
            header.placeRelative(0, y)
            y += header.height + gap
            slot.placeRelative((width - slot.width) / 2, y)
            y += slot.height + gap
            // La rosa, centrada en el hueco entre el estado y la tarjeta.
            val dialSpace = height - y - card.height - quote.height - gap * 2
            dial.placeRelative((width - dialSide) / 2, y + (dialSpace - dialSide) / 2)
            card.placeRelative((width - card.width) / 2, height - quote.height - gap - card.height)
            quote.placeRelative((width - quote.width) / 2, height - quote.height)
        }
    }
}

@Composable
private fun WideLayout(
    uiState: CompassUiState,
    needleRotation: Float,
    status: CompassStatus,
    distanceUnit: DistanceUnit,
    onOpenSettings: () -> Unit,
    maxWidth: Dp,
    maxHeight: Dp,
) {
    // Mitad del ancho útil (sin el padding ni el hueco entre columnas), o el alto útil.
    val dialSize = minOf((maxWidth - ContentPadding * 2 - WideGap) / 2, maxHeight - ContentPadding * 2)
        .coerceIn(200.dp, MaxDialSize)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(ContentPadding),
        horizontalArrangement = Arrangement.spacedBy(WideGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dial(uiState, needleRotation, Modifier.size(dialSize))
        Column(
            // Sin acotar, en una tablet de 1280dp el texto quedaría perdido en medio
            // de un vacío enorme y la rosa pegada al borde.
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onOpenSettings)
            StatusSlot(status)
            DirectionCard(uiState, distanceUnit)
            Quote()
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.compass_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = BrujulaIcons.Settings,
                contentDescription = stringResource(R.string.settings_open),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/**
 * Distancia, rumbo y cuánto girar. «Gira» desaparece al estar en el blanco: ahí ya no hay nada
 * que girar. La declinación magnética se aplica en el cálculo, pero no se enseña: no ayuda a
 * apuntar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionCard(uiState: CompassUiState, distanceUnit: DistanceUnit) {
    val colors = BrujulaTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    HeroCard(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.card_label).uppercase(locale),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onHeroMuted,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatDistance(uiState.distanceKm, distanceUnit, locale),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (distanceUnit == DistanceUnit.KILOMETERS) R.string.unit_km_short else R.string.unit_nmi_short
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.alignByBaseline(),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = colors.onHero.copy(alpha = 0.18f),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardFact(
                value = stringResource(R.string.card_bearing_value, roundedBearing(uiState.bearingDegrees), uiState.cardinal.abbreviation),
                label = stringResource(R.string.card_bearing_label),
            )
            AnimatedVisibility(visible = !uiState.isPointingAtLondon, enter = fadeIn(), exit = fadeOut()) {
                val magnitude = turnMagnitude(uiState.turnDegrees)
                CardFact(
                    value = stringResource(
                        if (uiState.turnDegrees >= 0f) R.string.card_turn_right else R.string.card_turn_left,
                        magnitude,
                    ),
                    label = stringResource(R.string.card_turn_label),
                )
            }
        }
    }
}

@Composable
private fun CardFact(value: String, label: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = BrujulaTheme.colors.onHeroMuted)
    }
}

/** Lo que dice el hueco de estado; solo uno a la vez, por orden de importancia. */
internal enum class CompassStatus { POINTING, CALIBRATE, ACCURACY_HIGH, ACCURACY_MEDIUM }

internal fun CompassUiState.status(): CompassStatus = when {
    isPointingAtLondon -> CompassStatus.POINTING
    accuracy == CompassAccuracy.BAJA || accuracy == CompassAccuracy.NO_FIABLE -> CompassStatus.CALIBRATE
    accuracy == CompassAccuracy.ALTA -> CompassStatus.ACCURACY_HIGH
    else -> CompassStatus.ACCURACY_MEDIUM
}

/**
 * Las cuatro pastillas están siempre compuestas, una encima de otra, y solo se ve la activa: el
 * hueco mide lo que la más alta y no cambia de tamaño al pasar de una a otra, así que nada de
 * alrededor se desplaza al empezar o dejar de apuntar.
 */
@Composable
private fun StatusSlot(status: CompassStatus) {
    val colors = BrujulaTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SlotPill(
            visible = status == CompassStatus.ACCURACY_HIGH,
            text = stringResource(R.string.compass_accuracy_high),
            container = colors.okContainer,
            content = colors.onOkContainer,
        )
        SlotPill(
            visible = status == CompassStatus.ACCURACY_MEDIUM,
            text = stringResource(R.string.compass_accuracy_medium),
            container = colors.neutralContainer,
            content = colors.onNeutralContainer,
        )
        SlotPill(
            visible = status == CompassStatus.CALIBRATE,
            text = stringResource(R.string.compass_calibration),
            container = colors.warnContainer,
            content = colors.onWarnContainer,
        )
        SlotPill(
            visible = status == CompassStatus.POINTING,
            text = stringResource(R.string.compass_pointing),
            container = colors.pointing,
            content = colors.onPointing,
            showDot = false,
        )
    }
}

@Composable
private fun SlotPill(visible: Boolean, text: String, container: Color, content: Color, showDot: Boolean = true) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "status_pill")
    StatusPill(
        text = text,
        containerColor = container,
        contentColor = content,
        showDot = showDot,
        modifier = Modifier
            .alpha(alpha)
            .then(
                if (visible) Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                else Modifier.clearAndSetSemantics { }
            ),
    )
}

@Composable
private fun Dial(uiState: CompassUiState, needleRotation: Float, modifier: Modifier) {
    val description = dialDescription(uiState)
    CompassDial(
        needleRotation = needleRotation,
        modifier = modifier.semantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
    )
}

/**
 * Lo que anuncia TalkBack. En pasos de 10° para no leer un número nuevo con cada temblor del
 * sensor, que con `liveRegion` lo convertiría en un parloteo continuo.
 */
@Composable
private fun dialDescription(uiState: CompassUiState): String {
    if (uiState.isPointingAtLondon) return stringResource(R.string.compass_a11y_pointing)
    val relative = BearingCalculator.relativeAngle(uiState.arrowRotation, 0f)
    val degrees = (((abs(relative) + 5f) / 10f).toInt() * 10).coerceIn(10, 180)
    return if (relative >= 0f) {
        stringResource(R.string.compass_a11y_right, degrees)
    } else {
        stringResource(R.string.compass_a11y_left, degrees)
    }
}

/** Acumula los giros para que la animación tome el camino corto al cruzar 0°/360°. */
@Composable
private fun rememberNeedleRotation(target: Float): Float {
    var accumulatedRotation by remember { mutableFloatStateOf(target) }

    LaunchedEffect(target) {
        val delta = shortestAngleDiff(target, accumulatedRotation % 360f)
        accumulatedRotation += delta
    }

    val animatedRotation by animateFloatAsState(
        targetValue = accumulatedRotation,
        animationSpec = tween(durationMillis = 300),
        label = "needle_rotation",
    )
    return animatedRotation
}

@Composable
private fun Quote() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.compass_quote),
            style = QuoteTextStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.compass_quote_author),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
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

private val previewPointing = CompassUiState(
    arrowRotation = 2f,
    isPointingAtLondon = true,
    accuracy = CompassAccuracy.ALTA,
    hasLocationPermission = true,
    bearingDegrees = 11.4f,
    cardinal = CardinalDirection.NNE,
    distanceKm = 1264.0,
    turnDegrees = 2f,
)

private val previewCalibrating = previewPointing.copy(
    arrowRotation = 300f,
    turnDegrees = -60f,
    isPointingAtLondon = false,
    accuracy = CompassAccuracy.BAJA,
)

@Preview(name = "Móvil vertical", widthDp = 360, heightDp = 800)
@Preview(name = "Móvil pequeño", widthDp = 320, heightDp = 568)
@Preview(name = "Móvil apaisado", widthDp = 800, heightDp = 360)
@Preview(name = "Tablet apaisada", widthDp = 1280, heightDp = 800)
@Preview(name = "Tablet vertical", widthDp = 800, heightDp = 1280)
@Preview(name = "Fuente x2", widthDp = 360, heightDp = 800, fontScale = 2f)
@Preview(name = "Fuente x2 pequeño", widthDp = 320, heightDp = 568, fontScale = 2f)
@Preview(name = "Oscuro", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CompassPointingPreview() {
    CompassPreview(previewPointing)
}

@Preview(name = "Calibrando", widthDp = 360, heightDp = 800)
@Preview(name = "Calibrando, pequeño", widthDp = 320, heightDp = 568)
@Composable
private fun CompassCalibratingPreview() {
    CompassPreview(previewCalibrating)
}

@Composable
private fun CompassPreview(state: CompassUiState) {
    BrujulaDeLezoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CompassContent(uiState = state)
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

private fun Context.openUrl(url: String) {
    // Sin navegador instalado no hay a quién mandarlo; mejor no hacer nada que reventar.
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
