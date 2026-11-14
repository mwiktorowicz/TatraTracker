package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.AppSettings
import com.example.tatratracker.data.AppSettingsStore
import com.example.tatratracker.data.MapLayerSettingsStore
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.data.SavedPlaceRepository
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.data.TrailProgressRepository
import com.example.tatratracker.data.TrailProgressState
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.ui.map.MapLayerSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Stan warstwy „Przebyte szlaki”. [geoJson] = null – warstwa pusta
 * (wyłączona, jeszcze nie policzona albo brak wyniku).
 */
data class WalkedTrailsLayerState(
    val isLoading: Boolean = false,
    val geoJson: String? = null,
    val hasError: Boolean = false
)

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val savedPlaceRepository = SavedPlaceRepository(AppDatabase.getDatabase(application))

    private val _tracks =
        MutableStateFlow<List<List<TrackPoint>>>(
            emptyList()
        )

    val tracks: StateFlow<List<List<TrackPoint>>> =
        _tracks.asStateFlow()

    private val layerSettingsStore = MapLayerSettingsStore(application)

    private val appSettingsStore = AppSettingsStore(application)

    private val _layerSettings = MutableStateFlow(MapLayerSettings())

    /** Widoczność warstw mapy, wczytana z DataStore przy otwarciu mapy. */
    val layerSettings: StateFlow<MapLayerSettings> = _layerSettings.asStateFlow()

    // Jeśli użytkownik przełączy warstwę, zanim skończy się odczyt, nie nadpisujemy jego wyboru.
    private var layerSettingsChangedByUser = false

    /** Zapisane miejsca do warstwy „Moje punkty”; nowe i usunięte widać od razu. */
    val savedPlaces: StateFlow<List<SavedPlaceEntity>> =
        savedPlaceRepository
            .observePlaces()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Samo pobranie repozytorium uruchamia kosztowne dopasowanie śladów,
    // więc sięgamy po nie dopiero, gdy warstwa „Przebyte szlaki” jest włączona.
    private val trailProgressRepository by lazy {
        TrailProgressRepository.getInstance(application)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val walkedTrails: StateFlow<WalkedTrailsLayerState> =
        _layerSettings
            .map { settings -> settings.walkedTrails }
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) {
                    observeWalkedTrails()
                } else {
                    flowOf(WalkedTrailsLayerState())
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WalkedTrailsLayerState())

    init {
        observeAllTracks()
        loadLayerSettings()
    }

    fun updateLayerSettings(settings: MapLayerSettings) {
        layerSettingsChangedByUser = true
        _layerSettings.value = settings
        viewModelScope.launch {
            layerSettingsStore.save(settings)
        }
    }

    /**
     * Aktualne ustawienia z ekranu Ustawień. Czytane przy każdym wyznaczeniu trasy,
     * więc zmiana działa od następnego wyznaczenia, także gdy mapa była otwarta w tle.
     */
    suspend fun currentAppSettings(): AppSettings {
        return appSettingsStore.settings.first()
    }

    /** „Dodaj własny punkt”: zapis w Schroniskach i punktach, kategoria „Inne”. */
    suspend fun saveOwnPlace(name: String, latitude: Double, longitude: Double): Result<Long> {
        return savedPlaceRepository.savePlace(
            name = name,
            category = SavedPlaceCategory.OTHER,
            latitude = latitude,
            longitude = longitude,
            note = null
        )
    }

    /*
     * Wywoływane przy każdym włączeniu warstwy (także zapisanej jako włączona).
     * Jeśli ostatnie liczenie skończyło się błędem, prosimy o ponowną próbę;
     * poprawnego wyniku nie liczymy od nowa.
     */
    private fun observeWalkedTrails(): Flow<WalkedTrailsLayerState> {
        val repository = trailProgressRepository
        if (repository.state.value.errorMessage != null) {
            repository.recalculate()
        }
        return repository.state.map { state -> state.toWalkedTrailsLayerState() }
    }

    private fun TrailProgressState.toWalkedTrailsLayerState(): WalkedTrailsLayerState {
        return WalkedTrailsLayerState(
            isLoading = isLoading,
            geoJson = result?.matchedGeoJson,
            hasError = errorMessage != null
        )
    }

    private fun loadLayerSettings() {
        viewModelScope.launch {
            val stored = layerSettingsStore.settings.first()
            if (!layerSettingsChangedByUser) {
                _layerSettings.value = stored
            }
        }
    }

    private fun observeAllTracks() {

        viewModelScope.launch {

            activityDao
                .getAllTrackPoints()
                .collectLatest { entities ->

                    if (entities.isEmpty()) {

                        _tracks.value =
                            emptyList()

                        return@collectLatest
                    }

                    _tracks.value =
                        entities
                            .groupBy { entity ->
                                entity.activityId
                            }
                            .values
                            .map { activityPoints ->

                                TrackPointMapper.toModels(
                                    activityPoints
                                )
                            }
                            .filter { track ->
                                track.isNotEmpty()
                            }
                }
        }
    }
}