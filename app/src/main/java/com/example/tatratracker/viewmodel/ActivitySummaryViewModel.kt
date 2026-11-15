package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.achievements.AchievementCalculator
import com.example.tatratracker.achievements.NewAchievements
import com.example.tatratracker.collection.CollectionCatalog
import com.example.tatratracker.collection.CollectionCatalogAsset
import com.example.tatratracker.collection.CollectionProgressCalculator
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.data.SavedPlaceRepository
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.data.TrailProgressBaselineStore
import com.example.tatratracker.data.TrailProgressRepository
import com.example.tatratracker.summary.ActivitySummaryCalculator
import com.example.tatratracker.summary.TrailProgressBaseline
import com.example.tatratracker.ui.buildActivityElevationProfile
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Plansza „Wysokość”; pola null = brak danych w śladzie. */
data class ActivitySummaryElevation(
    val highestMeters: Int?,
    val ascentMeters: Int?,
    val descentMeters: Int?
)

/** Plansza „Nowe miejsca”. Liczniki kolekcji są null, gdy katalogu nie udało się wczytać. */
data class ActivitySummaryPlaces(
    val newPlaces: List<SavedPlaceEntity>,
    val collectedCount: Int?,
    val totalCount: Int?
)

/** Plansza „Szlaki”. [growth] to przyrost ułamka sieci; null = brak punktu odniesienia. */
data class ActivitySummaryTrails(
    val isLoading: Boolean = true,
    val fraction: Double? = null,
    val completedLengthMeters: Double? = null,
    val totalLengthMeters: Double? = null,
    val growth: Double? = null,
    val failed: Boolean = false
)

data class ActivitySummaryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activity: ActivityEntity? = null,
    val elevation: ActivitySummaryElevation? = null,
    val places: ActivitySummaryPlaces? = null,
    val trails: ActivitySummaryTrails = ActivitySummaryTrails(),
    val newAchievements: NewAchievements = NO_ACHIEVEMENTS
)

private val NO_ACHIEVEMENTS = NewAchievements(achievements = emptyList(), records = emptyList())

/**
 * Plansze podsumowania jednej wędrówki. Dane wędrówki czytane są raz; nowe miejsca
 * i postęp na szlakach odświeżają się same, gdy skończy się ich liczenie po synchronizacji.
 */
class ActivitySummaryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val activityDao = AppDatabase.getDatabase(application).activityDao()

    private val savedPlaceRepository = SavedPlaceRepository(AppDatabase.getDatabase(application))

    private val trailProgressRepository = TrailProgressRepository.getInstance(application)

    private val baselineStore = TrailProgressBaselineStore(application)

    private val activityId: Int = savedStateHandle.get<Int>("activityId") ?: -1

    private data class ActivityPart(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val activity: ActivityEntity? = null,
        val elevation: ActivitySummaryElevation? = null
    )

    private val activityPart = MutableStateFlow(ActivityPart())

    // Brak katalogu nie blokuje listy nowych miejsc – znikają tylko liczniki kolekcji.
    private val catalog: Flow<CollectionCatalog?> =
        flow {
            emit(
                try {
                    CollectionCatalogAsset.load(application)
                } catch (error: IOException) {
                    null
                } catch (error: IllegalArgumentException) {
                    null
                }
            )
        }.flowOn(Dispatchers.IO)

    private val places: Flow<ActivitySummaryPlaces?> =
        combine(
            catalog,
            savedPlaceRepository.observePlaces(),
            savedPlaceRepository.observeVisits()
        ) { catalog, places, visits ->
            val progress = catalog?.let { CollectionProgressCalculator.calculate(it, places, visits) }

            ActivitySummaryPlaces(
                newPlaces = ActivitySummaryCalculator.newPlaces(activityId, places, visits),
                collectedCount = progress?.collectedCount,
                totalCount = progress?.totalCount
            )
        }.flowOn(Dispatchers.Default)

    private val trails: Flow<ActivitySummaryTrails> =
        combine(
            trailProgressRepository.state,
            baselineStore.baseline.catch { emit(TrailProgressBaseline()) }
        ) { progress, baseline ->
            val result = progress.result

            when {
                progress.isLoading -> ActivitySummaryTrails(isLoading = true)

                result == null -> ActivitySummaryTrails(isLoading = false, failed = true)

                else ->
                    ActivitySummaryTrails(
                        isLoading = false,
                        fraction = result.completedFraction,
                        completedLengthMeters = result.matchedLengthMeters,
                        totalLengthMeters = result.totalLengthMeters,
                        growth = ActivitySummaryCalculator.trailGrowth(baseline, activityId),
                        // Wynik sprzed nieudanego przeliczenia zostaje, ale może być nieaktualny.
                        failed = progress.errorMessage != null
                    )
            }
        }

    // Osiągnięcia i rekordy, które przyniosła ta wędrówka; puste, dopóki nie ma czego pokazać.
    private val newAchievements: Flow<NewAchievements> =
        combine(
            catalog,
            activityDao.getAllActivities(),
            savedPlaceRepository.observePlaces(),
            savedPlaceRepository.observeVisits(),
            combine(
                trailProgressRepository.state,
                baselineStore.baseline.catch { emit(TrailProgressBaseline()) }
            ) { progress, baseline -> progress to baseline }
        ) { catalog, activities, places, visits, (progress, baseline) ->
            val activity = activities.firstOrNull { it.id == activityId }

            if (activity == null) {
                NO_ACHIEVEMENTS
            } else {
                AchievementCalculator.newForActivity(
                    activity = activity,
                    result =
                        AchievementCalculator.calculate(
                            activities = activities,
                            places = places,
                            visits = visits,
                            catalog = catalog,
                            completedTrailCount = progress.result?.let { progress.completedTrailCount }
                        ),
                    activities = activities,
                    firstTrailCompleted =
                        !progress.isLoading &&
                            ActivitySummaryCalculator.firstTrailCompleted(baseline, activityId)
                )
            }
        }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<ActivitySummaryUiState> =
        combine(activityPart, places, trails, newAchievements) { activityPart, places, trails, newAchievements ->
            ActivitySummaryUiState(
                isLoading = activityPart.isLoading,
                errorMessage = activityPart.errorMessage,
                activity = activityPart.activity,
                elevation = activityPart.elevation,
                places = places,
                trails = trails,
                newAchievements = newAchievements
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivitySummaryUiState())

    init {
        loadActivity()
    }

    private fun loadActivity() {
        viewModelScope.launch {
            activityPart.value =
                try {
                    val activity = activityDao.getActivityById(activityId)

                    if (activity == null) {
                        ActivityPart(isLoading = false, errorMessage = "Nie znaleziono tej aktywności.")
                    } else {
                        ActivityPart(
                            isLoading = false,
                            activity = activity,
                            elevation = loadElevation(activity)
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ActivityPart(
                        isLoading = false,
                        errorMessage = error.message ?: "Nie udało się wczytać aktywności."
                    )
                }
        }
    }

    // Podejścia i zejścia z tego samego wygładzonego profilu co w szczegółach aktywności.
    private suspend fun loadElevation(activity: ActivityEntity): ActivitySummaryElevation {
        val entities = activityDao.getTrackPointsForActivity(activity.id).first()

        val profile = withContext(Dispatchers.Default) {
            buildActivityElevationProfile(TrackPointMapper.toModels(entities))
        }

        val highest =
            activity.maxAltitudeMeters
                .takeIf { it.isFinite() && it > 0.0 }
                ?.let { Math.round(it).toInt() }
                ?: profile?.maxElevationMeters

        return ActivitySummaryElevation(
            highestMeters = highest,
            ascentMeters = profile?.ascentMeters,
            descentMeters = profile?.descentMeters
        )
    }
}
