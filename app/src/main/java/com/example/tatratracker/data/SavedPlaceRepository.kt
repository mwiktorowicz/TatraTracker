package com.example.tatratracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Zapis miejsc w tabeli `saved_places` – wspólny dla ekranu „Schroniska i punkty”
 * i dla „Dodaj własny punkt” na mapie.
 */
class SavedPlaceRepository(
    private val database: AppDatabase
) {

    private val savedPlaceDao = database.savedPlaceDao()

    /** Wszystkie zapisane miejsca; emituje ponownie po każdym dodaniu i usunięciu. */
    fun observePlaces(): Flow<List<SavedPlaceEntity>> = savedPlaceDao.getAllPlaces()

    /** Wszystkie wizyty we wszystkich miejscach; emituje ponownie po imporcie aktywności. */
    fun observeVisits(): Flow<List<PoiVisitEntity>> = savedPlaceDao.getAllVisits()

    /**
     * Zapisuje miejsce razem z ręczną wizytą (źródło MANUAL).
     * Zwraca id nowego miejsca albo błąd walidacji / zapisu.
     */
    suspend fun savePlace(
        name: String,
        category: String,
        latitude: Double,
        longitude: Double,
        note: String?
    ): Result<Long> {
        return runCatching {
            val cleanName = name.trim()

            require(cleanName.isNotBlank()) {
                "Nazwa punktu nie może być pusta."
            }

            require(category in SavedPlaceCategory.all) {
                "Niepoprawna kategoria punktu."
            }

            require(latitude in -90.0..90.0) {
                "Szerokość geograficzna musi być w zakresie -90 do 90."
            }

            require(longitude in -180.0..180.0) {
                "Długość geograficzna musi być w zakresie -180 do 180."
            }

            database.withTransaction {
                val now = System.currentTimeMillis()

                val placeId =
                    savedPlaceDao.insertPlace(
                        SavedPlaceEntity(
                            name = cleanName,
                            category = category,
                            latitude = latitude,
                            longitude = longitude,
                            note = note?.trim()?.takeIf { it.isNotBlank() },
                            createdAt = now
                        )
                    )

                savedPlaceDao.insertVisit(
                    PoiVisitEntity(
                        placeId = placeId,
                        activityId = null,
                        visitedAt = now,
                        source = PoiVisitSource.MANUAL
                    )
                )

                placeId
            }
        }
    }
}
