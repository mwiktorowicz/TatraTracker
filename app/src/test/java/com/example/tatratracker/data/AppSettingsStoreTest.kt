package com.example.tatratracker.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsStoreTest {

    private fun roundTrip(settings: AppSettings): AppSettings {
        val preferences = mutablePreferencesOf()
        preferences.writeAppSettings(settings)
        return preferences.toAppSettings()
    }

    @Test
    fun defaults_matchBehaviourBeforeSettings() {
        val defaults = AppSettings()

        assertEquals(true, defaults.autoFitRoute)
        assertEquals(RoutingProfile.SHORTEST, defaults.routingProfile)
        assertEquals(ElevationSampling.METERS_100, defaults.elevationSampling)
    }

    @Test
    fun emptyPreferences_giveDefaults() {
        assertEquals(AppSettings(), emptyPreferences().toAppSettings())
    }

    @Test
    fun everyCombination_isReadBack() {
        for (autoFit in listOf(true, false)) {
            for (profile in RoutingProfile.entries) {
                for (sampling in ElevationSampling.entries) {
                    val settings = AppSettings(
                        autoFitRoute = autoFit,
                        routingProfile = profile,
                        elevationSampling = sampling
                    )

                    assertEquals(settings, roundTrip(settings))
                }
            }
        }
    }

    @Test
    fun storedValues_useStableKeysAndFormats() {
        val preferences = mutablePreferencesOf()
        preferences.writeAppSettings(
            AppSettings(
                autoFitRoute = false,
                routingProfile = RoutingProfile.STANDARD,
                elevationSampling = ElevationSampling.METERS_200
            )
        )

        assertEquals(false, preferences[booleanPreferencesKey("auto_fit_route")])
        assertEquals("STANDARD", preferences[stringPreferencesKey("routing_profile")])
        assertEquals(200, preferences[intPreferencesKey("elevation_sampling_meters")])
    }

    @Test
    fun unknownRoutingProfile_givesDefault() {
        val preferences = mutablePreferencesOf()
        preferences[stringPreferencesKey("routing_profile")] = "PREFER_TRAILS"

        assertEquals(RoutingProfile.SHORTEST, preferences.toAppSettings().routingProfile)
    }

    @Test
    fun samplingOutsideAllowedValues_givesDefault() {
        for (meters in listOf(0, -50, 75, 1000)) {
            val preferences = mutablePreferencesOf()
            preferences[intPreferencesKey("elevation_sampling_meters")] = meters

            assertEquals(
                ElevationSampling.METERS_100,
                preferences.toAppSettings().elevationSampling
            )
        }
    }

    @Test
    fun valuesOfWrongType_giveDefaults() {
        val preferences = mutablePreferencesOf()
        preferences[stringPreferencesKey("auto_fit_route")] = "false"
        preferences[intPreferencesKey("routing_profile")] = 1
        preferences[stringPreferencesKey("elevation_sampling_meters")] = "50"

        assertEquals(AppSettings(), preferences.toAppSettings())
    }

    @Test
    fun oneBrokenValue_keepsOthers() {
        val preferences = mutablePreferencesOf()
        preferences[booleanPreferencesKey("auto_fit_route")] = false
        preferences[stringPreferencesKey("routing_profile")] = ""
        preferences[intPreferencesKey("elevation_sampling_meters")] = 50

        assertEquals(
            AppSettings(
                autoFitRoute = false,
                routingProfile = RoutingProfile.SHORTEST,
                elevationSampling = ElevationSampling.METERS_50
            ),
            preferences.toAppSettings()
        )
    }
}
