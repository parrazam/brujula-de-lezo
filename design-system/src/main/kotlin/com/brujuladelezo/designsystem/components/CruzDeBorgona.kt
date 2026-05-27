package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Decorative Burgundy Cross (Cruz de Borgoña) drawn in the background.
 * Two diagonal lines crossing at the center with serifs, rendered in a
 * semi-transparent tertiary color.
 */
@Composable
fun CruzDeBorgona(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawCruz(color)
    }
}

private fun DrawScope.drawCruz(color: Color) {
    val strokeWidth = size.minDimension * 0.06f
    val serifLength = strokeWidth * 1.2f
    val alpha = 0.18f
    val paint = color.copy(alpha = alpha)
    val center = Offset(size.width / 2f, size.height / 2f)
    val halfDiag = size.minDimension * 0.48f

    // Draw both diagonal arms
    for (angle in listOf(45f, -45f)) {
        rotate(angle, pivot = center) {
            // Main arm
            drawLine(
                color = paint,
                start = Offset(center.x, center.y - halfDiag),
                end = Offset(center.x, center.y + halfDiag),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
            // Top serif
            drawLine(
                color = paint,
                start = Offset(center.x - serifLength, center.y - halfDiag),
                end = Offset(center.x + serifLength, center.y - halfDiag),
                strokeWidth = strokeWidth * 0.5f,
                cap = StrokeCap.Round,
            )
            // Bottom serif
            drawLine(
                color = paint,
                start = Offset(center.x - serifLength, center.y + halfDiag),
                end = Offset(center.x + serifLength, center.y + halfDiag),
                strokeWidth = strokeWidth * 0.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}
