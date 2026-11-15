package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.summary.ActivitySummaryCalculator
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenDark
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraWhite
import com.example.tatratracker.viewmodel.ActivitySummaryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Najwyżej tyle nazw nowych miejsc trafia na kartę.
private const val MAX_CARD_PLACE_NAMES = 3

private val CardTextMuted = TatraWhite.copy(alpha = 0.78f)

/**
 * Karta z najważniejszymi liczbami wędrówki – to ona staje się obrazkiem do udostępnienia.
 * Ciemny margines dookoła jest częścią obrazka, żeby zaokrąglone rogi karty nie były przezroczyste.
 */
@Composable
internal fun ActivitySummaryShareCard(
    activity: ActivityEntity,
    state: ActivitySummaryUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TatraDark)
            .padding(TatraSpacing.sm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(TatraGreen, TatraGreenDark)))
                .padding(TatraSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Landscape,
                    contentDescription = null,
                    tint = TatraWhite,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "TatraTracker",
                    color = TatraWhite,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Text(
                text = activity.name,
                color = TatraWhite,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TatraSpacing.md)
            )

            Text(
                text = formatCardDate(activity.timestamp),
                color = CardTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = TatraFormat.distanceExact(activity.distanceMeters),
                color = TatraWhite,
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = TatraSpacing.md)
            )

            val elevation = state.elevation

            CardRow(
                leftLabel = "Czas w ruchu",
                leftValue = TatraFormat.durationMillis(activity.movingDurationMillis),
                rightLabel = "Podejścia",
                rightValue = elevation?.ascentMeters?.let { "+${TatraFormat.elevation(it)}" } ?: "—"
            )

            CardRow(
                leftLabel = "Najwyższy punkt",
                leftValue = elevation?.highestMeters?.let { TatraFormat.elevation(it) } ?: "—",
                rightLabel = "Sieć szlaków Tatr",
                rightValue = state.trails.fraction?.let { formatPercent(it) } ?: "—"
            )

            val newPlaceNames = state.places?.newPlaces.orEmpty().map { it.name }

            if (newPlaceNames.isNotEmpty()) {
                Text(
                    text = "Nowe miejsca",
                    color = CardTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = TatraSpacing.md)
                )

                Text(
                    text = ActivitySummaryCalculator.placeNamesLine(newPlaceNames, MAX_CARD_PLACE_NAMES),
                    color = TatraWhite,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CardRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TatraSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
    ) {
        CardMetric(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f))
        CardMetric(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = CardTextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = value,
            color = TatraWhite,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatCardDate(timestamp: Long): String {
    return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
