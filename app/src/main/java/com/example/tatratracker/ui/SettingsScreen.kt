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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.AppSettings
import com.example.tatratracker.data.ElevationSampling
import com.example.tatratracker.data.RoutingProfile
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val current = settings
    if (current != null) {
        when (openDialog) {
            SettingsDialog.ROUTING -> SettingsChoiceDialog(
                title = "Profil routingu",
                description = null,
                options = ROUTING_OPTIONS,
                selected = current.routingProfile,
                onSelected = { profile ->
                    viewModel.setRoutingProfile(profile)
                    openDialog = null
                },
                onDismiss = { openDialog = null }
            )

            SettingsDialog.ELEVATION -> SettingsChoiceDialog(
                title = "Próbkowanie wysokości",
                description = ELEVATION_SAMPLING_HINT,
                options = ELEVATION_OPTIONS,
                selected = current.elevationSampling,
                onSelected = { sampling ->
                    viewModel.setElevationSampling(sampling)
                    openDialog = null
                },
                onDismiss = { openDialog = null }
            )

            null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TatraBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "Ustawienia",
            subtitle = "Planowanie trasy i zachowanie mapy",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TatraSpacing.lg)
        ) {
            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "Warstwy mapy włączysz na mapie, przyciskiem Warstwy.",
                    icon = Icons.Outlined.Layers
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Planowanie trasy")

            // Do końca pierwszego odczytu nie pokazujemy wartości, żeby przełącznik nie mignął.
            if (current != null) {
                RoutePlanningSettings(
                    settings = current,
                    onAutoFitRouteChange = viewModel::setAutoFitRoute,
                    onRoutingClick = { openDialog = SettingsDialog.ROUTING },
                    onElevationClick = { openDialog = SettingsDialog.ELEVATION }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}

@Composable
private fun RoutePlanningSettings(
    settings: AppSettings,
    onAutoFitRouteChange: (Boolean) -> Unit,
    onRoutingClick: () -> Unit,
    onElevationClick: () -> Unit
) {
    TatraCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingSwitchRow(
                title = "Dopasuj kamerę po wyznaczeniu",
                subtitle = "Pokaż automatycznie całą gotową trasę",
                checked = settings.autoFitRoute,
                onCheckedChange = onAutoFitRouteChange
            )

            TatraListRow(
                title = "Profil routingu",
                subtitle = ROUTING_OPTIONS.labelOf(settings.routingProfile),
                icon = Icons.Outlined.Route,
                onClick = onRoutingClick
            )

            TatraListRow(
                title = "Próbkowanie wysokości",
                subtitle = ELEVATION_OPTIONS.labelOf(settings.elevationSampling),
                icon = Icons.Outlined.Landscape,
                onClick = onElevationClick
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TatraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
private fun <T> SettingsChoiceDialog(
    title: String,
    description: String?,
    options: List<SettingsChoice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (description != null) {
                    Text(
                        text = description,
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = TatraSpacing.sm)
                    )
                }

                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.value) }
                            .padding(vertical = TatraSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.value == selected,
                            onClick = { onSelected(option.value) }
                        )

                        Column(modifier = Modifier.padding(start = TatraSpacing.xs)) {
                            Text(text = option.label)

                            if (option.description != null) {
                                Text(
                                    text = option.description,
                                    color = TatraTextMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = TatraSpacing.xxs)
                                )
                            }
                        }
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

private enum class SettingsDialog {
    ROUTING,
    ELEVATION
}

private data class SettingsChoice<T>(
    val value: T,
    val label: String,
    val description: String? = null
)

private fun <T> List<SettingsChoice<T>>.labelOf(value: T): String {
    return first { it.value == value }.label
}

private val ROUTING_OPTIONS = listOf(
    SettingsChoice(
        value = RoutingProfile.SHORTEST,
        label = "Najkrótsza trasa piesza",
        description = "Wybiera możliwie najkrótszą drogę, bez względu na rodzaj ścieżki."
    ),
    SettingsChoice(
        value = RoutingProfile.STANDARD,
        label = "Standardowa trasa piesza",
        description = "Bierze pod uwagę rodzaj drogi, więc może wybrać nieco dłuższą, " +
            "ale wygodniejszą trasę."
    )
)

private val ELEVATION_OPTIONS = listOf(
    SettingsChoice(value = ElevationSampling.METERS_50, label = "Około 50 m"),
    SettingsChoice(value = ElevationSampling.METERS_100, label = "Około 100 m"),
    SettingsChoice(value = ElevationSampling.METERS_200, label = "Około 200 m")
)

private const val ELEVATION_SAMPLING_HINT =
    "Gęstsze próbkowanie daje dokładniejszy profil, ale wolniej się liczy."
