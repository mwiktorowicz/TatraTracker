package com.example.tatratracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun TatraSubScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = TatraSpacing.xs,
                    end = TatraSpacing.lg,
                    top = TatraSpacing.sm,
                    bottom = TatraSpacing.sm
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Wstecz",
                tint = TatraDark
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = TatraSpacing.xs)
        ) {
            Text(
                text = title,
                color = TatraDark,
                style = MaterialTheme.typography.headlineSmall
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = TatraSpacing.xxs)
                )
            }
        }

        actions()
    }
}