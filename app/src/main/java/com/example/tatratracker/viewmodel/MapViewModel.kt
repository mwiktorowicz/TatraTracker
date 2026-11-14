package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.MapLayerSettingsStore
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.ui.map.MapLayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val _tracks =
        MutableStateFlow<List<List<TrackPoint>>>(
            emptyList()
        )

    val tracks: StateFlow<List<List<TrackPoint>>> =
        _tracks.asStateFlow()

    private val layerSettingsStore = MapLayerSettingsStore(application)

    private val _layerSettings = MutableStateFlow(MapLayerSettings())

    /** Widoczność warstw mapy, wczytana z DataStore przy otwarciu mapy. */
    val layerSettings: StateFlow<MapLayerSettings> = _layerSettings.asStateFlow()

    // Jeśli użytkownik przełączy warstwę, zanim skończy się odczyt, nie nadpisujemy jego wyboru.
    private var layerSettingsChangedByUser = false

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