package com.example.tatratracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.tatratracker.data.PlannedRoutePointEntity
import com.example.tatratracker.ui.format.TatraFormat
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import kotlin.math.max

@Composable
fun PlannedRouteElevationProfile(
    points: List<PlannedRoutePointEntity>,
    modifier: Modifier = Modifier
) {

    val samples =
        remember(
            points
        ) {

            points.mapNotNull {
                    point ->

                val altitude =
                    point.altitudeMeters

                if (
                    altitude == null
                ) {

                    null

                } else {

                    SavedElevationSample(
                        distanceMeters =
                            point.distanceMeters
                                ?: 0.0,
                        elevationMeters =
                            altitude
                    )
                }
            }
        }

    if (
        samples.size < 2
    ) {
        return
    }

    val minElevation =
        samples.minOf {
            it.elevationMeters
        }

    val maxElevation =
        samples.maxOf {
            it.elevationMeters
        }

    val maxDistance =
        max(
            samples.maxOf {
                it.distanceMeters
            },
            1.0
        )

    val elevationRange =
        max(
            maxElevation -
                    minElevation,
            1.0
        )

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text =
                    TatraFormat.elevation(minElevation),
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    TatraFormat.elevation(maxElevation),
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        180.dp
                    )
                    .padding(
                        top =
                            TatraSpacing.xs,
                        bottom =
                            TatraSpacing.xs
                    )
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        TatraGreenVerySoft
                    )
        ) {

            val horizontalPadding =
                12.dp.toPx()

            val verticalPadding =
                16.dp.toPx()

            val chartWidth =
                size.width -
                        horizontalPadding * 2f

            val chartHeight =
                size.height -
                        verticalPadding * 2f

            drawLine(
                color =
                    TatraOutline,
                start =
                    Offset(
                        horizontalPadding,
                        verticalPadding +
                                chartHeight
                    ),
                end =
                    Offset(
                        horizontalPadding +
                                chartWidth,
                        verticalPadding +
                                chartHeight
                    ),
                strokeWidth =
                    1.dp.toPx()
            )

            val path =
                Path()

            samples.forEachIndexed {
                    index,
                    sample ->

                val x =
                    horizontalPadding +
                            (
                                    sample.distanceMeters /
                                            maxDistance
                                    ).toFloat() *
                            chartWidth

                val normalizedElevation =
                    (
                            sample.elevationMeters -
                                    minElevation
                            ) /
                            elevationRange

                val y =
                    verticalPadding +
                            chartHeight -
                            normalizedElevation
                                .toFloat() *
                            chartHeight

                if (
                    index == 0
                ) {

                    path.moveTo(
                        x,
                        y
                    )

                } else {

                    path.lineTo(
                        x,
                        y
                    )
                }
            }

            drawPath(
                path =
                    path,
                color =
                    TatraGreen,
                style =
                    Stroke(
                        width =
                            3.dp.toPx(),
                        cap =
                            StrokeCap.Round,
                        join =
                            StrokeJoin.Round
                    )
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = "0 km",
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    TatraFormat.distance(maxDistance),
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

private data class SavedElevationSample(
    val distanceMeters: Double,
    val elevationMeters: Double
)