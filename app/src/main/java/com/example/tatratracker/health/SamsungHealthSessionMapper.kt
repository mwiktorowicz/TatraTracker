package com.example.tatratracker.health

import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.fit.resolveDurations
import com.example.tatratracker.fit.speedToPaceSecondsPerKm
import com.example.tatratracker.model.TrackPoint
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class MappedActivity(
    val activity: ActivityEntity,
    val trackPoints: List<TrackPoint>,
)

/**
 * Czyste funkcje: które treningi z Samsung Health importujemy i jak zamieniamy
 * sesję na model aplikacji (ActivityEntity + punkty trasy).
 */
object SamsungHealthSessionMapper {

    /** Jedyna aplikacja, z której bierzemy treningi (wpisy np. Stravy to puste duplikaty). */
    const val SAMSUNG_HEALTH_APP_ID = "com.sec.android.app.shealth"

    /**
     * Importowane typy ćwiczeń – nazwy wartości PredefinedExerciseType z SDK.
     * Żeby dodać typ, dopisz go tutaj (i jego nazwę w EXERCISE_TYPE_LABELS).
     */
    val ALLOWED_EXERCISE_TYPES: Set<String> = setOf("HIKING")

    private val EXERCISE_TYPE_LABELS = mapOf(
        "HIKING" to "Wędrówka",
    )

    const val SOURCE_ID_PREFIX = "samsung-health:"

    /** Wpis logu dalej niż tyle od punktu trasy nie jest do niego przypisywany. */
    const val MAX_LOG_DISTANCE_MILLIS = 90_000L

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun isImportable(record: HealthExerciseRecord, session: HealthSession): Boolean =
        record.appId == SAMSUNG_HEALTH_APP_ID &&
            exerciseTypeOf(record, session) in ALLOWED_EXERCISE_TYPES &&
            session.route.isNotEmpty()

    /** Pierwsza sesja punktu danych: "samsung-health:<uid>", kolejne: "samsung-health:<uid>:<n>". */
    fun sourceId(uid: String, sessionIndex: Int): String =
        if (sessionIndex == 0) {
            "$SOURCE_ID_PREFIX$uid"
        } else {
            "$SOURCE_ID_PREFIX$uid:$sessionIndex"
        }

    /**
     * Punkty trasy z tętnem i prędkością z najbliższego czasowo wpisu logu
     * (osobno dla każdej wartości), o ile wpis jest nie dalej niż 90 s.
     */
    fun toTrackPoints(route: List<HealthRoutePoint>, log: List<HealthLogEntry>): List<TrackPoint> {
        val heartRates = TimedValues(
            log.mapNotNull { entry ->
                entry.heartRate?.takeIf { it > 0.0 }?.let { entry.timestamp.toEpochMilli() to it }
            }
        )
        val speeds = TimedValues(
            log.mapNotNull { entry ->
                entry.speed?.takeIf { it >= 0.0 }?.let { entry.timestamp.toEpochMilli() to it }
            }
        )

        return route.map { point ->
            val timeMillis = point.timestamp.toEpochMilli()
            TrackPoint(
                lat = point.latitude,
                lon = point.longitude,
                ele = point.altitude ?: 0.0,
                timestampSeconds = point.timestamp.epochSecond,
                heartRate = heartRates.nearest(timeMillis)?.roundToInt(),
                speedMs = speeds.nearest(timeMillis),
            )
        }
    }

    fun toActivity(
        record: HealthExerciseRecord,
        sessionIndex: Int,
        session: HealthSession,
        fallbackZone: ZoneId,
    ): MappedActivity {
        val trackPoints = toTrackPoints(session.route, session.log)

        val durations = resolveDurations(
            totalMillis = Duration.between(session.startTime, session.endTime).toMillis(),
            movingMillis = session.duration?.toMillis(),
        )

        val distance = session.distanceMeters?.takeIf { it > 0.0 }
            ?: distanceFromPoints(trackPoints)

        val averageSpeed =
            if (durations.movingMillis > 0L) {
                distance / (durations.movingMillis / 1000.0)
            } else {
                0.0
            }

        val maxSpeed = session.maxSpeed?.takeIf { it > 0.0 }
            ?: trackPoints.mapNotNull { it.speedMs }.maxOrNull()
            ?: 0.0

        val pointAltitudes = session.route.mapNotNull { it.altitude }
        val pointHeartRates = trackPoints.mapNotNull { it.heartRate }

        val activity = ActivityEntity(
            timestamp = session.startTime.toEpochMilli(),
            name = activityName(record, session, record.zoneOffset ?: fallbackZone),
            sourceFileName = sourceId(record.uid, sessionIndex),
            durationMillis = durations.totalMillis,
            movingDurationMillis = durations.movingMillis,
            distanceMeters = distance,
            calories = session.calories.coerceAtLeast(0.0),
            avgSpeedMs = averageSpeed,
            maxSpeedMs = maxSpeed,
            avgPaceSecPerKm = speedToPaceSecondsPerKm(averageSpeed),
            maxPaceSecPerKm = speedToPaceSecondsPerKm(maxSpeed),
            minAltitudeMeters = session.minAltitude ?: pointAltitudes.minOrNull() ?: 0.0,
            maxAltitudeMeters = session.maxAltitude ?: pointAltitudes.maxOrNull() ?: 0.0,
            avgHeartRate = session.meanHeartRate?.takeIf { it > 0.0 }?.roundToInt()
                ?: pointHeartRates.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
                ?: 0,
            maxHeartRate = session.maxHeartRate?.takeIf { it > 0.0 }?.roundToInt()
                ?: pointHeartRates.maxOrNull()
                ?: 0,
        )

        return MappedActivity(activity = activity, trackPoints = trackPoints)
    }

    private fun exerciseTypeOf(record: HealthExerciseRecord, session: HealthSession): String? =
        session.exerciseType ?: record.exerciseType

    private fun activityName(record: HealthExerciseRecord, session: HealthSession, zone: ZoneId): String {
        session.customTitle?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val label = EXERCISE_TYPE_LABELS[exerciseTypeOf(record, session)] ?: "Aktywność"
        val date = dateFormatter.format(session.startTime.atZone(zone))
        return "$label $date"
    }

    private fun distanceFromPoints(points: List<TrackPoint>): Double =
        points.zipWithNext { a, b -> haversineMeters(a.lat, a.lon, b.lat, b.lon) }.sum()

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    /** Wartości z czasem (ms), posortowane; szuka najbliższej w czasie. */
    private class TimedValues(values: List<Pair<Long, Double>>) {

        private val sorted = values.sortedBy { it.first }
        private val times = LongArray(sorted.size) { sorted[it].first }

        fun nearest(timeMillis: Long): Double? {
            if (times.isEmpty()) {
                return null
            }

            val searchIndex = times.binarySearch(timeMillis)
            val index =
                if (searchIndex >= 0) {
                    searchIndex
                } else {
                    val insertion = -searchIndex - 1
                    when {
                        insertion == 0 -> 0
                        insertion == times.size -> times.size - 1
                        timeMillis - times[insertion - 1] <= times[insertion] - timeMillis -> insertion - 1
                        else -> insertion
                    }
                }

            return sorted[index].second.takeIf { abs(times[index] - timeMillis) <= MAX_LOG_DISTANCE_MILLIS }
        }
    }
}
