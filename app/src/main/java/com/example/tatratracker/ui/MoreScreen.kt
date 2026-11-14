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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraScreenHeader
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun MoreScreen(
    onMyRoutesClick: () -> Unit,
    onCompletedTrailsClick: () -> Unit,
    onSavedPlacesClick: () -> Unit,
    onOfflineMapsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDataExportClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraScreenHeader(
            title = "Więcej",
            subtitle = "Trasy, dane i ustawienia TatraTracker"
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
                        text = "Twoje centrum TatraTracker",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )

                    Text(
                        text = "Tutaj znajdziesz zapisane trasy, postęp na szlakach, dane offline, eksport i ustawienia aplikacji.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.xxs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Tatry i trasy")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Moje trasy",
                        subtitle = "Zapisane i zaplanowane przejścia",
                        icon = Icons.Outlined.Route,
                        onClick = onMyRoutesClick
                    )

                    TatraListRow(
                        title = "Ukończone szlaki",
                        subtitle = "Postęp na tatrzańskiej sieci szlaków",
                        icon = Icons.Outlined.Map,
                        onClick = onCompletedTrailsClick
                    )

                    TatraListRow(
                        title = "Schroniska i punkty",
                        subtitle = "Ulubione miejsca i zapisane POI",
                        icon = Icons.Outlined.Place,
                        onClick = onSavedPlacesClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Offline")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "Mapy offline",
                    subtitle = "Pobieranie obszarów przed wyjściem w góry",
                    icon = Icons.Outlined.DownloadForOffline,
                    badge = {
                        TatraStatusBadge(
                            text = "Wkrótce",
                            style = TatraBadgeStyle.Info
                        )
                    },
                    onClick = onOfflineMapsClick
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Dane i aplikacja")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Ustawienia",
                        subtitle = "Jednostki, mapa i zachowanie aplikacji",
                        icon = Icons.Outlined.Settings,
                        badge = {
                            TatraStatusBadge(
                                text = "Podgląd",
                                style = TatraBadgeStyle.Neutral
                            )
                        },
                        onClick = onSettingsClick
                    )

                    TatraListRow(
                        title = "Eksport danych",
                        subtitle = "GPX, GeoJSON i statystyki CSV",
                        icon = Icons.Outlined.FileUpload,
                        badge = {
                            TatraStatusBadge(
                                text = "Wkrótce",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = onDataExportClick
                    )

                    TatraListRow(
                        title = "Kopia zapasowa",
                        subtitle = "Zabezpieczenie lokalnych danych aplikacji",
                        icon = Icons.Outlined.Backup,
                        badge = {
                            TatraStatusBadge(
                                text = "Wkrótce",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = onBackupClick
                    )

                    TatraListRow(
                        title = "O aplikacji",
                        subtitle = "Wersja, źródła map i używane dane",
                        icon = Icons.Outlined.Info,
                        onClick = onAboutClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}