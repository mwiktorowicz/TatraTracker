package com.example.tatratracker.fit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityMetricsTest {

    private val hour = 3_600_000L

    @Test
    fun bothDurationsKnown_areKeptSeparately() {
        val result = resolveDurations(totalMillis = 8 * hour, movingMillis = 6 * hour)

        assertEquals(ActivityDurations(totalMillis = 8 * hour, movingMillis = 6 * hour), result)
    }

    @Test
    fun missingTotal_usesMovingForBoth() {
        val result = resolveDurations(totalMillis = null, movingMillis = 6 * hour)

        assertEquals(ActivityDurations(totalMillis = 6 * hour, movingMillis = 6 * hour), result)
    }

    @Test
    fun missingMoving_usesTotalForBoth() {
        val result = resolveDurations(totalMillis = 8 * hour, movingMillis = 0L)

        assertEquals(ActivityDurations(totalMillis = 8 * hour, movingMillis = 8 * hour), result)
    }

    @Test
    fun totalShorterThanMoving_isRaisedToMoving() {
        val result = resolveDurations(totalMillis = 5 * hour, movingMillis = 6 * hour)

        assertEquals(ActivityDurations(totalMillis = 6 * hour, movingMillis = 6 * hour), result)
    }

    @Test
    fun bothMissing_givesZero() {
        val result = resolveDurations(totalMillis = null, movingMillis = null)

        assertEquals(ActivityDurations(totalMillis = 0L, movingMillis = 0L), result)
    }

    @Test
    fun elapsedFromPoints_isDifferenceBetweenLastAndFirstTimestamp() {
        val result = elapsedMillisFromPoints(listOf(1_000L, null, 5_000L, 61_000L))

        assertEquals(60_000L, result)
    }

    @Test
    fun elapsedFromPoints_withoutTwoTimestamps_isNull() {
        assertNull(elapsedMillisFromPoints(listOf(null, 1_000L, null)))
        assertNull(elapsedMillisFromPoints(emptyList()))
    }

    @Test
    fun elapsedFromPoints_exactly24Hours_isAccepted() {
        assertEquals(24 * hour, elapsedMillisFromPoints(listOf(0L, 24 * hour)))
    }

    @Test
    fun elapsedFromPoints_over24Hours_isRejected() {
        assertNull(elapsedMillisFromPoints(listOf(0L, 24 * hour + 1)))
    }

    @Test
    fun pace_isSecondsPerKilometre() {
        assertEquals(500.0, speedToPaceSecondsPerKm(2.0), 1e-9)
        assertEquals(0.0, speedToPaceSecondsPerKm(0.0), 1e-9)
    }
}
