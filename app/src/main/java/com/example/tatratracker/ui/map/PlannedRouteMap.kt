package com.example.tatratracker.ui.map

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tatratracker.BuildConfig
import com.example.tatratracker.data.PlannedRoutePointEntity
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val SAVED_ROUTE_SOURCE_ID =
    "tt-saved-route-source"

private const val SAVED_ROUTE_CASING_LAYER_ID =
    "tt-saved-route-casing"

private const val SAVED_ROUTE_LAYER_ID =
    "tt-saved-route-layer"

private const val SAVED_ROUTE_START_SOURCE_ID =
    "tt-saved-route-start-source"

private const val SAVED_ROUTE_START_LAYER_ID =
    "tt-saved-route-start-layer"

private const val SAVED_ROUTE_END_SOURCE_ID =
    "tt-saved-route-end-source"

private const val SAVED_ROUTE_END_LAYER_ID =
    "tt-saved-route-end-layer"

@Composable
fun PlannedRouteMap(
    geometryPoints:
    List<PlannedRoutePointEntity>,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 280.dp
) {

    val context =
        LocalContext.current

    val apiKey =
        BuildConfig.MAPTILER_API_KEY

    if (
        apiKey.isBlank()
    ) {

        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(
                        mapHeight
                    )
                    .background(
                        ComposeColor(
                            0xFFE9ECE7
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text =
                    "Brak klucza MapTiler."
            )
        }

        return
    }

    var mapLibreMap by
    remember {
        mutableStateOf<MapLibreMap?>(
            null
        )
    }

    var loadedStyle by
    remember {
        mutableStateOf<Style?>(
            null
        )
    }

    val mapView =
        remember(
            context
        ) {

            MapLibre.getInstance(
                context.applicationContext
            )

            val options =
                MapLibreMapOptions()
                    .textureMode(
                        true
                    )
                    .compassEnabled(
                        false
                    )

            MapView(
                context,
                options
            ).apply {
                onCreate(
                    null
                )
            }
        }

    MapViewLifecycle(
        context =
            context,
        mapView =
            mapView
    )

    LaunchedEffect(
        mapView,
        apiKey
    ) {

        mapView.getMapAsync {
                map ->

            mapLibreMap =
                map

            map.cameraPosition =
                CameraPosition
                    .Builder()
                    .target(
                        TATRA_CENTER
                    )
                    .zoom(
                        11.0
                    )
                    .build()

            map.setStyle(
                Style
                    .Builder()
                    .fromUri(
                        landscapeStyleUrl(
                            apiKey
                        )
                    )
            ) {
                    style ->

                addSavedRouteLayers(
                    style
                )

                updateSavedRoute(
                    style =
                        style,
                    geometryPoints =
                        geometryPoints
                )

                loadedStyle =
                    style
            }
        }
    }

    LaunchedEffect(
        geometryPoints,
        loadedStyle,
        mapLibreMap
    ) {

        val style =
            loadedStyle
                ?: return@LaunchedEffect

        val map =
            mapLibreMap
                ?: return@LaunchedEffect

        updateSavedRoute(
            style =
                style,
            geometryPoints =
                geometryPoints
        )

        fitSavedRoute(
            map =
                map,
            mapView =
                mapView,
            geometryPoints =
                geometryPoints
        )
    }

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    mapHeight
                ),
        factory = {
            mapView
        },
        update = {
            it.requestLayout()
        }
    )
}

private fun addSavedRouteLayers(
    style: Style
) {

    if (
        style.getSource(
            SAVED_ROUTE_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                SAVED_ROUTE_SOURCE_ID,
                emptySavedRouteFeatureCollection()
            )
        )
    }

    if (
        style.getSource(
            SAVED_ROUTE_START_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                SAVED_ROUTE_START_SOURCE_ID,
                emptySavedRouteFeatureCollection()
            )
        )
    }

    if (
        style.getSource(
            SAVED_ROUTE_END_SOURCE_ID
        ) == null
    ) {

        style.addSource(
            GeoJsonSource(
                SAVED_ROUTE_END_SOURCE_ID,
                emptySavedRouteFeatureCollection()
            )
        )
    }

    if (
        style.getLayer(
            SAVED_ROUTE_CASING_LAYER_ID
        ) == null
    ) {

        style.addLayer(
            LineLayer(
                SAVED_ROUTE_CASING_LAYER_ID,
                SAVED_ROUTE_SOURCE_ID
            ).withProperties(
                PropertyFactory.lineColor(
                    Color.WHITE
                ),
                PropertyFactory.lineWidth(
                    7f
                ),
                PropertyFactory.lineOpacity(
                    0.92f
                )
            )
        )
    }

    if (
        style.getLayer(
            SAVED_ROUTE_LAYER_ID
        ) == null
    ) {

        style.addLayer(
            LineLayer(
                SAVED_ROUTE_LAYER_ID,
                SAVED_ROUTE_SOURCE_ID
            ).withProperties(
                PropertyFactory.lineColor(
                    Color.rgb(
                        114,
                        168,
                        50
                    )
                ),
                PropertyFactory.lineWidth(
                    4.8f
                ),
                PropertyFactory.lineOpacity(
                    1f
                )
            )
        )
    }

    if (
        style.getLayer(
            SAVED_ROUTE_START_LAYER_ID
        ) == null
    ) {

        style.addLayer(
            CircleLayer(
                SAVED_ROUTE_START_LAYER_ID,
                SAVED_ROUTE_START_SOURCE_ID
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
                )
            )
        )
    }

    if (
        style.getLayer(
            SAVED_ROUTE_END_LAYER_ID
        ) == null
    ) {

        style.addLayer(
            CircleLayer(
                SAVED_ROUTE_END_LAYER_ID,
                SAVED_ROUTE_END_SOURCE_ID
            ).withProperties(
                PropertyFactory.circleColor(
                    Color.rgb(
                        200,
                        71,
                        62
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
                )
            )
        )
    }
}

private fun updateSavedRoute(
    style: Style,
    geometryPoints:
    List<PlannedRoutePointEntity>
) {

    val routeSource =
        style.getSourceAs<GeoJsonSource>(
            SAVED_ROUTE_SOURCE_ID
        )
            ?: return

    val startSource =
        style.getSourceAs<GeoJsonSource>(
            SAVED_ROUTE_START_SOURCE_ID
        )

    val endSource =
        style.getSourceAs<GeoJsonSource>(
            SAVED_ROUTE_END_SOURCE_ID
        )

    if (
        geometryPoints.size < 2
    ) {

        routeSource.setGeoJson(
            emptySavedRouteFeatureCollection()
        )

        startSource?.setGeoJson(
            emptySavedRouteFeatureCollection()
        )

        endSource?.setGeoJson(
            emptySavedRouteFeatureCollection()
        )

        return
    }

    routeSource.setGeoJson(
        savedRouteLineGeoJson(
            geometryPoints
        )
    )

    startSource?.setGeoJson(
        savedRoutePointGeoJson(
            geometryPoints.first()
        )
    )

    endSource?.setGeoJson(
        savedRoutePointGeoJson(
            geometryPoints.last()
        )
    )
}

private fun savedRouteLineGeoJson(
    geometryPoints:
    List<PlannedRoutePointEntity>
): String {

    val coordinates =
        JSONArray()

    geometryPoints.forEach {
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

private fun savedRoutePointGeoJson(
    point:
    PlannedRoutePointEntity
): String {

    val coordinates =
        JSONArray()
            .put(
                point.longitude
            )
            .put(
                point.latitude
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

private fun emptySavedRouteFeatureCollection():
        String {

    return JSONObject()
        .put(
            "type",
            "FeatureCollection"
        )
        .put(
            "features",
            JSONArray()
        )
        .toString()
}

private fun fitSavedRoute(
    map: MapLibreMap,
    mapView: MapView,
    geometryPoints:
    List<PlannedRoutePointEntity>
) {

    if (
        geometryPoints.isEmpty()
    ) {
        return
    }

    val positions =
        geometryPoints.map {
                point ->

            LatLng(
                point.latitude,
                point.longitude
            )
        }

    mapView.post {

        if (
            positions.size == 1
        ) {

            map.moveCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        positions.first(),
                        15.0
                    )
            )

        } else {

            val bounds =
                LatLngBounds
                    .fromLatLngs(
                        positions
                    )

            map.moveCamera(
                CameraUpdateFactory
                    .newLatLngBounds(
                        bounds,
                        60
                    )
            )
        }
    }
}