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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tatratracker.BuildConfig
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "O aplikacji",
            subtitle = "TatraTracker ${BuildConfig.VERSION_NAME}",
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
                    Icon(
                        imageVector = Icons.Outlined.Landscape,
                        contentDescription = null,
                        tint = TatraGreen
                    )

                    Text(
                        text = "TatraTracker",
                        color = TatraDark,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )

                    Text(
                        text = "Twoje wędrówki. Twoja historia.",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = TatraSpacing.xxs)
                    )

                    Text(
                        text = "Planowanie tras, historia aktywności, statystyki oraz śledzenie postępu na tatrzańskich szlakach.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Źródła i technologie")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Mapa",
                        subtitle = "MapLibre Native + MapTiler",
                        icon = Icons.Outlined.Map
                    )

                    TatraListRow(
                        title = "Routing",
                        subtitle = "Valhalla + dane OpenStreetMap",
                        icon = Icons.Outlined.Route
                    )

                    TatraListRow(
                        title = "Wysokość",
                        subtitle = "Open-Meteo Elevation + Copernicus DEM",
                        icon = Icons.Outlined.Terrain
                    )

                    TatraListRow(
                        title = "Sieć szlaków",
                        subtitle = "OpenStreetMap + lokalny zbiór TatraTracker",
                        icon = Icons.Outlined.Info
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Wersja")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Wersja aplikacji",
                        subtitle = BuildConfig.VERSION_NAME,
                        icon = Icons.Outlined.Info,
                        badge = {
                            TatraStatusBadge(
                                text = "Build ${BuildConfig.VERSION_CODE}",
                                style = TatraBadgeStyle.Neutral
                            )
                        }
                    )

                    TatraListRow(
                        title = "Status projektu",
                        subtitle = "Wersja rozwojowa",
                        icon = Icons.Outlined.Landscape,
                        badge = {
                            TatraStatusBadge(
                                text = "DEV",
                                style = TatraBadgeStyle.Info
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}