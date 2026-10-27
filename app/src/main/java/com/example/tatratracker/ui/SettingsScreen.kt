package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var metricUnits by remember {
        mutableStateOf(true)
    }

    var showTrailLabels by remember {
        mutableStateOf(true)
    }

    var showPoi by remember {
        mutableStateOf(true)
    }

    var autoFitRoute by remember {
        mutableStateOf(true)
    }

    var selectedMapStyle by remember {
        mutableStateOf("Landscape")
    }

    var selectedRoutingProfile by remember {
        mutableStateOf("Najkrótsza trasa piesza")
    }

    var selectedElevationSampling by remember {
        mutableStateOf("Około 100 m")
    }

    var choiceDialog by remember {
        mutableStateOf<SettingsChoiceDialog?>(null)
    }

    choiceDialog?.let { dialog ->
        SettingsChoiceDialogContent(
            dialog = dialog,
            onSelected = { selected ->
                when (dialog.type) {
                    SettingsChoiceType.MAP_STYLE ->
                        selectedMapStyle = selected

                    SettingsChoiceType.ROUTING ->
                        selectedRoutingProfile = selected

                    SettingsChoiceType.ELEVATION ->
                        selectedElevationSampling = selected
                }

                choiceDialog = null
            },
            onDismiss = {
                choiceDialog = null
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
            title = "Ustawienia",
            subtitle = "Jednostki, mapa i zachowanie aplikacji",
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
                Column {
                    TatraStatusBadge(
                        text = "Podgląd ustawień",
                        style = TatraBadgeStyle.Info
                    )

                    Text(
                        text = "W tym etapie możesz ustawić docelowy wygląd i zachowanie aplikacji. Zapis preferencji oraz ich podpięcie do mapy zrobimy później.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Jednostki")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitchRow(
                    title = "Jednostki metryczne",
                    subtitle =
                        if (metricUnits) {
                            "Kilometry, metry i km/h"
                        } else {
                            "Mile, stopy i mph — podgląd"
                        },
                    checked = metricUnits,
                    onCheckedChange = {
                        metricUnits = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Mapa")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Styl mapy",
                        subtitle = selectedMapStyle,
                        icon = Icons.Outlined.Layers,
                        badge = {
                            TatraStatusBadge(
                                text = "Podgląd",
                                style = TatraBadgeStyle.Neutral
                            )
                        },
                        onClick = {
                            choiceDialog =
                                SettingsChoiceDialog(
                                    type = SettingsChoiceType.MAP_STYLE,
                                    title = "Styl mapy",
                                    selected = selectedMapStyle,
                                    options =
                                        listOf(
                                            "Landscape",
                                            "Outdoor",
                                            "Jasny podstawowy"
                                        )
                                )
                        }
                    )

                    SettingSwitchRow(
                        title = "Etykiety szlaków",
                        subtitle = "Pokazuj oznaczenia i podpisy tras",
                        checked = showTrailLabels,
                        onCheckedChange = {
                            showTrailLabels = it
                        }
                    )

                    SettingSwitchRow(
                        title = "Punkty POI",
                        subtitle = "Schroniska, szczyty, jaskinie i woda",
                        checked = showPoi,
                        onCheckedChange = {
                            showPoi = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Planowanie trasy")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingSwitchRow(
                        title = "Dopasuj kamerę po wyznaczeniu",
                        subtitle = "Pokaż automatycznie całą gotową trasę",
                        checked = autoFitRoute,
                        onCheckedChange = {
                            autoFitRoute = it
                        }
                    )

                    TatraListRow(
                        title = "Profil routingu",
                        subtitle = selectedRoutingProfile,
                        icon = Icons.Outlined.Route,
                        badge = {
                            TatraStatusBadge(
                                text = "Podgląd",
                                style = TatraBadgeStyle.Neutral
                            )
                        },
                        onClick = {
                            choiceDialog =
                                SettingsChoiceDialog(
                                    type = SettingsChoiceType.ROUTING,
                                    title = "Profil routingu",
                                    selected = selectedRoutingProfile,
                                    options =
                                        listOf(
                                            "Najkrótsza trasa piesza",
                                            "Standardowa trasa piesza",
                                            "Preferuj szlaki"
                                        )
                                )
                        }
                    )

                    TatraListRow(
                        title = "Próbkowanie wysokości",
                        subtitle = selectedElevationSampling,
                        icon = Icons.Outlined.Landscape,
                        badge = {
                            TatraStatusBadge(
                                text = "Podgląd",
                                style = TatraBadgeStyle.Neutral
                            )
                        },
                        onClick = {
                            choiceDialog =
                                SettingsChoiceDialog(
                                    type = SettingsChoiceType.ELEVATION,
                                    title = "Próbkowanie wysokości",
                                    selected = selectedElevationSampling,
                                    options =
                                        listOf(
                                            "Około 50 m",
                                            "Około 100 m",
                                            "Około 200 m"
                                        )
                                )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Aplikacja")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "Ustawienia zaawansowane",
                    subtitle = "Cache, diagnostyka i źródła danych",
                    icon = Icons.Outlined.Settings,
                    badge = {
                        TatraStatusBadge(
                            text = "W późniejszym etapie",
                            style = TatraBadgeStyle.Info
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = TatraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = TatraDark,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = subtitle,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = TatraSpacing.xxs)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsChoiceDialogContent(
    dialog: SettingsChoiceDialog,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(dialog.title)
        },
        text = {
            Column {
                dialog.options.forEach { option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option)
                                }
                                .padding(vertical = TatraSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == dialog.selected,
                            onClick = {
                                onSelected(option)
                            }
                        )

                        Text(
                            text = option,
                            modifier = Modifier.padding(start = TatraSpacing.xs)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

private data class SettingsChoiceDialog(
    val type: SettingsChoiceType,
    val title: String,
    val selected: String,
    val options: List<String>
)

private enum class SettingsChoiceType {
    MAP_STYLE,
    ROUTING,
    ELEVATION
}