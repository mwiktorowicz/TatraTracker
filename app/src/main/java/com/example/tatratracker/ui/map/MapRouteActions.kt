package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
internal fun MapRouteQuickActions(
    modifier: Modifier = Modifier,
    saveText: String = "Zapisz",
    onSaveClick: () -> Unit,
    onMoreClick: () -> Unit
) {

    Surface(
        modifier =
            modifier,
        color =
            TatraSurface,
        shape =
            RoundedCornerShape(
                18.dp
            ),
        shadowElevation =
            5.dp
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal = 6.dp,
                    vertical = 4.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            TextButton(
                onClick =
                    onSaveClick
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Save,
                    contentDescription =
                        null,
                    tint =
                        TatraGreen
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )

                Text(
                    text =
                        saveText,
                    color =
                        TatraDark
                )
            }

            IconButton(
                onClick =
                    onMoreClick
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.MoreVert,
                    contentDescription =
                        "Więcej akcji trasy",
                    tint =
                        TatraDark
                )
            }
        }
    }
}

@Composable
internal fun SavePlannedRouteDialog(
    route: PlannedRoute,
    elevationProfile: RouteElevationProfile?,
    suggestedName: String,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {

    var routeName by
    remember(
        suggestedName
    ) {
        mutableStateOf(
            suggestedName
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {

            Text(
                if (
                    isEditing
                ) {
                    "Zapisz zmiany"
                } else {
                    "Zapisz trasę"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                OutlinedTextField(
                    value =
                        routeName,
                    onValueChange = {
                        routeName =
                            it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true,
                    label = {
                        Text(
                            "Nazwa trasy"
                        )
                    },
                    placeholder = {
                        Text(
                            "Np. Kuźnice – Giewont"
                        )
                    }
                )

                TatraCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    backgroundColor =
                        TatraGreenVerySoft
                ) {

                    Column {

                        RouteSummaryLine(
                            label =
                                "Dystans",
                            value =
                                TatraFormat.distance(route.distanceKm * 1000.0)
                        )

                        RouteSummaryLine(
                            label =
                                "Szacowany czas",
                            value =
                                TatraFormat.duration(
                                    route.durationSeconds.takeIf { it > 0 }
                                )
                        )

                        elevationProfile
                            ?.let {
                                    profile ->

                                RouteSummaryLine(
                                    label =
                                        "Podejście",
                                    value =
                                        TatraFormat.elevation(profile.totalAscentMeters)
                                )

                                RouteSummaryLine(
                                    label =
                                        "Zejście",
                                    value =
                                        TatraFormat.elevation(profile.totalDescentMeters)
                                )
                            }
                    }
                }

                Text(
                    text =
                        if (
                            isEditing
                        ) {
                            "Zmiany zastąpią poprzedni przebieg zapisanej trasy."
                        } else {
                            "Trasa zostanie zapisana lokalnie w TatraTracker."
                        },
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        },
        confirmButton = {

            TextButton(
                onClick = {

                    val cleanName =
                        routeName.trim()

                    if (
                        cleanName.isNotEmpty()
                    ) {

                        onSave(
                            cleanName
                        )
                    }
                },
                enabled =
                    routeName
                        .trim()
                        .isNotEmpty()
            ) {

                Text(
                    if (
                        isEditing
                    ) {
                        "Zapisz zmiany"
                    } else {
                        "Zapisz"
                    }
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Anuluj"
                )
            }
        }
    )
}

@Composable
internal fun PlannedRouteActionsDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onExportGpx: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "Akcje trasy"
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                TatraSecondaryButton(
                    onClick =
                        onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Edit,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Edytuj punkty trasy"
                    )
                }

                TatraSecondaryButton(
                    onClick =
                        onExportGpx
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .FileDownload,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Eksportuj GPX"
                    )
                }

                TatraSecondaryButton(
                    onClick =
                        onDuplicate
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .ContentCopy,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Duplikuj trasę"
                    )
                }

                TatraSecondaryButton(
                    onClick =
                        onDelete,
                    containerColor =
                        androidx.compose.ui.graphics.Color(
                            0xFFF7E6E3
                        ),
                    contentColor =
                        androidx.compose.ui.graphics.Color(
                            0xFFC8473E
                        ),
                    borderColor =
                        androidx.compose.ui.graphics.Color(
                            0xFFC8473E
                        ).copy(
                            alpha =
                                0.35f
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .DeleteOutline,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Usuń z mapy"
                    )
                }
            }
        },
        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Zamknij"
                )
            }
        }
    )
}

@Composable
private fun RouteSummaryLine(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        4.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                label,
            color =
                TatraTextMuted,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            modifier =
                Modifier.weight(
                    1f
                )
        )

        Text(
            text =
                value,
            color =
                TatraDark,
            style =
                MaterialTheme
                    .typography
                    .titleSmall
        )
    }
}