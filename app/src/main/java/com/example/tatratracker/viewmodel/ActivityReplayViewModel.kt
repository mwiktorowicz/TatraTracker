package com.example.tatratracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.MapLayerSettingsStore
import com.example.tatratracker.data.TrackPointEntity
import com.example.tatratracker.replay.ActivityReplayTrack
import com.example.tatratracker.replay.ReplayPoint
import com.example.tatratracker.replay.ReplayTempo
import com.example.tatratracker.replay.replayStepMeters
import com.example.tatratracker.ui.map.MapLayerSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ActivityReplayLoadState {

    data object Loading : ActivityReplayLoadState

    data class Error(val message: String) : ActivityReplayLoadState

    data class Ready(
        val track: ActivityReplayTrack,
        /** Podkład i widoczność warstw zapisane na mapie głównej. */
        val layerSettings: MapLayerSettings
    ) : ActivityReplayLoadState
}

/**
 * Stan odtwarzania wędrówki. Przetrwa obrót ekranu; ekran przy zamknięciu (także przy obrocie)
 * woła [pause], więc odtwarzanie wraca jako pauza w tym samym miejscu.
 */
class ActivityReplayViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val activityDao = AppDatabase.getDatabase(application).activityDao()

    private val layerSettingsStore = MapLayerSettingsStore(application)

    private val activityId: Int = savedStateHandle.get<Int>("activityId") ?: -1

    private val _loadState = MutableStateFlow<ActivityReplayLoadState>(ActivityReplayLoadState.Loading)
    val loadState: StateFlow<ActivityReplayLoadState> = _loadState.asStateFlow()

    private val _progressMeters = MutableStateFlow(0.0)

    /** Postęp odtwarzania w metrach (dystans po filtrze postojów). */
    val progressMeters: StateFlow<Double> = _progressMeters.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _tempo = MutableStateFlow(ReplayTempo.NORMAL)
    val tempo: StateFlow<ReplayTempo> = _tempo.asStateFlow()

    private val _following = MutableStateFlow(true)

    /** Czy kamera podąża za znacznikiem. */
    val following: StateFlow<Boolean> = _following.asStateFlow()

    init {
        load()
    }

    /** Przesuwa znacznik o czas jednej klatki. */
    fun advance(elapsedNanos: Long) {
        val track = readyTrack() ?: return

        if (!_isPlaying.value) {
            return
        }

        // Pojedyncza długa klatka (np. przycięcie systemu) nie robi skoku znacznika.
        val step = replayStepMeters(
            totalProgressMeters = track.totalProgressMeters,
            tempo = _tempo.value,
            elapsedNanos = elapsedNanos.coerceAtMost(MAX_FRAME_NANOS)
        )

        val next = track.clampProgress(_progressMeters.value + step)
        _progressMeters.value = next

        if (next >= track.totalProgressMeters) {
            _isPlaying.value = false
        }
    }

    /** Odtwórz / pauza; na końcu trasy – od początku. */
    fun togglePlay() {
        val track = readyTrack() ?: return

        if (_isPlaying.value) {
            _isPlaying.value = false
            return
        }

        if (track.totalProgressMeters <= 0.0) {
            return
        }

        if (_progressMeters.value >= track.totalProgressMeters) {
            _progressMeters.value = 0.0
        }

        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    /** Przeniesienie znacznika (np. z profilu wysokości) – pauzuje odtwarzanie. */
    fun seekTo(progressMeters: Double) {
        val track = readyTrack() ?: return

        _isPlaying.value = false
        _progressMeters.value = track.clampProgress(progressMeters)
    }

    fun setTempo(tempo: ReplayTempo) {
        _tempo.value = tempo
    }

    fun setFollowing(following: Boolean) {
        _following.value = following
    }

    private fun readyTrack(): ActivityReplayTrack? {
        return (_loadState.value as? ActivityReplayLoadState.Ready)?.track
    }

    private fun load() {
        if (activityId < 0) {
            _loadState.value = ActivityReplayLoadState.Error("Nie znaleziono tej aktywności.")
            return
        }

        viewModelScope.launch {
            _loadState.value =
                try {
                    val layerSettings = readLayerSettings()
                    val entities = activityDao.getTrackPointsForActivity(activityId).first()

                    val track = withContext(Dispatchers.Default) {
                        ActivityReplayTrack.build(entities.map { it.toReplayPoint() })
                    }

                    if (track == null) {
                        ActivityReplayLoadState.Error("Ta aktywność nie ma śladu GPS do odtworzenia.")
                    } else {
                        ActivityReplayLoadState.Ready(track, layerSettings)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ActivityReplayLoadState.Error(
                        error.message ?: "Nie udało się wczytać śladu aktywności."
                    )
                }
        }
    }

    // Błąd odczytu ustawień nie blokuje odtwarzania – wtedy podkład domyślny.
    private suspend fun readLayerSettings(): MapLayerSettings {
        return try {
            layerSettingsStore.settings.first()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            MapLayerSettings()
        }
    }

    private companion object {
        const val MAX_FRAME_NANOS = 100_000_000L
    }
}

// Z encji, nie z TrackPoint: model zamienia brak wysokości na 0.
private fun TrackPointEntity.toReplayPoint(): ReplayPoint {
    return ReplayPoint(
        lat = latitude,
        lon = longitude,
        elevationMeters = altitudeMeters,
        timestampSeconds = timestampSeconds,
        heartRate = heartRate,
        speedMs = speedMs
    )
}
