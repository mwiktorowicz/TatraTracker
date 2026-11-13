package com.example.tatratracker.fit

import com.garmin.fit.Decode
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.RecordMesgListener
import com.garmin.fit.SessionMesgListener
import java.io.InputStream

data class FitTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val heartRate: Int?,
    val speedMetersPerSecond: Double?,
    val timestampSeconds: Long?
)

data class FitActivityData(

    // Początek treningu jako Unix timestamp w milisekundach
    val startTimeMillis: Long?,

    // Czas całkowity (total_elapsed_time) i w ruchu (total_timer_time); null = brak w pliku
    val totalElapsedSeconds: Double?,
    val totalTimerSeconds: Double?,
    val distanceMeters: Double,
    val calories: Int,

    val averageSpeedMetersPerSecond: Double,
    val maxSpeedMetersPerSecond: Double,

    val averageHeartRate: Int,
    val maxHeartRate: Int,

    val minAltitudeMeters: Double,
    val maxAltitudeMeters: Double,

    val trackPoints: List<FitTrackPoint>
)

object FitParser {

    private const val FIT_EPOCH_OFFSET_SECONDS =
        631065600L

    private const val FIT_DATE_TIME_MIN =
        0x10000000L

    fun parse(
        inputStream: InputStream
    ): FitActivityData {

        val decode = Decode()
        val broadcaster = MesgBroadcaster(decode)

        val trackPoints =
            mutableListOf<FitTrackPoint>()

        var sessionStartTimeMillis: Long? = null

        var sessionElapsedSeconds: Double? = null
        var sessionTimerSeconds: Double? = null
        var sessionDistance = 0.0
        var sessionCalories = 0

        var sessionAverageSpeed = 0.0
        var sessionMaxSpeed = 0.0

        var sessionAverageHeartRate = 0
        var sessionMaxHeartRate = 0

        broadcaster.addListener(
            RecordMesgListener { record ->

                val latitudeRaw =
                    record.positionLat

                val longitudeRaw =
                    record.positionLong

                if (
                    latitudeRaw != null &&
                    longitudeRaw != null
                ) {

                    val latitude =
                        semicirclesToDegrees(
                            latitudeRaw.toLong()
                        )

                    val longitude =
                        semicirclesToDegrees(
                            longitudeRaw.toLong()
                        )

                    val altitude =
                        when {

                            record.enhancedAltitude != null ->
                                record.enhancedAltitude.toDouble()

                            record.altitude != null ->
                                record.altitude.toDouble()

                            else ->
                                null
                        }

                    val heartRate =
                        record.heartRate
                            ?.toInt()

                    val speed =
                        when {

                            record.enhancedSpeed != null ->
                                record.enhancedSpeed.toDouble()

                            record.speed != null ->
                                record.speed.toDouble()

                            else ->
                                null
                        }

                    val timestamp =
                        record.timestamp
                            ?.timestamp

                    trackPoints.add(
                        FitTrackPoint(
                            latitude = latitude,
                            longitude = longitude,
                            altitudeMeters = altitude,
                            heartRate = heartRate,
                            speedMetersPerSecond = speed,
                            timestampSeconds = timestamp
                        )
                    )
                }
            }
        )

        broadcaster.addListener(
            SessionMesgListener { session ->

                val fitStartTime =
                    session.startTime
                        ?.timestamp

                if (fitStartTime != null) {

                    sessionStartTimeMillis =
                        fitTimestampToUnixMillis(
                            fitStartTime
                        )
                }

                sessionElapsedSeconds = session.totalElapsedTime?.toDouble()
                sessionTimerSeconds = session.totalTimerTime?.toDouble()

                sessionDistance =
                    session.totalDistance
                        ?.toDouble()
                        ?: 0.0

                sessionCalories =
                    session.totalCalories
                        ?.toInt()
                        ?: 0

                sessionAverageSpeed =
                    when {

                        session.enhancedAvgSpeed != null ->
                            session.enhancedAvgSpeed.toDouble()

                        session.avgSpeed != null ->
                            session.avgSpeed.toDouble()

                        else ->
                            0.0
                    }

                sessionMaxSpeed =
                    when {

                        session.enhancedMaxSpeed != null ->
                            session.enhancedMaxSpeed.toDouble()

                        session.maxSpeed != null ->
                            session.maxSpeed.toDouble()

                        else ->
                            0.0
                    }

                sessionAverageHeartRate =
                    session.avgHeartRate
                        ?.toInt()
                        ?: 0

                sessionMaxHeartRate =
                    session.maxHeartRate
                        ?.toInt()
                        ?: 0
            }
        )

        broadcaster.run(inputStream)

        if (trackPoints.isEmpty()) {
            throw IllegalArgumentException(
                "Plik FIT nie zawiera punktów GPS."
            )
        }

        /*
         * Jeśli Session nie zawiera start_time,
         * próbujemy wykorzystać timestamp
         * pierwszego punktu GPS.
         */
        if (sessionStartTimeMillis == null) {

            sessionStartTimeMillis =
                trackPoints
                    .firstOrNull()
                    ?.timestampSeconds
                    ?.let {
                        fitTimestampToUnixMillis(it)
                    }
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

        val averageSpeed =
            if (sessionAverageSpeed > 0.0) {

                sessionAverageSpeed

            } else {

                speeds.averageOrNull()
                    ?: 0.0
            }

        val maxSpeed =
            if (sessionMaxSpeed > 0.0) {

                sessionMaxSpeed

            } else {

                speeds.maxOrNull()
                    ?: 0.0
            }

        val heartRates =
            trackPoints
                .mapNotNull {
                    it.heartRate
                }
                .filter {
                    it > 0
                }

        val averageHeartRate =
            if (sessionAverageHeartRate > 0) {

                sessionAverageHeartRate

            } else if (heartRates.isNotEmpty()) {

                heartRates.average().toInt()

            } else {

                0
            }

        val maxHeartRate =
            if (sessionMaxHeartRate > 0) {

                sessionMaxHeartRate

            } else {

                heartRates.maxOrNull()
                    ?: 0
            }

        return FitActivityData(

            startTimeMillis =
                sessionStartTimeMillis,

            totalElapsedSeconds = sessionElapsedSeconds,
            totalTimerSeconds = sessionTimerSeconds,

            distanceMeters =
                sessionDistance,

            calories =
                sessionCalories,

            averageSpeedMetersPerSecond =
                averageSpeed,

            maxSpeedMetersPerSecond =
                maxSpeed,

            averageHeartRate =
                averageHeartRate,

            maxHeartRate =
                maxHeartRate,

            minAltitudeMeters =
                minAltitude,

            maxAltitudeMeters =
                maxAltitude,

            trackPoints =
                trackPoints
        )
    }

    private fun semicirclesToDegrees(
        semicircles: Long
    ): Double {

        return semicircles *
                (180.0 / 2147483648.0)
    }

    private fun fitTimestampToUnixMillis(
        fitTimestampSeconds: Long
    ): Long? {

        if (fitTimestampSeconds < FIT_DATE_TIME_MIN) {
            return null
        }

        return (
                fitTimestampSeconds +
                        FIT_EPOCH_OFFSET_SECONDS
                ) * 1000L
    }

    private fun List<Double>.averageOrNull(): Double? {

        if (isEmpty()) {
            return null
        }

        return average()
    }
}