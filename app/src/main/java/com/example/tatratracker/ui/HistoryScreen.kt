package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.ui.components.PlannedRouteCard
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraEmptyState
import com.example.tatratracker.ui.components.TatraScreenHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenSoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraSurfaceStrong
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.HistoryViewModel
import com.example.tatratracker.viewmodel.PlannedRoutesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    onActivityClick: (Int) -> Unit,
    onPlannedRouteClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    onOpenMap: () -> Unit,
    historyViewModel:
    HistoryViewModel =
        viewModel(),
    plannedRoutesViewModel:
    PlannedRoutesViewModel =
        viewModel()
) {

    val activities by
    historyViewModel
        .activities
        .collectAsState()

    val plannedRoutes by
    plannedRoutesViewModel
        .routes
        .collectAsState()

    var selectedTab by
    remember {
        mutableIntStateOf(
            0
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    TatraBackground
                )
    ) {

        TatraScreenHeader(
            title =
                "Aktywności",
            subtitle =
                when (
                    selectedTab
                ) {

                    0 ->
                        if (
                            activities.isEmpty()
                        ) {
                            "Twoje zapisane przejścia"
                        } else {
                            "${activities.size} zapisanych aktywności"
                        }

                    else ->
                        if (
                            plannedRoutes.isEmpty()
                        ) {
                            "Twoje zaplanowane trasy"
                        } else {
                            "${plannedRoutes.size} zapisanych tras"
                        }
                },
            actions = {

                IconButton(
                    onClick =
                        onImportClick
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Add,
                        contentDescription =
                            "Dodaj aktywność",
                        tint =
                            TatraDark
                    )
                }
            }
        )

        ActivityTabs(
            selectedTab =
                selectedTab,
            onSelected = {
                selectedTab =
                    it
            },
            modifier =
                Modifier.padding(
                    horizontal =
                        TatraSpacing.lg
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    TatraSpacing.md
                )
        )

        if (
            selectedTab == 0
        ) {

            if (
                activities.isEmpty()
            ) {

                TatraEmptyState(
                    title =
                        "Brak aktywności",
                    message =
                        "Zaimportuj pierwszy plik FIT lub TCX, aby zobaczyć go tutaj.",
                    icon =
                        Icons
                            .Outlined
                            .DirectionsWalk,
                    actionText =
                        "Dodaj aktywność",
                    onActionClick =
                        onImportClick,
                    modifier =
                        Modifier.padding(
                            horizontal =
                                TatraSpacing.lg,
                            vertical =
                                TatraSpacing.sm
                        )
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start =
                                TatraSpacing.lg,
                            end =
                                TatraSpacing.lg,
                            bottom =
                                TatraSpacing.xl
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            TatraSpacing.sm
                        )
                ) {

                    items(
                        items =
                            activities,
                        key = {
                                activity ->

                            activity.id
                        }
                    ) {
                            activity ->

                        ActivityHistoryItem(
                            activity =
                                activity,
                            onClick = {
                                onActivityClick(
                                    activity.id
                                )
                            }
                        )
                    }
                }
            }

        } else {

            if (
                plannedRoutes.isEmpty()
            ) {

                TatraEmptyState(
                    title =
                        "Brak zapisanych tras",
                    message =
                        "Wyznacz trasę na mapie i użyj przycisku „Zapisz”.",
                    icon =
                        Icons.Outlined.Map,
                    actionText =
                        "Otwórz mapę",
                    onActionClick =
                        onOpenMap,
                    modifier =
                        Modifier.padding(
                            horizontal =
                                TatraSpacing.lg,
                            vertical =
                                TatraSpacing.sm
                        )
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start =
                                TatraSpacing.lg,
                            end =
                                TatraSpacing.lg,
                            bottom =
                                TatraSpacing.xl
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            TatraSpacing.sm
                        )
                ) {

                    items(
                        items =
                            plannedRoutes,
                        key = {
                                route ->

                            route.id
                        }
                    ) {
                            route ->

                        PlannedRouteCard(
                            route =
                                route,
                            onClick = {
                                onPlannedRouteClick(
                                    route.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityTabs(
    selectedTab: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color =
                        TatraSurfaceStrong,
                    shape =
                        RoundedCornerShape(
                            13.dp
                        )
                )
                .padding(
                    4.dp
                )
    ) {

        ActivityTab(
            title =
                "Aktywności",
            selected =
                selectedTab == 0,
            onClick = {
                onSelected(
                    0
                )
            },
            modifier =
                Modifier.weight(
                    1f
                )
        )

        ActivityTab(
            title =
                "Zaplanowane",
            selected =
                selectedTab == 1,
            onClick = {
                onSelected(
                    1
                )
            },
            modifier =
                Modifier.weight(
                    1f
                )
        )
    }
}

@Composable
private fun ActivityTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier.clickable(
                onClick =
                    onClick
            ),
        shape =
            RoundedCornerShape(
                10.dp
            ),
        color =
            if (
                selected
            ) {
                TatraSurface
            } else {
                Color.Transparent
            },
        shadowElevation =
            if (
                selected
            ) {
                1.dp
            } else {
                0.dp
            }
    ) {

        Text(
            text =
                title,
            color =
                if (
                    selected
                ) {
                    TatraDark
                } else {
                    TatraTextMuted
                },
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            fontWeight =
                if (
                    selected
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
            modifier =
                Modifier.padding(
                    vertical =
                        10.dp
                ),
            textAlign =
                androidx.compose.ui
                    .text
                    .style
                    .TextAlign
                    .Center
        )
    }
}

@Composable
private fun ActivityHistoryItem(
    activity: ActivityEntity,
    onClick: () -> Unit
) {

    val dateFormatter =
        SimpleDateFormat(
            "dd MMM yyyy • HH:mm",
            Locale.getDefault()
        )

    val date =
        dateFormatter.format(
            Date(
                activity.timestamp
            )
        )

    val distanceText =
        TatraFormat.distance(activity.distanceMeters)

    val durationText =
        TatraFormat.durationMillis(activity.durationMillis)

    val sourceLabel =
        activityFormat(
            activity.sourceFileName
        )

    val altitudeText =
        if (
            activity.minAltitudeMeters >
            0.0 &&
            activity.maxAltitudeMeters >
            0.0
        ) {

            "${activity.minAltitudeMeters.roundToInt()}–${TatraFormat.elevation(activity.maxAltitudeMeters)}"

        } else {

            TatraFormat.NO_DATA
        }

    TatraCard(
        modifier =
            Modifier.fillMaxWidth(),
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
                            Icons
                                .Outlined
                                .DirectionsWalk,
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
                        Modifier.weight(
                            1f
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                activity.name,
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
                            text =
                                sourceLabel,
                            style =
                                TatraBadgeStyle.Info
                        )
                    }

                    Text(
                        text =
                            date,
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

                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )

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

            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
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

                ActivityMetric(
                    label =
                        "Dystans",
                    value =
                        distanceText,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                ActivityMetric(
                    label =
                        "Czas",
                    value =
                        durationText,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                ActivityMetric(
                    label =
                        "Wysokość",
                    value =
                        altitudeText,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            if (
                activity.calories > 0 ||
                activity.avgHeartRate > 0
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            TatraSpacing.md
                        )
                ) {

                    if (
                        activity.calories >
                        0
                    ) {

                        Text(
                            text =
                                "${activity.calories.toInt()} kcal",
                            color =
                                TatraTextMuted,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    if (
                        activity.avgHeartRate >
                        0
                    ) {

                        Text(
                            text =
                                "Śr. puls ${TatraFormat.heartRate(activity.avgHeartRate)}",
                            color =
                                TatraTextMuted,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMetric(
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

private fun activityFormat(
    fileName: String
): String {

    val extension =
        fileName
            .substringAfterLast(
                '.',
                ""
            )
            .trim()
            .uppercase(
                Locale.getDefault()
            )

    return when (
        extension
    ) {

        "FIT",
        "TCX",
        "GPX" ->
            extension

        else ->
            "IMPORT"
    }
}