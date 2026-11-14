package com.example.tatratracker.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.tatratracker.ui.map.MapLayerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLayerSettingsStoreTest {

    private fun roundTrip(settings: MapLayerSettings): MapLayerSettings {
        val preferences = mutablePreferencesOf()
        preferences.writeMapLayerSettings(settings)
        return preferences.toMapLayerSettings()
    }

    @Test
    fun emptyPreferences_giveDefaults() {
        assertEquals(MapLayerSettings(), emptyPreferences().toMapLayerSettings())
    }

    @Test
    fun allLayersOff_areReadBack() {
        val settings = MapLayerSettings(
            trails = false,
            contours = false,
            peaks = false,
            huts = false,
            caves = false,
            water = false,
            gpsTracks = false,
            walkedTrails = false,
            savedPlaces = false
        )

        assertEquals(settings, roundTrip(settings))
    }

    @Test
    fun defaults_areReadBack() {
        assertEquals(MapLayerSettings(), roundTrip(MapLayerSettings()))
    }

    @Test
    fun eachLayerOffAlone_isReadBackWithoutTouchingOthers() {
        val variants = listOf(
            MapLayerSettings(trails = false),
            MapLayerSettings(contours = false),
            MapLayerSettings(peaks = false),
            MapLayerSettings(huts = false),
            MapLayerSettings(caves = false),
            MapLayerSettings(water = false),
            MapLayerSettings(gpsTracks = false),
            MapLayerSettings(walkedTrails = true),
            MapLayerSettings(savedPlaces = false)
        )

        variants.forEach { settings ->
            assertEquals(settings, roundTrip(settings))
        }
    }

    @Test
    fun missingKeys_fallBackToDefaults() {
        val preferences = mutablePreferencesOf(
            MapLayerSettingsKeys.CONTOURS to false,
            MapLayerSettingsKeys.GPS_TRACKS to false
        )

        assertEquals(
            MapLayerSettings(contours = false, gpsTracks = false),
            preferences.toMapLayerSettings()
        )
    }

    @Test
    fun newLayerDefaults_walkedTrailsOff_savedPlacesOn() {
        val defaults = MapLayerSettings()

        assertEquals(false, defaults.walkedTrails)
        assertEquals(true, defaults.savedPlaces)
    }

    @Test
    fun oldSettingsWithoutNewKeys_loadWithNewDefaults() {
        // Ustawienia zapisane przed dodaniem warstw „Przebyte szlaki” i „Moje punkty”.
        val preferences = mutablePreferencesOf(
            MapLayerSettingsKeys.TRAILS to true,
            MapLayerSettingsKeys.CONTOURS to false,
            MapLayerSettingsKeys.PEAKS to true,
            MapLayerSettingsKeys.HUTS to false,
            MapLayerSettingsKeys.CAVES to true,
            MapLayerSettingsKeys.WATER to true,
            MapLayerSettingsKeys.GPS_TRACKS to false
        )

        assertEquals(
            MapLayerSettings(
                contours = false,
                huts = false,
                gpsTracks = false,
                walkedTrails = false,
                savedPlaces = true
            ),
            preferences.toMapLayerSettings()
        )
    }

    @Test
    fun keyNames_areUnique() {
        val names = listOf(
            MapLayerSettingsKeys.TRAILS,
            MapLayerSettingsKeys.CONTOURS,
            MapLayerSettingsKeys.PEAKS,
            MapLayerSettingsKeys.HUTS,
            MapLayerSettingsKeys.CAVES,
            MapLayerSettingsKeys.WATER,
            MapLayerSettingsKeys.GPS_TRACKS,
            MapLayerSettingsKeys.WALKED_TRAILS,
            MapLayerSettingsKeys.SAVED_PLACES
        ).map { it.name }

        assertEquals(names.size, names.toSet().size)
    }
}
