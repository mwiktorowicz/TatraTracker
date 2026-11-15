package com.example.tatratracker.summary

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivitySummaryTest {

    private fun place(id: Long, name: String, category: String = SavedPlaceCategory.MOUNTAIN) =
        SavedPlaceEntity(id = id, name = name, category = category, latitude = 49.2, longitude = 20.0)

    private fun visit(id: Long, placeId: Long, activityId: Int?, visitedAt: Long) =
        PoiVisitEntity(
            id = id,
            placeId = placeId,
            activityId = activityId,
            visitedAt = visitedAt,
            source = if (activityId == null) PoiVisitSource.MANUAL else PoiVisitSource.ACTIVITY
        )

    private val places = listOf(
        place(1, "Giewont"),
        place(2, "Schronisko na Hali Kondratowej", SavedPlaceCategory.HUT),
        place(3, "Kopa Kondracka"),
        place(4, "Wodospad", SavedPlaceCategory.OTHER)
    )

    private fun newPlaceNames(activityId: Int, visits: List<PoiVisitEntity>): List<String> {
        return ActivitySummaryCalculator.newPlaces(activityId, places, visits).map { it.name }
    }

    // ---------- nowe miejsca ----------

    @Test
    fun newPlaces_firstVisitByThisActivity_isNew() {
        val visits = listOf(
            visit(1, placeId = 1, activityId = 7, visitedAt = 1_000),
            visit(2, placeId = 2, activityId = 7, visitedAt = 1_000)
        )

        assertEquals(listOf("Giewont", "Schronisko na Hali Kondratowej"), newPlaceNames(7, visits))
    }

    @Test
    fun newPlaces_placeVisitedEarlierByOtherActivity_isNotNew() {
        val visits = listOf(
            visit(1, placeId = 1, activityId = 3, visitedAt = 500),
            visit(2, placeId = 1, activityId = 7, visitedAt = 1_000),
            visit(3, placeId = 3, activityId = 7, visitedAt = 1_000)
        )

        assertEquals(listOf("Kopa Kondracka"), newPlaceNames(7, visits))
    }

    @Test
    fun newPlaces_earlierManualVisit_countsAsEarlier() {
        val visits = listOf(
            visit(1, placeId = 1, activityId = null, visitedAt = 500),
            visit(2, placeId = 1, activityId = 7, visitedAt = 1_000)
        )

        assertEquals(emptyList<String>(), newPlaceNames(7, visits))
    }

    @Test
    fun newPlaces_laterVisitsDoNotTakeAwayFirstVisit() {
        // Starsza wędrówka zostaje „pierwszą” także po zaimportowaniu późniejszych.
        val visits = listOf(
            visit(1, placeId = 1, activityId = 9, visitedAt = 2_000),
            visit(2, placeId = 1, activityId = 7, visitedAt = 1_000)
        )

        assertEquals(listOf("Giewont"), newPlaceNames(7, visits))
        assertEquals(emptyList<String>(), newPlaceNames(9, visits))
    }

    @Test
    fun newPlaces_otherCategoryIsSkipped() {
        val visits = listOf(visit(1, placeId = 4, activityId = 7, visitedAt = 1_000))

        assertEquals(emptyList<String>(), newPlaceNames(7, visits))
    }

    @Test
    fun newPlaces_activityWithoutVisits_givesEmptyList() {
        val visits = listOf(visit(1, placeId = 1, activityId = 3, visitedAt = 500))

        assertEquals(emptyList<String>(), newPlaceNames(7, visits))
    }

    // ---------- nazwy na karcie do udostępnienia ----------

    @Test
    fun placeNamesLine_fewNames_listsAll() {
        assertEquals(
            "Giewont, Kopa Kondracka",
            ActivitySummaryCalculator.placeNamesLine(listOf("Giewont", "Kopa Kondracka"), 3)
        )
    }

    @Test
    fun placeNamesLine_tooManyNames_countsTheRest() {
        assertEquals(
            "Giewont, Kopa Kondracka +2",
            ActivitySummaryCalculator.placeNamesLine(listOf("Giewont", "Kopa Kondracka", "Rysy", "Krywań"), 2)
        )
    }

    @Test
    fun placeNamesLine_noNames_isEmpty() {
        assertEquals("", ActivitySummaryCalculator.placeNamesLine(emptyList(), 3))
    }

    // ---------- punkt odniesienia postępu ----------

    private fun record(fraction: Double, maxActivityId: Int) = TrailProgressRecord(fraction, maxActivityId)

    @Test
    fun advanceBaseline_firstResult_hasNoPrevious() {
        val baseline = ActivitySummaryCalculator.advanceBaseline(TrailProgressBaseline(), 0.10, 5)

        assertEquals(TrailProgressBaseline(current = record(0.10, 5)), baseline)
    }

    @Test
    fun advanceBaseline_newerActivity_movesCurrentToPrevious() {
        val start = TrailProgressBaseline(current = record(0.10, 5), previous = record(0.08, 4))

        val baseline = ActivitySummaryCalculator.advanceBaseline(start, 0.12, 6)

        assertEquals(TrailProgressBaseline(current = record(0.12, 6), previous = record(0.10, 5)), baseline)
    }

    @Test
    fun advanceBaseline_sameActivities_keepsPrevious() {
        val start = TrailProgressBaseline(current = record(0.12, 6), previous = record(0.10, 5))

        val baseline = ActivitySummaryCalculator.advanceBaseline(start, 0.12, 6)

        assertEquals(start, baseline)
    }

    @Test
    fun advanceBaseline_newestActivityDeleted_dropsPreviousThatIsNotOlder() {
        val start = TrailProgressBaseline(current = record(0.12, 6), previous = record(0.10, 5))

        val baseline = ActivitySummaryCalculator.advanceBaseline(start, 0.10, 5)

        assertEquals(TrailProgressBaseline(current = record(0.10, 5)), baseline)
    }

    // ---------- przyrost po wędrówce ----------

    @Test
    fun trailGrowth_activityBetweenPreviousAndCurrent_givesDifference() {
        val baseline = TrailProgressBaseline(current = record(0.125, 6), previous = record(0.10, 5))

        assertEquals(0.025, ActivitySummaryCalculator.trailGrowth(baseline, 6)!!, 1e-9)
    }

    @Test
    fun trailGrowth_severalActivitiesInOneSync_shareTheGrowth() {
        val baseline = TrailProgressBaseline(current = record(0.15, 8), previous = record(0.10, 5))

        assertEquals(0.05, ActivitySummaryCalculator.trailGrowth(baseline, 6)!!, 1e-9)
        assertEquals(0.05, ActivitySummaryCalculator.trailGrowth(baseline, 8)!!, 1e-9)
    }

    @Test
    fun trailGrowth_olderActivity_hasNoGrowth() {
        val baseline = TrailProgressBaseline(current = record(0.125, 6), previous = record(0.10, 5))

        assertNull(ActivitySummaryCalculator.trailGrowth(baseline, 5))
        assertNull(ActivitySummaryCalculator.trailGrowth(baseline, 2))
    }

    @Test
    fun trailGrowth_progressNotYetCoveringActivity_hasNoGrowth() {
        val baseline = TrailProgressBaseline(current = record(0.125, 6), previous = record(0.10, 5))

        assertNull(ActivitySummaryCalculator.trailGrowth(baseline, 7))
    }

    @Test
    fun trailGrowth_withoutPrevious_hasNoGrowth() {
        assertNull(ActivitySummaryCalculator.trailGrowth(TrailProgressBaseline(current = record(0.1, 6)), 6))
        assertNull(ActivitySummaryCalculator.trailGrowth(TrailProgressBaseline(), 6))
    }

    @Test
    fun trailGrowth_neverNegative() {
        val baseline = TrailProgressBaseline(current = record(0.09, 6), previous = record(0.10, 5))

        assertEquals(0.0, ActivitySummaryCalculator.trailGrowth(baseline, 6)!!, 1e-9)
    }
}
