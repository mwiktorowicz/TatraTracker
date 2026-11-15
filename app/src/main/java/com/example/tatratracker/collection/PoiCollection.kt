package com.example.tatratracker.collection

import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.poi.PoiPlaceMatcher
import com.google.gson.Gson
import com.google.gson.JsonParseException

/*
 * =========================================================
 * KOLEKCJA SZCZYTÓW I SCHRONISK
 * =========================================================
 *
 * Katalog (assets/kolekcja.json) to stałe zestawy miejsc do zdobycia. Nazwy
 * i współrzędne pochodzą z OpenStreetMap – z tego samego źródła, z którego import
 * aktywności bierze miejsca do wizyt – więc pozycja katalogu łączy się z zapisanym
 * miejscem tą samą regułą co wszędzie (PoiPlaceMatcher).
 */

/** Jedna pozycja do zdobycia. [category] to kategoria z [SavedPlaceCategory]. */
data class CollectionItem(
    val id: String,
    val name: String,
    val category: String,
    val elevationMeters: Int?,
    val latitude: Double,
    val longitude: Double
)

data class CollectionSet(
    val id: String,
    val title: String,
    val description: String,
    val items: List<CollectionItem>
)

data class CollectionCatalog(
    val source: String,
    val sets: List<CollectionSet>
)

/** Pozycja katalogu razem z wizytami użytkownika. */
data class CollectionItemProgress(
    val item: CollectionItem,
    val visitCount: Int,
    val firstVisitedAt: Long?,
    val lastVisitedAt: Long?
) {
    val isCollected: Boolean
        get() = visitCount > 0
}

data class CollectionSetProgress(
    val set: CollectionSet,
    val items: List<CollectionItemProgress>
) {
    val collectedCount: Int
        get() = items.count { it.isCollected }

    val totalCount: Int
        get() = items.size

    val fraction: Double
        get() = if (totalCount > 0) collectedCount.toDouble() / totalCount else 0.0
}

/** Postęp w całej kolekcji; miejsce obecne w kilku zestawach liczy się raz. */
data class CollectionProgress(
    val sets: List<CollectionSetProgress>
) {
    private val distinctItems: List<CollectionItemProgress>
        get() = sets.flatMap { it.items }.distinctBy { it.item.id }

    val collectedCount: Int
        get() = distinctItems.count { it.isCollected }

    val totalCount: Int
        get() = distinctItems.size
}

object CollectionCatalogParser {

    // Pola dopuszczają null, bo Gson nie sprawdza braków – robi to [parse].
    private class CatalogDto(val source: String?, val sets: List<SetDto?>?)

    private class SetDto(
        val id: String?,
        val title: String?,
        val description: String?,
        val items: List<ItemDto?>?
    )

    private class ItemDto(
        val id: String?,
        val name: String?,
        val category: String?,
        val elevation: Int?,
        val lat: Double?,
        val lon: Double?
    )

    /** Rzuca [IllegalArgumentException], gdy plik jest uszkodzony albo brakuje w nim pola. */
    fun parse(json: String): CollectionCatalog {
        val dto =
            try {
                Gson().fromJson(json, CatalogDto::class.java)
            } catch (error: JsonParseException) {
                throw IllegalArgumentException("Katalog kolekcji nie jest poprawnym JSON-em.", error)
            }

        requireNotNull(dto) { "Katalog kolekcji jest pusty." }

        val sets =
            requireNotNull(dto.sets) { "Katalog kolekcji nie ma zestawów." }
                .map { set -> toSet(requireNotNull(set) { "Pusty zestaw w katalogu kolekcji." }) }

        return CollectionCatalog(source = dto.source.orEmpty(), sets = sets)
    }

    private fun toSet(dto: SetDto): CollectionSet {
        val id = requireNotNull(dto.id) { "Zestaw bez identyfikatora." }

        return CollectionSet(
            id = id,
            title = requireNotNull(dto.title) { "Zestaw $id nie ma tytułu." },
            description = dto.description.orEmpty(),
            items =
                requireNotNull(dto.items) { "Zestaw $id nie ma pozycji." }
                    .map { item -> toItem(requireNotNull(item) { "Pusta pozycja w zestawie $id." }) }
        )
    }

    private fun toItem(dto: ItemDto): CollectionItem {
        val id = requireNotNull(dto.id) { "Pozycja bez identyfikatora." }
        val category = requireNotNull(dto.category) { "Pozycja $id nie ma kategorii." }

        require(category in SavedPlaceCategory.all) { "Pozycja $id ma nieznaną kategorię $category." }

        return CollectionItem(
            id = id,
            name = requireNotNull(dto.name) { "Pozycja $id nie ma nazwy." },
            category = category,
            elevationMeters = dto.elevation,
            latitude = requireNotNull(dto.lat) { "Pozycja $id nie ma szerokości." },
            longitude = requireNotNull(dto.lon) { "Pozycja $id nie ma długości." }
        )
    }
}

object CollectionProgressCalculator {

    /**
     * Pozycja jest zdobyta, gdy pasujące do niej zapisane miejsce ma choć jedną wizytę –
     * wykrytą z aktywności albo dodaną ręcznie w „Schroniska i punkty”.
     */
    fun calculate(
        catalog: CollectionCatalog,
        places: List<SavedPlaceEntity>,
        visits: List<PoiVisitEntity>
    ): CollectionProgress {
        val visitsByPlace = visits.groupBy { visit -> visit.placeId }

        return CollectionProgress(
            sets =
                catalog.sets.map { set ->
                    CollectionSetProgress(
                        set = set,
                        items = set.items.map { item -> itemProgress(item, places, visitsByPlace) }
                    )
                }
        )
    }

    private fun itemProgress(
        item: CollectionItem,
        places: List<SavedPlaceEntity>,
        visitsByPlace: Map<Long, List<PoiVisitEntity>>
    ): CollectionItemProgress {
        val place =
            PoiPlaceMatcher.findPlace(
                name = item.name,
                category = item.category,
                latitude = item.latitude,
                longitude = item.longitude,
                places = places
            )

        val placeVisits = place?.let { visitsByPlace[it.id] }.orEmpty()

        return CollectionItemProgress(
            item = item,
            visitCount = placeVisits.size,
            firstVisitedAt = placeVisits.minOfOrNull { visit -> visit.visitedAt },
            lastVisitedAt = placeVisits.maxOfOrNull { visit -> visit.visitedAt }
        )
    }
}
