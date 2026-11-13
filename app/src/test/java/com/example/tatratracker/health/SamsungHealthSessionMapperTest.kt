package com.example.tatratracker.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SamsungHealthSessionMapperTest {

    private val start: Instant = Instant.parse("2026-11-06T06:00:00Z")
    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    private fun routePoint(secondsFromStart: Long, altitude: Double? = 1000.0) =
        HealthRoutePoint(
            timestamp = start.plusSeconds(secondsFromStart),
            latitude = 49.2 + secondsFromStart * 0.0001,
            longitude = 20.0,
            altitude = altitude,
        )

    private fun logEntry(secondsFromStart: Long, heartRate: Double? = null, speed: Double? = null) =
        HealthLogEntry(
            timestamp = start.plusSeconds(secondsFromStart),
            heartRate = heartRate,
            speed = speed,
        )

    private fun session(
        exerciseType: String? = "HIKING",
        route: List<HealthRoutePoint> = listOf(routePoint(0), routePoint(60)),
        log: List<HealthLogEntry> = emptyList(),
        endTime: Instant = start.plus(Duration.ofHours(8)),
        duration: Duration? = Duration.ofHours(6),
        distanceMeters: Double? = 21_600.0,
        customTitle: String? = null,
        minAltitude: Double? = 900.0,
        maxAltitude: Double? = 2_300.0,
        meanHeartRate: Double? = 120.0,
        maxHeartRate: Double? = 170.0,
        maxSpeed: Double? = 2.5,
    ) = HealthSession(
        startTime = start,
        endTime = endTime,
        duration = duration,
        exerciseType = exerciseType,
        customTitle = customTitle,
        distanceMeters = distanceMeters,
        calories = 1500.0,
        minAltitude = minAltitude,
        maxAltitude = maxAltitude,
        meanHeartRate = meanHeartRate,
        maxHeartRate = maxHeartRate,
        maxSpeed = maxSpeed,
        route = route,
        log = log,
    )

    private fun record(
        appId: String? = "com.sec.android.app.shealth",
        exerciseType: String? = "HIKING",
        sessions: List<HealthSession> = listOf(session()),
    ) = HealthExerciseRecord(
        uid = "abc",
        appId = appId,
        exerciseType = exerciseType,
        zoneOffset = ZoneOffset.ofHours(1),
        sessions = sessions,
    )

    // --- filtr ---

    @Test
    fun hikingFromSamsungHealthWithRoute_isImportable() {
        val record = record()

        assertTrue(SamsungHealthSessionMapper.isImportable(record, record.sessions[0]))
    }

    @Test
    fun entryFromOtherApp_isNotImportable() {
        val record = record(appId = "com.strava")

        assertFalse(SamsungHealthSessionMapper.isImportable(record, record.sessions[0]))
    }

    @Test
    fun walking_isNotImportable() {
        val record = record(exerciseType = "WALKING", sessions = listOf(session(exerciseType = "WALKING")))

        assertFalse(SamsungHealthSessionMapper.isImportable(record, record.sessions[0]))
    }

    @Test
    fun sessionWithoutRoute_isNotImportable() {
        val record = record(sessions = listOf(session(route = emptyList())))

        assertFalse(SamsungHealthSessionMapper.isImportable(record, record.sessions[0]))
    }

    @Test
    fun sessionWithoutType_usesTypeOfDataPoint() {
        val hiking = record(exerciseType = "HIKING", sessions = listOf(session(exerciseType = null)))
        val walking = record(exerciseType = "WALKING", sessions = listOf(session(exerciseType = null)))

        assertTrue(SamsungHealthSessionMapper.isImportable(hiking, hiking.sessions[0]))
        assertFalse(SamsungHealthSessionMapper.isImportable(walking, walking.sessions[0]))
    }

    // --- identyfikator ---

    @Test
    fun firstSessionUsesUid_nextSessionsGetIndex() {
        assertEquals("samsung-health:abc", SamsungHealthSessionMapper.sourceId("abc", 0))
        assertEquals("samsung-health:abc:1", SamsungHealthSessionMapper.sourceId("abc", 1))
    }

    // --- przypisanie tętna i prędkości ---

    @Test
    fun pointGetsValuesFromNearestLogEntry() {
        val route = listOf(routePoint(100))
        val log = listOf(
            logEntry(40, heartRate = 110.0, speed = 1.0),
            logEntry(130, heartRate = 140.0, speed = 1.4),
        )

        val points = SamsungHealthSessionMapper.toTrackPoints(route, log)

        assertEquals(140, points[0].heartRate)
        assertEquals(1.4, points[0].speedMs!!, 1e-9)
    }

    @Test
    fun logEntryFurtherThan90Seconds_isNotAssigned() {
        val route = listOf(routePoint(0), routePoint(180))
        val log = listOf(logEntry(91, heartRate = 120.0, speed = 1.2)) // 91 s od pierwszego, 89 s od drugiego

        val points = SamsungHealthSessionMapper.toTrackPoints(route, log)

        assertNull(points[0].heartRate)
        assertNull(points[0].speedMs)
        assertEquals(120, points[1].heartRate)
    }

    @Test
    fun logEntryExactly90SecondsAway_isAssigned() {
        val points = SamsungHealthSessionMapper.toTrackPoints(
            listOf(routePoint(0)),
            listOf(logEntry(90, heartRate = 120.0)),
        )

        assertEquals(120, points[0].heartRate)
    }

    @Test
    fun heartRateAndSpeedAreLookedUpSeparately() {
        val route = listOf(routePoint(100))
        val log = listOf(
            logEntry(95, heartRate = null, speed = 1.1),
            logEntry(150, heartRate = 125.0, speed = null),
        )

        val points = SamsungHealthSessionMapper.toTrackPoints(route, log)

        assertEquals(125, points[0].heartRate)
        assertEquals(1.1, points[0].speedMs!!, 1e-9)
    }

    @Test
    fun emptyLog_leavesHeartRateAndSpeedEmpty() {
        val points = SamsungHealthSessionMapper.toTrackPoints(listOf(routePoint(0)), emptyList())

        assertNull(points[0].heartRate)
        assertNull(points[0].speedMs)
    }

    @Test
    fun routePointKeepsPositionAltitudeAndTime() {
        val points = SamsungHealthSessionMapper.toTrackPoints(
            listOf(routePoint(60, altitude = 1234.5), routePoint(120, altitude = null)),
            emptyList(),
        )

        assertEquals(49.206, points[0].lat, 1e-9)
        assertEquals(20.0, points[0].lon, 1e-9)
        assertEquals(1234.5, points[0].ele, 1e-9)
        assertEquals(start.epochSecond + 60, points[0].timestampSeconds)
        assertEquals(0.0, points[1].ele, 1e-9)
    }

    // --- podsumowanie ---

    @Test
    fun twoDurations_totalFromStartToEnd_movingFromSessionDuration() {
        val mapped = SamsungHealthSessionMapper.toActivity(record(), 0, session(), zone)

        assertEquals(8 * 3_600_000L, mapped.activity.durationMillis)
        assertEquals(6 * 3_600_000L, mapped.activity.movingDurationMillis)
        assertEquals(start.toEpochMilli(), mapped.activity.timestamp)
    }

    @Test
    fun missingSessionDuration_makesBothDurationsEqual() {
        val mapped = SamsungHealthSessionMapper.toActivity(record(), 0, session(duration = null), zone)

        assertEquals(8 * 3_600_000L, mapped.activity.durationMillis)
        assertEquals(8 * 3_600_000L, mapped.activity.movingDurationMillis)
    }

    @Test
    fun averageSpeedAndPace_useMovingTime() {
        val mapped = SamsungHealthSessionMapper.toActivity(record(), 0, session(), zone)

        // 21,6 km w 6 h w ruchu = 1,0 m/s
        assertEquals(1.0, mapped.activity.avgSpeedMs, 1e-9)
        assertEquals(1000.0, mapped.activity.avgPaceSecPerKm, 1e-9)
        assertEquals(2.5, mapped.activity.maxSpeedMs, 1e-9)
        assertEquals(400.0, mapped.activity.maxPaceSecPerKm, 1e-9)
    }

    @Test
    fun summaryUsesSessionValues() {
        val mapped = SamsungHealthSessionMapper.toActivity(record(), 0, session(), zone)
        val activity = mapped.activity

        assertEquals("samsung-health:abc", activity.sourceFileName)
        assertEquals(21_600.0, activity.distanceMeters, 1e-9)
        assertEquals(1500.0, activity.calories, 1e-9)
        assertEquals(900.0, activity.minAltitudeMeters, 1e-9)
        assertEquals(2_300.0, activity.maxAltitudeMeters, 1e-9)
        assertEquals(120, activity.avgHeartRate)
        assertEquals(170, activity.maxHeartRate)
        assertEquals(2, mapped.trackPoints.size)
    }

    @Test
    fun emptySessionFields_areCalculatedFromPoints() {
        val route = listOf(
            routePoint(0, altitude = 1000.0),
            routePoint(60, altitude = 1100.0),
            routePoint(120, altitude = null),
        )
        val log = listOf(
            logEntry(0, heartRate = 100.0, speed = 1.0),
            logEntry(60, heartRate = 130.0, speed = 3.0),
            logEntry(120, heartRate = 130.0, speed = 2.0),
        )
        val emptySession = session(
            route = route,
            log = log,
            distanceMeters = null,
            minAltitude = null,
            maxAltitude = null,
            meanHeartRate = null,
            maxHeartRate = null,
            maxSpeed = null,
        )

        val activity = SamsungHealthSessionMapper.toActivity(record(), 0, emptySession, zone).activity

        // 0,012° szerokości ≈ 1334 m
        assertEquals(1334.0, activity.distanceMeters, 2.0)
        assertEquals(1000.0, activity.minAltitudeMeters, 1e-9)
        assertEquals(1100.0, activity.maxAltitudeMeters, 1e-9)
        assertEquals(120, activity.avgHeartRate)
        assertEquals(130, activity.maxHeartRate)
        assertEquals(3.0, activity.maxSpeedMs, 1e-9)
    }

    @Test
    fun noAltitudeAnywhere_givesZero() {
        val noAltitude = session(
            route = listOf(routePoint(0, altitude = null), routePoint(60, altitude = null)),
            minAltitude = null,
            maxAltitude = null,
        )

        val activity = SamsungHealthSessionMapper.toActivity(record(), 0, noAltitude, zone).activity

        assertEquals(0.0, activity.minAltitudeMeters, 1e-9)
        assertEquals(0.0, activity.maxAltitudeMeters, 1e-9)
    }

    @Test
    fun customTitle_isUsedAsName() {
        val mapped = SamsungHealthSessionMapper.toActivity(
            record(), 0, session(customTitle = "  Rysy od Popradu "), zone,
        )

        assertEquals("Rysy od Popradu", mapped.activity.name)
    }

    @Test
    fun blankCustomTitle_givesTypeAndLocalDate() {
        val lateStart = Instant.parse("2026-11-05T23:30:00Z") // 06.11 00:30 w Warszawie
        val lateSession = session(customTitle = " ").copy(startTime = lateStart)

        val mapped = SamsungHealthSessionMapper.toActivity(record(), 0, lateSession, zone)

        assertEquals("Wędrówka 06.11.2026", mapped.activity.name)
    }

    @Test
    fun secondSession_getsIndexedSourceId() {
        val mapped = SamsungHealthSessionMapper.toActivity(record(), 1, session(), zone)

        assertEquals("samsung-health:abc:1", mapped.activity.sourceFileName)
    }
}
