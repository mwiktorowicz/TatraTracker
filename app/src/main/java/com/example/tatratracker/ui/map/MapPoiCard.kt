package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.poi.PoiVisitInfo
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Karta punktu dotkniętego na mapie: nazwa, rodzaj i wysokość, wizyty użytkownika
 * oraz start planowania trasy do tego punktu albo z niego.
 * [visitInfo] = null – wiersz o wizytach się nie pojawia.
 */
@Composable
internal fun MapPoiCard(
    poi: SelectedMapPoi,
    visitInfo: PoiVisitInfo?,
    onNavigateHere: () -> Unit,
    onRouteFromHere: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TatraGreenVerySoft
                ) {
                    Icon(
                        imageVector = poi.icon(),
                        contentDescription = null,
                        tint = TatraGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TatraSpacing.sm)
                ) {
                    Text(
                        text = poi.displayName,
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = poi.subtitle(),
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

            if (visitInfo != null) {
                Text(
                    text = visitText(visitInfo),
                    color = if (visitInfo.visitCount > 0) TatraDark else TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = TatraSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                TatraPrimaryButton(
                    onClick = onNavigateHere,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Prowadź tutaj", maxLines = 1)
                }

                TatraSecondaryButton(
                    onClick = onRouteFromHere,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trasa stąd", maxLines = 1)
                }
            }
        }
    }
}

private fun SelectedMapPoi.icon(): ImageVector {
    return when (kind) {
        MapPoiKind.PEAK -> Icons.Outlined.Landscape
        MapPoiKind.HUT -> Icons.Outlined.Cottage
        MapPoiKind.CAVE -> Icons.Outlined.Terrain
        MapPoiKind.WATER -> Icons.Outlined.Water
        MapPoiKind.SAVED_PLACE -> Icons.Outlined.Place
    }
}

// „Szczyt · 1987 m n.p.m.”, „Schronisko”, „Mój punkt · Inne”.
private fun SelectedMapPoi.subtitle(): String {
    val parts = mutableListOf(kind.label)

    if (kind == MapPoiKind.SAVED_PLACE && savedPlaceCategory != null) {
        parts += SavedPlaceCategory.label(savedPlaceCategory)
    }

    if (elevationMeters != null) {
        parts += TatraFormat.altitude(elevationMeters.toDouble())
    }

    return parts.joinToString(" · ")
}

private fun visitText(info: PoiVisitInfo): String {
    if (info.visitCount == 0) {
        return "Jeszcze Cię tu nie było"
    }

    val lastVisit =
        info.lastVisitedAt
            ?.let { timestamp ->
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
            }
            ?: return "Twoje wizyty: ${info.visitCount}"

    return "Twoje wizyty: ${info.visitCount} · ostatnia $lastVisit"
}
