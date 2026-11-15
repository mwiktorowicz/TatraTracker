package com.example.tatratracker.ui.map

import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tatratracker.BuildConfig
import com.example.tatratracker.replay.ActivityReplayTrack
import com.example.tatratracker.replay.ReplayCoordinate
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.abs
import kotlin.math.roundToInt

private const val REPLAY_TRACK_SOURCE_ID = "tt-replay-track-source"
private const val REPLAY_DONE_SOURCE_ID = "tt-replay-done-source"
private const val REPLAY_HEAD_SOURCE_ID = "tt-replay-head-source"
private const val REPLAY_START_SOURCE_ID = "tt-replay-start-source"
private const val REPLAY_END_SOURCE_ID = "tt-replay-end-source"
private const val REPLAY_MARKER_SOURCE_ID = "tt-replay-marker-source"

private const val REPLAY_TRACK_CASING_LAYER_ID = "tt-replay-track-casing"
private const val REPLAY_TRACK_LAYER_ID = "tt-replay-track"
private const val REPLAY_DONE_CASING_LAYER_ID = "tt-replay-done-casing"
private const val REPLAY_HEAD_CASING_LAYER_ID = "tt-replay-head-casing"
private const val REPLAY_DONE_LAYER_ID = "tt-replay-done"
private const val REPLAY_HEAD_LAYER_ID = "tt-replay-head"
private const val REPLAY_START_LAYER_ID = "tt-replay-start"
private const val REPLAY_END_LAYER_ID = "tt-replay-end"
private const val REPLAY_MARKER_HALO_LAYER_ID = "tt-replay-marker-halo"
private const val REPLAY_MARKER_LAYER_ID = "tt-replay-marker"

/*
 * Przebyta część śladu to dwie linie: „gotowa” do pełnego odcinka [DONE_CHUNK_POINTS] punktów
 * (wysyłana do mapy tylko co odcinek) i krótka „końcówka” do znacznika (co klatkę). Dzięki temu
 * w każdej klatce mapa dostaje najwyżej kilkadziesiąt punktów, a nie cały ślad.
 */
private const val DONE_CHUNK_POINTS = 48

// Kamera w trybie „Śledź”: przybliżenie, gdy startujemy z widoku całego śladu.
private const val FOLLOW_ZOOM = 14.5
private const val AUTO_ZOOM_BELOW = 13.0

private val REPLAY_GREEN = Color.rgb(114, 168, 50)
private val REPLAY_MUTED = Color.rgb(112, 124, 104)
private val REPLAY_END_RED = Color.rgb(200, 71, 62)
private val REPLAY_MARKER_BLUE = Color.rgb(38, 120, 220)

/**
 * Mapa odtwarzania: podkład zapisany na mapie głównej (z naszymi warstwami i korektami
 * czytelności), cały ślad, przebyta część i ruchomy znacznik.
 *
 * [progressMeters] jest czytane w przepływie migawek, więc zmiana postępu nie przebudowuje
 * kompozycji mapy – aktualizuje tylko źródła GeoJSON i kamerę.
 */
@Composable
internal fun ActivityReplayMap(
    track: ActivityReplayTrack,
    layerSettings: MapLayerSettings,
    progressMeters: () -> Double,
    isPlaying: Boolean,
    following: Boolean,
    onUserMovedMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY

    if (apiKey.isBlank()) {
        Box(
            modifier = modifier.background(ComposeColor(0xFFE9ECE7)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Brak klucza MapTiler.")
        }
        return
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var basemap by remember { mutableStateOf(layerSettings.basemap) }
    var cameraFitted by remember { mutableStateOf(false) }

    val currentOnUserMovedMap by rememberUpdatedState(onUserMovedMap)
    val playingState = rememberUpdatedState(isPlaying)
    val followingState = rememberUpdatedState(following)

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    MapViewLifecycle(context = context, mapView = mapView)

    LaunchedEffect(mapView) {
        // Zdjęcia satelitarne się nie wczytały: jak na mapie głównej, komunikat i „Teren”
        // (tu bez zmiany zapisanego ustawienia – to robi mapa główna).
        mapView.addOnDidFailLoadingMapListener {
            if (basemap == MapBasemap.SATELLITE && loadedStyle == null) {
                Toast.makeText(
                    context,
                    "Nie udało się wczytać widoku satelitarnego. Mapa wróciła do podkładu „Teren”.",
                    Toast.LENGTH_LONG
                ).show()
                basemap = MapBasemap.TERRAIN
            }
        }

        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(track.start.lat, track.start.lon))
                .zoom(12.0)
                .build()

            map.uiSettings.isCompassEnabled = false
            map.useMapTilerLogo(context)

            // Ręczne przesunięcie (każdy gest) wyłącza „Śledź”; ruchy kamery z kodu nie.
            map.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    currentOnUserMovedMap()
                }
            }

            mapLibreMap = map
        }
    }

    // Ten sam porządek co na mapie głównej: styl → nasze warstwy → widoczność → korekty Satelity.
    LaunchedEffect(mapLibreMap, basemap) {
        val map = mapLibreMap ?: return@LaunchedEffect

        loadedStyle = null

        map.setStyle(Style.Builder().fromUri(basemapStyleUrl(basemap, apiKey))) { style ->
            addTatraMapLayers(
                context = context,
                style = style,
                apiKey = apiKey,
                tracks = emptyList()
            )

            applyMapLayerVisibility(style = style, settings = layerSettings)

            if (basemap == MapBasemap.SATELLITE) {
                applySatelliteReadability(style)
            }

            addReplayLayers(style, track)

            if (!cameraFitted) {
                cameraFitted = true
                fitWholeTrack(map, mapView, track)
            }

            loadedStyle = style
        }
    }

    LaunchedEffect(loadedStyle) {
        val style = loadedStyle ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        var doneChunkStart = -1
        var firstUpdate = true
        var autoZoom = false

        snapshotFlow { progressMeters() to followingState.value }
            .collect { (progress, follow) ->
                val position = track.positionAt(progress)

                doneChunkStart = updateReplayProgress(
                    style = style,
                    track = track,
                    progressMeters = progress,
                    position = position,
                    previousChunkStart = doneChunkStart
                )

                // Na starcie (i po obrocie ekranu) kamera pokazuje cały ślad.
                if (firstUpdate) {
                    firstUpdate = false
                    return@collect
                }

                if (!follow) {
                    autoZoom = false
                    return@collect
                }

                val target = LatLng(position.lat, position.lon)
                val zoom = map.cameraPosition.zoom

                if (zoom < AUTO_ZOOM_BELOW) {
                    autoZoom = true
                }

                when {
                    !autoZoom -> map.moveCamera(CameraUpdateFactory.newLatLng(target))

                    playingState.value -> {
                        // W trakcie odtwarzania przybliżamy płynnie, klatka po klatce.
                        val nextZoom = zoom + (FOLLOW_ZOOM - zoom) * 0.12
                        if (nextZoom >= FOLLOW_ZOOM - 0.05) {
                            autoZoom = false
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, nextZoom))
                    }

                    else -> {
                        autoZoom = false
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, FOLLOW_ZOOM), 450)
                    }
                }
            }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // Logo MapTiler i autorzy danych jak na mapie głównej; przycisk „i” obok logo rysuje MapLibre.
        MapTilerLogo(modifier = Modifier.align(Alignment.BottomStart))

        MapDataAttribution(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

private fun addReplayLayers(style: Style, track: ActivityReplayTrack) {
    style.addSource(GeoJsonSource(REPLAY_TRACK_SOURCE_ID, track.path.toLineString()))
    style.addSource(GeoJsonSource(REPLAY_DONE_SOURCE_ID, emptyFeatureCollection()))
    style.addSource(GeoJsonSource(REPLAY_HEAD_SOURCE_ID, emptyFeatureCollection()))
    style.addSource(GeoJsonSource(REPLAY_START_SOURCE_ID, track.start.toPoint()))
    style.addSource(GeoJsonSource(REPLAY_END_SOURCE_ID, track.end.toPoint()))
    style.addSource(GeoJsonSource(REPLAY_MARKER_SOURCE_ID, track.start.toPoint()))

    // Cały ślad: cienka, przygaszona linia z jasną obwódką (czytelna także na zdjęciach).
    style.addLayer(
        LineLayer(REPLAY_TRACK_CASING_LAYER_ID, REPLAY_TRACK_SOURCE_ID).withProperties(
            PropertyFactory.lineColor(Color.WHITE),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineOpacity(0.75f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayer(
        LineLayer(REPLAY_TRACK_LAYER_ID, REPLAY_TRACK_SOURCE_ID).withProperties(
            PropertyFactory.lineColor(REPLAY_MUTED),
            PropertyFactory.lineWidth(2.6f),
            PropertyFactory.lineOpacity(0.85f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )

    // Przebyta część: grubsza, wyraźna. Najpierw obie obwódki, potem oba kolory,
    // żeby obwódka końcówki nie przecinała koloru w miejscu łączenia.
    listOf(
        REPLAY_DONE_CASING_LAYER_ID to REPLAY_DONE_SOURCE_ID,
        REPLAY_HEAD_CASING_LAYER_ID to REPLAY_HEAD_SOURCE_ID
    ).forEach { (layerId, sourceId) ->
        style.addLayer(
            LineLayer(layerId, sourceId).withProperties(
                PropertyFactory.lineColor(Color.WHITE),
                PropertyFactory.lineWidth(8.5f),
                PropertyFactory.lineOpacity(0.95f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    }
    listOf(
        REPLAY_DONE_LAYER_ID to REPLAY_DONE_SOURCE_ID,
        REPLAY_HEAD_LAYER_ID to REPLAY_HEAD_SOURCE_ID
    ).forEach { (layerId, sourceId) ->
        style.addLayer(
            LineLayer(layerId, sourceId).withProperties(
                PropertyFactory.lineColor(REPLAY_GREEN),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    }

    style.addLayer(endpointLayer(REPLAY_START_LAYER_ID, REPLAY_START_SOURCE_ID, REPLAY_GREEN))
    style.addLayer(endpointLayer(REPLAY_END_LAYER_ID, REPLAY_END_SOURCE_ID, REPLAY_END_RED))

    style.addLayer(
        CircleLayer(REPLAY_MARKER_HALO_LAYER_ID, REPLAY_MARKER_SOURCE_ID).withProperties(
            PropertyFactory.circleColor(REPLAY_MARKER_BLUE),
            PropertyFactory.circleRadius(17f),
            PropertyFactory.circleOpacity(0.22f)
        )
    )
    style.addLayer(
        CircleLayer(REPLAY_MARKER_LAYER_ID, REPLAY_MARKER_SOURCE_ID).withProperties(
            PropertyFactory.circleColor(REPLAY_MARKER_BLUE),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor(Color.WHITE),
            PropertyFactory.circleStrokeWidth(3.5f)
        )
    )
}

private fun endpointLayer(layerId: String, sourceId: String, color: Int): CircleLayer {
    return CircleLayer(layerId, sourceId).withProperties(
        PropertyFactory.circleColor(color),
        PropertyFactory.circleRadius(7f),
        PropertyFactory.circleStrokeColor(Color.WHITE),
        PropertyFactory.circleStrokeWidth(3f)
    )
}

/**
 * Znacznik i przebyta część dla danego postępu. Zwraca początek bieżącego odcinka końcówki;
 * linia „gotowa” jest wysyłana do mapy tylko, gdy ten początek się zmieni.
 */
private fun updateReplayProgress(
    style: Style,
    track: ActivityReplayTrack,
    progressMeters: Double,
    position: ReplayCoordinate,
    previousChunkStart: Int
): Int {
    style.getSourceAs<GeoJsonSource>(REPLAY_MARKER_SOURCE_ID)?.setGeoJson(position.toPoint())

    val reached = track.pathIndexAt(progressMeters)
    val chunkStart = reached / DONE_CHUNK_POINTS * DONE_CHUNK_POINTS

    if (chunkStart != previousChunkStart) {
        val done = style.getSourceAs<GeoJsonSource>(REPLAY_DONE_SOURCE_ID)
        if (chunkStart == 0) {
            done?.setGeoJson(emptyFeatureCollection())
        } else {
            done?.setGeoJson(track.path.subList(0, chunkStart + 1).toLineString())
        }
    }

    val head = ArrayList<Point>(reached - chunkStart + 2)
    for (index in chunkStart..reached) {
        head.add(track.path[index].toPoint())
    }
    head.add(position.toPoint())

    style.getSourceAs<GeoJsonSource>(REPLAY_HEAD_SOURCE_ID)?.setGeoJson(LineString.fromLngLats(head))

    return chunkStart
}

private fun fitWholeTrack(map: MapLibreMap, mapView: MapView, track: ActivityReplayTrack) {
    val positions = track.path.map { LatLng(it.lat, it.lon) }
    val latitudes = positions.map { it.latitude }
    val longitudes = positions.map { it.longitude }

    // Ślad w jednym miejscu (same postoje): granice bez rozpiętości – zwykłe przybliżenie.
    val degenerate =
        abs(latitudes.max() - latitudes.min()) < 1e-6 &&
            abs(longitudes.max() - longitudes.min()) < 1e-6

    val paddingPx = (56 * mapView.resources.displayMetrics.density).roundToInt()

    mapView.post {
        if (degenerate) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(positions.first(), 15.0))
        } else {
            map.moveCamera(
                CameraUpdateFactory.newLatLngBounds(LatLngBounds.fromLatLngs(positions), paddingPx)
            )
        }
    }
}

private fun ReplayCoordinate.toPoint(): Point {
    return Point.fromLngLat(lon, lat)
}

private fun List<ReplayCoordinate>.toLineString(): LineString {
    return LineString.fromLngLats(map { it.toPoint() })
}
