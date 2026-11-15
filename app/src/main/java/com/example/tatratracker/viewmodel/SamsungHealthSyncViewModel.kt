package com.example.tatratracker.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.health.ManualSyncResult
import com.example.tatratracker.health.SamsungHealthSyncService
import com.example.tatratracker.health.SyncSummary
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Zakończona synchronizacja, która coś zaimportowała – do komunikatu z przejściem do podsumowania. */
data class ImportedActivitiesEvent(
    val count: Int,
    /** Najpóźniej rozpoczęta z zaimportowanych aktywności. */
    val newestActivityId: Int?,
)

sealed interface SamsungHealthSyncState {

    data object Idle : SamsungHealthSyncState

    data class Running(
        val automatic: Boolean,
        val fullHistory: Boolean,
        val progress: SyncSummary,
    ) : SamsungHealthSyncState

    data class Finished(val summary: SyncSummary) : SamsungHealthSyncState

    /** Użytkownik nie nadał zgody na ćwiczenia lub lokalizację ćwiczeń. */
    data object PermissionsMissing : SamsungHealthSyncState

    data class Failed(val message: String) : SamsungHealthSyncState
}

/**
 * Stan synchronizacji z Samsung Health. Tworzony na poziomie aktywności (MainActivity),
 * żeby automatyczna synchronizacja przy starcie i przycisk na ekranie importu
 * dzieliły jeden stan.
 */
class SamsungHealthSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val service = SamsungHealthSyncService.getInstance(application)

    private val _state = MutableStateFlow<SamsungHealthSyncState>(SamsungHealthSyncState.Idle)
    val state: StateFlow<SamsungHealthSyncState> = _state.asStateFlow()

    private val _importedEvents = Channel<ImportedActivitiesEvent>(Channel.BUFFERED)

    /** Synchronizacje (automatyczne i ręczne), które zaimportowały choć jedną aktywność. */
    val importedEvents: Flow<ImportedActivitiesEvent> = _importedEvents.receiveAsFlow()

    init {
        startAutoSync()
    }

    fun sync(activity: Activity, fullHistory: Boolean) {
        if (_state.value is SamsungHealthSyncState.Running) {
            return
        }

        viewModelScope.launch {
            _state.value = SamsungHealthSyncState.Running(
                automatic = false,
                fullHistory = fullHistory,
                progress = SyncSummary(),
            )

            _state.value = try {
                val result = service.sync(activity, fullHistory) { progress ->
                    _state.value = SamsungHealthSyncState.Running(
                        automatic = false,
                        fullHistory = fullHistory,
                        progress = progress,
                    )
                }
                when (result) {
                    is ManualSyncResult.Completed -> {
                        notifyImported(result.summary)
                        SamsungHealthSyncState.Finished(result.summary)
                    }
                    ManualSyncResult.PermissionsMissing -> SamsungHealthSyncState.PermissionsMissing
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Synchronizacja nie powiodła się", e)
                SamsungHealthSyncState.Failed(describeFailure(e, activity))
            }
        }
    }

    private fun startAutoSync() {
        viewModelScope.launch {
            val summary = service.autoSyncOnce { progress ->
                _state.value = SamsungHealthSyncState.Running(
                    automatic = true,
                    fullHistory = false,
                    progress = progress,
                )
            }

            if (_state.value is SamsungHealthSyncState.Running) {
                _state.value =
                    if (summary != null && summary.imported > 0) {
                        SamsungHealthSyncState.Finished(summary)
                    } else {
                        SamsungHealthSyncState.Idle
                    }
            }

            if (summary != null) {
                notifyImported(summary)
            }
        }
    }

    private suspend fun notifyImported(summary: SyncSummary) {
        if (summary.imported > 0) {
            _importedEvents.send(ImportedActivitiesEvent(summary.imported, summary.newestActivityId))
        }
    }

    /** Dla błędu „brak lub stara wersja Samsung Health” uruchamia rozwiązanie podane przez SDK. */
    private fun describeFailure(error: Exception, activity: Activity): String {
        if (error is ResolvablePlatformException) {
            val resolved =
                error.hasResolution &&
                    runCatching { error.resolve(activity) }
                        .onFailure { Log.w(LOG_TAG, "Nie udało się uruchomić rozwiązania SDK", it) }
                        .isSuccess

            return if (resolved) {
                "Samsung Health wymaga instalacji, aktualizacji lub dodatkowej zgody. " +
                    "Dokończ ten krok i spróbuj ponownie."
            } else {
                "Samsung Health jest niedostępny na tym telefonie lub ma zbyt starą wersję (wymagana 6.30.2 lub nowsza)."
            }
        }

        if (error is HealthDataException) {
            val details = error.errorMessage ?: error.message ?: "brak opisu"
            return "Błąd Samsung Health (kod ${error.errorCode}): $details"
        }

        return error.message ?: "Nie udało się zsynchronizować z Samsung Health."
    }

    private companion object {
        const val LOG_TAG = "SamsungHealthSync"
    }
}
