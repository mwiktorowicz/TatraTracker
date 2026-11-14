package com.example.tatratracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.export.ActivityGpxExporter
import com.example.tatratracker.model.TrackPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _message = MutableStateFlow<ActivityDetailsMessage?>(null)

    /** Komunikat do pokazania w okienku (wynik zapisu lub eksportu). */
    val message: StateFlow<ActivityDetailsMessage?> = _message.asStateFlow()

    init {
        loadActivity()
    }

    fun consumeMessage() {
        _message.value = null
    }

    /**
     * Zapisuje nazwę i notatkę. Nazwa jest przycinana i nie może być pusta,
     * pusta notatka jest zapisywana jako brak notatki.
     */
    fun saveDetails(name: String, note: String) {
        val cleanName = name.trim()
        val cleanNote = note.trim().ifEmpty { null }

        if (activityId < 0 || cleanName.isEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                activityDao.updateNameAndNote(activityId, cleanName, cleanNote)
                _activity.value = activityDao.getActivityById(activityId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _message.value = ActivityDetailsMessage(
                    title = "Nie udało się zapisać zmian",
                    message = error.message ?: "Wystąpił nieznany błąd zapisu."
                )
            }
        }
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

    /**
     * Zapisuje ślad aktywności jako GPX do pliku wybranego w systemowym oknie „Utwórz dokument”.
     * Punkty są czytane z bazy na nowo, bo model [TrackPoint] zamienia brak wysokości na 0.
     */
    fun exportGpx(uri: Uri) {
        val currentActivity = _activity.value ?: return

        viewModelScope.launch {
            _message.value =
                try {
                    val points = activityDao.getTrackPointsForActivity(activityId).first()

                    val gpx = withContext(Dispatchers.Default) {
                        ActivityGpxExporter.buildGpx(currentActivity, points)
                    }

                    withContext(Dispatchers.IO) {
                        val outputStream =
                            getApplication<Application>().contentResolver.openOutputStream(uri)
                                ?: error("Nie udało się otworzyć wybranego pliku.")

                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(gpx) }
                    }

                    ActivityDetailsMessage(
                        title = "GPX zapisany",
                        message = "Ślad aktywności „${currentActivity.name}” został zapisany do pliku GPX."
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ActivityDetailsMessage(
                        title = "Nie udało się zapisać GPX",
                        message = error.message ?: "Wystąpił nieznany błąd zapisu pliku."
                    )
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

data class ActivityDetailsMessage(
    val title: String,
    val message: String
)
