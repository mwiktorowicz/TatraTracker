package com.example.tatratracker.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraTextMuted

/**
 * Jedyny panel warstw mapy: przełączniki warstw i akcje.
 * Każdy przełącznik występuje tu dokładnie raz.
 */
@Composable
internal fun MapLayersPanel(
    settings: MapLayerSettings,
    onSettingsChange: (MapLayerSettings) -> Unit,
    onAddOwnPlace: () -> Unit,
    onImportGpx: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(250.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 10.dp
    ) {
        Column {
            PanelHeader(onClose = onClose)

            HorizontalDivider()

            // weight(fill = false): panel ma naturalną wysokość, a gdy brakuje miejsca – przewija się.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                SectionTitle("Mapa")

                LayerSwitchRow(
                    title = "Szlaki",
                    checked = settings.trails,
                    onCheckedChange = { onSettingsChange(settings.copy(trails = it)) }
                )
                LayerSwitchRow(
                    title = "Poziomice",
                    checked = settings.contours,
                    onCheckedChange = { onSettingsChange(settings.copy(contours = it)) }
                )
                LayerSwitchRow(
                    title = "Moje ślady GPS",
                    checked = settings.gpsTracks,
                    onCheckedChange = { onSettingsChange(settings.copy(gpsTracks = it)) }
                )

                SectionDivider()
                SectionTitle("Punkty")

                LayerSwitchRow(
                    title = "Szczyty",
                    checked = settings.peaks,
                    onCheckedChange = { onSettingsChange(settings.copy(peaks = it)) }
                )
                LayerSwitchRow(
                    title = "Schroniska",
                    checked = settings.huts,
                    onCheckedChange = { onSettingsChange(settings.copy(huts = it)) }
                )
                LayerSwitchRow(
                    title = "Jaskinie",
                    checked = settings.caves,
                    onCheckedChange = { onSettingsChange(settings.copy(caves = it)) }
                )
                LayerSwitchRow(
                    title = "Jeziora i stawy",
                    checked = settings.water,
                    onCheckedChange = { onSettingsChange(settings.copy(water = it)) }
                )

                SectionDivider()

                // Warstwa jeszcze nie istnieje (punkt 9 planu) – wiersz celowo nieaktywny.
                ComingSoonRow(title = "Ukończone szlaki")

                SectionDivider()
                SectionTitle("Akcje")

                ActionRow(
                    title = "Dodaj własny punkt",
                    icon = Icons.Outlined.AddLocationAlt,
                    onClick = onAddOwnPlace
                )
                ActionRow(
                    title = "Importuj trasę GPX",
                    subtitle = "Dodaj zaplanowaną trasę z pliku",
                    icon = Icons.Outlined.FileOpen,
                    onClick = onImportGpx
                )
            }
        }
    }
}

@Composable
private fun PanelHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 10.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Warstwy mapy",
            modifier = Modifier.weight(1f),
            color = TatraDark,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Zamknij",
                tint = TatraDark
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 2.dp),
        color = TatraTextMuted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp)
    )
}

@Composable
private fun LayerSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TatraDark,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ComingSoonRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TatraTextMuted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        TatraStatusBadge(
            text = "Wkrótce",
            style = TatraBadgeStyle.Info
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = TatraDark
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TatraDark,
                fontSize = 14.sp
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TatraTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
