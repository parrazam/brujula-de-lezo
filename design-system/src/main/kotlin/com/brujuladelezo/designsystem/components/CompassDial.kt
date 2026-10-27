package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

/**
 * Rosa de los vientos con la aguja superpuesta. Al apuntar a Londres no cambia: lo avisan la
 * pastilla del hueco de estado y la vibración.
 */
@Composable
fun CompassDial(
    needleRotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CompassRose(modifier = Modifier.fillMaxSize())
        LondonNeedle(
            modifier = Modifier
                .fillMaxSize()
                .rotate(needleRotation),
        )
    }
}
