package com.example.tatratracker.ui.map

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val ROUTE_POINTS_SOURCE_ID =
    "tt-route-points-source"

private const val ROUTE_START_LAYER_ID =
    "tt-route-start"

private const val ROUTE_WAYPOINT_LAYER_ID =
    "tt-route-waypoints"

private const val ROUTE_WAYPOINT_LABEL_LAYER_ID =
    "tt-route-waypoint-labels"

private const val ROUTE_END_LAYER_ID =
    "tt-route-end"


internal fun addRouteSelectionLayers(
    style: Style
) {

    /*
     * =====================================================
     * SOURCE
     * =====================================================
     */

    if (
        style.getSource(
            ROUTE_POINTS_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                ROUTE_POINTS_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    /*
     * =====================================================
     * START
     * =====================================================
     */

    if (
        style.getLayer(
            ROUTE_START_LAYER_ID
        ) == null
    ) {

        val layer =
            CircleLayer(
                ROUTE_START_LAYER_ID,
                ROUTE_POINTS_SOURCE_ID
            ).apply {

                setFilter(
                    Expression.eq(
                        Expression.get(
                            "pointType"
                        ),
                        "start"
                    )
                )

                setProperties(

                    PropertyFactory.circleColor(
                        Color.rgb(
                            114,
                            168,
                            50
                        )
                    ),

                    PropertyFactory.circleRadius(
                        9f
                    ),

                    PropertyFactory.circleStrokeColor(
                        Color.WHITE
                    ),

                    PropertyFactory.circleStrokeWidth(
                        3f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }

    /*
     * =====================================================
     * PRZYSTANKI
     * =====================================================
     */

    if (
        style.getLayer(
            ROUTE_WAYPOINT_LAYER_ID
        ) == null
    ) {

        val layer =
            CircleLayer(
                ROUTE_WAYPOINT_LAYER_ID,
                ROUTE_POINTS_SOURCE_ID
            ).apply {

                setFilter(
                    Expression.eq(
                        Expression.get(
                            "pointType"
                        ),
                        "waypoint"
                    )
                )

                setProperties(

                    PropertyFactory.circleColor(
                        Color.rgb(
                            224,
                            157,
                            49
                        )
                    ),

                    PropertyFactory.circleRadius(
                        9f
                    ),

                    PropertyFactory.circleStrokeColor(
                        Color.WHITE
                    ),

                    PropertyFactory.circleStrokeWidth(
                        3f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }

    /*
     * Numer przystanku.
     */
    if (
        style.getLayer(
            ROUTE_WAYPOINT_LABEL_LAYER_ID
        ) == null
    ) {

        val layer =
            SymbolLayer(
                ROUTE_WAYPOINT_LABEL_LAYER_ID,
                ROUTE_POINTS_SOURCE_ID
            ).apply {

                setFilter(
                    Expression.eq(
                        Expression.get(
                            "pointType"
                        ),
                        "waypoint"
                    )
                )

                setProperties(

                    PropertyFactory.textField(
                        Expression.get(
                            "number"
                        )
                    ),

                    PropertyFactory.textSize(
                        11f
                    ),

                    PropertyFactory.textColor(
                        Color.WHITE
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
            layer
        )
    }

    /*
     * =====================================================
     * CEL
     * =====================================================
     */

    if (
        style.getLayer(
            ROUTE_END_LAYER_ID
        ) == null
    ) {

        val layer =
            CircleLayer(
                ROUTE_END_LAYER_ID,
                ROUTE_POINTS_SOURCE_ID
            ).apply {

                setFilter(
                    Expression.eq(
                        Expression.get(
                            "pointType"
                        ),
                        "end"
                    )
                )

                setProperties(

                    PropertyFactory.circleColor(
                        Color.rgb(
                            214,
                            81,
                            72
                        )
                    ),

                    PropertyFactory.circleRadius(
                        9f
                    ),

                    PropertyFactory.circleStrokeColor(
                        Color.WHITE
                    ),

                    PropertyFactory.circleStrokeWidth(
                        3f
                    )
                )
            }

        style.addLayer(
            layer
        )
    }
}


internal fun updateRouteSelection(
    style: Style,
    state: RoutePlanningState
) {

    style
        .getSourceAs<GeoJsonSource>(
            ROUTE_POINTS_SOURCE_ID
        )
        ?.setGeoJson(
            routePointsToGeoJson(
                state
            )
        )
}


private fun routePointsToGeoJson(
    state: RoutePlanningState
): String {

    val features =
        JSONArray()

    /*
     * START
     */
    state.start.point
        ?.let { point ->

            features.put(
                pointFeature(
                    point =
                        point,

                    pointType =
                        "start",

                    number =
                        ""
                )
            )
        }

    /*
     * PRZYSTANKI
     */
    state.waypoints
        .forEachIndexed {
                index,
                waypoint ->

            waypoint.point
                ?.let { point ->

                    features.put(
                        pointFeature(
                            point =
                                point,

                            pointType =
                                "waypoint",

                            number =
                                "${index + 1}"
                        )
                    )
                }
        }

    /*
     * CEL
     */
    state.end.point
        ?.let { point ->

            features.put(
                pointFeature(
                    point =
                        point,

                    pointType =
                        "end",

                    number =
                        ""
                )
            )
        }

    return JSONObject()
        .put(
            "type",
            "FeatureCollection"
        )
        .put(
            "features",
            features
        )
        .toString()
}


private fun pointFeature(
    point: RoutePlanPoint,
    pointType: String,
    number: String
): JSONObject {

    val coordinates =
        JSONArray()
            .put(
                point.position.longitude
            )
            .put(
                point.position.latitude
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
                "pointType",
                pointType
            )
            .put(
                "number",
                number
            )

    return JSONObject()
        .put(
            "type",
            "Feature"
        )
        .put(
            "geometry",
            geometry
        )
        .put(
            "properties",
            properties
        )
}