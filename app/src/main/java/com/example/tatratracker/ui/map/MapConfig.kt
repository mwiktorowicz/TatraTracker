package com.example.tatratracker.ui.map

import org.maplibre.android.geometry.LatLng

/*
 * =========================================================
 * ŹRÓDŁA
 * =========================================================
 */

internal const val OUTDOOR_SOURCE_ID =
    "tt-outdoor-source"

internal const val CONTOURS_SOURCE_ID =
    "tt-contours-source"

internal const val LANDFORM_SOURCE_ID =
    "tt-landform-source"

internal const val PLANET_SOURCE_ID =
    "tt-planet-source"

internal const val TRACK_SOURCE_ID =
    "user-tracks-source"

internal const val WALKED_TRAILS_SOURCE_ID = "tt-walked-trails-source"

internal const val SAVED_PLACES_SOURCE_ID = "tt-saved-places-source"

/*
 * =========================================================
 * WARSTWY
 * =========================================================
 */

internal const val CONTOUR_MINOR_LAYER_ID =
    "tt-contours-minor"

internal const val CONTOUR_MAJOR_LAYER_ID =
    "tt-contours-major"

internal const val TRAIL_CASING_LAYER_ID =
    "tt-trail-casing"

internal const val TRAIL_COLOR_LAYER_ID =
    "tt-trail-color"

internal const val HUT_ICON_LAYER_ID =
    "tt-hut-icons"

internal const val HUT_LABEL_LAYER_ID =
    "tt-hut-labels"

internal const val PEAK_ICON_LAYER_ID =
    "tt-peak-icons"

internal const val PEAK_LABEL_LAYER_ID =
    "tt-peak-labels"

internal const val CAVE_ICON_LAYER_ID =
    "tt-cave-icons"

internal const val CAVE_LABEL_LAYER_ID =
    "tt-cave-labels"

internal const val WATER_ICON_LAYER_ID =
    "tt-water-icons"

internal const val WATER_LABEL_LAYER_ID =
    "tt-water-labels"

internal const val TRACK_LAYER_ID =
    "user-tracks-layer"

internal const val WALKED_TRAILS_LAYER_ID = "tt-walked-trails-glow"

internal const val SAVED_PLACE_BADGE_LAYER_ID = "tt-saved-place-badges"

internal const val SAVED_PLACE_SYMBOL_LAYER_ID = "tt-saved-place-symbols"

/*
 * =========================================================
 * START MAPY
 * =========================================================
 */

internal val TATRA_CENTER =
    LatLng(
        49.205,
        19.98
    )

/*
 * =========================================================
 * MAPTILER
 * =========================================================
 */

internal fun landscapeStyleUrl(
    apiKey: String
): String {

    return "https://api.maptiler.com/maps/" +
            "landscape-v4/style.json" +
            "?key=$apiKey"
}


// Zdjęcia satelitarne z etykietami („Satellite Hybrid” MapTilera). Wersja v4 korzysta
// z tych samych kafli wektorowych (tiles/v4) co landscape-v4 i nasze punkty.
internal fun satelliteStyleUrl(apiKey: String): String {
    return "https://api.maptiler.com/maps/hybrid-v4/style.json?key=$apiKey"
}


internal fun outdoorTilesUrl(
    apiKey: String
): String {

    return "https://api.maptiler.com/tiles/" +
            "outdoor/tiles.json" +
            "?key=$apiKey"
}


internal fun contourTilesUrl(
    apiKey: String
): String {

    return "https://api.maptiler.com/tiles/" +
            "contours-v2/tiles.json" +
            "?key=$apiKey"
}


internal fun landformTilesUrl(
    apiKey: String
): String {

    return "https://api.maptiler.com/tiles/" +
            "landform/tiles.json" +
            "?key=$apiKey"
}


internal fun planetTilesUrl(
    apiKey: String
): String {

    return "https://api.maptiler.com/tiles/" +
            "v4/tiles.json" +
            "?key=$apiKey"
}