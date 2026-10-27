package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
internal fun MapPoiToolsPanel(
    settings: MapLayerSettings,
    onSettingsChange: (MapLayerSettings) -> Unit,
    onAddOwnPlace: () -> Unit,
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
                        text = "Punkty na mapie",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Wybierz kategorie POI",
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

            PoiSwitchRow(
                title = "Schroniska",
                icon = Icons.Outlined.Place,
                checked = settings.huts,
                onCheckedChange = {
                    onSettingsChange(
                        settings.copy(huts = it)
                    )
                }
            )

            PoiSwitchRow(
                title = "Szczyty",
                icon = Icons.Outlined.Landscape,
                checked = settings.peaks,
                onCheckedChange = {
                    onSettingsChange(
                        settings.copy(peaks = it)
                    )
                }
            )

            PoiSwitchRow(
                title = "Jaskinie",
                icon = Icons.Outlined.Public,
                checked = settings.caves,
                onCheckedChange = {
                    onSettingsChange(
                        settings.copy(caves = it)
                    )
                }
            )

            PoiSwitchRow(
                title = "Woda",
                icon = Icons.Outlined.WaterDrop,
                checked = settings.water,
                onCheckedChange = {
                    onSettingsChange(
                        settings.copy(water = it)
                    )
                }
            )

            Spacer(modifier = Modifier.padding(top = 4.dp))

            TatraSecondaryButton(
                onClick = onAddOwnPlace
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddLocationAlt,
                    contentDescription = null
                )

                Text(
                    text = "Dodaj własny punkt",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PoiSwitchRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    TatraListRow(
        title = title,
        icon = icon,
        badge = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        onClick = {
            onCheckedChange(!checked)
        }
    )
}

@Composable
internal fun AddOwnPlaceDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Dodaj własny punkt")
        },
        text = {
            Column {
                Text(
                    text = "Docelowo punkt zostanie zapisany w centrum aktualnego widoku mapy. Później dodamy również wybór dokładnej pozycji przez dotknięcie mapy.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    singleLine = true,
                    label = {
                        Text("Nazwa punktu")
                    },
                    placeholder = {
                        Text("Np. miejsce na odpoczynek")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        onSave(clean)
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}