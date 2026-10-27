package com.brujuladelezo.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.brujuladelezo.designsystem.R

/*
 * Las mismas familias que la web (docs/): Fraunces para títulos, la cita y la distancia; IBM Plex
 * Sans para todo lo demás. Van empaquetadas (TTF estáticos, subconjunto latino, licencias OFL en
 * licenses/) porque FontFamily.Serif cambiaba según el fabricante. Plex tiene cifras tabulares
 * por defecto, así que los grados no bailan al girar; Fraunces no, y por eso no se usa para
 * números que cambian a menudo.
 */
val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_extrabold, FontWeight.ExtraBold),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
)

val PlexSans = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
)

val BrujulaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    ),
)

/** La cita de Blas de Lezo: Fraunces en cursiva, sin rol equivalente en Material. */
val QuoteTextStyle = TextStyle(
    fontFamily = Fraunces,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
)
