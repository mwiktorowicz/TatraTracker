package com.example.tatratracker.ui.map

import android.graphics.Color
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import kotlin.math.abs

/**
 * Podkład mapy głównej. Nazw nie zmieniać – są zapisane w ustawieniach warstw.
 */
enum class MapBasemap {
    TERRAIN,
    SATELLITE
}

internal fun basemapStyleUrl(basemap: MapBasemap, apiKey: String): String {
    return when (basemap) {
        MapBasemap.TERRAIN -> landscapeStyleUrl(apiKey)
        MapBasemap.SATELLITE -> satelliteStyleUrl(apiKey)
    }
}

/**
 * Oba style MapTilera mają własną kamerę startową: środek [0, 0], zoom 1. MapLibre nie stosuje
 * jej, gdy kamerę ustawiono wcześniej (tak jest przy pierwszym wczytaniu mapy). Gdyby po zmianie
 * podkładu mapa jednak wróciła do tego widoku, przywracamy kamerę sprzed zmiany.
 */
internal fun MapLibreMap.restoreCameraIfReset(before: CameraPosition) {
    val now = cameraPosition
    val target = now.target ?: return
    val resetToStyleDefault =
        now.zoom <= 1.0 && abs(target.latitude) < 0.001 && abs(target.longitude) < 0.001

    if (resetToStyleDefault) {
        cameraPosition = before
    }
}

// Jasny beż: brązowe poziomice z „Teren” giną na ciemnych zdjęciach.
private val SATELLITE_CONTOUR_COLOR = Color.rgb(240, 228, 205)

// Jaśniejszy niebieski niż na „Teren” – ciemny niebieski zlewa się z lasem.
private val SATELLITE_TRACK_COLOR = Color.rgb(77, 148, 255)

private const val SATELLITE_LABEL_HALO_WIDTH = 2.4f

/**
 * Korekty czytelności naszych warstw na zdjęciach satelitarnych.
 *
 * Wołane tylko po wczytaniu podkładu „Satelita”. Przy każdej zmianie stylu warstwy
 * powstają od nowa z wyglądem dla „Teren”, więc ten podkład pozostaje bez zmian.
 */
internal fun applySatelliteReadability(style: Style) {
    style.getLayer(CONTOUR_MINOR_LAYER_ID)?.setProperties(
        PropertyFactory.lineColor(SATELLITE_CONTOUR_COLOR),
        PropertyFactory.lineOpacity(0.4f)
    )
    style.getLayer(CONTOUR_MAJOR_LAYER_ID)?.setProperties(
        PropertyFactory.lineColor(SATELLITE_CONTOUR_COLOR),
        PropertyFactory.lineOpacity(0.6f)
    )

    // Szlaki: pełna, nieco szersza biała obwódka pod kolorem szlaku.
    style.getLayer(TRAIL_CASING_LAYER_ID)?.setProperties(
        PropertyFactory.lineWidth(3.8f),
        PropertyFactory.lineOpacity(1f)
    )

    style.getLayer(TRACK_LAYER_ID)?.setProperties(
        PropertyFactory.lineColor(SATELLITE_TRACK_COLOR)
    )

    // Zielona poświata przebytych szlaków słabo odcina się od lasu.
    style.getLayer(WALKED_TRAILS_LAYER_ID)?.setProperties(
        PropertyFactory.lineOpacity(0.7f)
    )

    // Etykiety punktów mają ciemny tekst z białą obwódką – na zdjęciach obwódka szersza.
    listOf(
        PEAK_LABEL_LAYER_ID,
        HUT_LABEL_LAYER_ID,
        CAVE_LABEL_LAYER_ID,
        WATER_LABEL_LAYER_ID
    ).forEach { layerId ->
        style.getLayer(layerId)?.setProperties(
            PropertyFactory.textHaloWidth(SATELLITE_LABEL_HALO_WIDTH)
        )
    }
}
