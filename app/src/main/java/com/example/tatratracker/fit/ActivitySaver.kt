package com.example.tatratracker.fit

import android.content.Context
import androidx.room.withTransaction
import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import com.example.tatratracker.model.TrackPoint
import com.example.tatratracker.poi.ActivityPoiDetectionResult
import com.example.tatratracker.poi.ActivityPoiDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SavedActivity(
    val activityId: Int,
    /** Wynik wykrywania odwiedzonych miejsc; błąd tutaj nie cofa zapisu aktywności. */
    val poiDetection: Result<ActivityPoiDetectionResult>,
)

/**
 * Wspólna ścieżka zapisu nowej aktywności – dla importu pliku i synchronizacji
 * z Samsung Health: aktywność i punkty w jednej transakcji, potem wykrywanie POI.
 */
class ActivitySaver(context: Context) {

    private val database = AppDatabase.getDatabase(context.applicationContext)
    private val activityDao = database.activityDao()
    private val poiDetector = ActivityPoiDetector(context.applicationContext)

    suspend fun save(activity: ActivityEntity, trackPoints: List<TrackPoint>): SavedActivity =
        withContext(Dispatchers.IO) {
            val activityId = database.withTransaction {
                val insertedId = activityDao.insertActivity(activity)

                if (insertedId <= 0L || insertedId > Int.MAX_VALUE) {
                    throw IllegalStateException("Nie udało się zapisać aktywności.")
                }

                val id = insertedId.toInt()
                activityDao.insertTrackPoints(TrackPointMapper.toEntities(points = trackPoints, activityId = id))
                id
            }

            val poiDetection = poiDetector.detectAndRegister(
                activityId = activityId,
                visitedAt = activity.timestamp,
                trackPoints = trackPoints,
            )

            SavedActivity(activityId = activityId, poiDetection = poiDetection)
        }
}
