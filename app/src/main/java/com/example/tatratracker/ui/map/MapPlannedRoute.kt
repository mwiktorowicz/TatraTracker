package com.example.tatratracker.ui.map

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val PLANNED_ROUTE_SOURCE_ID =
    "tt-planned-route-source"

private const val PLANNED_ROUTE_CASING_LAYER_ID =
    "tt-planned-route-casing"

private const val PLANNED_ROUTE_LAYER_ID =
    "tt-planned-route"


internal fun addPlannedRouteLayers(
    style: Style
) {

    /*
     * =====================================================
     * SOURCE
     * =====================================================
     */

    if (
        style.getSource(
            PLANNED_ROUTE_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                PLANNED_ROUTE_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    /*
     * =====================================================
     * BIAŁA OBWÓDKA
     * =====================================================
     */

    if (
        style.getLayer(
            PLANNED_ROUTE_CASING_LAYER_ID
        ) == null
    ) {

        val casing =
            LineLayer(
                PLANNED_ROUTE_CASING_LAYER_ID,
                PLANNED_ROUTE_SOURCE_ID
            ).withProperties(

                PropertyFactory.lineColor(
                    Color.WHITE
                ),

                PropertyFactory.lineWidth(
                    7.5f
                ),

                PropertyFactory.lineOpacity(
                    0.92f
                )
            )

        style.addLayer(
            casing
        )
    }

    /*
     * =====================================================
     * PLANOWANA TRASA
     * =====================================================
     *
     * Limonkowa / zielona.
     */

    if (
        style.getLayer(
            PLANNED_ROUTE_LAYER_ID
        ) == null
    ) {

        val route =
            LineLayer(
                PLANNED_ROUTE_LAYER_ID,
                PLANNED_ROUTE_SOURCE_ID
            ).withProperties(

                PropertyFactory.lineColor(
                    Color.rgb(
                        114,
                        168,
                        50
                    )
                ),

                PropertyFactory.lineWidth(
                    5f
                ),

                PropertyFactory.lineOpacity(
                    1f
                )
            )

        style.addLayer(
            route
        )
    }
}


internal fun updatePlannedRoute(
    style: Style,
    route: PlannedRoute?
) {

    val source =
        style.getSourceAs<GeoJsonSource>(
            PLANNED_ROUTE_SOURCE_ID
        )
            ?: return

    if (
        route == null ||
        route.points.size < 2
    ) {

        source.setGeoJson(
            emptyFeatureCollection()
        )

        return
    }

    source.setGeoJson(
        plannedRouteToGeoJson(
            route
        )
    )
}


private fun plannedRouteToGeoJson(
    route: PlannedRoute
): String {

    val coordinates =
        JSONArray()

    route.points.forEach {
            point ->

        coordinates.put(
            JSONArray()
                .put(
                    point.longitude
                )
                .put(
                    point.latitude
                )
        )
    }

    val geometry =
        JSONObject()
            .put(
                "type",
                "LineString"
            )
            .put(
                "coordinates",
                coordinates
            )

    val feature =
        JSONObject()
            .put(
                "type",
                "Feature"
            )
            .put(
                "properties",
                JSONObject()
            )
            .put(
                "geometry",
                geometry
            )

    return JSONObject()
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
        .toString()
}