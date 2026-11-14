package com.example.tatratracker.ui.map

import android.content.Context
import android.graphics.Color
import com.example.tatratracker.model.TrackPoint
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource

/*
 * =========================================================
 * GŁÓWNE WARSTWY
 * =========================================================
 */

internal fun addTatraMapLayers(
    context: Context,
    style: Style,
    apiKey: String,
    tracks: List<List<TrackPoint>>
) {

    /*
     * Najpierw rejestrujemy nasze ikonki.
     */
    registerMapIcons(
        context = context,
        style = style
    )

    // Przebyte szlaki: najniżej z naszych warstw, tuż nad podkładem mapy.
    addWalkedTrailsLayer(style)

    /*
     * Planet:
     * - schroniska
     * - jaskinie
     * - jeziora
     */
    addPlanetSource(
        style = style,
        apiKey = apiKey
    )

    addWaterLayers(
        style = style
    )

    addHutLayers(
        style = style
    )

    addCaveLayers(
        style = style
    )

    /*
     * Poziomice.
     */
    addContourLayers(
        style = style,
        apiKey = apiKey
    )

    /*
     * Kolorowe szlaki.
     */
    addOutdoorLayers(
        style = style,
        apiKey = apiKey
    )

    /*
     * Szczyty.
     */
    addPeakLayers(
        style = style,
        apiKey = apiKey
    )

    /*
     * GPS użytkownika nad szlakami
     * i punktami mapy.
     */
    addUserTrackLayer(
        style = style,
        tracks = tracks
    )

    // Moje punkty nad śladem GPS: zapisane schroniska i szczyty leżą zwykle na trasie,
    // więc linia śladu przechodziłaby przez ikonę. Planowana trasa i jej znaczniki
    // są dodawane po tej funkcji, więc zostają nad nimi.
    addSavedPlacesLayers(style)
}


internal fun updateUserTracks(
    style: Style,
    tracks: List<List<TrackPoint>>
) {

    style
        .getSourceAs<GeoJsonSource>(
            TRACK_SOURCE_ID
        )
        ?.setGeoJson(
            tracksToGeoJson(
                tracks
            )
        )
}


/*
 * =========================================================
 * PLANET V4
 * =========================================================
 */

private fun addPlanetSource(
    style: Style,
    apiKey: String
) {

    if (
        style.getSource(
            PLANET_SOURCE_ID
        ) != null
    ) {
        return
    }

    style.addSource(
        VectorSource(
            PLANET_SOURCE_ID,
            planetTilesUrl(
                apiKey
            )
        )
    )
}


/*
 * =========================================================
 * JEZIORA / STAWY
 * =========================================================
 */

private fun addWaterLayers(
    style: Style
) {

    val waterFilter =
        Expression.any(

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "lake"
            ),

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "pond"
            ),

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "reservoir"
            )
        )

    /*
     * IKONA WODY
     */
    if (
        style.getLayer(
            WATER_ICON_LAYER_ID
        ) == null
    ) {

        val icons =
            SymbolLayer(
                WATER_ICON_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "water_centroid"
                )

                setMinZoom(
                    10f
                )

                setFilter(
                    waterFilter
                )

                setProperties(

                    PropertyFactory.iconImage(
                        WATER_ICON_ID
                    ),

                    PropertyFactory.iconSize(
                        0.72f
                    ),

                    PropertyFactory.iconAllowOverlap(
                        true
                    ),

                    PropertyFactory.iconIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            icons
        )
    }

    /*
     * NAZWA
     */
    if (
        style.getLayer(
            WATER_LABEL_LAYER_ID
        ) == null
    ) {

        val labels =
            SymbolLayer(
                WATER_LABEL_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "water_centroid"
                )

                setMinZoom(
                    10f
                )

                setFilter(
                    waterFilter
                )

                setProperties(

                    PropertyFactory.textField(
                        Expression.get(
                            "name"
                        )
                    ),

                    PropertyFactory.textSize(
                        11.5f
                    ),

                    PropertyFactory.textColor(
                        Color.rgb(
                            45,
                            105,
                            135
                        )
                    ),

                    PropertyFactory.textHaloColor(
                        Color.WHITE
                    ),

                    PropertyFactory.textHaloWidth(
                        1.7f
                    ),

                    PropertyFactory.textOffset(
                        arrayOf(
                            0f,
                            1.45f
                        )
                    ),

                    PropertyFactory.textAllowOverlap(
                        true
                    ),

                    PropertyFactory.textIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            labels
        )
    }
}


/*
 * =========================================================
 * SCHRONISKA
 * =========================================================
 */

private fun addHutLayers(
    style: Style
) {

    val filter =
        Expression.any(

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "alpine_hut"
            ),

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "wilderness_hut"
            )
        )

    /*
     * IKONA DOMKU
     */
    if (
        style.getLayer(
            HUT_ICON_LAYER_ID
        ) == null
    ) {

        val icons =
            SymbolLayer(
                HUT_ICON_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "poi_tourism"
                )

                setMinZoom(
                    12f
                )

                setFilter(
                    filter
                )

                setProperties(

                    PropertyFactory.iconImage(
                        HUT_ICON_ID
                    ),

                    PropertyFactory.iconSize(
                        0.82f
                    ),

                    PropertyFactory.iconAllowOverlap(
                        true
                    ),

                    PropertyFactory.iconIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            icons
        )
    }

    /*
     * NAZWA SCHRONISKA
     */
    if (
        style.getLayer(
            HUT_LABEL_LAYER_ID
        ) == null
    ) {

        val labels =
            SymbolLayer(
                HUT_LABEL_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "poi_tourism"
                )

                setMinZoom(
                    12f
                )

                setFilter(
                    filter
                )

                setProperties(

                    PropertyFactory.textField(
                        Expression.get(
                            "name"
                        )
                    ),

                    PropertyFactory.textSize(
                        13f
                    ),

                    PropertyFactory.textColor(
                        Color.rgb(
                            31,
                            62,
                            34
                        )
                    ),

                    PropertyFactory.textHaloColor(
                        Color.WHITE
                    ),

                    PropertyFactory.textHaloWidth(
                        2.2f
                    ),

                    PropertyFactory.textOffset(
                        arrayOf(
                            0f,
                            1.65f
                        )
                    ),

                    PropertyFactory.textAllowOverlap(
                        true
                    ),

                    PropertyFactory.textIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            labels
        )
    }
}


/*
 * =========================================================
 * JASKINIE
 * =========================================================
 *
 * Planet v4:
 *
 * source layer:
 * poi_tourism
 *
 * class:
 * cave_entrance
 * =========================================================
 */

private fun addCaveLayers(
    style: Style
) {

    val filter =
        Expression.eq(
            Expression.get(
                "class"
            ),
            "cave_entrance"
        )

    /*
     * IKONA WEJŚCIA DO JASKINI
     */
    if (
        style.getLayer(
            CAVE_ICON_LAYER_ID
        ) == null
    ) {

        val icons =
            SymbolLayer(
                CAVE_ICON_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "poi_tourism"
                )

                setMinZoom(
                    12f
                )

                setFilter(
                    filter
                )

                setProperties(

                    PropertyFactory.iconImage(
                        CAVE_ICON_ID
                    ),

                    PropertyFactory.iconSize(
                        0.82f
                    ),

                    PropertyFactory.iconAllowOverlap(
                        true
                    ),

                    PropertyFactory.iconIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            icons
        )
    }

    /*
     * NAZWA JASKINI
     */
    if (
        style.getLayer(
            CAVE_LABEL_LAYER_ID
        ) == null
    ) {

        val labels =
            SymbolLayer(
                CAVE_LABEL_LAYER_ID,
                PLANET_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "poi_tourism"
                )

                setMinZoom(
                    12f
                )

                setFilter(
                    filter
                )

                setProperties(

                    PropertyFactory.textField(
                        Expression.get(
                            "name"
                        )
                    ),

                    PropertyFactory.textSize(
                        11.5f
                    ),

                    PropertyFactory.textColor(
                        Color.rgb(
                            92,
                            66,
                            51
                        )
                    ),

                    PropertyFactory.textHaloColor(
                        Color.WHITE
                    ),

                    PropertyFactory.textHaloWidth(
                        1.8f
                    ),

                    PropertyFactory.textOffset(
                        arrayOf(
                            0f,
                            1.55f
                        )
                    ),

                    PropertyFactory.textAllowOverlap(
                        true
                    ),

                    PropertyFactory.textIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            labels
        )
    }
}


/*
 * =========================================================
 * IZOLINIE
 * =========================================================
 */

private fun addContourLayers(
    style: Style,
    apiKey: String
) {

    if (
        style.getSource(
            CONTOURS_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            VectorSource(
                CONTOURS_SOURCE_ID,
                contourTilesUrl(
                    apiKey
                )
            )
        )
    }

    /*
     * CIENKIE POZIOMICE
     */
    if (
        style.getLayer(
            CONTOUR_MINOR_LAYER_ID
        ) == null
    ) {

        val layer =
            LineLayer(
                CONTOUR_MINOR_LAYER_ID,
                CONTOURS_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "contour"
                )

                setMinZoom(
                    11f
                )

                setFilter(
                    Expression.any(

                        Expression.eq(
                            Expression.get(
                                "nth_line"
                            ),
                            0
                        ),

                        Expression.eq(
                            Expression.get(
                                "nth_line"
                            ),
                            1
                        ),

                        Expression.eq(
                            Expression.get(
                                "nth_line"
                            ),
                            2
                        )
                    )
                )

                setProperties(

                    PropertyFactory.lineColor(
                        Color.rgb(
                            125,
                            107,
                            82
                        )
                    ),

                    PropertyFactory.lineWidth(
                        0.55f
                    ),

                    PropertyFactory.lineOpacity(
                        0.24f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }

    /*
     * GŁÓWNE POZIOMICE
     */
    if (
        style.getLayer(
            CONTOUR_MAJOR_LAYER_ID
        ) == null
    ) {

        val layer =
            LineLayer(
                CONTOUR_MAJOR_LAYER_ID,
                CONTOURS_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "contour"
                )

                setMinZoom(
                    9f
                )

                setFilter(
                    Expression.any(

                        Expression.eq(
                            Expression.get(
                                "nth_line"
                            ),
                            5
                        ),

                        Expression.eq(
                            Expression.get(
                                "nth_line"
                            ),
                            10
                        )
                    )
                )

                setProperties(

                    PropertyFactory.lineColor(
                        Color.rgb(
                            105,
                            88,
                            67
                        )
                    ),

                    PropertyFactory.lineWidth(
                        0.9f
                    ),

                    PropertyFactory.lineOpacity(
                        0.42f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }
}


/*
 * =========================================================
 * SZLAKI
 * =========================================================
 */

private fun addOutdoorLayers(
    style: Style,
    apiKey: String
) {

    if (
        style.getSource(
            OUTDOOR_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            VectorSource(
                OUTDOOR_SOURCE_ID,
                outdoorTilesUrl(
                    apiKey
                )
            )
        )
    }

    val hikingFilter =
        Expression.any(

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "hiking"
            ),

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "foot"
            ),

            Expression.eq(
                Expression.get(
                    "class"
                ),
                "via_ferrata"
            )
        )

    /*
     * BIAŁA PRZERYWANA OBWÓDKA
     */
    if (
        style.getLayer(
            TRAIL_CASING_LAYER_ID
        ) == null
    ) {

        val layer =
            LineLayer(
                TRAIL_CASING_LAYER_ID,
                OUTDOOR_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "trail"
                )

                setMinZoom(
                    8.5f
                )

                setFilter(
                    hikingFilter
                )

                setProperties(

                    PropertyFactory.lineColor(
                        Color.WHITE
                    ),

                    PropertyFactory.lineWidth(
                        3.3f
                    ),

                    PropertyFactory.lineDasharray(
                        arrayOf(
                            1.5f,
                            1.15f
                        )
                    ),

                    PropertyFactory.lineOpacity(
                        0.92f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }

    /*
     * KOLOR SZLAKU
     */
    if (
        style.getLayer(
            TRAIL_COLOR_LAYER_ID
        ) == null
    ) {

        val trailColor =
            Expression.match(

                Expression.get(
                    "color"
                ),

                mapColor(
                    82,
                    86,
                    87
                ),

                Expression.stop(
                    "red",
                    mapColor(
                        215,
                        60,
                        52
                    )
                ),

                Expression.stop(
                    "blue",
                    mapColor(
                        34,
                        103,
                        210
                    )
                ),

                Expression.stop(
                    "green",
                    mapColor(
                        40,
                        151,
                        72
                    )
                ),

                Expression.stop(
                    "yellow",
                    mapColor(
                        231,
                        185,
                        30
                    )
                ),

                Expression.stop(
                    "black",
                    mapColor(
                        36,
                        40,
                        42
                    )
                ),

                Expression.stop(
                    "brown",
                    mapColor(
                        133,
                        86,
                        58
                    )
                ),

                Expression.stop(
                    "orange",
                    mapColor(
                        232,
                        125,
                        35
                    )
                ),

                Expression.stop(
                    "purple",
                    mapColor(
                        129,
                        67,
                        173
                    )
                )
            )

        val layer =
            LineLayer(
                TRAIL_COLOR_LAYER_ID,
                OUTDOOR_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "trail"
                )

                setMinZoom(
                    8.5f
                )

                setFilter(
                    hikingFilter
                )

                setProperties(

                    PropertyFactory.lineColor(
                        trailColor
                    ),

                    PropertyFactory.lineWidth(
                        1.9f
                    ),

                    PropertyFactory.lineDasharray(
                        arrayOf(
                            1.5f,
                            1.15f
                        )
                    ),

                    PropertyFactory.lineOpacity(
                        0.98f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }
}


/*
 * =========================================================
 * SZCZYTY
 * =========================================================
 */

private fun addPeakLayers(
    style: Style,
    apiKey: String
) {

    if (
        style.getSource(
            LANDFORM_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            VectorSource(
                LANDFORM_SOURCE_ID,
                landformTilesUrl(
                    apiKey
                )
            )
        )
    }

    /*
     * IKONA GÓRY
     */
    if (
        style.getLayer(
            PEAK_ICON_LAYER_ID
        ) == null
    ) {

        val icons =
            SymbolLayer(
                PEAK_ICON_LAYER_ID,
                LANDFORM_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "peak"
                )

                setMinZoom(
                    10f
                )

                setProperties(

                    PropertyFactory.iconImage(
                        PEAK_ICON_ID
                    ),

                    PropertyFactory.iconSize(
                        0.72f
                    ),

                    PropertyFactory.iconAllowOverlap(
                        true
                    ),

                    PropertyFactory.iconIgnorePlacement(
                        true
                    )
                )
            }

        style.addLayer(
            icons
        )
    }

    /*
     * NAZWA + WYSOKOŚĆ
     */
    if (
        style.getLayer(
            PEAK_LABEL_LAYER_ID
        ) == null
    ) {

        val peakText =
            Expression.concat(

                Expression.get(
                    "name"
                ),

                Expression.literal(
                    "\n"
                ),

                Expression.toString(
                    Expression.get(
                        "ele"
                    )
                ),

                Expression.literal(
                    " m"
                )
            )

        val labels =
            SymbolLayer(
                PEAK_LABEL_LAYER_ID,
                LANDFORM_SOURCE_ID
            ).apply {

                setSourceLayer(
                    "peak"
                )

                setMinZoom(
                    11f
                )

                setFilter(
                    Expression.has(
                        "name"
                    )
                )

                setProperties(

                    PropertyFactory.textField(
                        peakText
                    ),

                    PropertyFactory.textSize(
                        10.5f
                    ),

                    PropertyFactory.textColor(
                        Color.rgb(
                            42,
                            45,
                            46
                        )
                    ),

                    PropertyFactory.textHaloColor(
                        Color.WHITE
                    ),

                    PropertyFactory.textHaloWidth(
                        1.7f
                    ),

                    PropertyFactory.textOffset(
                        arrayOf(
                            0f,
                            1.55f
                        )
                    )
                )
            }

        style.addLayer(
            labels
        )
    }
}


/*
 * =========================================================
 * GPS UŻYTKOWNIKA
 * =========================================================
 */

private fun addUserTrackLayer(
    style: Style,
    tracks: List<List<TrackPoint>>
) {

    if (
        style.getSource(
            TRACK_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                TRACK_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    if (
        style.getLayer(
            TRACK_LAYER_ID
        ) == null
    ) {

        val layer =
            LineLayer(
                TRACK_LAYER_ID,
                TRACK_SOURCE_ID
            ).withProperties(

                PropertyFactory.lineColor(
                    Color.rgb(
                        31,
                        92,
                        220
                    )
                ),

                PropertyFactory.lineWidth(
                    4.7f
                ),

                PropertyFactory.lineOpacity(
                    0.95f
                )
            )

        style.addLayer(
            layer
        )
    }

    updateUserTracks(
        style = style,
        tracks = tracks
    )
}


private fun mapColor(
    red: Int,
    green: Int,
    blue: Int
): Expression {

    return Expression.rgba(

        Expression.literal(
            red
        ),

        Expression.literal(
            green
        ),

        Expression.literal(
            blue
        ),

        Expression.literal(
            1.0
        )
    )
}