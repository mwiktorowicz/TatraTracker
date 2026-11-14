package com.example.tatratracker.ui.map

import androidx.compose.ui.graphics.toArgb
import com.example.tatratracker.ui.theme.TatraGreen
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/*
 * =========================================================
 * PRZEBYTE SZLAKI
 * =========================================================
 *
 * Szeroka, półprzezroczysta zielona poświata pod odcinkami
 * szlaków, które użytkownik już przeszedł (TrailMatchResult.matchedGeoJson).
 * Dodawana jako pierwsza z naszych warstw: leży tuż nad podkładem,
 * pod poziomicami, punktami i kolorowymi liniami szlaków,
 * więc kolor szlaku pozostaje czytelny.
 */

internal fun addWalkedTrailsLayer(style: Style) {
    if (style.getSource(WALKED_TRAILS_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(WALKED_TRAILS_SOURCE_ID, emptyFeatureCollection()))
    }

    if (style.getLayer(WALKED_TRAILS_LAYER_ID) != null) {
        return
    }

    val layer =
        LineLayer(WALKED_TRAILS_LAYER_ID, WALKED_TRAILS_SOURCE_ID).apply {
            // Ten sam próg co kolorowe szlaki.
            setMinZoom(8.5f)

            setProperties(
                PropertyFactory.lineColor(TatraGreen.toArgb()),
                // Linia szlaku ma stałe 1,9 px; poświata ok. 3× szersza w średnim przybliżeniu
                // i rośnie przy zbliżaniu.
                PropertyFactory.lineWidth(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(10, 5f),
                        Expression.stop(13, 6f),
                        Expression.stop(16, 11f)
                    )
                ),
                PropertyFactory.lineOpacity(0.45f),
                PropertyFactory.lineBlur(1f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        }

    style.addLayer(layer)
}

/**
 * [geoJson] = null (warstwa wyłączona albo brak wyniku) czyści warstwę.
 */
internal fun updateWalkedTrails(style: Style, geoJson: String?) {
    style
        .getSourceAs<GeoJsonSource>(WALKED_TRAILS_SOURCE_ID)
        ?.setGeoJson(geoJson ?: emptyFeatureCollection())
}
