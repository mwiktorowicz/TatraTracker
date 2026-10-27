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
import com.example.tatratracker.model.TrackPoint
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

private const val ACTIVITY_TRACK_SOURCE_ID =
    "tt-activity-track-source"

private const val ACTIVITY_TRACK_CASING_LAYER_ID =
    "tt-activity-track-casing"

private const val ACTIVITY_TRACK_LAYER_ID =
    "tt-activity-track-layer"

private const val ACTIVITY_START_SOURCE_ID =
    "tt-activity-start-source"

private const val ACTIVITY_START_LAYER_ID =
    "tt-activity-start-layer"

private const val ACTIVITY_END_SOURCE_ID =
    "tt-activity-end-source"

private const val ACTIVITY_END_LAYER_ID =
    "tt-activity-end-layer"

@Composable
fun ActivityRouteMap(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
    mapHeight: Dp = 280.dp
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY

    if (apiKey.isBlank()) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .background(
                        ComposeColor(0xFFE9ECE7)
                    ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Brak klucza MapTiler."
            )
        }

        return
    }

    var mapLibreMap by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    var loadedStyle by remember {
        mutableStateOf<Style?>(null)
    }

    val mapView =
        remember(context) {
            MapLibre.getInstance(
                context.applicationContext
            )

            val mapOptions =
                MapLibreMapOptions()
                    .textureMode(true)
                    .compassEnabled(false)

            MapView(
                context,
                mapOptions
            ).apply {
                onCreate(null)
            }
        }

    MapViewLifecycle(
        context = context,
        mapView = mapView
    )

    LaunchedEffect(
        mapView,
        apiKey
    ) {
        mapView.getMapAsync { map ->
            mapLibreMap = map

            map.cameraPosition =
                CameraPosition.Builder()
                    .target(TATRA_CENTER)
                    .zoom(11.0)
                    .build()

            map.setStyle(
                Style.Builder()
                    .fromUri(
                        landscapeStyleUrl(apiKey)
                    )
            ) { style ->
                addActivityTrackLayers(style)
                updateActivityTrack(
                    style = style,
                    trackPoints = trackPoints
                )

                loadedStyle = style
            }
        }
    }

    LaunchedEffect(
        trackPoints,
        loadedStyle,
        mapLibreMap
    ) {
        val style =
            loadedStyle
                ?: return@LaunchedEffect

        val map =
            mapLibreMap
                ?: return@LaunchedEffect

        updateActivityTrack(
            style = style,
            trackPoints = trackPoints
        )

        fitActivityTrack(
            map = map,
            mapView = mapView,
            trackPoints = trackPoints
        )
    }

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .height(mapHeight),
        factory = {
            mapView
        },
        update = {
            it.requestLayout()
        }
    )
}

private fun addActivityTrackLayers(
    style: Style
) {
    if (style.getSource(ACTIVITY_TRACK_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                ACTIVITY_TRACK_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    if (style.getSource(ACTIVITY_START_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                ACTIVITY_START_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    if (style.getSource(ACTIVITY_END_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                ACTIVITY_END_SOURCE_ID,
                emptyFeatureCollection()
            )
        )
    }

    if (style.getLayer(ACTIVITY_TRACK_CASING_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(
                ACTIVITY_TRACK_CASING_LAYER_ID,
                ACTIVITY_TRACK_SOURCE_ID
            ).withProperties(
                PropertyFactory.lineColor(Color.WHITE),
                PropertyFactory.lineWidth(7f),
                PropertyFactory.lineOpacity(0.92f)
            )
        )
    }

    if (style.getLayer(ACTIVITY_TRACK_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(
                ACTIVITY_TRACK_LAYER_ID,
                ACTIVITY_TRACK_SOURCE_ID
            ).withProperties(
                PropertyFactory.lineColor(
                    Color.rgb(114, 168, 50)
                ),
                PropertyFactory.lineWidth(4.8f),
                PropertyFactory.lineOpacity(1f)
            )
        )
    }

    if (style.getLayer(ACTIVITY_START_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(
                ACTIVITY_START_LAYER_ID,
                ACTIVITY_START_SOURCE_ID
            ).withProperties(
                PropertyFactory.circleColor(
                    Color.rgb(114, 168, 50)
                ),
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
    }

    if (style.getLayer(ACTIVITY_END_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(
                ACTIVITY_END_LAYER_ID,
                ACTIVITY_END_SOURCE_ID
            ).withProperties(
                PropertyFactory.circleColor(
                    Color.rgb(200, 71, 62)
                ),
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
    }
}

private fun updateActivityTrack(
    style: Style,
    trackPoints: List<TrackPoint>
) {
    val trackSource =
        style.getSourceAs<GeoJsonSource>(
            ACTIVITY_TRACK_SOURCE_ID
        )
            ?: return

    val startSource =
        style.getSourceAs<GeoJsonSource>(
            ACTIVITY_START_SOURCE_ID
        )

    val endSource =
        style.getSourceAs<GeoJsonSource>(
            ACTIVITY_END_SOURCE_ID
        )

    if (trackPoints.size < 2) {
        trackSource.setGeoJson(
            emptyFeatureCollection()
        )

        startSource?.setGeoJson(
            emptyFeatureCollection()
        )

        endSource?.setGeoJson(
            emptyFeatureCollection()
        )

        return
    }

    trackSource.setGeoJson(
        tracksToGeoJson(
            listOf(trackPoints)
        )
    )

    startSource?.setGeoJson(
        pointToGeoJson(
            trackPoints.first()
        )
    )

    endSource?.setGeoJson(
        pointToGeoJson(
            trackPoints.last()
        )
    )
}

private fun pointToGeoJson(
    point: TrackPoint
): String {
    val coordinates =
        JSONArray()
            .put(point.lon)
            .put(point.lat)

    val geometry =
        JSONObject()
            .put("type", "Point")
            .put("coordinates", coordinates)

    val feature =
        JSONObject()
            .put("type", "Feature")
            .put("properties", JSONObject())
            .put("geometry", geometry)

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", JSONArray().put(feature))
        .toString()
}

private fun fitActivityTrack(
    map: MapLibreMap,
    mapView: MapView,
    trackPoints: List<TrackPoint>
) {
    if (trackPoints.isEmpty()) {
        return
    }

    val positions =
        trackPoints.map { point ->
            LatLng(
                point.lat,
                point.lon
            )
        }

    mapView.post {
        if (positions.size == 1) {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    positions.first(),
                    15.0
                )
            )
        } else {
            val bounds =
                LatLngBounds.fromLatLngs(
                    positions
                )

            map.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    60
                )
            )
        }
    }
}