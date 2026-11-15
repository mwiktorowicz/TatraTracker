package com.example.tatratracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tatratracker.summary.ActivitySummaryCalculator
import com.example.tatratracker.summary.TrailProgressBaseline
import com.example.tatratracker.summary.TrailProgressRecord
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Uszkodzony plik zastępujemy pustym – wtedy po prostu nie ma punktu odniesienia.
private val Context.trailProgressBaselineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trail_progress_baseline",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * Pamięta dwa ostatnie stany postępu na sieci szlaków (DataStore Preferences, plik
 * `trail_progress_baseline`), żeby podsumowanie wędrówki mogło pokazać, o ile postęp urósł.
 */
class TrailProgressBaselineStore(context: Context) {

    private val dataStore = context.applicationContext.trailProgressBaselineDataStore

    val baseline: Flow<TrailProgressBaseline> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences.toTrailProgressBaseline() }

    /** Zapisuje świeżo policzony postęp (atomowo, na aktualnie zapisanym stanie). */
    suspend fun record(fraction: Double, maxActivityId: Int) {
        dataStore.edit { preferences ->
            preferences.writeTrailProgressBaseline(
                ActivitySummaryCalculator.advanceBaseline(
                    baseline = preferences.toTrailProgressBaseline(),
                    fraction = fraction,
                    maxActivityId = maxActivityId
                )
            )
        }
    }
}

/**
 * Klucze zapisu. Nazw nie zmieniać – są zapisane na telefonie.
 */
internal object TrailProgressBaselineKeys {
    val CURRENT_FRACTION = doublePreferencesKey("current_fraction")
    val CURRENT_MAX_ACTIVITY_ID = intPreferencesKey("current_max_activity_id")
    val PREVIOUS_FRACTION = doublePreferencesKey("previous_fraction")
    val PREVIOUS_MAX_ACTIVITY_ID = intPreferencesKey("previous_max_activity_id")
}

/** Stan niepełny (brak jednego z kluczy, zły typ, ułamek spoza 0–1) liczy się jako brak. */
internal fun Preferences.toTrailProgressBaseline(): TrailProgressBaseline {
    return TrailProgressBaseline(
        current = readRecord(
            TrailProgressBaselineKeys.CURRENT_FRACTION,
            TrailProgressBaselineKeys.CURRENT_MAX_ACTIVITY_ID
        ),
        previous = readRecord(
            TrailProgressBaselineKeys.PREVIOUS_FRACTION,
            TrailProgressBaselineKeys.PREVIOUS_MAX_ACTIVITY_ID
        )
    )
}

internal fun MutablePreferences.writeTrailProgressBaseline(baseline: TrailProgressBaseline) {
    writeRecord(
        baseline.current,
        TrailProgressBaselineKeys.CURRENT_FRACTION,
        TrailProgressBaselineKeys.CURRENT_MAX_ACTIVITY_ID
    )
    writeRecord(
        baseline.previous,
        TrailProgressBaselineKeys.PREVIOUS_FRACTION,
        TrailProgressBaselineKeys.PREVIOUS_MAX_ACTIVITY_ID
    )
}

private fun Preferences.readRecord(
    fractionKey: Preferences.Key<Double>,
    maxActivityIdKey: Preferences.Key<Int>
): TrailProgressRecord? {
    // Przez asMap, bo pod naszą nazwą może leżeć wartość innego typu (por. AppSettingsStore).
    val fraction = asMap()[fractionKey] as? Double ?: return null
    val maxActivityId = asMap()[maxActivityIdKey] as? Int ?: return null

    if (!fraction.isFinite() || fraction < 0.0 || fraction > 1.0) {
        return null
    }

    return TrailProgressRecord(fraction = fraction, maxActivityId = maxActivityId)
}

private fun MutablePreferences.writeRecord(
    record: TrailProgressRecord?,
    fractionKey: Preferences.Key<Double>,
    maxActivityIdKey: Preferences.Key<Int>
) {
    if (record == null) {
        remove(fractionKey)
        remove(maxActivityIdKey)
    } else {
        this[fractionKey] = record.fraction
        this[maxActivityIdKey] = record.maxActivityId
    }
}
