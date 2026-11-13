package com.example.tatratracker.health

import com.example.tatratracker.data.ActivityEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateActivityRuleTest {

    private val start = 1_762_412_400_000L // 06.11.2025 07:00 UTC
    private val minute = 60_000L

    private fun fileActivity(
        startMillis: Long,
        distanceMeters: Double,
        sourceFileName: String = "Morning_Hike.fit",
    ) = ActivityEntity(
        timestamp = startMillis,
        name = "plik",
        sourceFileName = sourceFileName,
        distanceMeters = distanceMeters,
    )

    @Test
    fun fileActivityWithCloseStartAndDistance_isDuplicate() {
        val existing = listOf(fileActivity(start + 4 * minute, 20_600.0))

        assertTrue(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, existing))
    }

    @Test
    fun startExactlyFiveMinutesApart_isDuplicate() {
        val existing = listOf(fileActivity(start - 5 * minute, 20_000.0))

        assertTrue(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, existing))
    }

    @Test
    fun startMoreThanFiveMinutesApart_isNotDuplicate() {
        val existing = listOf(fileActivity(start + 5 * minute + 1_000L, 20_000.0))

        assertFalse(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, existing))
    }

    @Test
    fun distanceDifferingByFivePercentOrMore_isNotDuplicate() {
        val existing = listOf(fileActivity(start, 21_000.0))

        assertFalse(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, existing))
    }

    @Test
    fun activityFromSamsungHealth_isIgnored() {
        val existing = listOf(fileActivity(start, 20_000.0, sourceFileName = "samsung-health:abc"))

        assertFalse(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, existing))
    }

    @Test
    fun noExistingActivities_isNotDuplicate() {
        assertFalse(DuplicateActivityRule.isDuplicateOfFileActivity(start, 20_000.0, emptyList()))
    }
}
