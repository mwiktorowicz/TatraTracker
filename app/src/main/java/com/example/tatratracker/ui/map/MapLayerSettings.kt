package com.example.tatratracker.ui.map

import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory

/*
 * =========================================================
 * USTAWIENIA WIDOCZNOŚCI WARSTW
 * =========================================================
 */

data class MapLayerSettings(

    val trails: Boolean = true,

    val contours: Boolean = true,

    val peaks: Boolean = true,

    val huts: Boolean = true,

    val caves: Boolean = true,

    val water: Boolean = true,

    val gpsTracks: Boolean = true,

    // Dopasowanie śladów do szlaków jest kosztowne – domyślnie wyłączone.
    val walkedTrails: Boolean = false,

    val savedPlaces: Boolean = true
)


/*
 * =========================================================
 * ZASTOSOWANIE USTAWIEŃ
 * =========================================================
 */

internal fun applyMapLayerVisibility(
    style: Style,
    settings: MapLayerSettings
) {

    /*
     * SZLAKI
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                TRAIL_CASING_LAYER_ID,
                TRAIL_COLOR_LAYER_ID
            ),
        visible =
            settings.trails
    )

    /*
     * POZIOMICE
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                CONTOUR_MINOR_LAYER_ID,
                CONTOUR_MAJOR_LAYER_ID
            ),
        visible =
            settings.contours
    )

    /*
     * SZCZYTY
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                PEAK_ICON_LAYER_ID,
                PEAK_LABEL_LAYER_ID
            ),
        visible =
            settings.peaks
    )

    /*
     * SCHRONISKA
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                HUT_ICON_LAYER_ID,
                HUT_LABEL_LAYER_ID
            ),
        visible =
            settings.huts
    )

    /*
     * JASKINIE
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                CAVE_ICON_LAYER_ID,
                CAVE_LABEL_LAYER_ID
            ),
        visible =
            settings.caves
    )

    /*
     * JEZIORA / STAWY
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                WATER_ICON_LAYER_ID,
                WATER_LABEL_LAYER_ID
            ),
        visible =
            settings.water
    )

    /*
     * ZAPISANE TRASY GPS
     */
    setLayersVisible(
        style = style,
        layerIds =
            listOf(
                TRACK_LAYER_ID
            ),
        visible =
            settings.gpsTracks
    )

    // PRZEBYTE SZLAKI
    setLayersVisible(
        style = style,
        layerIds = listOf(WALKED_TRAILS_LAYER_ID),
        visible = settings.walkedTrails
    )

    // MOJE PUNKTY
    setLayersVisible(
        style = style,
        layerIds = listOf(SAVED_PLACE_BADGE_LAYER_ID, SAVED_PLACE_SYMBOL_LAYER_ID),
        visible = settings.savedPlaces
    )
}


/*
 * =========================================================
 * POMOCNICZA FUNKCJA
 * =========================================================
 */

private fun setLayersVisible(
    style: Style,
    layerIds: List<String>,
    visible: Boolean
) {

    val visibility =
        if (visible) {
            "visible"
        } else {
            "none"
        }

    layerIds.forEach { layerId ->

        style
            .getLayer(
                layerId
            )
            ?.setProperties(

                PropertyFactory.visibility(
                    visibility
                )
            )
    }
}