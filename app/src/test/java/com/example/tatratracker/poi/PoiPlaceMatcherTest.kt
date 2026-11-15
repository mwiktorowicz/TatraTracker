package com.example.tatratracker.poi

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiPlaceMatcherTest {

    // Kasprowy Wierch; 0.001° szerokości to ok. 111 m.
    private val lat = 49.2318
    private val lon = 19.9817

    private fun place(
        id: Long,
        name: String,
        category: String = SavedPlaceCategory.MOUNTAIN,
        latitude: Double = lat,
        longitude: Double = lon
    ) = SavedPlaceEntity(
        id = id,
        name = name,
        category = category,
        latitude = latitude,
        longitude = longitude,
        createdAt = 0L
    )

    private fun visit(placeId: Long, visitedAt: Long, activityId: Int? = null) =
        PoiVisitEntity(
            placeId = placeId,
            activityId = activityId,
            visitedAt = visitedAt,
            source = PoiVisitSource.ACTIVITY
        )

    private fun find(
        name: String,
        places: List<SavedPlaceEntity>,
        category: String? = SavedPlaceCategory.MOUNTAIN,
        latitude: Double = lat,
        longitude: Double = lon
    ) = PoiPlaceMatcher.findPlace(name, category, latitude, longitude, places)

    @Test
    fun normalizeName_ignoresCaseAccentsAndSeparators() {
        assertEquals("kasprowywierch", PoiPlaceMatcher.normalizeName("  Kasprowy Wierch "))
        assertEquals("kasprowywierch", PoiPlaceMatcher.normalizeName("kasprowy-wierch"))
        assertEquals("swinica", PoiPlaceMatcher.normalizeName("Świnica"))
        assertEquals("lomnica", PoiPlaceMatcher.normalizeName("Łomnica"))
    }

    @Test
    fun distanceMeters_matchesKnownDistance() {
        val distance = PoiPlaceMatcher.distanceMeters(lat, lon, lat + 0.001, lon)

        assertEquals(111.2, distance, 0.5)
    }

    @Test
    fun findPlace_sameNameWithin500m_matches() {
        val places = listOf(place(1, "Kasprowy Wierch", latitude = lat + 0.004))

        assertEquals(1L, find("kasprowy wierch", places)?.id)
    }

    @Test
    fun findPlace_sameNameBeyond500m_doesNotMatch() {
        val places = listOf(place(1, "Kasprowy Wierch", latitude = lat + 0.005))

        assertNull(find("Kasprowy Wierch", places))
    }

    @Test
    fun findPlace_differentNameWithin60m_matches() {
        val places = listOf(place(1, "Kasprowy", latitude = lat + 0.0005))

        assertEquals(1L, find("Kasprowy Wierch", places)?.id)
    }

    @Test
    fun findPlace_differentNameBeyond60m_doesNotMatch() {
        val places = listOf(place(1, "Beskid", latitude = lat + 0.001))

        assertNull(find("Kasprowy Wierch", places))
    }

    @Test
    fun findPlace_otherCategory_doesNotMatch() {
        val places = listOf(place(1, "Kasprowy Wierch", category = SavedPlaceCategory.HUT))

        assertNull(find("Kasprowy Wierch", places))
    }

    @Test
    fun findPlace_nullCategory_matchesAnyCategory() {
        val places = listOf(place(1, "Morskie Oko", category = SavedPlaceCategory.OTHER))

        assertEquals(1L, find("Morskie Oko", places, category = null)?.id)
    }

    @Test
    fun findPlace_severalCandidates_returnsNearest() {
        val places =
            listOf(
                place(1, "Kasprowy Wierch", latitude = lat + 0.003),
                place(2, "Kasprowy Wierch", latitude = lat + 0.0002),
                place(3, "Kasprowy Wierch", latitude = lat + 0.002)
            )

        assertEquals(2L, find("Kasprowy Wierch", places)?.id)
    }

    @Test
    fun findPlace_blankName_matchesOnlyByPosition() {
        val near = listOf(place(1, "Kasprowy Wierch", latitude = lat + 0.0003))
        val far = listOf(place(2, "Kasprowy Wierch", latitude = lat + 0.002))

        assertEquals(1L, find("", near)?.id)
        assertNull(find("", far))
    }

    @Test
    fun visitInfo_countsOnlyVisitsOfThePlace_andPicksLatest() {
        val visits =
            listOf(
                visit(placeId = 1, visitedAt = 1_000, activityId = 10),
                visit(placeId = 1, visitedAt = 5_000, activityId = 11),
                visit(placeId = 2, visitedAt = 9_000, activityId = 11),
                visit(placeId = 1, visitedAt = 3_000, activityId = 12)
            )

        val info = PoiPlaceMatcher.visitInfo(place(1, "Kasprowy Wierch"), visits)

        assertEquals(3, info.visitCount)
        assertEquals(5_000L, info.lastVisitedAt)
    }

    @Test
    fun visitInfo_noPlaceOrNoVisits_isEmpty() {
        val visits = listOf(visit(placeId = 2, visitedAt = 9_000))

        val withoutPlace = PoiPlaceMatcher.visitInfo(null, visits)
        val withoutVisits = PoiPlaceMatcher.visitInfo(place(1, "Kasprowy Wierch"), visits)

        assertEquals(0, withoutPlace.visitCount)
        assertNull(withoutPlace.lastVisitedAt)
        assertEquals(0, withoutVisits.visitCount)
        assertTrue(withoutVisits.lastVisitedAt == null)
    }
}
