package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.achievements.AchievementCalculator
import com.example.tatratracker.achievements.AchievementsResult
import com.example.tatratracker.collection.CollectionCatalog
import com.example.tatratracker.collection.CollectionCatalogAsset
import com.example.tatratracker.collection.CollectionProgress
import com.example.tatratracker.collection.CollectionProgressCalculator
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.SavedPlaceRepository
import com.example.tatratracker.data.TrailProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class CollectionUiState(
    val isLoading: Boolean = true,
    val progress: CollectionProgress? = null,
    val achievements: AchievementsResult? = null,
    val errorMessage: String? = null
)

/**
 * Ekran „Kolekcja”: katalog z pliku aplikacji zestawiony z zapisanymi miejscami
 * i wizytami. Stan odświeża się sam po każdej zmianie w bazie (import, synchronizacja,
 * ręczne dodanie miejsca).
 */
class CollectionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val savedPlaceRepository = SavedPlaceRepository(AppDatabase.getDatabase(application))

    private val activityDao = AppDatabase.getDatabase(application).activityDao()

    // Osiągnięcie „pierwszy ukończony szlak” – otwarcie ekranu uruchamia liczenie postępu, jeśli jeszcze nie szło.
    private val trailProgressRepository = TrailProgressRepository.getInstance(application)

    private val catalog: Flow<CollectionCatalog> =
        flow {
            emit(CollectionCatalogAsset.load(application))
        }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<CollectionUiState> =
        combine(
            catalog,
            savedPlaceRepository.observePlaces(),
            savedPlaceRepository.observeVisits(),
            activityDao.getAllActivities(),
            trailProgressRepository.state
        ) { catalog, places, visits, activities, trailProgress ->
            CollectionUiState(
                isLoading = false,
                progress = CollectionProgressCalculator.calculate(catalog, places, visits),
                achievements =
                    AchievementCalculator.calculate(
                        activities = activities,
                        places = places,
                        visits = visits,
                        catalog = catalog,
                        // Dopóki postęp się liczy, „pierwszy ukończony szlak” jest po prostu niezdobyty.
                        completedTrailCount = trailProgress.result?.let { trailProgress.completedTrailCount }
                    )
            )
        }
            .flowOn(Dispatchers.Default)
            .catch { error ->
                emit(
                    CollectionUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Nie udało się wczytać kolekcji."
                    )
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())
}
