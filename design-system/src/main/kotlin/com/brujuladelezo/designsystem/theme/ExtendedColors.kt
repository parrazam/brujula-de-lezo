package com.brujuladelezo.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores sin rol equivalente en [ColorScheme]: la tarjeta destacada, las pastillas de estado y
 * la pastilla de "apuntando". Se leen con `BrujulaTheme.colors`.
 */
@Immutable
data class ExtendedColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val okContainer: Color,
    val onOkContainer: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    /** Pastilla al apuntar a Londres. */
    val pointing: Color,
    val onPointing: Color,
)

val LightExtendedColors = ExtendedColors(
    hero = AzulMarinoImperial,
    onHero = BlancoPergamino,
    onHeroMuted = Color(0xFFC8BFAE),
    okContainer = Color(0xFFE1E7D3),
    onOkContainer = Color(0xFF46582A),
    warnContainer = Color(0xFFF5DDCF),
    onWarnContainer = Color(0xFF8B3A17),
    neutralContainer = Color(0xFFE7DCC2),
    onNeutralContainer = MarronCuero,
    pointing = RojoBorgona,
    onPointing = Color(0xFFFFF4E6),
)

val DarkExtendedColors = ExtendedColors(
    hero = Color(0xFF182A42),
    onHero = Color(0xFFEBD8AA),
    onHeroMuted = Color(0xFFA79D84),
    okContainer = Color(0xFF24351F),
    onOkContainer = Color(0xFFB9D08C),
    warnContainer = Color(0xFF40261A),
    onWarnContainer = Color(0xFFF3B08C),
    neutralContainer = Color(0xFF2A3A52),
    onNeutralContainer = OroViejoBrillante,
    // Más oscuro que RojoBorgonaVivo: con el texto claro encima, aquel no llegaba a 4,5:1.
    pointing = Color(0xFFB23A30),
    onPointing = Color(0xFFFFF1E4),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
