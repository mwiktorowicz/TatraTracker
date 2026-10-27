package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
internal fun MapTrailToolsPanel(
    settings: MapLayerSettings,
    onSettingsChange: (MapLayerSettings) -> Unit,
    onCompletedTrailsClick: () -> Unit,
    onImportGpxClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(282.dp),
        color = TatraSurface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Szlaki i ślady",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Widoczność i przyszły postęp",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Zamknij",
                        tint = TatraDark
                    )
                }
            }

            TatraListRow(
                title = "Sieć szlaków",
                subtitle = "Kolorowe trasy turystyczne",
                icon = Icons.Outlined.Terrain,
                badge = {
                    Switch(
                        checked = settings.trails,
                        onCheckedChange = {
                            onSettingsChange(
                                settings.copy(trails = it)
                            )
                        }
                    )
                },
                onClick = {
                    onSettingsChange(
                        settings.copy(
                            trails = !settings.trails
                        )
                    )
                }
            )

            TatraListRow(
                title = "Moje ślady GPS",
                subtitle = "Zaimportowane aktywności",
                icon = Icons.Outlined.Route,
                badge = {
                    Switch(
                        checked = settings.gpsTracks,
                        onCheckedChange = {
                            onSettingsChange(
                                settings.copy(gpsTracks = it)
                            )
                        }
                    )
                },
                onClick = {
                    onSettingsChange(
                        settings.copy(
                            gpsTracks = !settings.gpsTracks
                        )
                    )
                }
            )

            TatraListRow(
                title = "Ukończone szlaki",
                subtitle = "Postęp i zaliczone odcinki",
                icon = Icons.Outlined.Route,
                badge = {
                    TatraStatusBadge(
                        text = "Wkrótce",
                        style = TatraBadgeStyle.Info
                    )
                },
                onClick = onCompletedTrailsClick
            )

            TatraListRow(
                title = "Importuj GPX",
                subtitle = "Dodaj ślad lub trasę z pliku",
                icon = Icons.Outlined.FileOpen,
                badge = {
                    TatraStatusBadge(
                        text = ".gpx",
                        style = TatraBadgeStyle.Neutral
                    )
                },
                onClick = onImportGpxClick
            )
        }
    }
}