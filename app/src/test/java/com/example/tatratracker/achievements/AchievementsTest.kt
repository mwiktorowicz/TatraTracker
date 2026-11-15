package com.example.tatratracker.achievements

import com.example.tatratracker.collection.CollectionCatalog
import com.example.tatratracker.collection.CollectionItem
import com.example.tatratracker.collection.CollectionSet
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.summary.ActivitySummaryCalculator
import com.example.tatratracker.summary.TrailProgressBaseline
import com.example.tatratracker.summary.TrailProgressRecord
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 8): Long {
        return LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
    }

    private fun activity(
        id: Int,
        timestamp: Long,
        distanceKm: Double = 10.0,
        maxAltitude: Double = 1500.0
    ) = ActivityEntity(
        id = id,
        timestamp = timestamp,
        name = "Wędrówka $id",
        distanceMeters = distanceKm * 1000.0,
        maxAltitudeMeters = maxAltitude
    )

    // Każde miejsce ma własne współrzędne (ok. 1 km od sąsiada), żeby pozycje katalogu się nie myliły.
    private fun place(id: Long, name: String, category: String = SavedPlaceCategory.MOUNTAIN) =
        SavedPlaceEntity(
            id = id,
            name = name,
            category = category,
            latitude = 49.10 + id * 0.01,
            longitude = 20.0
        )

    private fun visit(placeId: Long, activityId: Int?, visitedAt: Long) =
        PoiVisitEntity(
            id = placeId * 1000 + (activityId ?: 0),
            placeId = placeId,
            activityId = activityId,
            visitedAt = visitedAt,
            source = if (activityId == null) PoiVisitSource.MANUAL else PoiVisitSource.ACTIVITY
        )

    private fun item(place: SavedPlaceEntity, elevation: Int) =
        CollectionItem(
            id = "node/${place.id}",
            name = place.name,
            category = place.category,
            elevationMeters = elevation,
            latitude = place.latitude,
            longitude = place.longitude
        )

    private val peaks = (1L..6L).map { place(it, "Szczyt $it") }
    private val highPeak = place(10, "Rysy")
    private val hutA = place(20, "Murowaniec", SavedPlaceCategory.HUT)
    private val hutB = place(21, "Ornak", SavedPlaceCategory.HUT)

    private val allPlaces = peaks + highPeak + hutA + hutB

    private val catalog =
        CollectionCatalog(
            source = "test",
            sets =
                listOf(
                    CollectionSet(
                        AchievementCalculator.POLISH_HUTS_SET_ID,
                        "Schroniska",
                        "",
                        listOf(item(hutA, 1500), item(hutB, 1100))
                    ),
                    CollectionSet(
                        "trail_peaks",
                        "Szczyty",
                        "",
                        listOf(item(highPeak, 2499), item(peaks[0], 1894))
                    )
                )
        )

    private fun calculate(
        activities: List<ActivityEntity> = emptyList(),
        visits: List<PoiVisitEntity> = emptyList(),
        catalog: CollectionCatalog? = this.catalog,
        completedTrailCount: Int? = 0
    ): AchievementsResult {
        return AchievementCalculator.calculate(activities, allPlaces, visits, catalog, completedTrailCount, zone)
    }

    private fun AchievementsResult.of(kind: AchievementKind): Achievement {
        return achievements.single { it.kind == kind }
    }

    // ---------- osiągnięcia ----------

    @Test
    fun noData_nothingEarned() {
        val result = calculate()

        assertEquals(AchievementKind.entries.size, result.achievements.size)
        assertEquals(0, result.earnedCount)
        assertEquals(emptyList<ActivityRecord>(), result.records)
    }

    @Test
    fun peaks_fifthDistinctPeakEarnsWithItsDate() {
        val visits =
            peaks.take(5).mapIndexed { index, peak -> visit(peak.id, index + 1, millis(2026, 6, index + 1)) } +
                // Powtórna wizyta na pierwszym szczycie nie liczy się jako kolejny szczyt.
                visit(peaks[0].id, 9, millis(2026, 5, 1))

        val result = calculate(visits = visits)
        val fivePeaks = result.of(AchievementKind.PEAKS_5)

        assertTrue(fivePeaks.isEarned)
        assertEquals(millis(2026, 6, 5), fivePeaks.earnedAt)
        assertEquals(5, fivePeaks.current)

        val tenPeaks = result.of(AchievementKind.PEAKS_10)

        assertFalse(tenPeaks.isEarned)
        assertNull(tenPeaks.earnedAt)
        assertEquals(5, tenPeaks.current)
        assertEquals(10, tenPeaks.target)
    }

    @Test
    fun firstHut_earnedByEarliestHutVisit_alsoManual() {
        val visits = listOf(
            visit(hutA.id, 3, millis(2026, 7, 10)),
            visit(hutB.id, null, millis(2026, 7, 2))
        )

        val firstHut = calculate(visits = visits).of(AchievementKind.FIRST_HUT)

        assertTrue(firstHut.isEarned)
        assertEquals(millis(2026, 7, 2), firstHut.earnedAt)
    }

    @Test
    fun highPeak_onlyCatalogPeakFrom2000Meters() {
        val lowOnly = calculate(visits = listOf(visit(peaks[0].id, 1, millis(2026, 6, 1))))

        assertFalse(lowOnly.of(AchievementKind.FIRST_PEAK_2000).isEarned)

        val withHigh = calculate(
            visits = listOf(
                visit(peaks[0].id, 1, millis(2026, 6, 1)),
                visit(highPeak.id, 2, millis(2026, 8, 15))
            )
        )

        assertTrue(withHigh.of(AchievementKind.FIRST_PEAK_2000).isEarned)
        assertEquals(millis(2026, 8, 15), withHigh.of(AchievementKind.FIRST_PEAK_2000).earnedAt)
    }

    @Test
    fun allPolishHuts_earnedWhenLastOneIsVisited() {
        val oneHut = calculate(visits = listOf(visit(hutA.id, 1, millis(2026, 6, 1))))
        val partial = oneHut.of(AchievementKind.ALL_POLISH_HUTS)

        assertFalse(partial.isEarned)
        assertEquals(1, partial.current)
        assertEquals(2, partial.target)

        val bothHuts = calculate(
            visits = listOf(
                visit(hutA.id, 1, millis(2026, 6, 1)),
                visit(hutB.id, 2, millis(2026, 9, 3))
            )
        )

        assertTrue(bothHuts.of(AchievementKind.ALL_POLISH_HUTS).isEarned)
        assertEquals(millis(2026, 9, 3), bothHuts.of(AchievementKind.ALL_POLISH_HUTS).earnedAt)
    }

    @Test
    fun withoutCatalog_catalogAchievementsStayUnearned() {
        val result = calculate(
            visits = listOf(
                visit(highPeak.id, 1, millis(2026, 6, 1)),
                visit(hutA.id, 1, millis(2026, 6, 1)),
                visit(hutB.id, 1, millis(2026, 6, 1))
            ),
            catalog = null
        )

        assertFalse(result.of(AchievementKind.FIRST_PEAK_2000).isEarned)
        assertFalse(result.of(AchievementKind.ALL_POLISH_HUTS).isEarned)
        assertTrue(result.of(AchievementKind.FIRST_HUT).isEarned)
    }

    @Test
    fun yearDistance_countsWithinOneCalendarYear() {
        // 60 km w 2025 i 60 km w 2026 – razem 120 km, ale w żadnym roku 100.
        val split = calculate(
            activities = listOf(
                activity(1, millis(2025, 12, 30), distanceKm = 60.0),
                activity(2, millis(2026, 1, 2), distanceKm = 60.0)
            )
        ).of(AchievementKind.DISTANCE_100_KM_YEAR)

        assertFalse(split.isEarned)
        assertEquals(60, split.current)

        val sameYear = calculate(
            activities = listOf(
                activity(1, millis(2026, 5, 1), distanceKm = 60.0),
                activity(2, millis(2026, 6, 1), distanceKm = 45.0),
                activity(3, millis(2026, 7, 1), distanceKm = 20.0)
            )
        ).of(AchievementKind.DISTANCE_100_KM_YEAR)

        assertTrue(sameYear.isEarned)
        assertEquals(millis(2026, 6, 1), sameYear.earnedAt)
        assertEquals(100, sameYear.current)
    }

    @Test
    fun completedTrail_followsTrailCount() {
        assertFalse(calculate(completedTrailCount = null).of(AchievementKind.FIRST_COMPLETED_TRAIL).isEarned)
        assertFalse(calculate(completedTrailCount = 0).of(AchievementKind.FIRST_COMPLETED_TRAIL).isEarned)
        assertTrue(calculate(completedTrailCount = 2).of(AchievementKind.FIRST_COMPLETED_TRAIL).isEarned)
    }

    // ---------- rekordy ----------

    @Test
    fun records_pickLongestAndHighest_earlierWinsTies() {
        val activities = listOf(
            activity(3, millis(2026, 8, 1), distanceKm = 21.0, maxAltitude = 1987.0),
            activity(1, millis(2026, 6, 1), distanceKm = 21.0, maxAltitude = 2499.0),
            activity(2, millis(2026, 7, 1), distanceKm = 12.0, maxAltitude = 0.0)
        )

        val records = calculate(activities = activities).records

        val distance = records.single { it.kind == ActivityRecordKind.DISTANCE }
        assertEquals(1, distance.activityId)
        assertEquals(21_000.0, distance.value, 1e-6)

        val altitude = records.single { it.kind == ActivityRecordKind.ALTITUDE }
        assertEquals(1, altitude.activityId)
        assertEquals(2499.0, altitude.value, 1e-6)
    }

    // ---------- co przyniosła wędrówka ----------

    @Test
    fun newForActivity_achievementEarnedByThisHike() {
        val activities = (1..5).map { activity(it, millis(2026, 6, it)) }
        val visits = peaks.take(5).mapIndexed { index, peak -> visit(peak.id, index + 1, millis(2026, 6, index + 1)) }
        val result = calculate(activities = activities, visits = visits)

        val fifth = AchievementCalculator.newForActivity(activities[4], result, activities, false)
        assertEquals(listOf(AchievementKind.PEAKS_5), fifth.achievements.map { it.kind })

        val fourth = AchievementCalculator.newForActivity(activities[3], result, activities, false)
        assertTrue(fourth.achievements.isEmpty())
    }

    @Test
    fun newForActivity_recordsBeatenAtThatTime() {
        val activities = listOf(
            activity(1, millis(2026, 6, 1), distanceKm = 10.0, maxAltitude = 1500.0),
            activity(2, millis(2026, 7, 1), distanceKm = 15.0, maxAltitude = 1400.0),
            activity(3, millis(2026, 8, 1), distanceKm = 20.0, maxAltitude = 2000.0)
        )
        val result = calculate(activities = activities)

        // Pierwsza wędrówka nie ma czego pobić.
        assertTrue(AchievementCalculator.newForActivity(activities[0], result, activities, false).isEmpty)

        // Druga pobiła dystans w swoim czasie, choć dziś rekord trzyma trzecia.
        val second = AchievementCalculator.newForActivity(activities[1], result, activities, false)
        assertEquals(listOf(ActivityRecordKind.DISTANCE), second.records.map { it.kind })
        assertEquals(15_000.0, second.records.single().value, 1e-6)

        val third = AchievementCalculator.newForActivity(activities[2], result, activities, false)
        assertEquals(
            listOf(ActivityRecordKind.DISTANCE, ActivityRecordKind.ALTITUDE),
            third.records.map { it.kind }
        )
    }

    @Test
    fun newForActivity_firstCompletedTrailOnlyWhenBaselineSaysSo() {
        val activities = listOf(activity(1, millis(2026, 6, 1)))
        val result = calculate(activities = activities, completedTrailCount = 1)

        val without = AchievementCalculator.newForActivity(activities[0], result, activities, false)
        assertTrue(without.achievements.isEmpty())

        val with = AchievementCalculator.newForActivity(activities[0], result, activities, true)
        assertEquals(listOf(AchievementKind.FIRST_COMPLETED_TRAIL), with.achievements.map { it.kind })
    }

    // ---------- pierwszy ukończony szlak z zapamiętanego postępu ----------

    @Test
    fun firstTrailCompleted_whenCountGoesFromZero() {
        val baseline = TrailProgressBaseline(
            current = TrailProgressRecord(0.12, 6, completedTrailCount = 1),
            previous = TrailProgressRecord(0.10, 5, completedTrailCount = 0)
        )

        assertTrue(ActivitySummaryCalculator.firstTrailCompleted(baseline, 6))
        // Starsza wędrówka nie leży między stanami.
        assertFalse(ActivitySummaryCalculator.firstTrailCompleted(baseline, 5))
    }

    @Test
    fun firstTrailCompleted_notWhenAlreadyHadOneOrCountUnknown() {
        val alreadyHad = TrailProgressBaseline(
            current = TrailProgressRecord(0.12, 6, completedTrailCount = 2),
            previous = TrailProgressRecord(0.10, 5, completedTrailCount = 1)
        )
        assertFalse(ActivitySummaryCalculator.firstTrailCompleted(alreadyHad, 6))

        // Stan zapisany przed tą funkcją nie zna liczby ukończonych szlaków.
        val unknown = TrailProgressBaseline(
            current = TrailProgressRecord(0.12, 6, completedTrailCount = 1),
            previous = TrailProgressRecord(0.10, 5)
        )
        assertFalse(ActivitySummaryCalculator.firstTrailCompleted(unknown, 6))
    }
}
