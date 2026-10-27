package com.example.tatratracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenSoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlannedRouteCard(
    route: PlannedRouteEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val dateFormatter =
        SimpleDateFormat(
            "dd MMM yyyy • HH:mm",
            Locale.getDefault()
        )

    TatraCard(
        modifier =
            modifier.fillMaxWidth(),
        onClick =
            onClick
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    shape =
                        RoundedCornerShape(
                            13.dp
                        ),
                    color =
                        TatraGreenSoft
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.Route,
                        contentDescription =
                            null,
                        tint =
                            TatraGreen,
                        modifier =
                            Modifier.padding(
                                11.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                route.name,
                            color =
                                TatraDark,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        TatraStatusBadge(
                            text = "PLAN",
                            style =
                                TatraBadgeStyle.Success
                        )
                    }

                    Text(
                        text =
                            dateFormatter.format(
                                Date(
                                    route.createdAt
                                )
                            ),
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        modifier =
                            Modifier.padding(
                                top = 3.dp
                            )
                    )
                }

                if (
                    onDeleteClick != null
                ) {

                    IconButton(
                        onClick =
                            onDeleteClick
                    ) {
                        Icon(
                            imageVector =
                                Icons
                                    .Outlined
                                    .DeleteOutline,
                            contentDescription =
                                "Usuń trasę",
                            tint =
                                TatraTextMuted
                        )
                    }

                } else if (
                    onClick != null
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .KeyboardArrowRight,
                        contentDescription =
                            null,
                        tint =
                            TatraTextMuted
                    )
                }
            }

            if (
                !route.startName.isNullOrBlank() ||
                !route.endName.isNullOrBlank()
            ) {

                Text(
                    text =
                        routeDirectionText(
                            route
                        ),
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    modifier =
                        Modifier.padding(
                            top =
                                TatraSpacing.sm
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        TatraSpacing.md
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        TatraSpacing.sm
                    )
            ) {
                PlannedMetric(
                    label = "Dystans",
                    value =
                        String.format(
                            Locale.getDefault(),
                            "%.2f km",
                            route.distanceMeters /
                                    1000.0
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                PlannedMetric(
                    label = "Czas",
                    value =
                        formatDuration(
                            route.durationSeconds
                        ),
                    modifier =
                        Modifier.weight(1f)
                )

                PlannedMetric(
                    label = "Podejście",
                    value =
                        route.ascentMeters
                            ?.let {
                                "$it m"
                            }
                            ?: "—",
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlannedMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
    ) {
        Text(
            text =
                label,
            color =
                TatraTextMuted,
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        Text(
            text =
                value,
            color =
                TatraDark,
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            modifier =
                Modifier.padding(
                    top = 2.dp
                )
        )
    }
}

private fun routeDirectionText(
    route: PlannedRouteEntity
): String {

    val start =
        route.startName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Start"

    val end =
        route.endName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Cel"

    return "$start  →  $end"
}

private fun formatDuration(
    seconds: Int
): String {

    if (
        seconds <= 0
    ) {
        return "—"
    }

    val totalMinutes =
        seconds / 60

    val hours =
        totalMinutes / 60

    val minutes =
        totalMinutes % 60

    return when {

        hours <= 0 ->
            "$minutes min"

        minutes == 0 ->
            "$hours h"

        else ->
            "$hours h $minutes min"
    }
}