package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.PlannedRouteEntity
import com.example.tatratracker.data.PlannedRoutePointEntity
import com.example.tatratracker.data.PlannedRoutePointType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannedRouteDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val database =
        AppDatabase.getDatabase(application)

    private val plannedRouteDao =
        database.plannedRouteDao()

    private val routeId =
        checkNotNull(
            savedStateHandle.get<Long>(
                "routeId"
            )
        )

    val uiState:
            StateFlow<PlannedRouteDetailsUiState> =

        combine(
            plannedRouteDao.observeRouteById(
                routeId
            ),
            plannedRouteDao.observeAllPointsForRoute(
                routeId
            )
        ) {
                route,
                allPoints ->

            if (
                route == null
            ) {

                PlannedRouteDetailsUiState.NotFound

            } else {

                val geometryPoints =
                    allPoints
                        .filter {
                            it.pointType ==
                                    PlannedRoutePointType.GEOMETRY
                        }
                        .sortedBy {
                            it.orderIndex
                        }

                val startPoint =
                    allPoints
                        .firstOrNull {
                            it.pointType ==
                                    PlannedRoutePointType.START
                        }

                val waypoints =
                    allPoints
                        .filter {
                            it.pointType ==
                                    PlannedRoutePointType.WAYPOINT
                        }
                        .sortedBy {
                            it.orderIndex
                        }

                val endPoint =
                    allPoints
                        .firstOrNull {
                            it.pointType ==
                                    PlannedRoutePointType.END
                        }

                val elevationPoints =
                    allPoints
                        .filter {
                            it.pointType ==
                                    PlannedRoutePointType.ELEVATION
                        }
                        .sortedBy {
                            it.orderIndex
                        }

                PlannedRouteDetailsUiState.Ready(
                    route = route,
                    allPoints = allPoints,
                    geometryPoints = geometryPoints,
                    startPoint = startPoint,
                    waypoints = waypoints,
                    endPoint = endPoint,
                    elevationPoints = elevationPoints
                )
            }
        }
            .stateIn(
                scope =
                    viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis =
                            5_000
                    ),
                initialValue =
                    PlannedRouteDetailsUiState.Loading
            )

    fun deleteRoute(
        onDeleted: () -> Unit
    ) {

        viewModelScope.launch {

            plannedRouteDao.deleteRoute(
                routeId
            )

            onDeleted()
        }
    }

    fun duplicateRoute(
        onResult: (Result<Long>) -> Unit
    ) {

        viewModelScope.launch {

            val result =
                runCatching {

                    val currentState =
                        uiState.value

                    require(
                        currentState
                                is PlannedRouteDetailsUiState.Ready
                    ) {
                        "Trasa nie jest jeszcze dostępna."
                    }

                    database.withTransaction {

                        val route =
                            currentState.route

                        val newRouteId =
                            plannedRouteDao.insertRoute(
                                route.copy(
                                    id = 0L,
                                    name =
                                        "${route.name} – kopia",
                                    createdAt =
                                        System.currentTimeMillis()
                                )
                            )

                        val copiedPoints =
                            currentState
                                .allPoints
                                .map {
                                        point ->

                                    point.copy(
                                        id = 0L,
                                        routeId =
                                            newRouteId
                                    )
                                }

                        if (
                            copiedPoints.isNotEmpty()
                        ) {
                            plannedRouteDao.insertPoints(
                                copiedPoints
                            )
                        }

                        newRouteId
                    }
                }

            onResult(
                result
            )
        }
    }
}

sealed interface PlannedRouteDetailsUiState {

    data object Loading :
        PlannedRouteDetailsUiState

    data object NotFound :
        PlannedRouteDetailsUiState

    data class Ready(
        val route: PlannedRouteEntity,
        val allPoints: List<PlannedRoutePointEntity>,
        val geometryPoints: List<PlannedRoutePointEntity>,
        val startPoint: PlannedRoutePointEntity?,
        val waypoints: List<PlannedRoutePointEntity>,
        val endPoint: PlannedRoutePointEntity?,
        val elevationPoints: List<PlannedRoutePointEntity>
    ) : PlannedRouteDetailsUiState
}