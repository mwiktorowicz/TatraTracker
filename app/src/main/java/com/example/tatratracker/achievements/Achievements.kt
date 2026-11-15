package com.example.tatratracker.achievements

import com.example.tatratracker.collection.CollectionCatalog
import com.example.tatratracker.collection.CollectionProgressCalculator
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import java.time.Instant
import java.time.ZoneId

/*
 * =========================================================
 * OSIĄGNIĘCIA I REKORDY
 * =========================================================
 *
 * Wszystko liczone na bieżąco z aktywności, wizyt i katalogu kolekcji – bez własnej
 * tabeli. Data zdobycia wynika z danych, więc osiągnięcia „działają wstecz” także
 * dla wędrówek sprzed tej funkcji.
 */

enum class AchievementKind(
    val title: String,
    val description: String,
    val target: Int
) {
    FIRST_HUT("Pierwsze schronisko", "Odwiedź dowolne schronisko.", 1),
    PEAKS_5("5 szczytów", "Zdobądź 5 różnych szczytów.", 5),
    PEAKS_10("10 szczytów", "Zdobądź 10 różnych szczytów.", 10),
    PEAKS_25("25 szczytów", "Zdobądź 25 różnych szczytów.", 25),
    FIRST_PEAK_2000("Dwutysięcznik", "Wejdź na szczyt z kolekcji o wysokości co najmniej 2000 m.", 1),
    ALL_POLISH_HUTS("Wszystkie polskie schroniska", "Odwiedź każde schronisko Tatr Polskich.", 0),
    DISTANCE_100_KM_YEAR("100 km w roku", "Przejdź 100 km w jednym roku kalendarzowym.", 100),
    FIRST_COMPLETED_TRAIL("Pierwszy ukończony szlak", "Przejdź cały znakowany szlak.", 1)
}

/**
 * Stan jednego osiągnięcia. [current] z [target] to postęp w jednostkach osiągnięcia
 * (szczyty, schroniska, kilometry). [earnedAt] bywa null także przy zdobytym –
 * ukończenie szlaku nie ma daty.
 */
data class Achievement(
    val kind: AchievementKind,
    val isEarned: Boolean,
    val earnedAt: Long?,
    val current: Int,
    val target: Int
)

enum class ActivityRecordKind {
    DISTANCE,
    ALTITUDE
}

/** Rekord: najdłuższa wędrówka albo najwyższy punkt. [value] w metrach. */
data class ActivityRecord(
    val kind: ActivityRecordKind,
    val value: Double,
    val activityId: Int,
    val activityName: String,
    val timestamp: Long
)

data class AchievementsResult(
    val achievements: List<Achievement>,
    val records: List<ActivityRecord>
) {
    val earnedCount: Int
        get() = achievements.count { it.isEarned }
}

/** To, co przyniosła jedna wędrówka. */
data class NewAchievements(
    val achievements: List<Achievement>,
    val records: List<ActivityRecord>
) {
    val isEmpty: Boolean
        get() = achievements.isEmpty() && records.isEmpty()
}

object AchievementCalculator {

    const val POLISH_HUTS_SET_ID = "huts_pl"

    private const val HIGH_PEAK_METERS = 2000

    private const val YEAR_DISTANCE_TARGET_METERS = 100_000.0

    /**
     * [catalog] = null (nie udało się wczytać) zostawia „Dwutysięcznik” i polskie schroniska
     * niezdobyte; [completedTrailCount] = null – postęp na szlakach jeszcze nie jest policzony.
     */
    fun calculate(
        activities: List<ActivityEntity>,
        places: List<SavedPlaceEntity>,
        visits: List<PoiVisitEntity>,
        catalog: CollectionCatalog?,
        completedTrailCount: Int?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): AchievementsResult {
        val firstVisitByPlace: Map<Long, Long> =
            visits
                .groupBy { visit -> visit.placeId }
                .mapValues { (_, placeVisits) -> placeVisits.minOf { it.visitedAt } }

        // Daty pierwszych wizyt, od najstarszej – n-ta data to dzień zdobycia n-tego miejsca.
        fun firstVisits(category: String): List<Long> {
            return places
                .filter { place -> place.category == category }
                .mapNotNull { place -> firstVisitByPlace[place.id] }
                .sorted()
        }

        val peakVisits = firstVisits(SavedPlaceCategory.MOUNTAIN)
        val hutVisits = firstVisits(SavedPlaceCategory.HUT)

        fun countAchievement(kind: AchievementKind, firstVisits: List<Long>): Achievement {
            return Achievement(
                kind = kind,
                isEarned = firstVisits.size >= kind.target,
                earnedAt = firstVisits.getOrNull(kind.target - 1),
                current = firstVisits.size.coerceAtMost(kind.target),
                target = kind.target
            )
        }

        val collection = catalog?.let { CollectionProgressCalculator.calculate(it, places, visits) }

        val highPeakVisit =
            collection
                ?.sets
                ?.flatMap { set -> set.items }
                ?.filter { item ->
                    item.item.category == SavedPlaceCategory.MOUNTAIN &&
                        (item.item.elevationMeters ?: 0) >= HIGH_PEAK_METERS
                }
                ?.mapNotNull { item -> item.firstVisitedAt }
                ?.minOrNull()

        val polishHuts = collection?.sets?.firstOrNull { set -> set.set.id == POLISH_HUTS_SET_ID }
        val allPolishHuts = polishHuts != null && polishHuts.totalCount > 0 &&
            polishHuts.collectedCount == polishHuts.totalCount

        val yearDistance = yearDistance(activities, zoneId)

        val trailCompleted = (completedTrailCount ?: 0) >= 1

        val achievements =
            listOf(
                countAchievement(AchievementKind.FIRST_HUT, hutVisits),
                countAchievement(AchievementKind.PEAKS_5, peakVisits),
                countAchievement(AchievementKind.PEAKS_10, peakVisits),
                countAchievement(AchievementKind.PEAKS_25, peakVisits),
                Achievement(
                    kind = AchievementKind.FIRST_PEAK_2000,
                    isEarned = highPeakVisit != null,
                    earnedAt = highPeakVisit,
                    current = if (highPeakVisit != null) 1 else 0,
                    target = 1
                ),
                Achievement(
                    kind = AchievementKind.ALL_POLISH_HUTS,
                    isEarned = allPolishHuts,
                    earnedAt =
                        if (allPolishHuts) {
                            polishHuts?.items?.mapNotNull { it.firstVisitedAt }?.maxOrNull()
                        } else {
                            null
                        },
                    current = polishHuts?.collectedCount ?: 0,
                    target = polishHuts?.totalCount ?: 0
                ),
                Achievement(
                    kind = AchievementKind.DISTANCE_100_KM_YEAR,
                    isEarned = yearDistance.earnedAt != null,
                    earnedAt = yearDistance.earnedAt,
                    current =
                        (yearDistance.bestYearMeters / 1000.0)
                            .toInt()
                            .coerceAtMost(AchievementKind.DISTANCE_100_KM_YEAR.target),
                    target = AchievementKind.DISTANCE_100_KM_YEAR.target
                ),
                Achievement(
                    kind = AchievementKind.FIRST_COMPLETED_TRAIL,
                    isEarned = trailCompleted,
                    earnedAt = null,
                    current = if (trailCompleted) 1 else 0,
                    target = 1
                )
            )

        return AchievementsResult(
            achievements = achievements,
            records = listOfNotNull(
                record(ActivityRecordKind.DISTANCE, activities) { it.distanceMeters },
                record(ActivityRecordKind.ALTITUDE, activities) { it.maxAltitudeMeters }
            )
        )
    }

    /**
     * Osiągnięcia zdobyte dokładnie tą wędrówką (data zdobycia = start wędrówki, bo taką
     * datę mają jej wizyty) i rekordy, które pobiła w chwili przejścia. Pierwsza wędrówka
     * w ogóle nie „bije” rekordów. [firstTrailCompleted] – czy to po niej pojawił się
     * pierwszy ukończony szlak (wiadomo to tylko z zapamiętanego postępu).
     */
    fun newForActivity(
        activity: ActivityEntity,
        result: AchievementsResult,
        activities: List<ActivityEntity>,
        firstTrailCompleted: Boolean
    ): NewAchievements {
        val earned =
            result.achievements.filter { achievement ->
                if (achievement.kind == AchievementKind.FIRST_COMPLETED_TRAIL) {
                    achievement.isEarned && firstTrailCompleted
                } else {
                    achievement.isEarned && achievement.earnedAt == activity.timestamp
                }
            }

        val earlier = activities.filter { other -> other.id != activity.id && other.timestamp < activity.timestamp }

        fun beaten(kind: ActivityRecordKind, value: (ActivityEntity) -> Double): ActivityRecord? {
            val own = value(activity)
            val previousBest = earlier.map(value).filter { it.isFinite() && it > 0.0 }.maxOrNull()

            return if (previousBest != null && own.isFinite() && own > previousBest) {
                activity.toRecord(kind, own)
            } else {
                null
            }
        }

        return NewAchievements(
            achievements = earned,
            records = listOfNotNull(
                beaten(ActivityRecordKind.DISTANCE) { it.distanceMeters },
                beaten(ActivityRecordKind.ALTITUDE) { it.maxAltitudeMeters }
            )
        )
    }

    private class YearDistance(val bestYearMeters: Double, val earnedAt: Long?)

    // Dystans narasta w kolejności wędrówek; data zdobycia = wędrówka, która pierwsza dobiła do 100 km.
    private fun yearDistance(activities: List<ActivityEntity>, zoneId: ZoneId): YearDistance {
        val byYear = mutableMapOf<Int, Double>()
        var earnedAt: Long? = null

        activities
            .filter { it.distanceMeters.isFinite() && it.distanceMeters > 0.0 }
            .sortedBy { it.timestamp }
            .forEach { activity ->
                val year = Instant.ofEpochMilli(activity.timestamp).atZone(zoneId).year
                val total = (byYear[year] ?: 0.0) + activity.distanceMeters
                byYear[year] = total

                if (earnedAt == null && total >= YEAR_DISTANCE_TARGET_METERS) {
                    earnedAt = activity.timestamp
                }
            }

        return YearDistance(bestYearMeters = byYear.values.maxOrNull() ?: 0.0, earnedAt = earnedAt)
    }

    // Przy równych wartościach rekord trzyma wcześniejsza wędrówka.
    private fun record(
        kind: ActivityRecordKind,
        activities: List<ActivityEntity>,
        value: (ActivityEntity) -> Double
    ): ActivityRecord? {
        return activities
            .filter { value(it).isFinite() && value(it) > 0.0 }
            .sortedBy { it.timestamp }
            .maxByOrNull(value)
            ?.let { it.toRecord(kind, value(it)) }
    }

    private fun ActivityEntity.toRecord(kind: ActivityRecordKind, value: Double): ActivityRecord {
        return ActivityRecord(
            kind = kind,
            value = value,
            activityId = id,
            activityName = name,
            timestamp = timestamp
        )
    }
}
