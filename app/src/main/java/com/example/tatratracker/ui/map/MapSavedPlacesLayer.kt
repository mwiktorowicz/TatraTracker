package com.example.tatratracker.ui.map

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreenDark
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/*
 * =========================================================
 * MOJE PUNKTY (tabela saved_places)
 * =========================================================
 *
 * Biały krążek z zieloną obwódką, na nim ikona według kategorii,
 * pod spodem nazwa. Krążek odróżnia zapisane miejsca od zwykłych
 * punktów mapy, które są samą ikoną.
 */

internal const val SAVED_PLACE_PROPERTY_NAME = "name"
internal const val SAVED_PLACE_PROPERTY_CATEGORY = "category"

// Zapisywany jako tekst: liczba typu Long mogłaby stracić dokładność w GeoJSON.
internal const val SAVED_PLACE_PROPERTY_ID = "id"

internal fun addSavedPlacesLayers(style: Style) {
    if (style.getSource(SAVED_PLACES_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(SAVED_PLACES_SOURCE_ID, emptyFeatureCollection()))
    }

    if (style.getLayer(SAVED_PLACE_BADGE_LAYER_ID) == null) {
        val badges =
            CircleLayer(SAVED_PLACE_BADGE_LAYER_ID, SAVED_PLACES_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(14f),
                PropertyFactory.circleColor(Color.WHITE),
                PropertyFactory.circleStrokeColor(TatraGreenDark.toArgb()),
                PropertyFactory.circleStrokeWidth(2.5f)
            )

        style.addLayer(badges)
    }

    if (style.getLayer(SAVED_PLACE_SYMBOL_LAYER_ID) == null) {
        val iconByCategory =
            Expression.match(
                Expression.get(SAVED_PLACE_PROPERTY_CATEGORY),
                Expression.literal(PIN_ICON_ID),
                Expression.stop(SavedPlaceCategory.HUT, HUT_ICON_ID),
                Expression.stop(SavedPlaceCategory.MOUNTAIN, PEAK_ICON_ID)
            )

        // Nakładanie dozwolone, żeby ikona nigdy nie zniknęła z krążka (krążek nie ma kolizji).
        val symbols =
            SymbolLayer(SAVED_PLACE_SYMBOL_LAYER_ID, SAVED_PLACES_SOURCE_ID).withProperties(
                PropertyFactory.iconImage(iconByCategory),
                PropertyFactory.iconSize(0.75f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(Expression.get(SAVED_PLACE_PROPERTY_NAME)),
                PropertyFactory.textSize(12.5f),
                PropertyFactory.textColor(TatraDark.toArgb()),
                PropertyFactory.textHaloColor(Color.WHITE),
                PropertyFactory.textHaloWidth(2f),
                PropertyFactory.textOffset(arrayOf(0f, 1.9f)),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )

        style.addLayer(symbols)
    }
}

internal fun updateSavedPlaces(style: Style, places: List<SavedPlaceEntity>) {
    style
        .getSourceAs<GeoJsonSource>(SAVED_PLACES_SOURCE_ID)
        ?.setGeoJson(savedPlacesToGeoJson(places))
}

internal fun savedPlacesToGeoJson(places: List<SavedPlaceEntity>): String {
    val features = JSONArray()

    places.forEach { place ->
        val geometry =
            JSONObject()
                .put("type", "Point")
                .put("coordinates", JSONArray().put(place.longitude).put(place.latitude))

        val properties =
            JSONObject()
                .put(SAVED_PLACE_PROPERTY_ID, place.id.toString())
                .put(SAVED_PLACE_PROPERTY_NAME, place.name)
                .put(SAVED_PLACE_PROPERTY_CATEGORY, place.category)

        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("properties", properties)
                .put("geometry", geometry)
        )
    }

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}
