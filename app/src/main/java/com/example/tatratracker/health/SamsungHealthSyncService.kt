package com.example.tatratracker.health

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.fit.ActivitySaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/** Podsumowanie synchronizacji: „zaimportowano X, pominięto Y, błędy Z”. */
data class SyncSummary(
    val imported: Int = 0,
    /** Treningi już zaimportowane wcześniej albo duplikaty aktywności z pliku. */
    val skipped: Int = 0,
    val failed: Int = 0,
)

sealed interface ManualSyncResult {
    data class Completed(val summary: SyncSummary) : ManualSyncResult
    data object PermissionsMissing : ManualSyncResult
}

/**
 * Synchronizacja wędrówek z Samsung Health: klient SDK → mapper → filtr duplikatów
 * → wspólny zapis (ActivitySaver, z wykrywaniem POI). Jedna instancja na proces;
 * synchronizacja ręczna i automatyczna nie biegną naraz.
 */
class SamsungHealthSyncService private constructor(context: Context) {

    private val client = SamsungHealthClient(context)
    private val activityDao = AppDatabase.getDatabase(context.applicationContext).activityDao()
    private val activitySaver = ActivitySaver(context)

    private val mutex = Mutex()

    @Volatile
    private var autoSyncStarted = false

    /**
     * Synchronizacja z przycisku. Prosi o zgody, jeśli ich brakuje.
     * Wyjątki SDK (np. ResolvablePlatformException) przepuszcza do wywołującego.
     */
    suspend fun sync(
        activity: Activity,
        fullHistory: Boolean,
        onProgress: (SyncSummary) -> Unit,
    ): ManualSyncResult = mutex.withLock {
        if (!client.requestPermissions(activity)) {
            return@withLock ManualSyncResult.PermissionsMissing
        }

        val since = if (fullHistory) null else LocalDateTime.now().minusDays(RECENT_DAYS)
        ManualSyncResult.Completed(runSync(since, onProgress))
    }

    /**
     * Synchronizacja automatyczna – raz na uruchomienie aplikacji, po cichu: tylko gdy zgody
     * są już nadane i nic innego nie synchronizuje. Zwraca null, gdy się nie odbyła albo
     * zakończyła błędem (błąd trafia tylko do logu).
     */
    suspend fun autoSyncOnce(onProgress: (SyncSummary) -> Unit): SyncSummary? {
        if (autoSyncStarted) {
            return null
        }
        autoSyncStarted = true

        if (!mutex.tryLock()) {
            return null
        }

        return try {
            if (client.hasPermissions()) {
                runSync(LocalDateTime.now().minusDays(RECENT_DAYS), onProgress)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Automatyczna synchronizacja nie powiodła się", e)
            null
        } finally {
            mutex.unlock()
        }
    }

    /** Na wątku IO: przepisywanie tras (ok. 1 punkt/s) nie może blokować interfejsu. */
    private suspend fun runSync(
        since: LocalDateTime?,
        onProgress: (SyncSummary) -> Unit,
    ): SyncSummary = withContext(Dispatchers.IO) {
        var summary = SyncSummary()
        onProgress(summary)

        client.readExercises(since) { page ->
            for (record in page) {
                record.sessions.forEachIndexed { index, session ->
                    if (!SamsungHealthSessionMapper.isImportable(record, session)) {
                        return@forEachIndexed
                    }

                    summary = try {
                        if (importSession(record, index, session)) {
                            summary.copy(imported = summary.imported + 1)
                        } else {
                            summary.copy(skipped = summary.skipped + 1)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Nie udało się zaimportować treningu ${record.uid} (sesja $index)", e)
                        summary.copy(failed = summary.failed + 1)
                    }
                    onProgress(summary)
                }
            }
        }

        summary
    }

    /** Zwraca true, gdy trening zapisano; false, gdy pominięto jako już istniejący. */
    private suspend fun importSession(record: HealthExerciseRecord, index: Int, session: HealthSession): Boolean {
        if (activityDao.activityExists(SamsungHealthSessionMapper.sourceId(record.uid, index))) {
            return false
        }

        val mapped = SamsungHealthSessionMapper.toActivity(record, index, session, ZoneId.systemDefault())
        val start = mapped.activity.timestamp
        val nearby = activityDao.getActivitiesStartedBetween(
            start - DuplicateActivityRule.MAX_START_DIFFERENCE_MILLIS,
            start + DuplicateActivityRule.MAX_START_DIFFERENCE_MILLIS,
        )
        if (DuplicateActivityRule.isDuplicateOfFileActivity(start, mapped.activity.distanceMeters, nearby)) {
            return false
        }

        val saved = activitySaver.save(mapped.activity, mapped.trackPoints)
        saved.poiDetection.onFailure { error ->
            Log.w(LOG_TAG, "Aktywność ${saved.activityId} zapisana, ale wykrywanie POI nie powiodło się", error)
        }
        return true
    }

    companion object {

        private const val LOG_TAG = "SamsungHealthSync"

        /** Zakres przycisku „Synchronizuj” i synchronizacji automatycznej. */
        const val RECENT_DAYS = 90L

        @Volatile
        private var instance: SamsungHealthSyncService? = null

        fun getInstance(context: Context): SamsungHealthSyncService =
            instance ?: synchronized(this) {
                instance ?: SamsungHealthSyncService(context.applicationContext).also { instance = it }
            }
    }
}
