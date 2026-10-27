package com.example.tatratracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraSurface

@Composable
fun TatraCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TatraSurface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        val clickableModifier =
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }

        Box(
            modifier =
                clickableModifier
                    .padding(contentPadding)
        ) {
            content()
        }
    }
}