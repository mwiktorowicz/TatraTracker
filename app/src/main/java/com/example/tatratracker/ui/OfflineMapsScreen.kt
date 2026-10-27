package com.example.tatratracker.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
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
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun OfflineMapsScreen(
    onBack: () -> Unit
) {
    var selectedArea by remember {
        mutableStateOf<String?>(null)
    }

    selectedArea?.let { area ->
        TatraFeatureDialog(
            title = area,
            message = "Pobieranie tego obszaru zostanie podłączone później. Ekran jest już przygotowany pod pobieranie, aktualizację i usuwanie lokalnych danych mapowych.",
            onDismiss = {
                selectedArea = null
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
            title = "Mapy offline",
            subtitle = "Przygotuj mapę przed wyjściem w góry",
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
                Row {
                    Icon(
                        imageVector = Icons.Outlined.DownloadForOffline,
                        contentDescription = null,
                        tint = TatraGreen
                    )

                    Column(
                        modifier = Modifier.padding(start = TatraSpacing.sm)
                    ) {
                        Text(
                            text = "Tryb offline",
                            color = TatraDark,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Docelowo pobierzesz mapę bazową, szlaki, poziomice i punkty POI przed wyjściem poza zasięg.",
                            color = TatraTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = TatraSpacing.xxs)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Obszary")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Tatry Polskie",
                        subtitle = "Mapa, szlaki, poziomice i POI",
                        icon = Icons.Outlined.Map,
                        badge = {
                            TatraStatusBadge(
                                text = "Pobierz",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = {
                            selectedArea = "Tatry Polskie"
                        }
                    )

                    TatraListRow(
                        title = "Tatry Słowackie",
                        subtitle = "Mapa, szlaki, poziomice i POI",
                        icon = Icons.Outlined.Map,
                        badge = {
                            TatraStatusBadge(
                                text = "Pobierz",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = {
                            selectedArea = "Tatry Słowackie"
                        }
                    )

                    TatraListRow(
                        title = "Całe Tatry",
                        subtitle = "Połączony obszar polski i słowacki",
                        icon = Icons.Outlined.Map,
                        badge = {
                            TatraStatusBadge(
                                text = "Pobierz",
                                style = TatraBadgeStyle.Info
                            )
                        },
                        onClick = {
                            selectedArea = "Całe Tatry"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Pamięć urządzenia")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TatraListRow(
                        title = "Pobrane obszary",
                        subtitle = "Brak pobranych map",
                        icon = Icons.Outlined.DownloadForOffline
                    )

                    TatraListRow(
                        title = "Automatyczne aktualizacje",
                        subtitle = "Sprawdzanie nowszych danych będzie dostępne później",
                        icon = Icons.Outlined.DownloadForOffline,
                        badge = {
                            TatraStatusBadge(
                                text = "Wkrótce",
                                style = TatraBadgeStyle.Neutral
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}