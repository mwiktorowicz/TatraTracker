package com.example.tatratracker.poi

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.SavedPlaceEntity
import java.text.Normalizer
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Wizyty użytkownika w jednym miejscu: ile ich było i kiedy ostatnia. */
data class PoiVisitInfo(
    val visitCount: Int,
    val lastVisitedAt: Long?
)

/**
 * Łączenie punktu (z Overpass przy imporcie albo dotkniętego na mapie) z miejscem
 * zapisanym w `saved_places`. Jedna reguła dla wykrywania wizyt i dla karty punktu
 * na mapie, żeby karta pokazywała wizyty tego samego miejsca, które zapisał import.
 */
object PoiPlaceMatcher {

    /** Ta sama nazwa liczy się jako to samo miejsce do tej odległości. */
    const val SAME_NAME_DISTANCE_METERS = 500.0

    /** Bez zgodnej nazwy miejsce musi leżeć praktycznie w tym samym punkcie. */
    const val POSITION_TOLERANCE_METERS = 60.0

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    private val accentMarks = Regex("\\p{Mn}+")

    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    /** „Kasprowy Wierch” i „kasprowy-wierch” dają ten sam wynik; „ł” liczy się jak „l”. */
    fun normalizeName(value: String): String {
        val lower = value.trim().lowercase(Locale.ROOT).replace("ł", "l")
        val withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(accentMarks, "")
        return withoutAccents.replace(nonAlphanumeric, "")
    }

    /**
     * Najbliższe zapisane miejsce pasujące do punktu: ta sama nazwa do 500 m albo dowolna
     * nazwa do 60 m. [category] = null oznacza „dowolna kategoria”.
     */
    fun findPlace(
        name: String,
        category: String?,
        latitude: Double,
        longitude: Double,
        places: List<SavedPlaceEntity>
    ): SavedPlaceEntity? {
        val normalizedName = normalizeName(name)

        return places
            .filter { place -> category == null || place.category == category }
            .map { place ->
                place to distanceMeters(latitude, longitude, place.latitude, place.longitude)
            }
            .filter { (place, distance) ->
                val sameName = normalizedName == normalizeName(place.name)

                (sameName && distance <= SAME_NAME_DISTANCE_METERS) ||
                    distance <= POSITION_TOLERANCE_METERS
            }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    /** Wizyty w [place]; brak miejsca = zero wizyt. */
    fun visitInfo(place: SavedPlaceEntity?, visits: List<PoiVisitEntity>): PoiVisitInfo {
        if (place == null) {
            return PoiVisitInfo(visitCount = 0, lastVisitedAt = null)
        }

        val placeVisits = visits.filter { visit -> visit.placeId == place.id }

        return PoiVisitInfo(
            visitCount = placeVisits.size,
            lastVisitedAt = placeVisits.maxOfOrNull { visit -> visit.visitedAt }
        )
    }

    fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = Math.toRadians(latitude2 - latitude1)
        val deltaLon = Math.toRadians(longitude2 - longitude1)

        val a =
            sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)

        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
