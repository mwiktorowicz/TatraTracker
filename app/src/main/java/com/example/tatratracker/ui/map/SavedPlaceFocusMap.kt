package com.example.tatratracker.ui.map

import android.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tatratracker.BuildConfig
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraSurface
import com.example.tatratracker.ui.theme.TatraTextMuted
import com.example.tatratracker.viewmodel.SavedPlaceSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val SAVED_PLACE_SOURCE_ID =
    "tt-saved-place-focus-source"

private const val SAVED_PLACE_HALO_LAYER_ID =
    "tt-saved-place-focus-halo"

private const val SAVED_PLACE_LAYER_ID =
    "tt-saved-place-focus-marker"

@Composable
fun SavedPlaceFocusMap(
    summary: SavedPlaceSummary,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val apiKey =
        BuildConfig.MAPTILER_API_KEY

    val place =
        summary.place

    var mapLoading by
    remember {
        mutableStateOf(true)
    }

    BackHandler(
        onBack = onBack
    )

    if (apiKey.isBlank()) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        ComposeColor(
                            0xFFE9ECE7
                        )
                    )
        ) {

            Text(
                text =
                    "Brak klucza MapTiler.",
                modifier =
                    Modifier.align(
                        Alignment.Center
                    )
            )

            SavedPlaceMapTopBar(
                place = place,
                onBack = onBack,
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(
                            16.dp
                        )
            )

            SavedPlaceMapInfoCard(
                summary = summary,
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            16.dp
                        )
            )
        }

        return
    }

    val mapOptions =
        remember {

            MapLibreMapOptions()
                .textureMode(
                    true
                )
                .compassEnabled(
                    false
                )
        }

    val mapView =
        remember(
            context
        ) {

            MapLibre.getInstance(
                context.applicationContext
            )

            MapView(
                context,
                mapOptions
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
        place.id
    ) {

        mapLoading =
            true

        mapView
            .addOnDidFailLoadingMapListener {

                mapLoading =
                    false
            }

        mapView.getMapAsync {
                map ->

            map.uiSettings
                .setCompassEnabled(
                    false
                )

            map.cameraPosition =
                CameraPosition
                    .Builder()
                    .target(
                        org.maplibre.android.geometry.LatLng(
                            place.latitude,
                            place.longitude
                        )
                    )
                    .zoom(
                        15.5
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

                addSavedPlaceFocusLayers(
                    style =
                        style,
                    place =
                        place
                )

                mapView.post {

                    map.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory
                            .newLatLngZoom(
                                org.maplibre.android.geometry.LatLng(
                                    place.latitude,
                                    place.longitude
                                ),
                                15.5
                            )
                    )
                }

                mapLoading =
                    false
            }
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier =
                Modifier.fillMaxSize(),
            factory = {
                mapView
            },
            update = {

                it.requestLayout()
            }
        )

        SavedPlaceMapTopBar(
            place =
                place,
            onBack =
                onBack,
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp
                    )
        )

        SavedPlaceMapInfoCard(
            summary =
                summary,
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        16.dp
                    )
        )

        if (
            mapLoading
        ) {

            Surface(
                modifier =
                    Modifier.align(
                        Alignment.Center
                    ),
                shape =
                    RoundedCornerShape(
                        999.dp
                    ),
                color =
                    TatraSurface.copy(
                        alpha =
                            0.94f
                    ),
                shadowElevation =
                    4.dp
            ) {

                CircularProgressIndicator(
                    color =
                        TatraGreen,
                    modifier =
                        Modifier.padding(
                            14.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun SavedPlaceMapTopBar(
    place: SavedPlaceEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier
                .fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            TatraSurface.copy(
                alpha =
                    0.96f
            ),
        shadowElevation =
            5.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            6.dp,
                        vertical =
                            6.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick =
                    onBack
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .ArrowBack,
                    contentDescription =
                        "Wróć do odwiedzonych miejsc",
                    tint =
                        TatraDark
                )
            }

            Column(
                modifier =
                    Modifier.padding(
                        start =
                            4.dp
                    )
            ) {

                Text(
                    text =
                        place.name,
                    color =
                        TatraDark,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        SavedPlaceCategory
                            .label(
                                place.category
                            ),
                    color =
                        TatraTextMuted,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun SavedPlaceMapInfoCard(
    summary: SavedPlaceSummary,
    modifier: Modifier = Modifier
) {

    val place =
        summary.place

    Surface(
        modifier =
            modifier
                .fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        color =
            TatraSurface.copy(
                alpha =
                    0.97f
            ),
        shadowElevation =
            7.dp
    ) {

        Column(
            modifier =
                Modifier.padding(
                    TatraSpacing.md
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    TatraSpacing.sm
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    color =
                        TatraGreenVerySoft
                ) {

                    Icon(
                        imageVector =
                            iconForPlaceCategory(
                                place.category
                            ),
                        contentDescription =
                            null,
                        tint =
                            TatraGreen,
                        modifier =
                            Modifier.padding(
                                10.dp
                            )
                    )
                }

                Column(
                    modifier =
                        Modifier.padding(
                            start =
                                TatraSpacing.sm
                        )
                ) {

                    Text(
                        text =
                            place.name,
                        color =
                            TatraDark,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text =
                            SavedPlaceCategory
                                .label(
                                    place.category
                                ),
                        color =
                            TatraTextMuted,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        modifier =
                            Modifier.padding(
                                top =
                                    2.dp
                            )
                    )
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        TatraSpacing.sm
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TatraStatusBadge(
                    text =
                        visitBadgeText(
                            summary.visitCount
                        ),
                    style =
                        TatraBadgeStyle.Success
                )

                summary.lastVisitedAt
                    ?.let {
                            timestamp ->

                        Text(
                            text =
                                "Ostatnio: ${
                                    formatMapVisitDate(
                                        timestamp
                                    )
                                }",
                            color =
                                TatraTextMuted,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
            }

            Text(
                text =
                    String.format(
                        Locale.getDefault(),
                        "%.5f, %.5f",
                        place.latitude,
                        place.longitude
                    ),
                color =
                    TatraTextMuted,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            place.note
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                        note ->

                    Text(
                        text =
                            note,
                        color =
                            TatraDark,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
        }
    }
}

private fun addSavedPlaceFocusLayers(
    style: Style,
    place: SavedPlaceEntity
) {

    if (
        style.getSource(
            SAVED_PLACE_SOURCE_ID
        ) ==
        null
    ) {

        style.addSource(
            GeoJsonSource(
                SAVED_PLACE_SOURCE_ID,
                savedPlaceToGeoJson(
                    place
                )
            )
        )
    }

    if (
        style.getLayer(
            SAVED_PLACE_HALO_LAYER_ID
        ) ==
        null
    ) {

        style.addLayer(
            CircleLayer(
                SAVED_PLACE_HALO_LAYER_ID,
                SAVED_PLACE_SOURCE_ID
            ).withProperties(

                PropertyFactory.circleColor(
                    Color.WHITE
                ),

                PropertyFactory.circleRadius(
                    15f
                ),

                PropertyFactory.circleOpacity(
                    0.88f
                )
            )
        )
    }

    if (
        style.getLayer(
            SAVED_PLACE_LAYER_ID
        ) ==
        null
    ) {

        style.addLayer(
            CircleLayer(
                SAVED_PLACE_LAYER_ID,
                SAVED_PLACE_SOURCE_ID
            ).withProperties(

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
                    2.5f
                )
            )
        )
    }
}

private fun savedPlaceToGeoJson(
    place: SavedPlaceEntity
): String {

    val coordinates =
        JSONArray()
            .put(
                place.longitude
            )
            .put(
                place.latitude
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
                "id",
                place.id
            )
            .put(
                "name",
                place.name
            )
            .put(
                "category",
                place.category
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

private fun iconForPlaceCategory(
    category: String
) =

    when (
        category
    ) {

        SavedPlaceCategory.HUT ->
            Icons.Outlined.Place

        SavedPlaceCategory.MOUNTAIN ->
            Icons.Outlined.Landscape

        else ->
            Icons.Outlined.Route
    }

private fun visitBadgeText(
    count: Int
): String {

    return when (
        count
    ) {

        1 ->
            "1 WIZYTA"

        else ->
            "$count WIZYT"
    }
}

private fun formatMapVisitDate(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

    return formatter.format(
        Date(
            timestamp
        )
    )
}