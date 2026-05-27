package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.brujuladelezo.designsystem.R

@Composable
fun CompassRose(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.compass_face),
        contentDescription = null,
        modifier = modifier,
    )
}
