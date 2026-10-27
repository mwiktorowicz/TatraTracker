package com.example.tatratracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraGreenDark
import com.example.tatratracker.ui.theme.TatraGreenSoft
import com.example.tatratracker.ui.theme.TatraInfo
import com.example.tatratracker.ui.theme.TatraInfoSoft
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.ui.theme.TatraSurfaceStrong

@Composable
fun TatraStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: TatraBadgeStyle = TatraBadgeStyle.Neutral
) {
    val colors =
        when (style) {
            TatraBadgeStyle.Success -> TatraGreenSoft to TatraGreenDark
            TatraBadgeStyle.Info -> TatraInfoSoft to TatraInfo
            TatraBadgeStyle.Neutral -> TatraSurfaceStrong to TatraTextMuted
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.first
    ) {
        Text(
            text = text,
            color = colors.second,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

enum class TatraBadgeStyle {
    Success,
    Info,
    Neutral
}