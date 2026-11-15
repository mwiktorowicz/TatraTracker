package com.example.tatratracker.data

import android.content.Context
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

data class TrailProgressState(
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

/**
 * Jedno źródło postępu na szlakach dla całego procesu aplikacji
 * (ekran „Ukończone szlaki” i Statystyki), żeby dopasowanie śladów
 * nie liczyło się dwa razy. Wynik jest trzymany tylko w pamięci;
 * zapis w bazie to osobne zadanie (punkt 38).
 *
 * Liczenie rusza przy pierwszym użyciu repozytorium i powtarza się
 * tylko po zmianie listy aktywności albo na żądanie ([recalculate]).
 */
class TrailProgressRepository private constructor(
    context: Context
) {

    private val activityDao = AppDatabase.getDatabase(context).activityDao()

    private val trailMatcher = TrailMatcher(context)

    private val baselineStore = TrailProgressBaselineStore(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val recalculateRequests = MutableStateFlow(0)

    private val _state = MutableStateFlow(TrailProgressState())

    val state: StateFlow<TrailProgressState> = _state.asStateFlow()

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
        scope.launch {
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

    private suspend fun calculateProgress(activityIds: List<Int>) {
        _state.value =
            _state.value.copy(
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

            // Przed ogłoszeniem wyniku, żeby podsumowanie wędrówki od razu widziało przyrost.
            rememberProgress(result.completedFraction, activityIds.maxOrNull() ?: 0)

            _state.value =
                TrailProgressState(
                    isLoading = false,
                    activityCount = activityIds.size,
                    result = result
                )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.value =
                _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Nie udało się policzyć postępu."
                )
        }
    }

    // Błąd zapisu nie może zepsuć samego postępu – najwyżej zabraknie przyrostu w podsumowaniu.
    private suspend fun rememberProgress(fraction: Double, maxActivityId: Int) {
        try {
            baselineStore.record(fraction, maxActivityId)
        } catch (error: IOException) {
            Log.w(LOG_TAG, "Nie udało się zapamiętać postępu na szlakach", error)
        }
    }

    companion object {

        private const val LOG_TAG = "TrailProgress"

        @Volatile
        private var INSTANCE: TrailProgressRepository? = null

        fun getInstance(context: Context): TrailProgressRepository {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: TrailProgressRepository(context.applicationContext).also { INSTANCE = it }
                }
        }
    }
}
