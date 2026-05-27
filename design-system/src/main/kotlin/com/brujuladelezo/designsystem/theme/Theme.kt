package com.brujuladelezo.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
)

@Composable
fun BrujulaDeLezoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BrujulaTypography,
        content = content,
    )
}
