package com.example.tatratracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Profil wyznaczania trasy pieszej w Valhalli.
 */
enum class RoutingProfile {
    /** Najkrótsza droga (`shortest` w żądaniu) – dotychczasowe zachowanie. */
    SHORTEST,

    /** Zwykły koszt pieszy Valhalli, bez wymuszania najkrótszej drogi. */
    STANDARD
}

/**
 * Docelowa odległość między próbkami profilu wysokości.
 */
enum class ElevationSampling(val meters: Int) {
    METERS_50(50),
    METERS_100(100),
    METERS_200(200);

    companion object {
        fun fromMeters(meters: Int): ElevationSampling? {
            return entries.firstOrNull { it.meters == meters }
        }
    }
}

/**
 * Ustawienia z ekranu Ustawień. Wartości domyślne = zachowanie aplikacji sprzed ekranu ustawień.
 */
data class AppSettings(
    val autoFitRoute: Boolean = true,
    val routingProfile: RoutingProfile = RoutingProfile.SHORTEST,
    val elevationSampling: ElevationSampling = ElevationSampling.METERS_100
)

// Uszkodzony plik ustawień zastępujemy pustym – wtedy obowiązują wartości domyślne.
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/**
 * Zapis i odczyt ustawień aplikacji (DataStore Preferences, plik `app_settings`).
 */
class AppSettingsStore(context: Context) {

    private val dataStore = context.applicationContext.appSettingsDataStore

    val settings: Flow<AppSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences.toAppSettings() }

    /** Zmiana liczona na aktualnie zapisanych ustawieniach (atomowo). */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            preferences.writeAppSettings(transform(preferences.toAppSettings()))
        }
    }
}

/**
 * Klucze ustawień. Nazw nie zmieniać – są zapisane na telefonie.
 */
internal object AppSettingsKeys {
    val AUTO_FIT_ROUTE = booleanPreferencesKey("auto_fit_route")
    val ROUTING_PROFILE = stringPreferencesKey("routing_profile")
    val ELEVATION_SAMPLING_METERS = intPreferencesKey("elevation_sampling_meters")
}

/**
 * Brakujący klucz, nieznana wartość albo wartość złego typu = wartość domyślna z [AppSettings].
 */
internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        autoFitRoute = readOrNull(AppSettingsKeys.AUTO_FIT_ROUTE) ?: defaults.autoFitRoute,
        routingProfile = readOrNull(AppSettingsKeys.ROUTING_PROFILE)
            ?.let { name -> RoutingProfile.entries.firstOrNull { it.name == name } }
            ?: defaults.routingProfile,
        elevationSampling = readOrNull(AppSettingsKeys.ELEVATION_SAMPLING_METERS)
            ?.let { meters -> ElevationSampling.fromMeters(meters) }
            ?: defaults.elevationSampling
    )
}

internal fun MutablePreferences.writeAppSettings(settings: AppSettings) {
    this[AppSettingsKeys.AUTO_FIT_ROUTE] = settings.autoFitRoute
    this[AppSettingsKeys.ROUTING_PROFILE] = settings.routingProfile.name
    this[AppSettingsKeys.ELEVATION_SAMPLING_METERS] = settings.elevationSampling.meters
}

/*
 * Klucze porównywane są tylko po nazwie, więc pod naszą nazwą może leżeć wartość innego typu.
 * Zwykłe `this[key]` rzuciłoby wtedy ClassCastException – tu dostajemy null.
 */
private inline fun <reified T : Any> Preferences.readOrNull(key: Preferences.Key<T>): T? {
    return asMap()[key] as? T
}
