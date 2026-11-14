package com.example.tatratracker.ui.map

import android.graphics.Color
import com.example.tatratracker.ui.format.TatraFormat
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val ELEVATION_MARKER_SOURCE_ID =
    "tt-elevation-marker-source"

private const val ELEVATION_MARKER_LAYER_ID =
    "tt-elevation-marker-layer"

private const val ELEVATION_MARKER_LABEL_LAYER_ID =
    "tt-elevation-marker-label-layer"


internal fun addElevationMarkerLayers(
    style: Style
) {

    /*
     * =====================================================
     * SOURCE
     * =====================================================
     */

    if (
        style.getSource(
            ELEVATION_MARKER_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                ELEVATION_MARKER_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    /*
     * =====================================================
     * PUNKT
     * =====================================================
     */

    if (
        style.getLayer(
            ELEVATION_MARKER_LAYER_ID
        ) == null
    ) {

        val marker =
            CircleLayer(
                ELEVATION_MARKER_LAYER_ID,
                ELEVATION_MARKER_SOURCE_ID
            ).withProperties(

                PropertyFactory.circleColor(
                    Color.rgb(
                        114,
                        168,
                        50
                    )
                ),

                PropertyFactory.circleRadius(
                    7f
                ),

                PropertyFactory.circleStrokeColor(
                    Color.WHITE
                ),

                PropertyFactory.circleStrokeWidth(
                    3f
                ),

                PropertyFactory.circleOpacity(
                    1f
                )
            )

        style.addLayer(
            marker
        )
    }

    /*
     * =====================================================
     * ETYKIETA
     * =====================================================
     */

    if (
        style.getLayer(
            ELEVATION_MARKER_LABEL_LAYER_ID
        ) == null
    ) {

        val label =
            SymbolLayer(
                ELEVATION_MARKER_LABEL_LAYER_ID,
                ELEVATION_MARKER_SOURCE_ID
            ).withProperties(

                PropertyFactory.textField(
                    Expression.get(
                        "label"
                    )
                ),

                PropertyFactory.textSize(
                    11f
                ),

                PropertyFactory.textColor(
                    Color.rgb(
                        24,
                        33,
                        35
                    )
                ),

                PropertyFactory.textHaloColor(
                    Color.WHITE
                ),

                PropertyFactory.textHaloWidth(
                    2f
                ),

                PropertyFactory.textOffset(
                    arrayOf(
                        0f,
                        -1.7f
                    )
                ),

                PropertyFactory.textAllowOverlap(
                    true
                ),

                PropertyFactory.textIgnorePlacement(
                    true
                )
            )

        style.addLayer(
            label
        )
    }
}


internal fun updateElevationMarker(
    style: Style,
    sample: RouteElevationSample?
) {

    val source =
        style.getSourceAs<GeoJsonSource>(
            ELEVATION_MARKER_SOURCE_ID
        )
            ?: return

    if (
        sample == null
    ) {

        source.setGeoJson(
            emptyFeatureCollection()
        )

        return
    }

    val label =
        TatraFormat.distance(sample.distanceMeters) +
            " • " +
            TatraFormat.elevation(sample.elevationMeters)

    val coordinates =
        JSONArray()
            .put(
                sample.position.longitude
            )
            .put(
                sample.position.latitude
            )

    val geometry =
        JSONObject()
            .put(
                "type",
                "Point"
            )
            .put(
                "coordinates",
                coordinates
            )

    val properties =
        JSONObject()
            .put(
                "label",
                label
            )

    val feature =
        JSONObject()
            .put(
                "type",
                "Feature"
            )
            .put(
                "properties",
                properties
            )
            .put(
                "geometry",
                geometry
            )

    val featureCollection =
        JSONObject()
            .put(
                "type",
                "FeatureCollection"
            )
            .put(
                "features",
                JSONArray()
                    .put(
                        feature
                    )
            )

    source.setGeoJson(
        featureCollection.toString()
    )
}