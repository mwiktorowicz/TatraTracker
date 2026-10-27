package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.model.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ActivityDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val activityId: Int =
        savedStateHandle.get<Int>("activityId")
            ?: -1

    private val _activity =
        MutableStateFlow<ActivityEntity?>(null)

    val activity: StateFlow<ActivityEntity?> =
        _activity.asStateFlow()

    private val _trackPoints =
        MutableStateFlow<List<TrackPoint>>(
            emptyList()
        )

    val trackPoints: StateFlow<List<TrackPoint>> =
        _trackPoints.asStateFlow()

    private val _isLoading =
        MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _isDeleted =
        MutableStateFlow(false)

    val isDeleted: StateFlow<Boolean> =
        _isDeleted.asStateFlow()

    init {
        loadActivity()
    }

    private fun loadActivity() {

        if (activityId < 0) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {

            _activity.value =
                activityDao.getActivityById(
                    activityId
                )

            activityDao
                .getTrackPointsForActivity(
                    activityId
                )
                .collectLatest { entities ->

                    _trackPoints.value =
                        TrackPointMapper.toModels(
                            entities
                        )

                    _isLoading.value =
                        false
                }
        }
    }

    fun deleteActivity() {

        if (activityId < 0) {
            return
        }

        viewModelScope.launch {

            activityDao.deleteActivity(
                activityId
            )

            _activity.value = null
            _trackPoints.value = emptyList()
            _isDeleted.value = true
        }
    }
}