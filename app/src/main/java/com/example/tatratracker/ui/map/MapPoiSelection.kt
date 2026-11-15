package com.example.tatratracker.ui.map

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.poi.PoiPlaceMatcher
import com.example.tatratracker.poi.PoiVisitInfo
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenDark
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import kotlin.math.hypot

/*
 * =========================================================
 * PUNKT WYBRANY DOTKNIĘCIEM MAPY
 * =========================================================
 *
 * Dotknięcie szczytu, schroniska, jaskini, stawu albo „mojego punktu”
 * wybiera go: mapa rysuje wokół niego pierścień, a ekran pokazuje kartę
 * (MapPoiCard) z wizytami i startem planowania trasy.
 */

/**
 * Rodzaj punktu na mapie. [placeCategory] to kategoria, pod którą import aktywności
 * zapisuje wizyty takich punktów; null – wizyt tego rodzaju import nie wykrywa.
 */
internal enum class MapPoiKind(val label: String, val placeCategory: String?) {
    PEAK("Szczyt", SavedPlaceCategory.MOUNTAIN),
    HUT("Schronisko", SavedPlaceCategory.HUT),
    CAVE("Jaskinia", null),
    WATER("Jezioro lub staw", null),
    SAVED_PLACE("Mój punkt", null)
}

internal data class SelectedMapPoi(
    val kind: MapPoiKind,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int? = null,
    /** Zapisane miejsce, jeśli dotknięto „mojego punktu” (także leżącego na punkcie mapy). */
    val savedPlaceId: Long? = null,
    val savedPlaceCategory: String? = null
) {
    /** Nazwa do pokazania i do pola trasy; punkt bez nazwy dostaje nazwę rodzaju. */
    val displayName: String
        get() = name.ifBlank { kind.label }

    val position: LatLng
        get() = LatLng(latitude, longitude)
}

private const val POI_SELECTION_SOURCE_ID = "tt-poi-selection-source"
private const val POI_SELECTION_CASING_LAYER_ID = "tt-poi-selection-casing"
private const val POI_SELECTION_RING_LAYER_ID = "tt-poi-selection-ring"

private const val PROPERTY_NAME = "name"
private const val PROPERTY_ELEVATION = "ele"

private val poiLayersByKind =
    listOf(
        MapPoiKind.PEAK to arrayOf(PEAK_ICON_LAYER_ID, PEAK_LABEL_LAYER_ID),
        MapPoiKind.HUT to arrayOf(HUT_ICON_LAYER_ID, HUT_LABEL_LAYER_ID),
        MapPoiKind.CAVE to arrayOf(CAVE_ICON_LAYER_ID, CAVE_LABEL_LAYER_ID),
        MapPoiKind.WATER to arrayOf(WATER_ICON_LAYER_ID, WATER_LABEL_LAYER_ID)
    )

private val savedPlaceLayers = arrayOf(SAVED_PLACE_BADGE_LAYER_ID, SAVED_PLACE_SYMBOL_LAYER_ID)

/**
 * Punkt pod palcem albo null, gdy dotknięto pustego miejsca. Szuka w kwadracie
 * o boku 2 × [tapRadiusPx] wokół dotknięcia, tylko w widocznych warstwach.
 *
 * „Mój punkt” leżący na szczycie albo schronisku mapy (tak zapisuje je import aktywności)
 * jest zwracany jako ten punkt mapy – z wysokością – ale z identyfikatorem zapisanego miejsca.
 */
internal fun findTappedPoi(
    map: MapLibreMap,
    tap: LatLng,
    tapRadiusPx: Float
): SelectedMapPoi? {
    val tapScreen = map.projection.toScreenLocation(tap)

    val box =
        RectF(
            tapScreen.x - tapRadiusPx,
            tapScreen.y - tapRadiusPx,
            tapScreen.x + tapRadiusPx,
            tapScreen.y + tapRadiusPx
        )

    fun nearest(layerIds: Array<String>, toPoi: (Feature, Point) -> SelectedMapPoi): SelectedMapPoi? {
        return map
            .queryRenderedFeatures(box, *layerIds)
            .mapNotNull { feature ->
                val point = feature.geometry() as? Point ?: return@mapNotNull null
                val poi = toPoi(feature, point)
                poi to map.screenDistance(tapScreen, poi.position)
            }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    val mapPois =
        poiLayersByKind.mapNotNull { (kind, layerIds) ->
            nearest(layerIds) { feature, point ->
                SelectedMapPoi(
                    kind = kind,
                    name = feature.stringProperty(PROPERTY_NAME).orEmpty(),
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    elevationMeters = feature.elevationMeters()
                )
            }
        }

    val savedPlace =
        nearest(savedPlaceLayers) { feature, point ->
            SelectedMapPoi(
                kind = MapPoiKind.SAVED_PLACE,
                name = feature.stringProperty(SAVED_PLACE_PROPERTY_NAME).orEmpty(),
                latitude = point.latitude(),
                longitude = point.longitude(),
                savedPlaceId = feature.stringProperty(SAVED_PLACE_PROPERTY_ID)?.toLongOrNull(),
                savedPlaceCategory = feature.stringProperty(SAVED_PLACE_PROPERTY_CATEGORY)
            )
        }

    if (savedPlace != null) {
        val samePlaceOnMap =
            mapPois.firstOrNull { poi ->
                poi.kind.placeCategory == savedPlace.savedPlaceCategory && poi.isSamePlaceAs(savedPlace)
            }

        return samePlaceOnMap?.copy(
            savedPlaceId = savedPlace.savedPlaceId,
            savedPlaceCategory = savedPlace.savedPlaceCategory
        ) ?: savedPlace
    }

    return mapPois.minByOrNull { poi -> map.screenDistance(tapScreen, poi.position) }
}

/**
 * Wizyty do pokazania na karcie albo null, gdy dla tego punktu nie ma czego pokazać
 * (jaskinia lub staw bez zapisanego miejsca – import nie wykrywa tam wizyt).
 */
internal fun poiVisitInfoFor(
    poi: SelectedMapPoi,
    places: List<SavedPlaceEntity>,
    visits: List<PoiVisitEntity>
): PoiVisitInfo? {
    val place =
        poi.savedPlaceId
            ?.let { id -> places.firstOrNull { place -> place.id == id } }
            ?: PoiPlaceMatcher.findPlace(
                name = poi.name,
                category = poi.kind.placeCategory,
                latitude = poi.latitude,
                longitude = poi.longitude,
                places = places
            )

    if (place == null && poi.kind.placeCategory == null) {
        return null
    }

    return PoiPlaceMatcher.visitInfo(place, visits)
}

internal fun addPoiSelectionLayers(style: Style) {
    if (style.getSource(POI_SELECTION_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(POI_SELECTION_SOURCE_ID, emptyFeatureCollection()))
    }

    // Biała obwódka pod spodem, żeby pierścień było widać na lesie i na zdjęciach.
    if (style.getLayer(POI_SELECTION_CASING_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(POI_SELECTION_CASING_LAYER_ID, POI_SELECTION_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(20f),
                PropertyFactory.circleColor(Color.WHITE),
                PropertyFactory.circleOpacity(0f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(6f)
            )
        )
    }

    if (style.getLayer(POI_SELECTION_RING_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(POI_SELECTION_RING_LAYER_ID, POI_SELECTION_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(20f),
                PropertyFactory.circleColor(TatraGreen.toArgb()),
                PropertyFactory.circleOpacity(0.2f),
                PropertyFactory.circleStrokeColor(TatraGreenDark.toArgb()),
                PropertyFactory.circleStrokeWidth(3f)
            )
        )
    }
}

internal fun updatePoiSelection(style: Style, poi: SelectedMapPoi?) {
    val geoJson =
        if (poi == null) {
            emptyFeatureCollection()
        } else {
            JSONObject()
                .put("type", "Feature")
                .put("properties", JSONObject())
                .put(
                    "geometry",
                    JSONObject()
                        .put("type", "Point")
                        .put("coordinates", JSONArray().put(poi.longitude).put(poi.latitude))
                )
                .toString()
        }

    style.getSourceAs<GeoJsonSource>(POI_SELECTION_SOURCE_ID)?.setGeoJson(geoJson)
}

private fun SelectedMapPoi.isSamePlaceAs(other: SelectedMapPoi): Boolean {
    val distance =
        PoiPlaceMatcher.distanceMeters(latitude, longitude, other.latitude, other.longitude)

    val sameName =
        PoiPlaceMatcher.normalizeName(name) == PoiPlaceMatcher.normalizeName(other.name)

    return (sameName && distance <= PoiPlaceMatcher.SAME_NAME_DISTANCE_METERS) ||
        distance <= PoiPlaceMatcher.POSITION_TOLERANCE_METERS
}

private fun MapLibreMap.screenDistance(from: PointF, position: LatLng): Float {
    val screen = projection.toScreenLocation(position)
    return hypot(screen.x - from.x, screen.y - from.y)
}

private fun Feature.stringProperty(key: String): String? {
    if (!hasNonNullValueForProperty(key)) {
        return null
    }

    return runCatching { getProperty(key).asString }.getOrNull()
}

// Wysokość w kaflach bywa liczbą albo tekstem; zero i wartości ujemne traktujemy jak brak.
private fun Feature.elevationMeters(): Int? {
    return stringProperty(PROPERTY_ELEVATION)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
        ?.let { Math.round(it).toInt() }
}
