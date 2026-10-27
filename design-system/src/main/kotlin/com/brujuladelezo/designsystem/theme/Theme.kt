package com.brujuladelezo.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AzulMarinoImperial,
    onPrimary = BlancoPergamino,
    secondary = OroViejo,
    onSecondary = MarronCuero,
    tertiary = RojoBorgona,
    onTertiary = BlancoPergamino,
    background = BlancoPergamino,
    onBackground = MarronCuero,
    surface = BlancoPergamino,
    onSurface = MarronCuero,
    surfaceVariant = OroViejo.copy(alpha = 0.25f),
    onSurfaceVariant = MarronCuero,
    outline = MarronCuero.copy(alpha = 0.4f),
    // Sin estos roles, la hoja de ajustes y los botones segmentados salían con el lila de
    // Material por defecto.
    outlineVariant = Color(0xFFD8C9A8),
    secondaryContainer = Color(0xFFE7DCC2),
    onSecondaryContainer = AzulMarinoImperial,
    surfaceTint = AzulMarinoImperial,
    surfaceContainerLowest = Color(0xFFFBF6EA),
    surfaceContainerLow = Color(0xFFF6EEDC),
    surfaceContainer = Color(0xFFEFE4CB),
    surfaceContainerHigh = Color(0xFFE9DCC0),
    surfaceContainerHighest = Color(0xFFE3D4B5),
)

private val DarkColors = darkColorScheme(
    primary = OroViejoBrillante,
    onPrimary = AzulAbismoProfundo,
    secondary = AzulMarinoMedio,
    onSecondary = OroViejoBrillante,
    tertiary = RojoBorgonaVivo,
    onTertiary = AzulAbismoProfundo,
    background = AzulAbismoProfundo,
    onBackground = OroViejoBrillante,
    surface = AzulAbismoProfundo,
    onSurface = OroViejoBrillante,
    surfaceVariant = AzulMarinoMedio,
    onSurfaceVariant = BronceEnvejecido,
    outline = BronceEnvejecido.copy(alpha = 0.5f),
    outlineVariant = Color(0xFF2B3D57),
    secondaryContainer = Color(0xFF2A3A52),
    onSecondaryContainer = OroViejoBrillante,
    surfaceTint = OroViejoBrillante,
    surfaceContainerLowest = Color(0xFF0B1624),
    surfaceContainerLow = Color(0xFF13233A),
    surfaceContainer = Color(0xFF172942),
    surfaceContainerHigh = Color(0xFF1D3150),
    surfaceContainerHighest = Color(0xFF233A5C),
)

@Composable
fun BrujulaDeLezoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = BrujulaTypography,
            content = content,
        )
    }
}

object BrujulaTheme {
    /** Colores propios de la app; ver [ExtendedColors]. */
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
