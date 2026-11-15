package com.example.tatratracker.ui.map

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import com.example.tatratracker.data.TrailInfo
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/*
 * =========================================================
 * SZLAK WYBRANY DOTKNIĘCIEM MAPY
 * =========================================================
 *
 * Linię szlaku rysują kafle MapTiler, a nazwy, długości i postęp przejścia
 * pochodzą z lokalnego pliku szlaków (TrailMatcher). Kafle mówią tylko,
 * że pod palcem jest szlak; który to szlak, ustalamy po położeniu.
 */

/**
 * Szlaki biegnące dotkniętym odcinkiem; [selectedIndex] wskazuje ten pokazany na karcie
 * i podświetlony na mapie. Pusta lista – odcinka nie ma w pliku szlaków Tatr.
 */
internal data class SelectedMapTrail(
    val trails: List<TrailInfo>,
    val selectedIndex: Int = 0
) {
    val selected: TrailInfo?
        get() = trails.getOrNull(selectedIndex)
}

private const val TRAIL_SELECTION_SOURCE_ID = "tt-trail-selection-source"
private const val TRAIL_SELECTION_OUTLINE_LAYER_ID = "tt-trail-selection-outline"
private const val TRAIL_SELECTION_FILL_LAYER_ID = "tt-trail-selection-fill"

// Poniżej tej odległości nie schodzimy: linia w kaflach i w pliku szlaków różnią się o kilka metrów.
private const val MIN_TAP_TOLERANCE_METERS = 15.0

/** Czy pod palcem (kwadrat o boku 2 × [tapRadiusPx]) widać linię szlaku. */
internal fun isTrailUnderTap(map: MapLibreMap, tap: LatLng, tapRadiusPx: Float): Boolean {
    val screen = map.projection.toScreenLocation(tap)

    val box =
        RectF(
            screen.x - tapRadiusPx,
            screen.y - tapRadiusPx,
            screen.x + tapRadiusPx,
            screen.y + tapRadiusPx
        )

    return map
        .queryRenderedFeatures(box, TRAIL_CASING_LAYER_ID, TRAIL_COLOR_LAYER_ID)
        .isNotEmpty()
}

/** Ile metrów w terenie odpowiada promieniowi dotknięcia przy bieżącym przybliżeniu. */
internal fun tapToleranceMeters(map: MapLibreMap, tap: LatLng, tapRadiusPx: Float): Double {
    val screen = map.projection.toScreenLocation(tap)
    val edge = map.projection.fromScreenLocation(PointF(screen.x + tapRadiusPx, screen.y))

    return maxOf(tap.distanceTo(edge), MIN_TAP_TOLERANCE_METERS)
}

/**
 * Podświetlenie szlaku: ciemna obwódka i biały środek pod kolorową linią szlaku,
 * żeby kolor szlaku został czytelny na obu podkładach. Wołać po addTatraMapLayers.
 */
internal fun addTrailSelectionLayers(style: Style) {
    if (style.getSource(TRAIL_SELECTION_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(TRAIL_SELECTION_SOURCE_ID, emptyFeatureCollection()))
    }

    if (style.getLayer(TRAIL_SELECTION_OUTLINE_LAYER_ID) == null) {
        style.addLayerBelow(
            LineLayer(TRAIL_SELECTION_OUTLINE_LAYER_ID, TRAIL_SELECTION_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(Color.rgb(16, 26, 29)),
                PropertyFactory.lineWidth(15f),
                PropertyFactory.lineOpacity(0.6f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            ),
            TRAIL_CASING_LAYER_ID
        )
    }

    if (style.getLayer(TRAIL_SELECTION_FILL_LAYER_ID) == null) {
        style.addLayerBelow(
            LineLayer(TRAIL_SELECTION_FILL_LAYER_ID, TRAIL_SELECTION_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(Color.WHITE),
                PropertyFactory.lineWidth(11f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            ),
            TRAIL_CASING_LAYER_ID
        )
    }
}

/** [geoJson] = null czyści podświetlenie. */
internal fun updateTrailSelection(style: Style, geoJson: String?) {
    style
        .getSourceAs<GeoJsonSource>(TRAIL_SELECTION_SOURCE_ID)
        ?.setGeoJson(geoJson ?: emptyFeatureCollection())
}
