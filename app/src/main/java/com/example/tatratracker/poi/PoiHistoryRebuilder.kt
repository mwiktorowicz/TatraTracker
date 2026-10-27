package com.example.tatratracker.poi

import android.content.Context
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.TrackPointMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PoiHistoryRebuilder(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    private val database =
        AppDatabase.getDatabase(
            applicationContext
        )

    private val activityDao =
        database.activityDao()

    private val poiDetector =
        ActivityPoiDetector(
            applicationContext
        )

    suspend fun rebuild():
            Result<PoiHistoryRebuildResult> {

        return withContext(
            Dispatchers.IO
        ) {

            try {

                Result.success(
                    rebuildInternal()
                )

            } catch (error: Throwable) {

                Result.failure(
                    error
                )
            }
        }
    }

    private suspend fun rebuildInternal():
            PoiHistoryRebuildResult {

        val activities =
            activityDao
                .getAllActivities()
                .first()
                .sortedBy {
                    it.timestamp
                }

        if (
            activities.isEmpty()
        ) {

            return PoiHistoryRebuildResult(
                totalActivities = 0
            )
        }

        val allTrackPointEntities =
            activityDao
                .getAllTrackPoints()
                .first()

        val trackPointsByActivity =
            allTrackPointEntities
                .groupBy {
                    it.activityId
                }

        var analyzedActivities =
            0

        var activitiesOutsideTatras =
            0

        var activitiesWithoutTrack =
            0

        var failedActivities =
            0

        var matchedPlaces =
            0

        var newPlaces =
            0

        var newVisits =
            0

        val detectedNames =
            linkedSetOf<String>()

        val failures =
            mutableListOf<PoiHistoryFailure>()

        activities.forEachIndexed {
                index,
                activity ->

            val activityPointEntities =
                trackPointsByActivity[
                    activity.id
                ]
                    .orEmpty()

            val trackPoints =
                TrackPointMapper.toModels(
                    activityPointEntities
                )

            val detectionResult =
                poiDetector.detectAndRegister(
                    activityId =
                        activity.id,
                    visitedAt =
                        activity.timestamp,
                    trackPoints =
                        trackPoints
                )

            if (
                detectionResult.isFailure
            ) {

                failedActivities++

                val error =
                    detectionResult.exceptionOrNull()

                failures +=
                    PoiHistoryFailure(
                        activityId =
                            activity.id,
                        activityName =
                            activity.name,
                        timestamp =
                            activity.timestamp,
                        reason =
                            formatFailureReason(
                                error
                            )
                    )

            } else {

                val detection =
                    detectionResult.getOrThrow()

                when {

                    detection.skippedNoTrack -> {

                        activitiesWithoutTrack++
                    }

                    detection.skippedOutsideTatras -> {

                        activitiesOutsideTatras++
                    }

                    else -> {

                        analyzedActivities++

                        matchedPlaces +=
                            detection.matchedPlaces

                        newPlaces +=
                            detection.newPlaces

                        newVisits +=
                            detection.newVisits

                        detectedNames.addAll(
                            detection.detectedNames
                        )
                    }
                }
            }

            if (
                index <
                activities.lastIndex
            ) {

                delay(
                    HISTORY_REQUEST_SPACING_MS
                )
            }
        }

        return PoiHistoryRebuildResult(
            totalActivities =
                activities.size,
            analyzedActivities =
                analyzedActivities,
            activitiesOutsideTatras =
                activitiesOutsideTatras,
            activitiesWithoutTrack =
                activitiesWithoutTrack,
            failedActivities =
                failedActivities,
            matchedPlaces =
                matchedPlaces,
            newPlaces =
                newPlaces,
            newVisits =
                newVisits,
            detectedNames =
                detectedNames.toList(),
            failures =
                failures
        )
    }

    private fun formatFailureReason(
        error: Throwable?
    ): String {

        if (
            error == null
        ) {

            return "Nieznany błąd."
        }

        val messages =
            mutableListOf<String>()

        var current:
                Throwable? =
            error

        var level =
            0

        while (
            current != null &&
            level < 4
        ) {

            val text =
                current.message
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            if (
                text != null &&
                text !in messages
            ) {

                messages +=
                    text
            }

            current =
                current.cause

            level++
        }

        if (
            messages.isNotEmpty()
        ) {

            return messages
                .joinToString(
                    separator =
                        " → "
                )
        }

        return error
            .javaClass
            .simpleName
            .takeIf {
                it.isNotBlank()
            }
            ?: "Nieznany błąd."
    }

    companion object {

        private const val HISTORY_REQUEST_SPACING_MS =
            800L
    }
}

data class PoiHistoryRebuildResult(

    val totalActivities: Int,

    val analyzedActivities: Int = 0,

    val activitiesOutsideTatras: Int = 0,

    val activitiesWithoutTrack: Int = 0,

    val failedActivities: Int = 0,

    val matchedPlaces: Int = 0,

    val newPlaces: Int = 0,

    val newVisits: Int = 0,

    val detectedNames: List<String> =
        emptyList(),

    val failures: List<PoiHistoryFailure> =
        emptyList()
)

data class PoiHistoryFailure(

    val activityId: Int,

    val activityName: String,

    val timestamp: Long,

    val reason: String
) {

    fun displayDate(): String {

        val formatter =
            SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            )

        return formatter.format(
            Date(
                timestamp
            )
        )
    }
}