package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classic compass rose drawn with Canvas. The rose itself is fixed (it represents
 * the cardinal directions). Rotate the parent composable to reflect device orientation.
 */
@Composable
fun CompassRose(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawCompassRose(primaryColor, secondaryColor)
    }
}

private fun DrawScope.drawCompassRose(primary: Color, secondary: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f

    // Outer ring
    drawCircle(
        color = primary.copy(alpha = 0.25f),
        radius = r * 0.98f,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.03f),
    )

    // Cardinal and inter-cardinal tick marks
    for (i in 0 until 36) {
        val angle = Math.toRadians(i * 10.0)
        val isCardinal = i % 9 == 0
        val isIntercardinal = i % 9 == 0 || i % 45 == 0
        val outerR = r * 0.95f
        val innerR = when {
            isCardinal -> r * 0.80f
            isIntercardinal -> r * 0.85f
            else -> r * 0.90f
        }
        val strokeW = if (isCardinal) r * 0.025f else r * 0.012f
        drawLine(
            color = primary.copy(alpha = 0.7f),
            start = Offset(
                cx + outerR * sin(angle).toFloat(),
                cy - outerR * cos(angle).toFloat(),
            ),
            end = Offset(
                cx + innerR * sin(angle).toFloat(),
                cy - innerR * cos(angle).toFloat(),
            ),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
        )
    }

    // Cardinal points — diamond petals
    for (cardinalIndex in 0 until 4) {
        val angle = cardinalIndex * 90f
        rotate(angle, pivot = Offset(cx, cy)) {
            val petalLength = r * 0.72f
            val petalWidth = r * 0.18f
            val path = Path().apply {
                moveTo(cx, cy - petalLength)
                lineTo(cx + petalWidth, cy)
                lineTo(cx, cy + petalLength * 0.15f)
                lineTo(cx - petalWidth, cy)
                close()
            }
            // North petal alternates color
            val color = if (cardinalIndex == 0) secondary else primary
            drawPath(path, color = color)
        }
    }

    // Inter-cardinal smaller diamond petals
    for (i in 0 until 4) {
        rotate(45f + i * 90f, pivot = Offset(cx, cy)) {
            val petalLength = r * 0.48f
            val petalWidth = r * 0.10f
            val path = Path().apply {
                moveTo(cx, cy - petalLength)
                lineTo(cx + petalWidth, cy)
                lineTo(cx, cy + petalLength * 0.15f)
                lineTo(cx - petalWidth, cy)
                close()
            }
            drawPath(path, color = primary.copy(alpha = 0.6f))
        }
    }

    // Center circle
    drawCircle(
        color = secondary,
        radius = r * 0.07f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = primary,
        radius = r * 0.04f,
        center = Offset(cx, cy),
    )
}
