package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Pastilla de estado: precisión del sensor, aviso de calibración o "apuntando". */
@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Row(
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(contentColor, CircleShape),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
