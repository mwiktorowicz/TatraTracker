package com.example.tatratracker.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tatratracker.data.COMPLETED_TRAIL_THRESHOLD
import com.example.tatratracker.data.TrailInfo
import com.example.tatratracker.data.TrailNameProgress
import com.example.tatratracker.data.TrailProgressState
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.formatPercent
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraSurfaceStrong
import com.example.tatratracker.ui.theme.TatraTextMuted

// Najwyżej tyle innych szlaków z tego samego odcinka mieści się na karcie.
private const val MAX_OTHER_TRAILS = 3

/**
 * Karta szlaku dotkniętego na mapie: kolory, nazwa, długość i postęp przejścia.
 * Gdy odcinkiem biegnie kilka szlaków, pozostałe są pod spodem i można się na nie przełączyć.
 * [progressState] = null – postęp jeszcze się nie zaczął liczyć.
 */
@Composable
internal fun MapTrailCard(
    trail: SelectedMapTrail,
    progressState: TrailProgressState?,
    onSelectTrail: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = trail.selected

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = TatraSurface.copy(alpha = 0.97f),
        shadowElevation = 7.dp
    ) {
        Column(
            modifier = Modifier.padding(
                start = TatraSpacing.md,
                end = TatraSpacing.xs,
                top = TatraSpacing.xs,
                bottom = TatraSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    TrailColorDots(colors = selected.colors, dotSize = 18)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (selected != null) TatraSpacing.sm else 0.dp)
                ) {
                    Text(
                        text = selected?.name ?: "Szlak pieszy",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            if (selected != null) {
                                "Szlak · ${TatraFormat.distance(selected.lengthMeters)}"
                            } else {
                                "Poza siecią szlaków TPN i TANAP – brak szczegółów"
                            },
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Zamknij",
                        tint = TatraTextMuted
                    )
                }
            }

            if (selected != null) {
                TrailProgressSection(
                    trail = selected,
                    progressState = progressState,
                    modifier = Modifier.padding(end = TatraSpacing.xs)
                )
            }

            val otherTrails =
                trail.trails
                    .withIndex()
                    .filter { (index, _) -> index != trail.selectedIndex }

            if (otherTrails.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(end = TatraSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tym odcinkiem biegną też:",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    otherTrails.take(MAX_OTHER_TRAILS).forEach { (index, other) ->
                        OtherTrailRow(trail = other, onClick = { onSelectTrail(index) })
                    }

                    if (otherTrails.size > MAX_OTHER_TRAILS) {
                        Text(
                            text = "i jeszcze ${otherTrails.size - MAX_OTHER_TRAILS}",
                            color = TatraTextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailProgressSection(
    trail: TrailInfo,
    progressState: TrailProgressState?,
    modifier: Modifier = Modifier
) {
    val progress: TrailNameProgress? =
        progressState?.result?.trailProgress?.firstOrNull { it.name == trail.name }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            progress != null -> {
                val fraction = progress.completedFraction.coerceIn(0.0, 1.0)

                TrailProgressBar(fraction = fraction.toFloat())

                Text(
                    text = progressText(progress, fraction),
                    color = TatraDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            progressState?.errorMessage != null ->
                Text(
                    text = "Nie udało się policzyć postępu.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

            else ->
                Text(
                    text = "Liczę postęp…",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
        }
    }
}

// „Przejście: 63,0% · 7,8 km z 12,4 km”, od 90% dodatkowo „ukończony”.
private fun progressText(progress: TrailNameProgress, fraction: Double): String {
    val percent = formatPercent(fraction)

    if (progress.completedLengthMeters <= 0.0) {
        return "Przejście: $percent – jeszcze nieprzebyty"
    }

    val lengths =
        "${TatraFormat.distance(progress.completedLengthMeters)} z " +
            TatraFormat.distance(progress.totalLengthMeters)

    return if (fraction >= COMPLETED_TRAIL_THRESHOLD) {
        "Przejście: $percent · $lengths · ukończony"
    } else {
        "Przejście: $percent · $lengths"
    }
}

@Composable
private fun TrailProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TatraSurfaceStrong)
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(TatraGreen)
            )
        }
    }
}

@Composable
private fun OtherTrailRow(trail: TrailInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TatraGreenVerySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrailColorDots(colors = trail.colors, dotSize = 12)

        Text(
            text = trail.name,
            color = TatraDark,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        Text(
            text = TatraFormat.distance(trail.lengthMeters),
            color = TatraTextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Kropki w kolorach szlaku; szlak bez wspólnego koloru dostaje jedną szarą.
@Composable
private fun TrailColorDots(colors: List<String>, dotSize: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.ifEmpty { listOf("") }.forEach { color ->
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .clip(CircleShape)
                    .background(trailDotColor(color))
                    .border(1.dp, TatraOutline, CircleShape)
            )
        }
    }
}

// Te same barwy, którymi mapa rysuje linie szlaków (MapLayers.addOutdoorLayers).
private fun trailDotColor(color: String): Color {
    return when (color) {
        "red" -> Color(215, 60, 52)
        "blue" -> Color(34, 103, 210)
        "green" -> Color(40, 151, 72)
        "yellow" -> Color(231, 185, 30)
        "black" -> Color(36, 40, 42)
        "brown" -> Color(133, 86, 58)
        "orange" -> Color(232, 125, 35)
        "purple" -> Color(129, 67, 173)
        else -> Color(82, 86, 87)
    }
}
