package com.example.tatratracker.export

import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.data.PlannedRoutePointEntity
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PlannedRouteGpxExporter {

    private const val MAX_ELEVATION_MATCH_DISTANCE_METERS =
        150.0

    fun buildGpx(
        route: PlannedRouteEntity,
        geometryPoints: List<PlannedRoutePointEntity>,
        startPoint: PlannedRoutePointEntity?,
        waypoints: List<PlannedRoutePointEntity>,
        endPoint: PlannedRoutePointEntity?,
        elevationPoints: List<PlannedRoutePointEntity>
    ): String {

        require(
            geometryPoints.size >= 2
        ) {
            "Trasa nie zawiera wystarczającej liczby punktów geometrii."
        }

        val sortedGeometry =
            geometryPoints.sortedBy {
                it.orderIndex
            }

        val sortedWaypoints =
            waypoints.sortedBy {
                it.orderIndex
            }

        val sortedElevation =
            elevationPoints
                .filter {
                    it.altitudeMeters != null
                }
                .sortedBy {
                    it.orderIndex
                }

        return buildString {

            appendLine(
                """<?xml version="1.0" encoding="UTF-8"?>"""
            )

            appendLine(
                """
                <gpx
                    version="1.1"
                    creator="TatraTracker"
                    xmlns="http://www.topografix.com/GPX/1/1"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
                """.trimIndent()
            )

            appendLine(
                "  <metadata>"
            )

            appendLine(
                "    <name>${escapeXml(route.name)}</name>"
            )

            appendLine(
                "    <desc>${escapeXml(buildDescription(route))}</desc>"
            )

            appendLine(
                "  </metadata>"
            )

            startPoint?.let { point ->

                appendWaypoint(
                    builder = this,
                    point = point,
                    fallbackName = route.startName
                        ?: "Start",
                    type = "start"
                )
            }

            sortedWaypoints.forEachIndexed {
                    index,
                    point ->

                appendWaypoint(
                    builder = this,
                    point = point,
                    fallbackName =
                        "Przystanek ${index + 1}",
                    type = "waypoint"
                )
            }

            endPoint?.let { point ->

                appendWaypoint(
                    builder = this,
                    point = point,
                    fallbackName = route.endName
                        ?: "Cel",
                    type = "finish"
                )
            }

            appendLine(
                "  <trk>"
            )

            appendLine(
                "    <name>${escapeXml(route.name)}</name>"
            )

            appendLine(
                "    <type>hiking</type>"
            )

            appendLine(
                "    <trkseg>"
            )

            sortedGeometry.forEach { point ->

                val elevation =
                    findElevationForGeometryPoint(
                        geometryPoint = point,
                        elevationPoints = sortedElevation
                    )

                appendTrackPoint(
                    builder = this,
                    point = point,
                    elevationMeters = elevation
                )
            }

            appendLine(
                "    </trkseg>"
            )

            appendLine(
                "  </trk>"
            )

            appendLine(
                "</gpx>"
            )
        }
    }

    fun suggestedFileName(
        routeName: String
    ): String {

        val cleanName =
            routeName
                .trim()
                .lowercase(
                    Locale.getDefault()
                )
                .replace(
                    "ą",
                    "a"
                )
                .replace(
                    "ć",
                    "c"
                )
                .replace(
                    "ę",
                    "e"
                )
                .replace(
                    "ł",
                    "l"
                )
                .replace(
                    "ń",
                    "n"
                )
                .replace(
                    "ó",
                    "o"
                )
                .replace(
                    "ś",
                    "s"
                )
                .replace(
                    "ź",
                    "z"
                )
                .replace(
                    "ż",
                    "z"
                )
                .replace(
                    Regex(
                        "[^a-z0-9]+"
                    ),
                    "_"
                )
                .trim(
                    '_'
                )
                .take(
                    80
                )

        val baseName =
            cleanName.ifBlank {
                "tatratracker_trasa"
            }

        return "$baseName.gpx"
    }

    private fun appendWaypoint(
        builder: StringBuilder,
        point: PlannedRoutePointEntity,
        fallbackName: String,
        type: String
    ) {

        val displayName =
            point.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: fallbackName

        builder.appendLine(
            "  <wpt lat=\"${formatCoordinate(point.latitude)}\" lon=\"${formatCoordinate(point.longitude)}\">"
        )

        point.altitudeMeters
            ?.let { altitude ->

                builder.appendLine(
                    "    <ele>${formatElevation(altitude)}</ele>"
                )
            }

        builder.appendLine(
            "    <name>${escapeXml(displayName)}</name>"
        )

        builder.appendLine(
            "    <type>${escapeXml(type)}</type>"
        )

        builder.appendLine(
            "  </wpt>"
        )
    }

    private fun appendTrackPoint(
        builder: StringBuilder,
        point: PlannedRoutePointEntity,
        elevationMeters: Double?
    ) {

        if (
            elevationMeters == null
        ) {

            builder.appendLine(
                "      <trkpt lat=\"${formatCoordinate(point.latitude)}\" lon=\"${formatCoordinate(point.longitude)}\" />"
            )

            return
        }

        builder.appendLine(
            "      <trkpt lat=\"${formatCoordinate(point.latitude)}\" lon=\"${formatCoordinate(point.longitude)}\">"
        )

        builder.appendLine(
            "        <ele>${formatElevation(elevationMeters)}</ele>"
        )

        builder.appendLine(
            "      </trkpt>"
        )
    }

    private fun findElevationForGeometryPoint(
        geometryPoint: PlannedRoutePointEntity,
        elevationPoints: List<PlannedRoutePointEntity>
    ): Double? {

        if (
            geometryPoint.altitudeMeters != null
        ) {

            return geometryPoint.altitudeMeters
        }

        if (
            elevationPoints.isEmpty()
        ) {

            return null
        }

        var nearestElevation: Double? =
            null

        var nearestDistance =
            Double.MAX_VALUE

        elevationPoints.forEach { elevationPoint ->

            val elevation =
                elevationPoint.altitudeMeters
                    ?: return@forEach

            val distance =
                distanceMeters(
                    latitude1 =
                        geometryPoint.latitude,
                    longitude1 =
                        geometryPoint.longitude,
                    latitude2 =
                        elevationPoint.latitude,
                    longitude2 =
                        elevationPoint.longitude
                )

            if (
                distance < nearestDistance
            ) {

                nearestDistance =
                    distance

                nearestElevation =
                    elevation
            }
        }

        return if (
            nearestDistance <=
            MAX_ELEVATION_MATCH_DISTANCE_METERS
        ) {

            nearestElevation

        } else {

            null
        }
    }

    private fun buildDescription(
        route: PlannedRouteEntity
    ): String {

        val distanceKm =
            route.distanceMeters /
                    1000.0

        val parts =
            mutableListOf<String>()

        parts +=
            String.format(
                Locale.US,
                "Dystans: %.2f km",
                distanceKm
            )

        if (
            route.durationSeconds > 0
        ) {

            parts +=
                "Czas: ${formatDuration(route.durationSeconds)}"
        }

        route.ascentMeters
            ?.let {
                parts +=
                    "Podejście: $it m"
            }

        route.descentMeters
            ?.let {
                parts +=
                    "Zejście: $it m"
            }

        return parts.joinToString(
            separator = " | "
        )
    }

    private fun formatDuration(
        seconds: Int
    ): String {

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

    private fun formatCoordinate(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.7f",
            value
        )
    }

    private fun formatElevation(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.1f",
            value
        )
    }

    private fun escapeXml(
        value: String
    ): String {

        return value
            .replace(
                "&",
                "&amp;"
            )
            .replace(
                "<",
                "&lt;"
            )
            .replace(
                ">",
                "&gt;"
            )
            .replace(
                "\"",
                "&quot;"
            )
            .replace(
                "'",
                "&apos;"
            )
    }

    private fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {

        val earthRadiusMeters =
            6_371_000.0

        val latitude1Radians =
            Math.toRadians(
                latitude1
            )

        val latitude2Radians =
            Math.toRadians(
                latitude2
            )

        val deltaLatitude =
            Math.toRadians(
                latitude2 -
                        latitude1
            )

        val deltaLongitude =
            Math.toRadians(
                longitude2 -
                        longitude1
            )

        val a =
            sin(
                deltaLatitude /
                        2.0
            ) *
                    sin(
                        deltaLatitude /
                                2.0
                    ) +
                    cos(
                        latitude1Radians
                    ) *
                    cos(
                        latitude2Radians
                    ) *
                    sin(
                        deltaLongitude /
                                2.0
                    ) *
                    sin(
                        deltaLongitude /
                                2.0
                    )

        val c =
            2.0 *
                    atan2(
                        sqrt(
                            a
                        ),
                        sqrt(
                            1.0 -
                                    a
                        )
                    )

        return earthRadiusMeters *
                c
    }
}