package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun DataExportScreen(
    onBack: () -> Unit
) {
    var selectedExport by remember {
        mutableStateOf<String?>(null)
    }

    selectedExport?.let { exportName ->
        TatraFeatureDialog(
            title = exportName,
            message = "Interfejs eksportu jest już gotowy. Generowanie i zapisywanie pliku podepniemy w etapie operacji na danych.",
            onDismiss = {
                selectedExport = null
            }
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "Eksport danych",
            subtitle = "Przenieś swoje dane poza TatraTracker",
            onBack = onBack
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TatraSpacing.lg)
        ) {
            TatraCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = TatraGreenVerySoft
            ) {
                Text(
                    text = "Eksport nie zmienia ani nie usuwa danych zapisanych w aplikacji. Docelowo pliki będzie można zapisać na telefonie lub udostępnić do innej aplikacji.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Trasy GPS")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "GPX",
                        subtitle = "Ślad GPS do innych aplikacji i serwisów",
                        icon = Icons.Outlined.Map,
                        badge = {
                            TatraStatusBadge(
                                text = ".gpx",
                                style = TatraBadgeStyle.Success
                            )
                        },
                        onClick = {
                            selectedExport = "Eksport GPX"
                        }
                    )

                    TatraListRow(
                        title = "GeoJSON",
                        subtitle = "Geometria trasy i jej właściwości",
                        icon = Icons.Outlined.FileUpload,
                        badge = {
                            TatraStatusBadge(
                                text = ".geojson",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = {
                            selectedExport = "Eksport GeoJSON"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Aktywności i statystyki")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "CSV ze statystykami",
                    subtitle = "Daty, dystanse, czasy, wysokości, kalorie i puls",
                    icon = Icons.Outlined.BarChart,
                    badge = {
                        TatraStatusBadge(
                            text = ".csv",
                            style = TatraBadgeStyle.Neutral
                        )
                    },
                    onClick = {
                        selectedExport = "Eksport statystyk CSV"
                    }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Cała aplikacja")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "Pełny eksport TatraTracker",
                    subtitle = "Aktywności, trasy, postęp i ustawienia w jednym pakiecie",
                    icon = Icons.Outlined.FileUpload,
                    badge = {
                        TatraStatusBadge(
                            text = "W późniejszym etapie",
                            style = TatraBadgeStyle.Neutral
                        )
                    },
                    onClick = {
                        selectedExport = "Pełny eksport TatraTracker"
                    }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}