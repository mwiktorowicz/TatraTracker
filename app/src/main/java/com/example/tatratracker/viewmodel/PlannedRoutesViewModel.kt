package com.example.tatratracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.PlannedRouteDraft
import com.example.tatratracker.data.PlannedRouteDraftPoint
import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.data.PlannedRoutePointEntity
import com.example.tatratracker.gpx.GpxPlannedRouteImportResult
import com.example.tatratracker.gpx.GpxPlannedRouteImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannedRoutesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database =
        AppDatabase.getDatabase(
            application
        )

    private val plannedRouteDao =
        database.plannedRouteDao()

    val routes: StateFlow<List<PlannedRouteEntity>> =
        plannedRouteDao
            .getAllRoutes()
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = 5_000
                    ),
                initialValue = emptyList()
            )

    private val gpxImporter = GpxPlannedRouteImporter(application)

    /**
     * Import zaplanowanej trasy z pliku GPX i zapis w „Moich trasach”.
     * Wspólny dla ekranu „Moje trasy” i mapy; komunikat o sukcesie każdy ekran buduje sam.
     */
    suspend fun importGpxRoute(uri: Uri): GpxRouteImportOutcome {
        val imported = gpxImporter.importRoute(uri).getOrElse { error ->
            return GpxRouteImportOutcome.Failed(
                title = "Nie udało się zaimportować GPX",
                message = error.message ?: "Plik nie zawiera poprawnej trasy GPX."
            )
        }

        return saveRoute(imported.draft).fold(
            onSuccess = { routeId ->
                GpxRouteImportOutcome.Imported(routeId = routeId, details = imported)
            },
            onFailure = { error ->
                GpxRouteImportOutcome.Failed(
                    title = "Nie udało się zapisać trasy",
                    message = error.message ?: "Wystąpił nieznany błąd zapisu w bazie."
                )
            }
        )
    }

    suspend fun saveRoute(
        draft: PlannedRouteDraft
    ): Result<Long> {

        return runCatching {

            validateDraft(
                draft
            )

            database.withTransaction {

                val routeId =
                    plannedRouteDao.insertRoute(
                        PlannedRouteEntity(
                            name =
                                draft.name.trim(),
                            createdAt =
                                System.currentTimeMillis(),
                            distanceMeters =
                                draft.distanceMeters,
                            durationSeconds =
                                draft.durationSeconds,
                            ascentMeters =
                                draft.ascentMeters,
                            descentMeters =
                                draft.descentMeters,
                            minElevationMeters =
                                draft.minElevationMeters,
                            maxElevationMeters =
                                draft.maxElevationMeters,
                            startName =
                                draft.startName,
                            endName =
                                draft.endName
                        )
                    )

                val points =
                    draft.points.map { point ->

                        point.toEntity(
                            routeId = routeId
                        )
                    }

                if (
                    points.isNotEmpty()
                ) {

                    plannedRouteDao.insertPoints(
                        points
                    )
                }

                routeId
            }
        }
    }

    suspend fun updateRoute(
        routeId: Long,
        draft: PlannedRouteDraft
    ): Result<Unit> {

        return runCatching {

            validateDraft(
                draft
            )

            database.withTransaction {

                val existingRoute =
                    plannedRouteDao.getRouteById(
                        routeId
                    )
                        ?: error(
                            "Nie znaleziono zapisanej trasy."
                        )

                plannedRouteDao.updateRoute(
                    existingRoute.copy(
                        name =
                            draft.name.trim(),
                        distanceMeters =
                            draft.distanceMeters,
                        durationSeconds =
                            draft.durationSeconds,
                        ascentMeters =
                            draft.ascentMeters,
                        descentMeters =
                            draft.descentMeters,
                        minElevationMeters =
                            draft.minElevationMeters,
                        maxElevationMeters =
                            draft.maxElevationMeters,
                        startName =
                            draft.startName,
                        endName =
                            draft.endName
                    )
                )

                plannedRouteDao.deletePointsForRoute(
                    routeId
                )

                val points =
                    draft.points.map { point ->

                        point.toEntity(
                            routeId = routeId
                        )
                    }

                if (
                    points.isNotEmpty()
                ) {

                    plannedRouteDao.insertPoints(
                        points
                    )
                }
            }
        }
    }

    suspend fun loadRouteForEditing(
        routeId: Long
    ): Result<PlannedRouteEditData> {

        return runCatching {

            val route =
                plannedRouteDao.getRouteById(
                    routeId
                )
                    ?: error(
                        "Nie znaleziono zapisanej trasy."
                    )

            val points =
                plannedRouteDao.getAllPointsForRoute(
                    routeId
                )

            PlannedRouteEditData(
                route = route,
                points = points
            )
        }
    }

    fun deleteRoute(
        routeId: Long
    ) {

        viewModelScope.launch {

            plannedRouteDao.deleteRoute(
                routeId
            )
        }
    }

    private fun validateDraft(
        draft: PlannedRouteDraft
    ) {

        require(
            draft.name.isNotBlank()
        ) {
            "Nazwa trasy nie może być pusta."
        }

        require(
            draft.points.isNotEmpty()
        ) {
            "Trasa nie zawiera punktów."
        }
    }
}

data class PlannedRouteEditData(
    val route: PlannedRouteEntity,
    val points: List<PlannedRoutePointEntity>
)

sealed interface GpxRouteImportOutcome {

    data class Imported(
        val routeId: Long,
        val details: GpxPlannedRouteImportResult
    ) : GpxRouteImportOutcome

    data class Failed(
        val title: String,
        val message: String
    ) : GpxRouteImportOutcome
}

private fun PlannedRouteDraftPoint.toEntity(
    routeId: Long
): PlannedRoutePointEntity {

    return PlannedRoutePointEntity(
        routeId = routeId,
        pointType = pointType,
        orderIndex = orderIndex,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        distanceMeters = distanceMeters,
        name = name
    )
}