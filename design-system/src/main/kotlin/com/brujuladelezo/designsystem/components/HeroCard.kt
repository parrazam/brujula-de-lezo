package com.brujuladelezo.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brujuladelezo.designsystem.theme.BrujulaTheme

/** Tarjeta destacada en azul marino: la única superficie con relleno propio de la pantalla. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = BrujulaTheme.colors.hero,
        contentColor = BrujulaTheme.colors.onHero,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), content = content)
    }
}
