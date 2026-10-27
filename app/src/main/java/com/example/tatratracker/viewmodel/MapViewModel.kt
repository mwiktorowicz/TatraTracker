package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.model.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    init {
        observeAllTracks()
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