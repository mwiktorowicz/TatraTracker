package com.example.tatratracker.ui

import android.net.Uri
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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.gpx.GpxPlannedRouteImportResult
import com.example.tatratracker.ui.components.PlannedRouteCard
import com.example.tatratracker.ui.components.TatraEmptyState
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraLoadingState
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatCard
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.viewmodel.GpxRouteImportOutcome
import com.example.tatratracker.viewmodel.PlannedRoutesViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MyRoutesScreen(
    onBack: () -> Unit,
    onPlanRoute: () -> Unit,
    onRouteClick: (Long) -> Unit,
    viewModel:
    PlannedRoutesViewModel =
        viewModel()
) {

    val scope =
        rememberCoroutineScope()

    val routes by
    viewModel
        .routes
        .collectAsState()

    var isImportingGpx by
    remember {
        mutableStateOf(
            false
        )
    }

    var dialogContent by
    remember {
        mutableStateOf<
                Pair<String, String>?
                >(
            null
        )
    }

    var routeToDelete by
    remember {
        mutableStateOf<
                PlannedRouteEntity?
                >(
            null
        )
    }

    val gpxPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) {
                uri: Uri? ->

            if (
                uri ==
                null
            ) {

                return@rememberLauncherForActivityResult
            }

            scope.launch {
                isImportingGpx = true

                dialogContent =
                    when (val outcome = viewModel.importGpxRoute(uri)) {
                        is GpxRouteImportOutcome.Imported ->
                            "Trasa GPX zaimportowana" to importedRouteSummary(outcome.details)

                        is GpxRouteImportOutcome.Failed ->
                            outcome.title to outcome.message
                    }

                isImportingGpx = false
            }
        }

    dialogContent
        ?.let {
                dialog ->

            TatraFeatureDialog(
                title =
                    dialog.first,
                message =
                    dialog.second,
                onDismiss = {

                    dialogContent =
                        null
                }
            )
        }

    routeToDelete
        ?.let {
                route ->

            AlertDialog(
                onDismissRequest = {

                    routeToDelete =
                        null
                },
                title = {

                    Text(
                        "Usunąć trasę?"
                    )
                },
                text = {

                    Text(
                        "Trasa „${route.name}” zostanie trwale usunięta razem z zapisanymi punktami."
                    )
                },
                confirmButton = {

                    TextButton(
                        onClick = {

                            viewModel.deleteRoute(
                                route.id
                            )

                            routeToDelete =
                                null
                        }
                    ) {

                        Text(
                            "Usuń"
                        )
                    }
                },
                dismissButton = {

                    TextButton(
                        onClick = {

                            routeToDelete =
                                null
                        }
                    ) {

                        Text(
                            "Anuluj"
                        )
                    }
                }
            )
        }

    val totalDistanceKm =
        routes.sumOf {
            it.distanceMeters
        } /
                1000.0

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    TatraBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        TatraSubScreenHeader(
            title =
                "Moje trasy",
            subtitle =
                if (
                    routes.isEmpty()
                ) {

                    "Zapisane i importowane trasy"

                } else {

                    "${routes.size} zapisanych tras"
                },
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

                TatraStatCard(
                    label =
                        "Zapisane",
                    value =
                        routes
                            .size
                            .toString(),
                    helper =
                        "Trasy",
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                TatraStatCard(
                    label =
                        "Dystans",
                    value =
                        if (
                            routes.isEmpty()
                        ) {

                            "0 km"

                        } else {

                            String.format(
                                Locale.getDefault(),
                                "%.1f km",
                                totalDistanceKm
                            )
                        },
                    helper =
                        "Łącznie",
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            if (
                routes.isEmpty()
            ) {

                TatraEmptyState(
                    title =
                        "Nie masz jeszcze zapisanych tras",
                    message =
                        "Zaplanuj trasę na mapie albo zaimportuj gotowy plik GPX.",
                    icon =
                        Icons.Outlined.Route,
                    actionText =
                        "Zaplanuj trasę",
                    onActionClick =
                        onPlanRoute
                )

            } else {

                TatraSectionHeader(
                    title =
                        "Zapisane trasy"
                )

                routes.forEach {
                        route ->

                    PlannedRouteCard(
                        route =
                            route,
                        onClick = {

                            onRouteClick(
                                route.id
                            )
                        },
                        onDeleteClick = {

                            routeToDelete =
                                route
                        }
                    )
                }
            }

            TatraSectionHeader(
                title =
                    "Import"
            )

            TatraSecondaryButton(
                onClick = {

                    gpxPicker.launch(
                        arrayOf(
                            "*/*"
                        )
                    )
                },
                enabled =
                    !isImportingGpx
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .FileOpen,
                    contentDescription =
                        null
                )

                Text(
                    text =
                        if (
                            isImportingGpx
                        ) {

                            "Importowanie GPX..."

                        } else {

                            "Importuj trasę GPX"
                        },
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.xs
                        )
                )
            }

            if (
                isImportingGpx
            ) {

                TatraLoadingState(
                    message =
                        "Analizowanie i zapisywanie trasy..."
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.md
                    )
            )
        }
    }
}

/**
 * Szczegóły zaimportowanej trasy pokazywane na ekranie „Moje trasy”.
 */
private fun importedRouteSummary(imported: GpxPlannedRouteImportResult): String {
    val draft = imported.draft
    val distanceKm = draft.distanceMeters / 1000.0

    return buildString {
        append("„${draft.name}” została zapisana w Moich trasach.")
        append("\n\nDystans: ")
        append(String.format(Locale.getDefault(), "%.2f km", distanceKm))
        append("\nPunkty geometrii: ${imported.geometryPointCount}")

        if (imported.waypointCount > 0) {
            append("\nPrzystanki: ${imported.waypointCount}")
        }

        if (draft.minElevationMeters != null && draft.maxElevationMeters != null) {
            append("\nWysokość: ${draft.minElevationMeters}–${draft.maxElevationMeters} m")
        }
    }
}