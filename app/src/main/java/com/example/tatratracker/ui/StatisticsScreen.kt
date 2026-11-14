package com.example.tatratracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.South
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.TrailProgressState
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraEmptyState
import com.example.tatratracker.ui.components.TatraScreenHeader
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatCard
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenSoft
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraSurfaceStrong
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.StatisticsChartPoint
import com.example.tatratracker.viewmodel.StatisticsPeriod
import com.example.tatratracker.viewmodel.StatisticsUiState
import com.example.tatratracker.viewmodel.StatisticsViewModel
import java.util.Locale
import kotlin.math.max

@Composable
fun StatisticsScreen(
    onCompletedTrailsClick: () -> Unit,
    viewModel: StatisticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val trailProgress by viewModel.trailProgress.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraScreenHeader(
            title = "Statystyki",
            subtitle = "Podsumowanie Twoich wędrówek"
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TatraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.md)
        ) {
            StatisticsPeriodSelector(
                selected = state.period,
                onSelected = viewModel::setPeriod
            )

            if (!state.hasActivities) {
                TatraEmptyState(
                    title = "Brak aktywności w tym okresie",
                    message =
                        "Zmień zakres czasu albo zaimportuj aktywność, aby zobaczyć statystyki.",
                    icon = Icons.Outlined.TrendingUp
                )
            } else {
                StatisticsSummary(state)
                DistanceChartCard(state)
                AdditionalStatistics(state)
                MountainRecords(state)
            }

            CompletedTrailsProgressCard(
                progress = trailProgress,
                onClick = onCompletedTrailsClick
            )

            Spacer(modifier = Modifier.height(TatraSpacing.md))
        }
    }
}

@Composable
private fun StatisticsPeriodSelector(
    selected: StatisticsPeriod,
    onSelected: (StatisticsPeriod) -> Unit
) {
    Column {
        TatraSectionHeader(title = "Zakres")

        Spacer(modifier = Modifier.height(TatraSpacing.xs))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = TatraSurfaceStrong,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatisticsPeriod.entries.forEach { period ->
                val isSelected = period == selected

                Surface(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable {
                                onSelected(period)
                            },
                    shape = RoundedCornerShape(11.dp),
                    color =
                        if (isSelected) {
                            TatraSurface
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    shadowElevation =
                        if (isSelected) {
                            1.dp
                        } else {
                            0.dp
                        }
                ) {
                    Text(
                        text = period.label,
                        color =
                            if (isSelected) {
                                TatraDark
                            } else {
                                TatraTextMuted
                            },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight =
                            if (isSelected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Text(
            text = selected.helper,
            color = TatraTextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

@Composable
private fun StatisticsSummary(
    state: StatisticsUiState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
    ) {
        TatraSectionHeader(title = "Podsumowanie")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
        ) {
            TatraStatCard(
                label = "Dystans",
                value = formatDistance(state.distanceMeters),
                helper = "Łącznie",
                modifier = Modifier.weight(1f)
            )

            TatraStatCard(
                label = "Czas",
                value = formatDuration(state.durationMillis),
                helper = "W ruchu ${formatDuration(state.movingDurationMillis)}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
        ) {
            TatraStatCard(
                label = "Aktywności",
                value = state.activityCount.toString(),
                helper = "${state.activeDays} aktywnych dni",
                modifier = Modifier.weight(1f)
            )

            TatraStatCard(
                label = "Podejście",
                value = formatElevation(state.totalAscentMeters),
                helper = "Suma podejść",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DistanceChartCard(
    state: StatisticsUiState
) {
    Column {
        TatraSectionHeader(title = "Dystans w czasie")

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Aktywność w czasie",
                            color = TatraDark,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Dystans zarejestrowany w wybranym zakresie.",
                            color = TatraTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    TatraStatusBadge(
                        text = formatDistance(state.distanceMeters),
                        style = TatraBadgeStyle.Success
                    )
                }

                Spacer(modifier = Modifier.height(TatraSpacing.lg))

                StatisticsBarChart(
                    points = state.chartPoints,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                )
            }
        }
    }
}

@Composable
private fun StatisticsBarChart(
    points: List<StatisticsChartPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Brak danych do wykresu",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val barColor = TatraGreen
    val baselineColor = TatraOutline
    val maxDistance =
        max(
            points.maxOfOrNull { it.distanceKm } ?: 0.0,
            0.01
        )

    Column(
        modifier = modifier
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        ) {
            val baselineY = size.height - 2.dp.toPx()

            drawLine(
                color = baselineColor,
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            val slotWidth = size.width / points.size
            val barWidth =
                (slotWidth * 0.48f)
                    .coerceAtLeast(4.dp.toPx())
                    .coerceAtMost(24.dp.toPx())

            points.forEachIndexed { index, point ->
                val fraction =
                    (point.distanceKm / maxDistance)
                        .toFloat()
                        .coerceIn(0f, 1f)

                val usableHeight = size.height - 10.dp.toPx()
                val barHeight = usableHeight * fraction
                val left =
                    slotWidth * index +
                            (slotWidth - barWidth) / 2f

                drawRoundRect(
                    color = barColor,
                    topLeft =
                        Offset(
                            x = left,
                            y = baselineY - barHeight
                        ),
                    size =
                        Size(
                            width = barWidth,
                            height = barHeight.coerceAtLeast(2.dp.toPx())
                        ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            x = 5.dp.toPx(),
                            y = 5.dp.toPx()
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        ChartLabels(points)
    }
}

@Composable
private fun ChartLabels(
    points: List<StatisticsChartPoint>
) {
    val labels =
        when {
            points.size <= 7 ->
                points

            points.size <= 14 ->
                points.filterIndexed { index, _ ->
                    index % 2 == 0 || index == points.lastIndex
                }

            else ->
                points.filterIndexed { index, _ ->
                    index % 4 == 0 || index == points.lastIndex
                }
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { point ->
            Text(
                text = point.label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AdditionalStatistics(
    state: StatisticsUiState
) {
    Column {
        TatraSectionHeader(title = "Dodatkowe dane")

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticsDetailRow(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = "Kalorie",
                    value =
                        if (state.calories > 0.0) {
                            String.format(
                                Locale.getDefault(),
                                "%.0f kcal",
                                state.calories
                            )
                        } else {
                            "Brak danych"
                        }
                )

                StatisticsDetailRow(
                    icon = Icons.Outlined.South,
                    label = "Zejście",
                    value = formatElevation(state.totalDescentMeters)
                )

                StatisticsDetailRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Średni puls",
                    value =
                        state.averageHeartRate?.let {
                            "$it bpm"
                        } ?: "Brak danych"
                )

                StatisticsDetailRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Maksymalny puls",
                    value =
                        state.maxHeartRate?.let {
                            "$it bpm"
                        } ?: "Brak danych"
                )

                StatisticsDetailRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Aktywne dni",
                    value = state.activeDays.toString()
                )

                StatisticsDetailRow(
                    icon = Icons.Outlined.Route,
                    label = "Średni dystans wyjścia",
                    value = formatDistance(state.averageActivityDistanceMeters),
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun MountainRecords(
    state: StatisticsUiState
) {
    Column {
        TatraSectionHeader(title = "Rekordy")

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
        ) {
            RecordCard(
                icon = Icons.Outlined.Route,
                label = "Najdłuższa trasa",
                value = formatDistance(state.longestActivityDistanceMeters),
                helper = state.longestActivityName,
                modifier = Modifier.weight(1f)
            )

            RecordCard(
                icon = Icons.Outlined.Landscape,
                label = "Najwyższy punkt",
                value =
                    state.highestElevationMeters?.let {
                        formatElevation(it)
                    } ?: "—",
                helper = state.highestActivityName,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        RecordCard(
            icon = Icons.Outlined.Timer,
            label = "Najdłuższa aktywność",
            value = formatDuration(state.longestActivityDurationMillis),
            helper = "Najdłuższy czas pojedynczego wyjścia",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecordCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    helper: String?,
    modifier: Modifier = Modifier
) {
    TatraCard(
        modifier = modifier,
        backgroundColor = TatraGreenVerySoft
    ) {
        Column {
            Surface(
                color = TatraGreenSoft,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TatraGreen,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Text(
                text = label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp)
            )

            Text(
                text = value,
                color = TatraDark,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (!helper.isNullOrBlank()) {
                Text(
                    text = helper,
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletedTrailsProgressCard(
    progress: TrailProgressState,
    onClick: () -> Unit
) {
    // Te same wartości co na ekranie „Ukończone szlaki” (wspólny TrailProgressRepository).
    val result = progress.result
    val completedFraction = result?.completedFraction?.toFloat()?.coerceIn(0f, 1f) ?: 0f

    val summary =
        when {
            progress.isLoading -> "Liczę postęp…"
            progress.errorMessage != null -> "Nie udało się policzyć postępu."
            result == null -> "Brak wyniku."
            else -> "Ukończono ${progress.completedTrailCount} z ${progress.totalTrailCount} szlaków"
        }

    Column {
        TatraSectionHeader(title = "Postęp w Tatrach")

        Spacer(modifier = Modifier.height(TatraSpacing.sm))

        TatraCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = TatraGreenSoft,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = TatraGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                    ) {
                        Text(
                            text = "Ukończone szlaki",
                            color = TatraDark,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Przebyta część sieci szlaków TPN i TANAP",
                            color = TatraTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    Text(
                        text = result?.let { formatPercent(it.completedFraction) } ?: "—%",
                        color = TatraDark,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.height(TatraSpacing.md))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                color = TatraSurfaceStrong,
                                shape = RoundedCornerShape(99.dp)
                            )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(completedFraction)
                            .height(8.dp)
                            .background(
                                color = TatraGreen,
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary,
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TatraStatusBadge(
                        text = "Otwórz",
                        style = TatraBadgeStyle.Info
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TatraGreen
            )

            Text(
                text = label,
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
            )

            Text(
                text = value,
                color = TatraDark,
                style = MaterialTheme.typography.titleSmall
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = TatraOutline.copy(alpha = 0.75f)
            )
        }
    }
}

private fun formatDistance(
    meters: Double
): String {
    if (meters <= 0.0) {
        return "—"
    }

    return String.format(
        Locale.getDefault(),
        "%.1f km",
        meters / 1000.0
    )
}

private fun formatElevation(
    meters: Double
): String {
    if (meters <= 0.0) {
        return "—"
    }

    return String.format(
        Locale.getDefault(),
        "%.0f m",
        meters
    )
}

private fun formatDuration(
    durationMillis: Long
): String {
    if (durationMillis <= 0L) {
        return "—"
    }

    val totalMinutes = durationMillis / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours <= 0L ->
            "$minutes min"

        minutes == 0L ->
            "$hours h"

        else ->
            "$hours h $minutes min"
    }
}