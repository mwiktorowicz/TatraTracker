package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.data.SavedPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedPlacesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        AppDatabase
            .getDatabase(
                application
            )

    private val savedPlaceDao =
        database
            .savedPlaceDao()

    private val savedPlaceRepository = SavedPlaceRepository(database)

    val places:
            StateFlow<List<SavedPlaceEntity>> =

        savedPlaceDao
            .getAllPlaces()
            .stateIn(
                scope =
                    viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(
                            stopTimeoutMillis =
                                5_000
                        ),
                initialValue =
                    emptyList()
            )

    val placeSummaries:
            StateFlow<List<SavedPlaceSummary>> =

        combine(
            savedPlaceDao
                .getAllPlaces(),
            savedPlaceDao
                .getAllVisits()
        ) {
                places,
                visits ->

            val visitsByPlace =
                visits.groupBy {
                    it.placeId
                }

            places
                .map {
                        place ->

                    val placeVisits =
                        visitsByPlace[
                            place.id
                        ]
                            .orEmpty()
                            .sortedByDescending {
                                it.visitedAt
                            }

                    SavedPlaceSummary(
                        place =
                            place,
                        visits =
                            placeVisits
                    )
                }
                .sortedWith(
                    compareByDescending<
                            SavedPlaceSummary
                            > {
                        it.lastVisitedAt
                            ?: Long.MIN_VALUE
                    }.thenBy {
                        it.place.name
                    }
                )
        }
            .stateIn(
                scope =
                    viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(
                            stopTimeoutMillis =
                                5_000
                        ),
                initialValue =
                    emptyList()
            )

    // Walidacja i zapis są w SavedPlaceRepository – tej samej logiki używa mapa.
    suspend fun savePlace(
        name: String,
        category: String,
        latitude: Double,
        longitude: Double,
        note: String?
    ): Result<Long> {
        return savedPlaceRepository.savePlace(
            name = name,
            category = category,
            latitude = latitude,
            longitude = longitude,
            note = note
        )
    }

    /**
     * Ta metoda będzie używana w Q4 przez automatyczne
     * wykrywanie POI na podstawie śladu aktywności.
     *
     * Dzięki unikalnemu indeksowi placeId + activityId
     * ta sama aktywność nie zaliczy miejsca dwa razy.
     */
    suspend fun registerActivityVisit(
        placeId: Long,
        activityId: Int,
        visitedAt: Long
    ): Result<Boolean> {

        return runCatching {

            val place =
                savedPlaceDao
                    .getPlaceById(
                        placeId
                    )
                    ?: error(
                        "Nie znaleziono punktu."
                    )

            val insertResult =
                savedPlaceDao
                    .insertVisit(
                        PoiVisitEntity(
                            placeId =
                                place.id,
                            activityId =
                                activityId,
                            visitedAt =
                                visitedAt,
                            source =
                                PoiVisitSource.ACTIVITY
                        )
                    )

            insertResult !=
                    -1L
        }
    }

    fun deletePlace(
        placeId: Long
    ) {

        viewModelScope.launch {

            savedPlaceDao
                .deletePlace(
                    placeId
                )
        }
    }
}

data class SavedPlaceSummary(
    val place: SavedPlaceEntity,
    val visits: List<PoiVisitEntity>
) {

    val visitCount: Int
        get() =
            visits.size

    val firstVisitedAt: Long?
        get() =
            visits
                .minOfOrNull {
                    it.visitedAt
                }

    val lastVisitedAt: Long?
        get() =
            visits
                .maxOfOrNull {
                    it.visitedAt
                }
}