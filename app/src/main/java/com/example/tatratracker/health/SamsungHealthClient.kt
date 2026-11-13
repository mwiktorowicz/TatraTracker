package com.example.tatratracker.health

import android.app.Activity
import android.content.Context
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.ExerciseLocation
import com.samsung.android.sdk.health.data.data.entries.ExerciseLog
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import java.time.LocalDateTime

/**
 * Jedyne miejsce w aplikacji (poza ekranem testowym), które używa Samsung Health Data SDK.
 * Wyjątki SDK (HealthDataException i podklasy) przepuszcza dalej – obsługuje je serwis.
 */
class SamsungHealthClient(context: Context) {

    private val appContext = context.applicationContext

    private val store: HealthDataStore by lazy { HealthDataService.getStore(appContext) }

    /** Czy obie zgody (ćwiczenia i lokalizacja ćwiczeń) są nadane. Nigdy nie otwiera okna zgód. */
    suspend fun hasPermissions(): Boolean =
        store.getGrantedPermissions(PERMISSIONS).containsAll(PERMISSIONS)

    /** Otwiera okno zgód, jeśli którejś brakuje. Zwraca true, gdy obie są nadane. */
    suspend fun requestPermissions(activity: Activity): Boolean {
        if (hasPermissions()) {
            return true
        }
        return store.requestPermissions(PERMISSIONS, activity).containsAll(PERMISSIONS)
    }

    /**
     * Czyta treningi od [since] (null = cała historia) do teraz, od najnowszych,
     * i przekazuje je stronami do [onPage], żeby nie trzymać całej historii tras w pamięci.
     */
    suspend fun readExercises(
        since: LocalDateTime?,
        onPage: suspend (List<HealthExerciseRecord>) -> Unit,
    ) {
        val end = LocalDateTime.now()
        val timeFilter = LocalTimeFilter.of(since ?: FULL_HISTORY_START, end)

        var pageToken: String? = null
        var pageCount = 0

        do {
            val builder = DataTypes.EXERCISE.readDataRequestBuilder
                .setLocalTimeFilter(timeFilter)
                .setOrdering(Ordering.DESC)
            pageToken?.let { builder.setPageToken(it) }

            val response = store.readData(builder.build())
            onPage(response.dataList.map { it.toRecord() })

            pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
            pageCount++
        } while (pageToken != null && pageCount < MAX_PAGES)

        if (pageToken != null) {
            Log.w(LOG_TAG, "Przerwano odczyt po $MAX_PAGES stronach – starsze treningi pominięte.")
        }
    }

    private fun HealthDataPoint.toRecord(): HealthExerciseRecord =
        HealthExerciseRecord(
            uid = uid,
            appId = dataSource?.appId,
            exerciseType = getValue(DataType.ExerciseType.EXERCISE_TYPE)?.name,
            zoneOffset = zoneOffset,
            sessions = getValue(DataType.ExerciseType.SESSIONS).orEmpty().map { it.toModel() },
        )

    private fun ExerciseSession.toModel(): HealthSession =
        HealthSession(
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            exerciseType = exerciseType?.name,
            customTitle = customTitle,
            distanceMeters = distance?.toDouble(),
            calories = calories.toDouble(),
            minAltitude = minAltitude?.toDouble(),
            maxAltitude = maxAltitude?.toDouble(),
            meanHeartRate = meanHeartRate?.toDouble(),
            maxHeartRate = maxHeartRate?.toDouble(),
            maxSpeed = maxSpeed?.toDouble(),
            route = route.orEmpty().map { it.toModel() },
            log = log.orEmpty().map { it.toModel() },
        )

    private fun ExerciseLocation.toModel(): HealthRoutePoint =
        HealthRoutePoint(
            timestamp = timestamp,
            latitude = latitude.toDouble(),
            longitude = longitude.toDouble(),
            altitude = altitude?.toDouble(),
        )

    private fun ExerciseLog.toModel(): HealthLogEntry =
        HealthLogEntry(
            timestamp = timestamp,
            heartRate = heartRate?.toDouble(),
            speed = speed?.toDouble(),
        )

    companion object {

        private const val LOG_TAG = "SamsungHealthClient"

        /** Zabezpieczenie przed nieskończonym stronicowaniem. */
        private const val MAX_PAGES = 500

        /** Początek zakresu dla „całej historii” – sprzed powstania Samsung Health. */
        private val FULL_HISTORY_START: LocalDateTime = LocalDateTime.of(2010, 1, 1, 0, 0)

        private val PERMISSIONS: Set<Permission> = setOf(
            Permission.of(DataTypes.EXERCISE, AccessType.READ),
            Permission.of(DataTypes.EXERCISE_LOCATION, AccessType.READ),
        )
    }
}
