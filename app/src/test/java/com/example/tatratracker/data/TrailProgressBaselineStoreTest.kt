package com.example.tatratracker.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.tatratracker.summary.TrailProgressBaseline
import com.example.tatratracker.summary.TrailProgressRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class TrailProgressBaselineStoreTest {

    private fun roundTrip(baseline: TrailProgressBaseline): TrailProgressBaseline {
        val preferences = mutablePreferencesOf()
        preferences.writeTrailProgressBaseline(baseline)
        return preferences.toTrailProgressBaseline()
    }

    @Test
    fun emptyPreferences_giveEmptyBaseline() {
        assertEquals(TrailProgressBaseline(), emptyPreferences().toTrailProgressBaseline())
    }

    @Test
    fun fullBaseline_isReadBack() {
        val baseline = TrailProgressBaseline(
            current = TrailProgressRecord(0.125, 12),
            previous = TrailProgressRecord(0.1, 11)
        )

        assertEquals(baseline, roundTrip(baseline))
    }

    @Test
    fun currentOnly_isReadBack() {
        val baseline = TrailProgressBaseline(current = TrailProgressRecord(0.0, 0))

        assertEquals(baseline, roundTrip(baseline))
    }

    @Test
    fun writingWithoutPrevious_removesStoredPrevious() {
        val preferences = mutablePreferencesOf()
        preferences.writeTrailProgressBaseline(
            TrailProgressBaseline(
                current = TrailProgressRecord(0.125, 12),
                previous = TrailProgressRecord(0.1, 11)
            )
        )

        val onlyCurrent = TrailProgressBaseline(current = TrailProgressRecord(0.1, 11))
        preferences.writeTrailProgressBaseline(onlyCurrent)

        assertEquals(onlyCurrent, preferences.toTrailProgressBaseline())
    }

    @Test
    fun incompleteRecord_countsAsMissing() {
        val preferences = mutablePreferencesOf()
        preferences[TrailProgressBaselineKeys.CURRENT_FRACTION] = 0.2

        assertEquals(TrailProgressBaseline(), preferences.toTrailProgressBaseline())
    }

    @Test
    fun fractionOutsideRange_countsAsMissing() {
        val preferences = mutablePreferencesOf()
        preferences[TrailProgressBaselineKeys.CURRENT_FRACTION] = 1.5
        preferences[TrailProgressBaselineKeys.CURRENT_MAX_ACTIVITY_ID] = 3

        assertEquals(TrailProgressBaseline(), preferences.toTrailProgressBaseline())
    }

    @Test
    fun valueOfWrongType_countsAsMissing() {
        val preferences = mutablePreferencesOf()
        preferences[stringPreferencesKey("current_fraction")] = "dużo"
        preferences[TrailProgressBaselineKeys.CURRENT_MAX_ACTIVITY_ID] = 3

        assertEquals(TrailProgressBaseline(), preferences.toTrailProgressBaseline())
    }
}
