package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.data.TrailMatchResult
import com.example.tatratracker.data.TrailMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/*
 * Szlak liczy się jako ukończony od 90% długości,
 * bo na końcach szlaków GPS często nie dobija
 * kilku ostatnich metrów.
 */
const val COMPLETED_TRAIL_THRESHOLD = 0.9

data class CompletedTrailsUiState(
    val isLoading: Boolean = true,
    val activityCount: Int = 0,
    val result: TrailMatchResult? = null,
    val errorMessage: String? = null
) {
    val completedTrailCount: Int
        get() = result
            ?.trailProgress
            ?.count { it.completedFraction >= COMPLETED_TRAIL_THRESHOLD }
            ?: 0

    val totalTrailCount: Int
        get() = result?.trailProgress?.size ?: 0
}

class CompletedTrailsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val trailMatcher =
        TrailMatcher(application)

    private val recalculateRequests =
        MutableStateFlow(0)

    private val _uiState =
        MutableStateFlow(CompletedTrailsUiState())

    val uiState: StateFlow<CompletedTrailsUiState> =
        _uiState.asStateFlow()

    init {
        observeActivities()
    }

    fun recalculate() {
        recalculateRequests.value++
    }

    /*
     * Import lub usunięcie aktywności zmienia listę ID
     * i uruchamia liczenie od nowa. collectLatest przerywa
     * poprzednie liczenie, jeśli jeszcze trwało.
     */
    private fun observeActivities() {
        viewModelScope.launch {
            combine(
                activityDao
                    .getAllActivities()
                    .map { activities -> activities.map { it.id } }
                    .distinctUntilChanged(),
                recalculateRequests
            ) { activityIds, _ ->
                activityIds
            }.collectLatest { activityIds ->
                calculateProgress(activityIds)
            }
        }
    }

    private suspend fun calculateProgress(
        activityIds: List<Int>
    ) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = true,
                activityCount = activityIds.size,
                errorMessage = null
            )

        try {
            val result =
                trailMatcher.matchTracks(
                    flow {
                        activityIds.forEach { activityId ->
                            emit(
                                TrackPointMapper.toModels(
                                    activityDao
                                        .getTrackPointsForActivity(activityId)
                                        .first()
                                )
                            )
                        }
                    }
                )

            _uiState.value =
                CompletedTrailsUiState(
                    isLoading = false,
                    activityCount = activityIds.size,
                    result = result
                )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        error.message
                            ?: "Nie udało się policzyć postępu."
                )
        }
    }
}
