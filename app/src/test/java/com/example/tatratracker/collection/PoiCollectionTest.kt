package com.example.tatratracker.collection

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PoiCollectionTest {

    private val rysy =
        CollectionItem("node/1", "Rysy", SavedPlaceCategory.MOUNTAIN, 2503, 49.17932, 20.08846)

    private val krywan =
        CollectionItem("node/2", "Krywań", SavedPlaceCategory.MOUNTAIN, 2494, 49.16283, 20.00005)

    private val murowaniec =
        CollectionItem("way/3", "Murowaniec", SavedPlaceCategory.HUT, 1500, 49.24342, 20.00716)

    private val catalog =
        CollectionCatalog(
            source = "test",
            sets =
                listOf(
                    CollectionSet("peaks", "Szczyty", "", listOf(rysy, krywan)),
                    CollectionSet("crown", "Korona", "", listOf(rysy)),
                    CollectionSet("huts", "Schroniska", "", listOf(murowaniec))
                )
        )

    private fun place(id: Long, item: CollectionItem, category: String = item.category) =
        SavedPlaceEntity(
            id = id,
            name = item.name,
            category = category,
            latitude = item.latitude,
            longitude = item.longitude,
            createdAt = 0L
        )

    private fun visit(placeId: Long, visitedAt: Long, source: String = PoiVisitSource.ACTIVITY) =
        PoiVisitEntity(placeId = placeId, activityId = null, visitedAt = visitedAt, source = source)

    // --- katalog z pliku aplikacji ---

    // Testy jednostkowe modułu app startują w katalogu app/.
    private fun shippedCatalog(): CollectionCatalog =
        CollectionCatalogParser.parse(File("src/main/assets/kolekcja.json").readText(Charsets.UTF_8))

    @Test
    fun shippedCatalog_hasFourSetsWithExpectedSizes() {
        val sizes = shippedCatalog().sets.associate { set -> set.id to set.items.size }

        assertEquals(
            mapOf("huts_pl" to 7, "huts_sk" to 15, "trail_peaks" to 48, "great_crown" to 14),
            sizes
        )
    }

    @Test
    fun shippedCatalog_itemsAreCompleteAndInsideTatras() {
        shippedCatalog().sets.forEach { set ->
            assertTrue(set.title.isNotBlank())
            assertEquals("zestaw ${set.id} ma powtórzone pozycje", set.items.size, set.items.map { it.id }.toSet().size)

            set.items.forEach { item ->
                assertTrue("${item.id}: nazwa", item.name.isNotBlank())
                assertTrue("${item.id}: szerokość", item.latitude in 49.05..49.36)
                assertTrue("${item.id}: długość", item.longitude in 19.55..20.45)
                assertTrue("${item.id}: wysokość", item.elevationMeters in 900..2700)
            }
        }
    }

    @Test
    fun shippedCatalog_categoriesMatchSets() {
        val sets = shippedCatalog().sets.associateBy { it.id }

        assertTrue(sets.getValue("huts_pl").items.all { it.category == SavedPlaceCategory.HUT })
        assertTrue(sets.getValue("huts_sk").items.all { it.category == SavedPlaceCategory.HUT })
        assertTrue(sets.getValue("trail_peaks").items.all { it.category == SavedPlaceCategory.MOUNTAIN })
        assertTrue(sets.getValue("great_crown").items.all { it.category == SavedPlaceCategory.MOUNTAIN })
    }

    // --- parser ---

    @Test
    fun parse_readsSetsAndItems() {
        val parsed =
            CollectionCatalogParser.parse(
                """
                {"source":"OSM","sets":[{"id":"s","title":"Zestaw","description":"Opis","items":[
                  {"id":"node/1","name":"Rysy","category":"MOUNTAIN","elevation":2503,"lat":49.1,"lon":20.1},
                  {"id":"way/2","name":"Chata","category":"HUT","elevation":null,"lat":49.2,"lon":20.2}
                ]}]}
                """.trimIndent()
            )

        assertEquals("OSM", parsed.source)
        assertEquals(1, parsed.sets.size)
        assertEquals(CollectionItem("node/1", "Rysy", "MOUNTAIN", 2503, 49.1, 20.1), parsed.sets[0].items[0])
        assertNull(parsed.sets[0].items[1].elevationMeters)
    }

    @Test
    fun parse_missingFieldOrUnknownCategory_throws() {
        val withoutName =
            """{"sets":[{"id":"s","title":"T","items":[{"id":"n","category":"HUT","lat":49.1,"lon":20.1}]}]}"""
        val unknownCategory =
            """{"sets":[{"id":"s","title":"T","items":[{"id":"n","name":"X","category":"LAKE","lat":49.1,"lon":20.1}]}]}"""

        assertThrows(IllegalArgumentException::class.java) { CollectionCatalogParser.parse(withoutName) }
        assertThrows(IllegalArgumentException::class.java) { CollectionCatalogParser.parse(unknownCategory) }
        assertThrows(IllegalArgumentException::class.java) { CollectionCatalogParser.parse("{nie json") }
        assertThrows(IllegalArgumentException::class.java) { CollectionCatalogParser.parse("") }
    }

    // --- postęp ---

    @Test
    fun calculate_noPlaces_nothingCollected() {
        val progress = CollectionProgressCalculator.calculate(catalog, emptyList(), emptyList())

        assertEquals(0, progress.collectedCount)
        assertEquals(3, progress.totalCount)
        assertTrue(progress.sets.all { it.collectedCount == 0 })
        assertFalse(progress.sets[0].items[0].isCollected)
        assertNull(progress.sets[0].items[0].firstVisitedAt)
    }

    @Test
    fun calculate_visitsGiveCountAndFirstAndLastDate() {
        val places = listOf(place(1, rysy))
        val visits = listOf(visit(1, 5_000), visit(1, 2_000), visit(1, 9_000))

        val item = CollectionProgressCalculator.calculate(catalog, places, visits).sets[0].items[0]

        assertTrue(item.isCollected)
        assertEquals(3, item.visitCount)
        assertEquals(2_000L, item.firstVisitedAt)
        assertEquals(9_000L, item.lastVisitedAt)
    }

    @Test
    fun calculate_manualVisitCounts() {
        val places = listOf(place(1, murowaniec))
        val visits = listOf(visit(1, 1_000, PoiVisitSource.MANUAL))

        val huts = CollectionProgressCalculator.calculate(catalog, places, visits).sets[2]

        assertEquals(1, huts.collectedCount)
        assertEquals(1.0, huts.fraction, 0.0)
    }

    @Test
    fun calculate_placeWithoutVisitsOrInOtherCategory_isNotCollected() {
        val places =
            listOf(
                place(1, rysy),
                place(2, murowaniec, category = SavedPlaceCategory.OTHER)
            )
        val visits = listOf(visit(2, 1_000))

        val progress = CollectionProgressCalculator.calculate(catalog, places, visits)

        assertEquals(0, progress.collectedCount)
    }

    @Test
    fun calculate_itemInTwoSets_countsInBothSetsButOnceOverall() {
        val places = listOf(place(1, rysy))
        val visits = listOf(visit(1, 1_000))

        val progress = CollectionProgressCalculator.calculate(catalog, places, visits)

        assertEquals(1, progress.sets[0].collectedCount)
        assertEquals(2, progress.sets[0].totalCount)
        assertEquals(0.5, progress.sets[0].fraction, 0.0)
        assertEquals(1, progress.sets[1].collectedCount)
        assertEquals(1, progress.collectedCount)
        assertEquals(3, progress.totalCount)
    }

    @Test
    fun calculate_placeSavedUnderSameNameNearby_matches() {
        // Import zapisał szczyt 150 m obok pozycji katalogu, pod tą samą nazwą.
        val places = listOf(place(1, krywan).copy(latitude = krywan.latitude + 0.00135))
        val visits = listOf(visit(1, 1_000))

        val item = CollectionProgressCalculator.calculate(catalog, places, visits).sets[0].items[1]

        assertTrue(item.isCollected)
    }
}
