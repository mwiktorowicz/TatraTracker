package com.example.tatratracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.tatratracker.ui.map.MapLayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mapLayerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "map_layer_settings"
)

/**
 * Zapis i odczyt widoczności warstw mapy (DataStore Preferences).
 */
class MapLayerSettingsStore(context: Context) {

    private val dataStore = context.applicationContext.mapLayerSettingsDataStore

    val settings: Flow<MapLayerSettings> =
        dataStore.data.map { preferences -> preferences.toMapLayerSettings() }

    suspend fun save(settings: MapLayerSettings) {
        dataStore.edit { preferences -> preferences.writeMapLayerSettings(settings) }
    }
}

/**
 * Klucze ustawień warstw. Nazw nie zmieniać – są zapisane na telefonie.
 */
internal object MapLayerSettingsKeys {
    val TRAILS = booleanPreferencesKey("trails")
    val CONTOURS = booleanPreferencesKey("contours")
    val PEAKS = booleanPreferencesKey("peaks")
    val HUTS = booleanPreferencesKey("huts")
    val CAVES = booleanPreferencesKey("caves")
    val WATER = booleanPreferencesKey("water")
    val GPS_TRACKS = booleanPreferencesKey("gps_tracks")
    val WALKED_TRAILS = booleanPreferencesKey("walked_trails")
    val SAVED_PLACES = booleanPreferencesKey("saved_places")
}

/**
 * Brakujący klucz (np. pierwsze uruchomienie) = wartość domyślna z [MapLayerSettings].
 */
internal fun Preferences.toMapLayerSettings(): MapLayerSettings {
    val defaults = MapLayerSettings()
    return MapLayerSettings(
        trails = this[MapLayerSettingsKeys.TRAILS] ?: defaults.trails,
        contours = this[MapLayerSettingsKeys.CONTOURS] ?: defaults.contours,
        peaks = this[MapLayerSettingsKeys.PEAKS] ?: defaults.peaks,
        huts = this[MapLayerSettingsKeys.HUTS] ?: defaults.huts,
        caves = this[MapLayerSettingsKeys.CAVES] ?: defaults.caves,
        water = this[MapLayerSettingsKeys.WATER] ?: defaults.water,
        gpsTracks = this[MapLayerSettingsKeys.GPS_TRACKS] ?: defaults.gpsTracks,
        walkedTrails = this[MapLayerSettingsKeys.WALKED_TRAILS] ?: defaults.walkedTrails,
        savedPlaces = this[MapLayerSettingsKeys.SAVED_PLACES] ?: defaults.savedPlaces
    )
}

internal fun MutablePreferences.writeMapLayerSettings(settings: MapLayerSettings) {
    this[MapLayerSettingsKeys.TRAILS] = settings.trails
    this[MapLayerSettingsKeys.CONTOURS] = settings.contours
    this[MapLayerSettingsKeys.PEAKS] = settings.peaks
    this[MapLayerSettingsKeys.HUTS] = settings.huts
    this[MapLayerSettingsKeys.CAVES] = settings.caves
    this[MapLayerSettingsKeys.WATER] = settings.water
    this[MapLayerSettingsKeys.GPS_TRACKS] = settings.gpsTracks
    this[MapLayerSettingsKeys.WALKED_TRAILS] = settings.walkedTrails
    this[MapLayerSettingsKeys.SAVED_PLACES] = settings.savedPlaces
}
