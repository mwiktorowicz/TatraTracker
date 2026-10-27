package com.example.tatratracker.ui.map

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MapRoundButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color =
            Color.White.copy(
                alpha = 0.96f
            ),
        shadowElevation = 5.dp
    ) {

        IconButton(
            onClick = onClick
        ) {

            content()
        }
    }
}