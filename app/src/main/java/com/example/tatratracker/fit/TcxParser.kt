package com.example.tatratracker.fit

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.Instant

data class TcxTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val heartRate: Int?,
    val speedMetersPerSecond: Double?,
    val timestampMillis: Long?
)

data class TcxActivityData(
    val startTimeMillis: Long?,
    val durationSeconds: Double,
    val distanceMeters: Double,
    val calories: Int,
    val averageSpeedMetersPerSecond: Double,
    val maxSpeedMetersPerSecond: Double,
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val minAltitudeMeters: Double,
    val maxAltitudeMeters: Double,
    val trackPoints: List<TcxTrackPoint>
)

object TcxParser {

    fun parse(
        inputStream: InputStream
    ): TcxActivityData {

        val parserFactory =
            XmlPullParserFactory.newInstance()

        parserFactory.isNamespaceAware = true

        val parser =
            parserFactory.newPullParser()

        parser.setInput(
            inputStream,
            null
        )

        val trackPoints =
            mutableListOf<TcxTrackPoint>()

        var totalDurationSeconds = 0.0
        var totalDistanceMeters = 0.0
        var totalCalories = 0

        var lapMaxSpeed = 0.0
        var lapAverageHeartRate = 0
        var lapMaximumHeartRate = 0

        var activityStartTimeMillis: Long? = null

        var currentLatitude: Double? = null
        var currentLongitude: Double? = null
        var currentAltitude: Double? = null
        var currentHeartRate: Int? = null
        var currentSpeed: Double? = null
        var currentTimestampMillis: Long? = null

        var insideTrackPoint = false
        var insideHeartRate = false
        var insideAverageHeartRate = false
        var insideMaximumHeartRate = false

        var eventType =
            parser.eventType

        while (
            eventType != XmlPullParser.END_DOCUMENT
        ) {

            when (eventType) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "Lap" -> {

                            if (
                                activityStartTimeMillis == null
                            ) {

                                activityStartTimeMillis =
                                    parser.getAttributeValue(
                                        null,
                                        "StartTime"
                                    )?.let {
                                        parseTimestamp(it)
                                    }
                            }
                        }

                        "Trackpoint" -> {

                            insideTrackPoint = true

                            currentLatitude = null
                            currentLongitude = null
                            currentAltitude = null
                            currentHeartRate = null
                            currentSpeed = null
                            currentTimestampMillis = null
                        }

                        "HeartRateBpm" -> {

                            if (insideTrackPoint) {
                                insideHeartRate = true
                            }
                        }

                        "AverageHeartRateBpm" -> {
                            insideAverageHeartRate = true
                        }

                        "MaximumHeartRateBpm" -> {
                            insideMaximumHeartRate = true
                        }

                        "TotalTimeSeconds" -> {

                            if (!insideTrackPoint) {

                                totalDurationSeconds +=
                                    readText(parser)
                                        .toDoubleOrNull()
                                        ?: 0.0
                            }
                        }

                        "DistanceMeters" -> {

                            if (!insideTrackPoint) {

                                totalDistanceMeters +=
                                    readText(parser)
                                        .toDoubleOrNull()
                                        ?: 0.0
                            }
                        }

                        "Calories" -> {

                            totalCalories +=
                                readText(parser)
                                    .toDoubleOrNull()
                                    ?.toInt()
                                    ?: 0
                        }

                        "MaximumSpeed" -> {

                            val value =
                                readText(parser)
                                    .toDoubleOrNull()
                                    ?: 0.0

                            if (value > lapMaxSpeed) {
                                lapMaxSpeed = value
                            }
                        }

                        "LatitudeDegrees" -> {

                            if (insideTrackPoint) {

                                currentLatitude =
                                    readText(parser)
                                        .toDoubleOrNull()
                            }
                        }

                        "LongitudeDegrees" -> {

                            if (insideTrackPoint) {

                                currentLongitude =
                                    readText(parser)
                                        .toDoubleOrNull()
                            }
                        }

                        "AltitudeMeters" -> {

                            if (insideTrackPoint) {

                                currentAltitude =
                                    readText(parser)
                                        .toDoubleOrNull()
                            }
                        }

                        "Time" -> {

                            if (insideTrackPoint) {

                                currentTimestampMillis =
                                    parseTimestamp(
                                        readText(parser)
                                    )
                            }
                        }

                        "Speed" -> {

                            if (insideTrackPoint) {

                                currentSpeed =
                                    readText(parser)
                                        .toDoubleOrNull()
                            }
                        }

                        "Value" -> {

                            val value =
                                readText(parser)
                                    .toDoubleOrNull()
                                    ?.toInt()

                            when {

                                insideHeartRate -> {
                                    currentHeartRate =
                                        value
                                }

                                insideAverageHeartRate -> {

                                    if (
                                        value != null &&
                                        value > 0
                                    ) {
                                        lapAverageHeartRate =
                                            value
                                    }
                                }

                                insideMaximumHeartRate -> {

                                    if (
                                        value != null &&
                                        value >
                                        lapMaximumHeartRate
                                    ) {
                                        lapMaximumHeartRate =
                                            value
                                    }
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    when (parser.name) {

                        "Trackpoint" -> {

                            if (
                                currentLatitude != null &&
                                currentLongitude != null
                            ) {

                                trackPoints.add(
                                    TcxTrackPoint(
                                        latitude =
                                            currentLatitude!!,

                                        longitude =
                                            currentLongitude!!,

                                        altitudeMeters =
                                            currentAltitude,

                                        heartRate =
                                            currentHeartRate,

                                        speedMetersPerSecond =
                                            currentSpeed,

                                        timestampMillis =
                                            currentTimestampMillis
                                    )
                                )
                            }

                            insideTrackPoint = false
                        }

                        "HeartRateBpm" -> {
                            insideHeartRate = false
                        }

                        "AverageHeartRateBpm" -> {
                            insideAverageHeartRate = false
                        }

                        "MaximumHeartRateBpm" -> {
                            insideMaximumHeartRate = false
                        }
                    }
                }
            }

            eventType =
                parser.next()
        }

        if (trackPoints.isEmpty()) {
            throw IllegalArgumentException(
                "Plik TCX nie zawiera punktów GPS."
            )
        }

        if (activityStartTimeMillis == null) {

            activityStartTimeMillis =
                trackPoints
                    .firstOrNull()
                    ?.timestampMillis
        }

        val altitudes =
            trackPoints
                .mapNotNull {
                    it.altitudeMeters
                }

        val minAltitude =
            altitudes.minOrNull()
                ?: 0.0

        val maxAltitude =
            altitudes.maxOrNull()
                ?: 0.0

        val speeds =
            trackPoints
                .mapNotNull {
                    it.speedMetersPerSecond
                }
                .filter {
                    it >= 0.0
                }

        val calculatedAverageSpeed =
            if (
                totalDistanceMeters > 0.0 &&
                totalDurationSeconds > 0.0
            ) {

                totalDistanceMeters /
                        totalDurationSeconds

            } else {

                speeds.averageOrNull()
                    ?: 0.0
            }

        val calculatedMaxSpeed =
            maxOf(
                lapMaxSpeed,
                speeds.maxOrNull()
                    ?: 0.0
            )

        val heartRates =
            trackPoints
                .mapNotNull {
                    it.heartRate
                }
                .filter {
                    it > 0
                }

        val calculatedAverageHeartRate =
            if (lapAverageHeartRate > 0) {

                lapAverageHeartRate

            } else if (heartRates.isNotEmpty()) {

                heartRates
                    .average()
                    .toInt()

            } else {

                0
            }

        val calculatedMaxHeartRate =
            if (lapMaximumHeartRate > 0) {

                lapMaximumHeartRate

            } else {

                heartRates.maxOrNull()
                    ?: 0
            }

        return TcxActivityData(
            startTimeMillis =
                activityStartTimeMillis,

            durationSeconds =
                totalDurationSeconds,

            distanceMeters =
                totalDistanceMeters,

            calories =
                totalCalories,

            averageSpeedMetersPerSecond =
                calculatedAverageSpeed,

            maxSpeedMetersPerSecond =
                calculatedMaxSpeed,

            averageHeartRate =
                calculatedAverageHeartRate,

            maxHeartRate =
                calculatedMaxHeartRate,

            minAltitudeMeters =
                minAltitude,

            maxAltitudeMeters =
                maxAltitude,

            trackPoints =
                trackPoints
        )
    }

    private fun readText(
        parser: XmlPullParser
    ): String {

        return parser.nextText()
            .trim()
    }

    private fun parseTimestamp(
        value: String
    ): Long? {

        return try {

            Instant.parse(value)
                .toEpochMilli()

        } catch (_: Exception) {

            null
        }
    }

    private fun List<Double>.averageOrNull():
            Double? {

        if (isEmpty()) {
            return null
        }

        return average()
    }
}