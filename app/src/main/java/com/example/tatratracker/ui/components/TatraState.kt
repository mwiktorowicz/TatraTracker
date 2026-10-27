package com.example.tatratracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraDanger
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun TatraEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    TatraCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TatraGreen,
                    modifier = Modifier.size(34.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = title,
                color = TatraDark,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = message,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 5.dp)
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(14.dp))

                TatraPrimaryButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(0.72f)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
fun TatraLoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = TatraGreen,
            modifier = Modifier.size(28.dp)
        )

        Text(
            text = message,
            color = TatraTextMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun TatraErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    TatraCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = TatraDanger,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}