package com.example.tatratracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraOutline
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class ActivityElevationSample(
    val distanceMeters: Double,
    val elevationMeters: Double
)

private data class ActivityElevationProfileData(
    val samples: List<ActivityElevationSample>,
    val totalDistanceMeters: Double,
    val ascentMeters: Int,
    val descentMeters: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int
)

@Composable
fun ActivityElevationSection(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val profile =
        remember(trackPoints) {
            buildActivityElevationProfile(trackPoints)
        }

    if (profile == null || profile.samples.size < 2) {
        return
    }

    TatraCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Profil wysokościowy",
                color = TatraDark,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Na podstawie wysokości zapisanej w śladzie GPS",
                color = TatraTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(modifier = Modifier.height(TatraSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                ElevationMetric(
                    label = "Podejście",
                    value = "+${profile.ascentMeters} m",
                    modifier = Modifier.weight(1f)
                )

                ElevationMetric(
                    label = "Zejście",
                    value = "-${profile.descentMeters} m",
                    modifier = Modifier.weight(1f)
                )

                ElevationMetric(
                    label = "Najwyżej",
                    value = "${profile.maxElevationMeters} m",
                    modifier = Modifier.weight(1f)
                )

                ElevationMetric(
                    label = "Najniżej",
                    value = "${profile.minElevationMeters} m",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.md))

            ElevationChart(
                samples = profile.samples,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 km",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text =
                        String.format(
                            Locale.getDefault(),
                            "%.1f km",
                            profile.totalDistanceMeters / 1000.0
                        ),
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ElevationMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = TatraTextMuted,
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = value,
            color = TatraDark,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ElevationChart(
    samples: List<ActivityElevationSample>,
    modifier: Modifier = Modifier
) {
    val minElevation = samples.minOf { it.elevationMeters }
    val maxElevation = samples.maxOf { it.elevationMeters }
    val elevationRange = (maxElevation - minElevation).coerceAtLeast(1.0)
    val maxDistance = samples.last().distanceMeters.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        for (line in 0..3) {
            val y = size.height * line / 3f

            drawLine(
                color = TatraOutline.copy(alpha = 0.75f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val linePath = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, sample ->
            val x =
                (sample.distanceMeters / maxDistance).toFloat() *
                        size.width

            val normalized =
                ((sample.elevationMeters - minElevation) / elevationRange)
                    .toFloat()

            val y =
                size.height -
                        normalized * size.height * 0.84f -
                        size.height * 0.08f

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = TatraGreen.copy(alpha = 0.13f),
            style = Fill
        )

        drawPath(
            path = linePath,
            color = TatraGreen,
            style =
                Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
        )
    }
}

private fun buildActivityElevationProfile(
    trackPoints: List<TrackPoint>
): ActivityElevationProfileData? {
    if (trackPoints.size < 2) {
        return null
    }

    val rawSamples = mutableListOf<ActivityElevationSample>()
    var totalDistanceMeters = 0.0

    trackPoints.forEachIndexed { index, point ->
        if (index > 0) {
            totalDistanceMeters +=
                distanceMeters(
                    trackPoints[index - 1],
                    point
                )
        }

        if (point.ele.isFinite() && point.ele > 0.0) {
            rawSamples.add(
                ActivityElevationSample(
                    distanceMeters = totalDistanceMeters,
                    elevationMeters = point.ele
                )
            )
        }
    }

    if (rawSamples.size < 2) {
        return null
    }

    val smoothed = smoothElevation(rawSamples)

    var ascent = 0.0
    var descent = 0.0

    for (index in 1 until smoothed.size) {
        val difference =
            smoothed[index].elevationMeters -
                    smoothed[index - 1].elevationMeters

        when {
            difference > 2.0 -> ascent += difference
            difference < -2.0 -> descent += -difference
        }
    }

    val chartSamples = downsample(smoothed, maxPoints = 500)

    return ActivityElevationProfileData(
        samples = chartSamples,
        totalDistanceMeters = totalDistanceMeters,
        ascentMeters = ascent.roundToInt(),
        descentMeters = descent.roundToInt(),
        minElevationMeters =
            smoothed.minOf { it.elevationMeters }.roundToInt(),
        maxElevationMeters =
            smoothed.maxOf { it.elevationMeters }.roundToInt()
    )
}

private fun smoothElevation(
    samples: List<ActivityElevationSample>
): List<ActivityElevationSample> {
    if (samples.size < 5) {
        return samples
    }

    return samples.mapIndexed { index, sample ->
        val from = (index - 2).coerceAtLeast(0)
        val to = (index + 2).coerceAtMost(samples.lastIndex)

        var sum = 0.0
        var count = 0

        for (windowIndex in from..to) {
            sum += samples[windowIndex].elevationMeters
            count++
        }

        sample.copy(
            elevationMeters = sum / count
        )
    }
}

private fun downsample(
    samples: List<ActivityElevationSample>,
    maxPoints: Int
): List<ActivityElevationSample> {
    if (samples.size <= maxPoints) {
        return samples
    }

    val step =
        (samples.size - 1).toDouble() /
                (maxPoints - 1).toDouble()

    return buildList {
        for (index in 0 until maxPoints) {
            val sourceIndex =
                (index * step)
                    .roundToInt()
                    .coerceIn(0, samples.lastIndex)

            add(samples[sourceIndex])
        }
    }.distinctBy { it.distanceMeters }
}

private fun distanceMeters(
    first: TrackPoint,
    second: TrackPoint
): Double {
    val earthRadiusMeters = 6_371_000.0

    val lat1 = Math.toRadians(first.lat)
    val lat2 = Math.toRadians(second.lat)
    val deltaLat = Math.toRadians(second.lat - first.lat)
    val deltaLon = Math.toRadians(second.lon - first.lon)

    val a =
        sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2.0) * sin(deltaLon / 2.0)

    val c =
        2.0 * atan2(
            sqrt(a),
            sqrt(1.0 - a)
        )

    return earthRadiusMeters * c
}