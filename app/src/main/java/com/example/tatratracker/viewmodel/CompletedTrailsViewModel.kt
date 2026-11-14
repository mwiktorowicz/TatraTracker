package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.tatratracker.data.TrailProgressRepository
import com.example.tatratracker.data.TrailProgressState
import kotlinx.coroutines.flow.StateFlow

/**
 * Ekran „Ukończone szlaki”. Wynik dopasowania pochodzi ze wspólnego
 * [TrailProgressRepository] – tego samego, z którego korzystają Statystyki.
 */
class CompletedTrailsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val trailProgressRepository = TrailProgressRepository.getInstance(application)

    val uiState: StateFlow<TrailProgressState> = trailProgressRepository.state

    fun recalculate() {
        trailProgressRepository.recalculate()
    }
}
