package com.example.tatratracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.export.ActivityGpxExporter
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.format.TatraFormat
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
import com.example.tatratracker.ui.theme.TatraGreen
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
    onReplay: () -> Unit,
    onSummary: () -> Unit,
    viewModel: ActivityDetailsViewModel = viewModel()
) {
    val activity by viewModel.activity.collectAsState()
    val trackPoints by viewModel.trackPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    val message by viewModel.message.collectAsState()

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    // Plik zapisuje ViewModel; tutaj tylko systemowe okno wyboru miejsca zapisu.
    val createGpxLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/gpx+xml")
        ) { uri ->
            if (uri != null) {
                viewModel.exportGpx(uri)
            }
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

    message?.let { current ->
        TatraFeatureDialog(
            title = current.title,
            message = current.message,
            onDismiss = viewModel::consumeMessage
        )
    }

    val editedActivity = activity
    if (showEditDialog && editedActivity != null) {
        ActivityEditDialog(
            initialName = editedActivity.name,
            initialNote = editedActivity.note,
            onDismiss = {
                showEditDialog = false
            },
            onSave = { name, note ->
                showEditDialog = false
                viewModel.saveDetails(name, note)
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
                        showEditDialog = true
                    },
                    onExport = {
                        val current = activity!!

                        // Okno „Utwórz dokument” od razu tworzy plik, więc pusty ślad blokujemy wcześniej.
                        if (trackPoints.isEmpty()) {
                            featureDialog =
                                ActivityFeatureDialog(
                                    title = "Nie można wyeksportować aktywności",
                                    message = ActivityGpxExporter.EMPTY_TRACK_MESSAGE
                                )
                        } else {
                            createGpxLauncher.launch(
                                ActivityGpxExporter.suggestedFileName(current.name, current.timestamp)
                            )
                        }
                    },
                    onDelete = {
                        showDeleteDialog = true
                    },
                    onReplay = onReplay,
                    onSummary = onSummary
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
    onDelete: () -> Unit,
    onReplay: () -> Unit,
    onSummary: () -> Unit
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

                activity.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = "Notatka",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Text(
                        text = note,
                        color = TatraDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
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
                value = TatraFormat.distanceExact(activity.distanceMeters.takeIf { it > 0.0 }),
                modifier = Modifier.weight(1f)
            )

            QuickMetricCard(
                label = "Czas",
                value = TatraFormat.durationMillis(activity.durationMillis.takeIf { it > 0L }),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }

                Spacer(modifier = Modifier.width(TatraSpacing.sm))

                // Odtwarzanie wędrówki na pełnym ekranie (tylko gdy jest ślad GPS).
                Button(
                    onClick = onReplay,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TatraGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Odtwórz")
                }
            }

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
                    label = "Czas całkowity",
                    value = TatraFormat.durationMillis(activity.durationMillis.takeIf { it > 0L })
                )

                DetailRow(
                    label = "W ruchu",
                    value = TatraFormat.durationMillis(
                        activity.movingDurationMillis.takeIf { it > 0L }
                    )
                )

                // Postoje pokazujemy, gdy po zaokrągleniu do minut jest co pokazać.
                val stopsMillis = activity.durationMillis - activity.movingDurationMillis
                if (stopsMillis >= 60_000L) {
                    DetailRow(
                        label = "Postoje",
                        value = TatraFormat.durationMillis(stopsMillis)
                    )
                }

                DetailRow(
                    label = "Średnia prędkość",
                    value = TatraFormat.speed(activity.avgSpeedMs)
                )

                DetailRow(
                    label = "Maksymalna prędkość",
                    value = TatraFormat.speed(activity.maxSpeedMs)
                )

                DetailRow(
                    label = "Średnie tempo",
                    value = TatraFormat.pace(activity.avgPaceSecPerKm)
                )

                DetailRow(
                    label = "Najlepsze tempo",
                    value = TatraFormat.pace(activity.maxPaceSecPerKm),
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
                    value = TatraFormat.altitude(activity.minAltitudeMeters)
                )

                DetailRow(
                    label = "Najwyższy punkt",
                    value = TatraFormat.altitude(activity.maxAltitudeMeters)
                )

                DetailRow(
                    label = "Średni puls",
                    value = TatraFormat.heartRate(activity.avgHeartRate)
                )

                DetailRow(
                    label = "Maksymalny puls",
                    value = TatraFormat.heartRate(activity.maxHeartRate)
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

        // Plansze podsumowania – te same, które można otworzyć po synchronizacji.
        TatraSecondaryButton(
            onClick = onSummary
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Podsumowanie wędrówki")
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
