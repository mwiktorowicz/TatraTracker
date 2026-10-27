package com.example.tatratracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun TatraStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    helper: String? = null
) {
    TatraCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = value,
                color = TatraDark,
                style = MaterialTheme.typography.headlineSmall
            )

            if (!helper.isNullOrBlank()) {
                Text(
                    text = helper,
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}