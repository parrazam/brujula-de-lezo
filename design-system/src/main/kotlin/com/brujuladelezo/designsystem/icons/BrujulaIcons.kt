package com.brujuladelezo.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos propios. material3 ya no arrastra material-icons, y traer la biblioteca extendida
 * (varios MB antes de R8) por un solo icono no compensa.
 */
object BrujulaIcons {

    /** Tres reguladores: el botón de ajustes. */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "BrujulaSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke = SolidColor(Color.Black)
            path(stroke = stroke, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 6f); lineTo(14f, 6f)
                moveTo(18f, 6f); lineTo(20f, 6f)
                moveTo(4f, 12f); lineTo(8f, 12f)
                moveTo(12f, 12f); lineTo(20f, 12f)
                moveTo(4f, 18f); lineTo(16f, 18f)
            }
            path(stroke = stroke, strokeLineWidth = 2f) {
                // Los mandos: círculos de radio 2 en (16,6), (10,12) y (18,18).
                for ((cx, cy) in listOf(16f to 6f, 10f to 12f, 18f to 18f)) {
                    moveTo(cx + 2f, cy)
                    arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - 2f, y1 = cy)
                    arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + 2f, y1 = cy)
                    close()
                }
            }
        }.build()
    }
}
