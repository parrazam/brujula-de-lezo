package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Arrow needle pointing upward (toward Londres when rotated by the parent).
 * Rendered in the accent/tertiary color.
 */
@Composable
fun LondonNeedle(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawNeedle(color)
    }
}

private fun DrawScope.drawNeedle(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val len = size.minDimension * 0.42f
    val tipWidth = size.minDimension * 0.10f
    val tailWidth = size.minDimension * 0.06f
    val tailLength = len * 0.35f

    // Arrow head (points up)
    val arrowPath = Path().apply {
        moveTo(cx, cy - len)
        lineTo(cx + tipWidth, cy)
        lineTo(cx, cy - len * 0.15f)
        lineTo(cx - tipWidth, cy)
        close()
    }
    drawPath(arrowPath, color = color)

    // Tail shaft
    val tailPath = Path().apply {
        moveTo(cx - tailWidth / 2f, cy)
        lineTo(cx + tailWidth / 2f, cy)
        lineTo(cx + tailWidth / 2f, cy + tailLength)
        lineTo(cx - tailWidth / 2f, cy + tailLength)
        close()
    }
    drawPath(tailPath, color = color.copy(alpha = 0.55f))

    // Outline
    drawPath(arrowPath, color = color.copy(alpha = 0.4f), style = Stroke(width = 2f))

    // Center pivot circle
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = size.minDimension * 0.04f,
        center = Offset(cx, cy),
    )
}
