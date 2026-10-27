package com.example.tatratracker.ui.map

import com.example.tatratracker.model.TrackPoint
import org.json.JSONArray
import org.json.JSONObject

internal fun tracksToGeoJson(
    tracks: List<List<TrackPoint>>
): String {

    val validTracks =
        tracks.filter { track ->
            track.size >= 2
        }

    if (validTracks.isEmpty()) {
        return emptyFeatureCollection()
    }

    val multiLineCoordinates =
        JSONArray()

    validTracks.forEach { track ->

        val lineCoordinates =
            JSONArray()

        track.forEach { point ->

            lineCoordinates.put(
                JSONArray()
                    .put(point.lon)
                    .put(point.lat)
            )
        }

        multiLineCoordinates.put(
            lineCoordinates
        )
    }

    val geometry =
        JSONObject()
            .put(
                "type",
                "MultiLineString"
            )
            .put(
                "coordinates",
                multiLineCoordinates
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
                .put(feature)
        )
        .toString()
}


internal fun emptyFeatureCollection():
        String {

    return """
        {
          "type": "FeatureCollection",
          "features": []
        }
    """.trimIndent()
}