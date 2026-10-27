package com.brujuladelezo.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Hueco que se reserva alrededor de la rosa para que el halo no se recorte. */
val CompassDialHaloInset: Dp = 14.dp

/**
 * Rosa de los vientos con la aguja superpuesta y, al apuntar a Londres, un aro con resplandor
 * en [haloColor]. El halo se dibuja dentro del propio tamaño del componente (la rosa se encoge
 * [CompassDialHaloInset]), así que nunca invade lo que haya alrededor.
 */
@Composable
fun CompassDial(
    needleRotation: Float,
    pointing: Boolean,
    haloColor: Color,
    ringGapColor: Color,
    modifier: Modifier = Modifier,
) {
    val halo by animateFloatAsState(
        targetValue = if (pointing) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "pointing_halo",
    )

    Box(
        modifier = modifier.drawBehind {
            if (halo == 0f) return@drawBehind
            val inset = CompassDialHaloInset.toPx()
            val roseRadius = size.minDimension / 2f - inset
            val ringWidth = 3.dp.toPx()
            val ringRadius = roseRadius + 4.dp.toPx() + ringWidth / 2f
            // Resplandor que nace en el aro y se apaga hacia el borde del componente.
            val outer = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    (roseRadius / outer) to haloColor.copy(alpha = 0.55f * halo),
                    1f to Color.Transparent,
                    center = center,
                    radius = outer,
                ),
                radius = outer,
            )
            drawCircle(color = ringGapColor.copy(alpha = halo), radius = ringRadius - ringWidth / 2f, center = center)
            drawCircle(
                color = haloColor.copy(alpha = halo),
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringWidth),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(CompassDialHaloInset)) {
            CompassRose(modifier = Modifier.fillMaxSize())
            LondonNeedle(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(needleRotation),
            )
        }
    }
}
