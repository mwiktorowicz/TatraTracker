package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val activityDao =
        AppDatabase
            .getDatabase(application)
            .activityDao()

    private val _activities =
        MutableStateFlow<List<ActivityEntity>>(
            emptyList()
        )

    val activities: StateFlow<List<ActivityEntity>> =
        _activities.asStateFlow()

    init {
        observeActivities()
    }

    private fun observeActivities() {

        viewModelScope.launch {

            activityDao
                .getAllActivities()
                .collectLatest { activities ->

                    _activities.value =
                        activities
                }
        }
    }
}