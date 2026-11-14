package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppSettings
import com.example.tatratracker.data.AppSettingsStore
import com.example.tatratracker.data.ElevationSampling
import com.example.tatratracker.data.RoutingProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val store = AppSettingsStore(application)

    /** Zapisane ustawienia; null, dopóki nie skończy się pierwszy odczyt. */
    val settings: StateFlow<AppSettings?> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setAutoFitRoute(enabled: Boolean) {
        update { it.copy(autoFitRoute = enabled) }
    }

    fun setRoutingProfile(profile: RoutingProfile) {
        update { it.copy(routingProfile = profile) }
    }

    fun setElevationSampling(sampling: ElevationSampling) {
        update { it.copy(elevationSampling = sampling) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            store.update(transform)
        }
    }
}
