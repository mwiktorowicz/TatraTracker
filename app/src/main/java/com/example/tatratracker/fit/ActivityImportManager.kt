package com.example.tatratracker.fit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.poi.ActivityPoiDetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ActivityImportResult(
    val activityId: Int,
    val poiDetection: Result<ActivityPoiDetectionResult>,
    val fileName: String,
    // Czas całkowity
    val durationSeconds: Double,
    val distanceMeters: Double,
    val calories: Int,
    val averageSpeedMetersPerSecond: Double,
    val maxSpeedMetersPerSecond: Double,
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val minAltitudeMeters: Double,
    val maxAltitudeMeters: Double,
    val startTimeMillis: Long?,
    val trackPoints: List<TrackPoint>
)

class ActivityImportManager(
    private val context: Context
) {

    private val database =
        AppDatabase.getDatabase(context)

    private val activityDao =
        database.activityDao()

    private val activitySaver = ActivitySaver(context)

    suspend fun importActivity(
        uri: Uri
    ): Result<ActivityImportResult> {

        return withContext(Dispatchers.IO) {

            runCatching {

                val fileName =
                    getFileName(uri)

                val extension =
                    fileName
                        .substringAfterLast(
                            ".",
                            ""
                        )
                        .lowercase()

                if (
                    extension != "fit" &&
                    extension != "tcx"
                ) {
                    throw IllegalArgumentException(
                        "Obsługiwane są tylko pliki FIT i TCX."
                    )
                }

                if (
                    activityDao.activityExists(
                        fileName
                    )
                ) {
                    throw IllegalStateException(
                        "Ta aktywność została już zaimportowana."
                    )
                }

                val inputStream =
                    context.contentResolver
                        .openInputStream(uri)
                        ?: throw IllegalArgumentException(
                            "Nie udało się otworzyć pliku."
                        )

                val parsed =
                    inputStream.use { stream ->

                        when (extension) {

                            "fit" -> {
                                parseFit(stream)
                            }

                            "tcx" -> {
                                parseTcx(stream)
                            }

                            else -> {
                                throw IllegalArgumentException(
                                    "Nieobsługiwany format."
                                )
                            }
                        }
                    }

                if (parsed.trackPoints.isEmpty()) {
                    throw IllegalArgumentException(
                        "Aktywność nie zawiera punktów GPS."
                    )
                }

                val activityName =
                    fileName.substringBeforeLast(
                        ".",
                        fileName
                    )

                val averagePace =
                    speedToPaceSecondsPerKm(
                        parsed.averageSpeedMetersPerSecond
                    )

                val bestPace =
                    speedToPaceSecondsPerKm(
                        parsed.maxSpeedMetersPerSecond
                    )

                val activity =
                    ActivityEntity(
                        timestamp =
                            parsed.startTimeMillis
                                ?: System.currentTimeMillis(),

                        name =
                            activityName,

                        sourceFileName =
                            fileName,

                        durationMillis = parsed.durations.totalMillis,
                        movingDurationMillis = parsed.durations.movingMillis,

                        distanceMeters =
                            parsed.distanceMeters,

                        calories =
                            parsed.calories.toDouble(),

                        avgSpeedMs =
                            parsed.averageSpeedMetersPerSecond,

                        maxSpeedMs =
                            parsed.maxSpeedMetersPerSecond,

                        avgPaceSecPerKm =
                            averagePace,

                        maxPaceSecPerKm =
                            bestPace,

                        minAltitudeMeters =
                            parsed.minAltitudeMeters,

                        maxAltitudeMeters =
                            parsed.maxAltitudeMeters,

                        avgHeartRate =
                            parsed.averageHeartRate,

                        maxHeartRate =
                            parsed.maxHeartRate
                    )

                val saved = activitySaver.save(activity, parsed.trackPoints)

                ActivityImportResult(
                    activityId = saved.activityId,
                    poiDetection = saved.poiDetection,

                    fileName =
                        fileName,

                    durationSeconds = parsed.durations.totalMillis / 1000.0,

                    distanceMeters =
                        parsed.distanceMeters,

                    calories =
                        parsed.calories,

                    averageSpeedMetersPerSecond =
                        parsed.averageSpeedMetersPerSecond,

                    maxSpeedMetersPerSecond =
                        parsed.maxSpeedMetersPerSecond,

                    averageHeartRate =
                        parsed.averageHeartRate,

                    maxHeartRate =
                        parsed.maxHeartRate,

                    minAltitudeMeters =
                        parsed.minAltitudeMeters,

                    maxAltitudeMeters =
                        parsed.maxAltitudeMeters,

                    startTimeMillis =
                        parsed.startTimeMillis,

                    trackPoints =
                        parsed.trackPoints
                )
            }
        }
    }

    private fun parseFit(
        inputStream: java.io.InputStream
    ): ParsedActivity {

        val data =
            FitParser.parse(
                inputStream
            )

        val points =
            data.trackPoints.map { point ->

                TrackPoint(
                    lat =
                        point.latitude,

                    lon =
                        point.longitude,

                    ele =
                        point.altitudeMeters
                            ?: 0.0,

                    timestampSeconds =
                        point.timestampSeconds
                            ?.let {
                                fitTimestampToUnixSeconds(
                                    it
                                )
                            },

                    heartRate =
                        point.heartRate,

                    speedMs =
                        point.speedMetersPerSecond
                )
            }

        return ParsedActivity(
            startTimeMillis =
                data.startTimeMillis,

            // FIT: total_elapsed_time → całkowity, total_timer_time → w ruchu
            durations = resolveDurations(
                totalMillis = data.totalElapsedSeconds?.let { (it * 1000.0).toLong() },
                movingMillis = data.totalTimerSeconds?.let { (it * 1000.0).toLong() },
            ),

            distanceMeters =
                data.distanceMeters,

            calories =
                data.calories,

            averageSpeedMetersPerSecond =
                data.averageSpeedMetersPerSecond,

            maxSpeedMetersPerSecond =
                data.maxSpeedMetersPerSecond,

            averageHeartRate =
                data.averageHeartRate,

            maxHeartRate =
                data.maxHeartRate,

            minAltitudeMeters =
                data.minAltitudeMeters,

            maxAltitudeMeters =
                data.maxAltitudeMeters,

            trackPoints =
                points
        )
    }

    private fun parseTcx(
        inputStream: java.io.InputStream
    ): ParsedActivity {

        val data =
            TcxParser.parse(
                inputStream
            )

        val points =
            data.trackPoints.map { point ->

                TrackPoint(
                    lat =
                        point.latitude,

                    lon =
                        point.longitude,

                    ele =
                        point.altitudeMeters
                            ?: 0.0,

                    timestampSeconds =
                        point.timestampMillis
                            ?.div(1000L),

                    heartRate =
                        point.heartRate,

                    speedMs =
                        point.speedMetersPerSecond
                )
            }

        return ParsedActivity(
            startTimeMillis =
                data.startTimeMillis,

            // TCX: czas z punktów (≤ 24 h) → całkowity, suma TotalTimeSeconds → w ruchu
            durations = resolveDurations(
                totalMillis = data.elapsedMillisFromPoints,
                movingMillis = (data.movingSeconds * 1000.0).toLong(),
            ),

            distanceMeters =
                data.distanceMeters,

            calories =
                data.calories,

            averageSpeedMetersPerSecond =
                data.averageSpeedMetersPerSecond,

            maxSpeedMetersPerSecond =
                data.maxSpeedMetersPerSecond,

            averageHeartRate =
                data.averageHeartRate,

            maxHeartRate =
                data.maxHeartRate,

            minAltitudeMeters =
                data.minAltitudeMeters,

            maxAltitudeMeters =
                data.maxAltitudeMeters,

            trackPoints =
                points
        )
    }

    private fun fitTimestampToUnixSeconds(
        fitTimestampSeconds: Long
    ): Long {

        return fitTimestampSeconds +
                FIT_EPOCH_OFFSET_SECONDS
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var fileName =
            "activity"

        context.contentResolver
            .query(
                uri,
                null,
                null,
                null,
                null
            )
            ?.use { cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (
                    nameIndex >= 0 &&
                    cursor.moveToFirst()
                ) {

                    fileName =
                        cursor.getString(
                            nameIndex
                        )
                            ?: "activity"
                }
            }

        return fileName
    }

    private data class ParsedActivity(
        val startTimeMillis: Long?,
        val durations: ActivityDurations,
        val distanceMeters: Double,
        val calories: Int,
        val averageSpeedMetersPerSecond: Double,
        val maxSpeedMetersPerSecond: Double,
        val averageHeartRate: Int,
        val maxHeartRate: Int,
        val minAltitudeMeters: Double,
        val maxAltitudeMeters: Double,
        val trackPoints: List<TrackPoint>
    )

    companion object {
        private const val FIT_EPOCH_OFFSET_SECONDS =
            631065600L
    }
}