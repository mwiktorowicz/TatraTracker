package com.example.tatratracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.data.PlannedRoutePointEntity
import com.example.tatratracker.export.PlannedRouteGpxExporter
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraErrorState
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.map.PlannedRouteMap
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDanger
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.PlannedRouteDetailsUiState
import com.example.tatratracker.viewmodel.PlannedRouteDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlannedRouteDetailsScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel:
    PlannedRouteDetailsViewModel =
        viewModel()
) {

    val context =
        LocalContext.current

    val uiState by
    viewModel
        .uiState
        .collectAsState()

    var featureDialog by
    remember {
        mutableStateOf<
                Pair<String, String>?
                >(
            null
        )
    }

    var showDeleteDialog by
    remember {
        mutableStateOf(
            false
        )
    }

    var pendingGpxContent by
    remember {
        mutableStateOf<String?>(
            null
        )
    }

    val createGpxLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/gpx+xml"
                )
        ) {
                uri ->

            if (
                uri == null
            ) {

                pendingGpxContent =
                    null

                return@rememberLauncherForActivityResult
            }

            val gpxContent =
                pendingGpxContent

            if (
                gpxContent == null
            ) {

                featureDialog =
                    "Błąd eksportu" to
                            "Nie udało się przygotować danych pliku GPX."

                return@rememberLauncherForActivityResult
            }

            val result =
                runCatching {

                    val outputStream =
                        context
                            .contentResolver
                            .openOutputStream(
                                uri
                            )
                            ?: error(
                                "Nie udało się otworzyć wybranego pliku."
                            )

                    outputStream
                        .bufferedWriter(
                            Charsets.UTF_8
                        )
                        .use {
                                writer ->

                            writer.write(
                                gpxContent
                            )
                        }
                }

            pendingGpxContent =
                null

            result
                .onSuccess {

                    featureDialog =
                        "GPX zapisany" to
                                "Plik trasy został poprawnie wyeksportowany."
                }
                .onFailure {
                        error ->

                    featureDialog =
                        "Nie udało się zapisać GPX" to
                                (
                                        error.message
                                            ?: "Wystąpił nieznany błąd zapisu pliku."
                                        )
                }
        }

    featureDialog
        ?.let {
                dialog ->

            TatraFeatureDialog(
                title =
                    dialog.first,
                message =
                    dialog.second,
                onDismiss = {

                    featureDialog =
                        null
                }
            )
        }

    if (
        showDeleteDialog
    ) {

        AlertDialog(
            onDismissRequest = {

                showDeleteDialog =
                    false
            },
            title = {

                Text(
                    "Usunąć trasę?"
                )
            },
            text = {

                Text(
                    "Trasa i wszystkie zapisane punkty zostaną trwale usunięte."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog =
                            false

                        viewModel.deleteRoute(
                            onDeleted =
                                onDeleted
                        )
                    }
                ) {

                    Text(
                        text =
                            "Usuń",
                        color =
                            TatraDanger
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {

                        showDeleteDialog =
                            false
                    }
                ) {

                    Text(
                        "Anuluj"
                    )
                }
            }
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    TatraBackground
                )
    ) {

        when (
            val state =
                uiState
        ) {

            PlannedRouteDetailsUiState.Loading -> {

                TatraSubScreenHeader(
                    title =
                        "Szczegóły trasy",
                    onBack =
                        onBack
                )

                TatraLoadingState(
                    message =
                        "Wczytywanie trasy..."
                )
            }

            PlannedRouteDetailsUiState.NotFound -> {

                TatraSubScreenHeader(
                    title =
                        "Szczegóły trasy",
                    onBack =
                        onBack
                )

                TatraErrorState(
                    message =
                        "Nie znaleziono zapisanej trasy.",
                    modifier =
                        Modifier.padding(
                            TatraSpacing.lg
                        )
                )
            }

            is PlannedRouteDetailsUiState.Ready -> {

                PlannedRouteReadyContent(
                    state =
                        state,
                    onBack =
                        onBack,
                    onEdit =
                        onEdit,
                    onExport = {

                        val exportResult =
                            runCatching {

                                PlannedRouteGpxExporter
                                    .buildGpx(
                                        route =
                                            state.route,
                                        geometryPoints =
                                            state.geometryPoints,
                                        startPoint =
                                            state.startPoint,
                                        waypoints =
                                            state.waypoints,
                                        endPoint =
                                            state.endPoint,
                                        elevationPoints =
                                            state.elevationPoints
                                    )
                            }

                        exportResult
                            .onSuccess {
                                    gpxContent ->

                                pendingGpxContent =
                                    gpxContent

                                createGpxLauncher.launch(
                                    PlannedRouteGpxExporter
                                        .suggestedFileName(
                                            state.route.name
                                        )
                                )
                            }
                            .onFailure {
                                    error ->

                                featureDialog =
                                    "Nie można wyeksportować trasy" to
                                            (
                                                    error.message
                                                        ?: "Nie udało się przygotować pliku GPX."
                                                    )
                            }
                    },
                    onDuplicate = {

                        viewModel.duplicateRoute {
                                result ->

                            result
                                .onSuccess {

                                    featureDialog =
                                        "Trasa skopiowana" to
                                                "Utworzono pełną kopię trasy wraz z geometrią, punktami i profilem wysokościowym."
                                }
                                .onFailure {
                                        error ->

                                    featureDialog =
                                        "Nie udało się skopiować trasy" to
                                                (
                                                        error.message
                                                            ?: "Nieznany błąd."
                                                        )
                                }
                        }
                    },
                    onDelete = {

                        showDeleteDialog =
                            true
                    }
                )
            }
        }
    }
}

@Composable
private fun PlannedRouteReadyContent(
    state:
    PlannedRouteDetailsUiState.Ready,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {

    val route =
        state.route

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        TatraSubScreenHeader(
            title =
                route.name,
            subtitle =
                formatPlannedRouteDate(
                    route.createdAt
                ),
            onBack =
                onBack
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            TatraSpacing.lg
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    TatraSpacing.md
                )
        ) {

            TatraCard(
                modifier =
                    Modifier.fillMaxWidth(),
                backgroundColor =
                    TatraGreenVerySoft
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Route,
                        contentDescription =
                            null,
                        tint =
                            TatraGreen
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .padding(
                                    start =
                                        TatraSpacing.sm
                                )
                    ) {

                        Text(
                            text =
                                routeDirection(
                                    route
                                ),
                            color =
                                TatraDark,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                        )

                        Text(
                            text =
                                "Zapisana trasa",
                            color =
                                TatraTextMuted,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            modifier =
                                Modifier.padding(
                                    top =
                                        3.dp
                                )
                        )
                    }

                    TatraStatusBadge(
                        text =
                            "PLAN",
                        style =
                            TatraBadgeStyle.Success
                    )
                }
            }

            if (
                state.geometryPoints.size >=
                2
            ) {

                PlannedRouteMap(
                    geometryPoints =
                        state.geometryPoints,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            TatraSectionHeader(
                title =
                    "Podsumowanie"
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        TatraSpacing.sm
                    )
            ) {

                RouteMetricCard(
                    label =
                        "Dystans",
                    value =
                        TatraFormat.distanceExact(route.distanceMeters),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                RouteMetricCard(
                    label =
                        "Czas",
                    value =
                        TatraFormat.duration(route.durationSeconds.takeIf { it > 0 }),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        TatraSpacing.sm
                    )
            ) {

                RouteMetricCard(
                    label =
                        "Podejście",
                    value =
                        TatraFormat.elevation(route.ascentMeters),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                RouteMetricCard(
                    label =
                        "Zejście",
                    value =
                        TatraFormat.elevation(route.descentMeters),
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            if (
                route.minElevationMeters !=
                null ||
                route.maxElevationMeters !=
                null
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            TatraSpacing.sm
                        )
                ) {

                    RouteMetricCard(
                        label =
                            "Najniżej",
                        value =
                            TatraFormat.elevation(route.minElevationMeters),
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    RouteMetricCard(
                        label =
                            "Najwyżej",
                        value =
                            TatraFormat.elevation(route.maxElevationMeters),
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }

            if (
                state.startPoint !=
                null ||
                state.endPoint !=
                null ||
                state.waypoints
                    .isNotEmpty()
            ) {

                TatraSectionHeader(
                    title =
                        "Punkty trasy"
                )

                TatraCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        state.startPoint
                            ?.let {
                                    point ->

                                PlannedPointRow(
                                    icon =
                                        Icons
                                            .Outlined
                                            .Place,
                                    label =
                                        "Start",
                                    point =
                                        point
                                )
                            }

                        state.waypoints
                            .forEachIndexed {
                                    index,
                                    point ->

                                PlannedPointRow(
                                    icon =
                                        Icons
                                            .Outlined
                                            .Flag,
                                    label =
                                        "Przystanek ${index + 1}",
                                    point =
                                        point
                                )
                            }

                        state.endPoint
                            ?.let {
                                    point ->

                                PlannedPointRow(
                                    icon =
                                        Icons
                                            .Outlined
                                            .Flag,
                                    label =
                                        "Cel",
                                    point =
                                        point,
                                    showDivider =
                                        false
                                )
                            }
                    }
                }
            }

            if (
                state.elevationPoints.size >=
                2
            ) {

                TatraSectionHeader(
                    title =
                        "Profil wysokościowy"
                )

                TatraCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    PlannedRouteElevationProfile(
                        points =
                            state.elevationPoints
                    )
                }
            }

            TatraSectionHeader(
                title =
                    "Akcje"
            )

            TatraPrimaryButton(
                onClick =
                    onEdit
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Edit,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Edytuj trasę",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            TatraSecondaryButton(
                onClick =
                    onExport
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .FileDownload,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Eksportuj GPX",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            TatraSecondaryButton(
                onClick =
                    onDuplicate
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .ContentCopy,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Duplikuj trasę",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            TatraSecondaryButton(
                onClick =
                    onDelete,
                contentColor =
                    TatraDanger,
                borderColor =
                    TatraDanger.copy(
                        alpha =
                            0.35f
                    )
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .DeleteOutline,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        "Usuń trasę",
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.xl
                    )
            )
        }
    }
}

@Composable
private fun RouteMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    TatraCard(
        modifier =
            modifier,
        backgroundColor =
            TatraGreenVerySoft
    ) {

        Column {

            Text(
                text =
                    label,
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    value,
                color =
                    TatraDark,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                modifier =
                    Modifier.padding(
                        top =
                            4.dp
                    )
            )
        }
    }
}

@Composable
private fun PlannedPointRow(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    point:
    PlannedRoutePointEntity,
    showDivider: Boolean = true
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical =
                            TatraSpacing.sm
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    TatraGreen
            )

            Column(
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start =
                                TatraSpacing.sm
                        )
            ) {

                Text(
                    text =
                        label,
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Text(
                    text =
                        point.name
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: formatCoordinates(
                                point
                            ),
                    color =
                        TatraDark,
                    style =
                        MaterialTheme
                            .typography
                            .titleSmall,
                    modifier =
                        Modifier.padding(
                            top =
                                2.dp
                        )
                )
            }
        }

        if (
            showDivider
        ) {

            HorizontalDivider(
                color =
                    TatraOutline
            )
        }
    }
}

private fun routeDirection(
    route:
    PlannedRouteEntity
): String {

    val start =
        route.startName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Start"

    val end =
        route.endName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Cel"

    return "$start → $end"
}

private fun formatPlannedRouteDate(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd MMMM yyyy • HH:mm",
            Locale.getDefault()
        )

    return formatter.format(
        Date(
            timestamp
        )
    )
}

private fun formatCoordinates(
    point:
    PlannedRoutePointEntity
): String {

    return String.format(
        Locale.getDefault(),
        "%.5f, %.5f",
        point.latitude,
        point.longitude
    )
}