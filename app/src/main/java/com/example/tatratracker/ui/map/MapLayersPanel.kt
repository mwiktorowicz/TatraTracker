package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.ui.theme.TatraDark

@Composable
internal fun MapLayersPanel(
    settings: MapLayerSettings,
    onSettingsChange: (MapLayerSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier.width(
                250.dp
            ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        color =
            Color.White.copy(
                alpha = 0.97f
            ),

        shadowElevation =
            10.dp
    ) {

        Column {

            /*
             * =================================================
             * NAGŁÓWEK
             * =================================================
             */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            top = 10.dp,
                            end = 6.dp,
                            bottom = 6.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "Warstwy mapy",

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    color =
                        TatraDark,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        17.sp
                )

                IconButton(
                    onClick =
                        onClose
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Close,

                        contentDescription =
                            "Zamknij",

                        tint =
                            TatraDark
                    )
                }
            }

            HorizontalDivider()

            /*
             * =================================================
             * SZLAKI
             * =================================================
             */

            LayerSwitchRow(
                title =
                    "Szlaki",

                enabled =
                    settings.trails,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            trails = it
                        )
                    )
                }
            )

            /*
             * POZIOMICE
             */
            LayerSwitchRow(
                title =
                    "Poziomice",

                enabled =
                    settings.contours,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            contours = it
                        )
                    )
                }
            )

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp
                    )
            )

            /*
             * SZCZYTY
             */
            LayerSwitchRow(
                title =
                    "Szczyty",

                enabled =
                    settings.peaks,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            peaks = it
                        )
                    )
                }
            )

            /*
             * SCHRONISKA
             */
            LayerSwitchRow(
                title =
                    "Schroniska",

                enabled =
                    settings.huts,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            huts = it
                        )
                    )
                }
            )

            /*
             * JASKINIE
             */
            LayerSwitchRow(
                title =
                    "Jaskinie",

                enabled =
                    settings.caves,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            caves = it
                        )
                    )
                }
            )

            /*
             * JEZIORA
             */
            LayerSwitchRow(
                title =
                    "Jeziora i stawy",

                enabled =
                    settings.water,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            water = it
                        )
                    )
                }
            )

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp
                    )
            )

            /*
             * TRASY GPS
             */
            LayerSwitchRow(
                title =
                    "Moje trasy",

                enabled =
                    settings.gpsTracks,

                onEnabledChange = {

                    onSettingsChange(
                        settings.copy(
                            gpsTracks = it
                        )
                    )
                }
            )
        }
    }
}


@Composable
private fun LayerSwitchRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 10.dp,
                    top = 4.dp,
                    bottom = 4.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                title,

            modifier =
                Modifier.weight(
                    1f
                ),

            color =
                TatraDark,

            fontSize =
                14.sp
        )

        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )

        Switch(
            checked =
                enabled,

            onCheckedChange =
                onEnabledChange
        )
    }
}