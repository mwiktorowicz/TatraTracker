package com.example.tatratracker.summary

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity

/*
 * =========================================================
 * PODSUMOWANIE WĘDRÓWKI
 * =========================================================
 *
 * Czysta logika plansz podsumowania: które miejsca wędrówka odwiedziła jako pierwsza
 * i o ile po niej urósł postęp na sieci szlaków.
 */

/**
 * Postęp na sieci szlaków policzony, gdy najnowsza aktywność miała numer [maxActivityId].
 * [completedTrailCount] = null w stanach zapisanych, zanim aplikacja zaczęła go pamiętać.
 */
data class TrailProgressRecord(
    val fraction: Double,
    val maxActivityId: Int,
    val completedTrailCount: Int? = null
)

/**
 * Dwa ostatnie stany postępu: [current] – ostatnio policzony, [previous] – ostatni
 * sprzed przybycia nowszych aktywności. Ich różnica to przyrost po nowych wędrówkach.
 */
data class TrailProgressBaseline(
    val current: TrailProgressRecord? = null,
    val previous: TrailProgressRecord? = null
)

object ActivitySummaryCalculator {

    /**
     * Szczyty i schroniska, w których wędrówka [activityId] była jako pierwsza:
     * mają wizytę z tej wędrówki i żadnej wcześniejszej (także dodanej ręcznie).
     * Kolejność jak przy wykrywaniu wizyt.
     */
    fun newPlaces(
        activityId: Int,
        places: List<SavedPlaceEntity>,
        visits: List<PoiVisitEntity>
    ): List<SavedPlaceEntity> {
        val placesById = places.associateBy { place -> place.id }
        val visitsByPlace = visits.groupBy { visit -> visit.placeId }

        return visits
            .filter { visit -> visit.activityId == activityId }
            .sortedBy { visit -> visit.id }
            .filter { visit ->
                visitsByPlace[visit.placeId].orEmpty().none { other ->
                    other.visitedAt < visit.visitedAt
                }
            }
            .mapNotNull { visit -> placesById[visit.placeId] }
            .filter { place ->
                place.category == SavedPlaceCategory.MOUNTAIN ||
                    place.category == SavedPlaceCategory.HUT
            }
            .distinctBy { place -> place.id }
    }

    /**
     * Zapamiętuje świeżo policzony postęp. Poprzedni stan przesuwa się do [TrailProgressBaseline.previous]
     * tylko wtedy, gdy przybyła nowsza aktywność – ponowne liczenie tej samej listy go nie rusza.
     */
    fun advanceBaseline(
        baseline: TrailProgressBaseline,
        fraction: Double,
        maxActivityId: Int,
        completedTrailCount: Int? = null
    ): TrailProgressBaseline {
        val record =
            TrailProgressRecord(
                fraction = fraction,
                maxActivityId = maxActivityId,
                completedTrailCount = completedTrailCount
            )
        val current = baseline.current

        return when {
            current == null ->
                TrailProgressBaseline(current = record, previous = baseline.previous?.before(record))

            maxActivityId > current.maxActivityId ->
                TrailProgressBaseline(current = record, previous = current)

            // Ta sama lista albo usunięta najnowsza aktywność: punkt odniesienia zostaje,
            // o ile nadal jest starszy od bieżącego stanu.
            else ->
                TrailProgressBaseline(current = record, previous = baseline.previous?.before(record))
        }
    }

    /**
     * Przyrost postępu (ułamek sieci szlaków), który przyniosła m.in. wędrówka [activityId];
     * null, gdy nie ma punktu odniesienia sprzed tej wędrówki albo postęp jeszcze jej nie obejmuje.
     */
    fun trailGrowth(baseline: TrailProgressBaseline, activityId: Int): Double? {
        val (previous, current) = statesAround(baseline, activityId) ?: return null

        return (current.fraction - previous.fraction).coerceAtLeast(0.0)
    }

    /**
     * Czy pierwszy ukończony szlak pojawił się po przybyciu m.in. wędrówki [activityId]:
     * przed nią nie było żadnego, po niej jest co najmniej jeden.
     */
    fun firstTrailCompleted(baseline: TrailProgressBaseline, activityId: Int): Boolean {
        val (previous, current) = statesAround(baseline, activityId) ?: return false

        return previous.completedTrailCount == 0 && (current.completedTrailCount ?: 0) >= 1
    }

    // Stan sprzed wędrówki i stan, który już ją obejmuje – albo null, gdy któregoś brakuje.
    private fun statesAround(
        baseline: TrailProgressBaseline,
        activityId: Int
    ): Pair<TrailProgressRecord, TrailProgressRecord>? {
        val current = baseline.current ?: return null
        val previous = baseline.previous ?: return null

        if (current.maxActivityId < activityId || previous.maxActivityId >= activityId) {
            return null
        }

        return previous to current
    }

    /**
     * Nazwy miejsc w jednym wierszu karty do udostępnienia: najwyżej [maxNames] nazw,
     * reszta jako liczba – „Giewont, Kopa Kondracka +2”.
     */
    fun placeNamesLine(names: List<String>, maxNames: Int): String {
        val shown = names.take(maxNames.coerceAtLeast(0))
        val hidden = names.size - shown.size

        return when {
            hidden <= 0 -> shown.joinToString(", ")
            shown.isEmpty() -> "+$hidden"
            else -> "${shown.joinToString(", ")} +$hidden"
        }
    }

    private fun TrailProgressRecord.before(record: TrailProgressRecord): TrailProgressRecord? {
        return takeIf { maxActivityId < record.maxActivityId }
    }
}
