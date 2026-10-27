package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.map.ActivityRouteMap
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDanger
import com.example.tatratracker.ui.theme.TatraDangerSoft
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.ActivityDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityDetailsScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ActivityDetailsViewModel = viewModel()
) {
    val activity by viewModel.activity.collectAsState()
    val trackPoints by viewModel.trackPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var featureDialog by remember {
        mutableStateOf<ActivityFeatureDialog?>(null)
    }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            onDeleted()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Usunąć aktywność?")
            },
            text = {
                Text(
                    "Aktywność oraz wszystkie jej punkty GPS zostaną trwale usunięte."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteActivity()
                    }
                ) {
                    Text(
                        text = "Usuń",
                        color = TatraDanger
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    featureDialog?.let { dialog ->
        TatraFeatureDialog(
            title = dialog.title,
            message = dialog.message,
            onDismiss = {
                featureDialog = null
            }
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
    ) {
        TatraSubScreenHeader(
            title = "Szczegóły aktywności",
            subtitle = activity?.let { formatDate(it.timestamp) },
            onBack = onBack
        )

        when {
            isLoading -> {
                TatraLoadingState(
                    message = "Wczytuję aktywność...",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(TatraSpacing.lg)
                )
            }

            activity == null -> {
                TatraErrorState(
                    message = "Nie znaleziono tej aktywności.",
                    modifier =
                        Modifier.padding(
                            horizontal = TatraSpacing.lg,
                            vertical = TatraSpacing.sm
                        )
                )
            }

            else -> {
                ActivityDetailsContent(
                    activity = activity!!,
                    modifier = Modifier.weight(1f),
                    trackPoints = trackPoints,
                    onEdit = {
                        featureDialog =
                            ActivityFeatureDialog(
                                title = "Edycja aktywności",
                                message =
                                    "Edycja nazwy, daty i podstawowych informacji jest już przygotowana w interfejsie. Podepniemy zapis zmian w kolejnym etapie warstwy danych."
                            )
                    },
                    onExport = {
                        featureDialog =
                            ActivityFeatureDialog(
                                title = "Eksport GPX",
                                message =
                                    "Eksport śladu tej aktywności do pliku GPX podepniemy razem z pozostałymi operacjami na plikach."
                            )
                    },
                    onDelete = {
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityDetailsContent(
    activity: ActivityEntity,
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val scrollState = rememberScrollState()
    val sourceLabel = activityFormat(activity.sourceFileName)

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(
                    start = TatraSpacing.lg,
                    end = TatraSpacing.lg,
                    bottom = TatraSpacing.xxl
                )
    ) {
        TatraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = activity.name,
                            color = TatraDark,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = formatDate(activity.timestamp),
                            color = TatraTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    TatraStatusBadge(
                        text = sourceLabel,
                        style = TatraBadgeStyle.Info
                    )
                }

                if (activity.sourceFileName.isNotBlank()) {
                    Text(
                        text = activity.sourceFileName,
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
        ) {
            QuickMetricCard(
                label = "Dystans",
                value = formatDistance(activity.distanceMeters),
                modifier = Modifier.weight(1f)
            )

            QuickMetricCard(
                label = "Czas",
                value = formatDurationCompact(activity.durationMillis),
                modifier = Modifier.weight(1f)
            )

            QuickMetricCard(
                label = "Kalorie",
                value =
                    if (activity.calories > 0.0) {
                        "${activity.calories.toInt()} kcal"
                    } else {
                        "—"
                    },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(TatraSpacing.md))

        if (trackPoints.size >= 2) {
            Text(
                text = "Przebieg trasy",
                color = TatraDark,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Zielony punkt oznacza start, czerwony koniec śladu.",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(modifier = Modifier.height(TatraSpacing.sm))

            ActivityRouteMap(
                trackPoints = trackPoints,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                mapHeight = 280.dp
            )

            Spacer(modifier = Modifier.height(TatraSpacing.md))
        }

        ActivityElevationSection(
            trackPoints = trackPoints
        )

        if (trackPoints.any { it.ele > 0.0 }) {
            Spacer(modifier = Modifier.height(TatraSpacing.md))
        }

        Text(
            text = "Ruch",
            color = TatraDark,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                DetailRow(
                    label = "Średnia prędkość",
                    value = formatSpeed(activity.avgSpeedMs)
                )

                DetailRow(
                    label = "Maksymalna prędkość",
                    value = formatSpeed(activity.maxSpeedMs)
                )

                DetailRow(
                    label = "Średnie tempo",
                    value = formatPace(activity.avgPaceSecPerKm)
                )

                DetailRow(
                    label = "Najlepsze tempo",
                    value = formatPace(activity.maxPaceSecPerKm),
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(TatraSpacing.md))

        Text(
            text = "Wysokość i organizm",
            color = TatraDark,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                DetailRow(
                    label = "Najniższy punkt",
                    value = formatAltitude(activity.minAltitudeMeters)
                )

                DetailRow(
                    label = "Najwyższy punkt",
                    value = formatAltitude(activity.maxAltitudeMeters)
                )

                DetailRow(
                    label = "Średni puls",
                    value = formatHeartRate(activity.avgHeartRate)
                )

                DetailRow(
                    label = "Maksymalny puls",
                    value = formatHeartRate(activity.maxHeartRate)
                )

                DetailRow(
                    label = "Punkty GPS",
                    value = trackPoints.size.toString(),
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(TatraSpacing.lg))

        Text(
            text = "Akcje",
            color = TatraDark,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraPrimaryButton(
            onClick = onExport
        ) {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Eksportuj GPX")
        }

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraSecondaryButton(
            onClick = onEdit
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Edytuj aktywność")
        }

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraSecondaryButton(
            onClick = onDelete,
            containerColor = TatraDangerSoft,
            contentColor = TatraDanger,
            borderColor = TatraDanger.copy(alpha = 0.35f)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Usuń aktywność")
        }
    }
}

@Composable
private fun QuickMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    TatraCard(
        modifier = modifier,
        backgroundColor = TatraGreenVerySoft,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(13.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = value,
                color = TatraDark,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = value,
                color = TatraDark,
                style = MaterialTheme.typography.titleSmall
            )
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = TatraOutline.copy(alpha = 0.75f)
            )
        }
    }
}

private data class ActivityFeatureDialog(
    val title: String,
    val message: String
)

private fun formatDate(
    timestamp: Long
): String {
    val formatter =
        SimpleDateFormat(
            "dd MMMM yyyy • HH:mm",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}

private fun activityFormat(
    fileName: String
): String {
    val extension =
        fileName
            .substringAfterLast('.', "")
            .trim()
            .uppercase(Locale.getDefault())

    return when (extension) {
        "FIT", "TCX", "GPX" -> extension
        else -> "IMPORT"
    }
}

private fun formatDistance(
    distanceMeters: Double
): String {
    return if (distanceMeters > 0.0) {
        String.format(
            Locale.getDefault(),
            "%.2f km",
            distanceMeters / 1000.0
        )
    } else {
        "—"
    }
}

private fun formatDurationCompact(
    durationMillis: Long
): String {
    if (durationMillis <= 0L) {
        return "—"
    }

    val totalMinutes = durationMillis / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours <= 0 -> "$minutes min"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes min"
    }
}

private fun formatSpeed(
    speedMs: Double
): String {
    if (speedMs <= 0.0) {
        return "Brak danych"
    }

    return String.format(
        Locale.getDefault(),
        "%.2f km/h",
        speedMs * 3.6
    )
}

private fun formatPace(
    secondsPerKm: Double
): String {
    if (secondsPerKm <= 0.0) {
        return "Brak danych"
    }

    val totalSeconds = secondsPerKm.toLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(
        Locale.getDefault(),
        "%d:%02d min/km",
        minutes,
        seconds
    )
}

private fun formatAltitude(
    altitudeMeters: Double
): String {
    if (altitudeMeters <= 0.0) {
        return "Brak danych"
    }

    return String.format(
        Locale.getDefault(),
        "%.0f m n.p.m.",
        altitudeMeters
    )
}

private fun formatHeartRate(
    heartRate: Int
): String {
    return if (heartRate > 0) {
        "$heartRate bpm"
    } else {
        "Brak danych"
    }
}