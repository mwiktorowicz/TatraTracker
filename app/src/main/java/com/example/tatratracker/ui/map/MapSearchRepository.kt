package com.example.tatratracker.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class MapSearchResult(
    val id: String,
    val name: String,
    val fullName: String,
    val type: String,
    val position: LatLng
)

data class MapSearchResponse(
    val results: List<MapSearchResult>,
    val error: String? = null
)

class MapSearchRepository(
    private val apiKey: String
) {

    suspend fun search(
        query: String,
        proximity: LatLng
    ): MapSearchResponse {

        if (
            query.trim().length < 2
        ) {

            return MapSearchResponse(
                results = emptyList()
            )
        }

        return withContext(
            Dispatchers.IO
        ) {

            try {

                val encodedQuery =
                    URLEncoder.encode(
                        query.trim(),
                        StandardCharsets.UTF_8.toString()
                    )
                        .replace(
                            "+",
                            "%20"
                        )

                val encodedKey =
                    URLEncoder.encode(
                        apiKey,
                        StandardCharsets.UTF_8.toString()
                    )

                /*
                 * Nie ograniczamy wyszukiwania
                 * wyłącznie do Polski/Tatr.
                 *
                 * proximity jedynie preferuje
                 * obiekty blisko aktualnej mapy.
                 *
                 * poi dodajemy jawnie, ponieważ
                 * MapTiler nie włącza POI
                 * w domyślnych typach wyszukiwania.
                 */
                val url =
                    URL(
                        "https://api.maptiler.com/" +
                                "geocoding/" +
                                "$encodedQuery.json" +
                                "?key=$encodedKey" +
                                "&language=pl" +
                                "&limit=6" +
                                "&autocomplete=true" +
                                "&types=" +
                                "poi," +
                                "major_landform," +
                                "place," +
                                "locality," +
                                "road," +
                                "address" +
                                "&proximity=" +
                                "${proximity.longitude}," +
                                "${proximity.latitude}"
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    6000

                connection.readTimeout =
                    6000

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val responseCode =
                    connection.responseCode

                val stream =
                    if (
                        responseCode in 200..299
                    ) {

                        connection.inputStream

                    } else {

                        connection.errorStream
                    }

                val responseBody =
                    stream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        .orEmpty()

                connection.disconnect()

                if (
                    responseCode !in 200..299
                ) {

                    return@withContext MapSearchResponse(
                        results =
                            emptyList(),

                        error =
                            "Błąd wyszukiwania: HTTP $responseCode"
                    )
                }

                parseResponse(
                    responseBody
                )

            } catch (
                exception: Exception
            ) {

                MapSearchResponse(
                    results =
                        emptyList(),

                    error =
                        "Nie udało się pobrać wyników."
                )
            }
        }
    }


    private fun parseResponse(
        json: String
    ): MapSearchResponse {

        val root =
            JSONObject(
                json
            )

        val features =
            root.optJSONArray(
                "features"
            )
                ?: return MapSearchResponse(
                    results =
                        emptyList()
                )

        val results =
            mutableListOf<MapSearchResult>()

        for (
        index in 0 until features.length()
        ) {

            val feature =
                features.optJSONObject(
                    index
                )
                    ?: continue

            val center =
                feature.optJSONArray(
                    "center"
                )
                    ?: continue

            if (
                center.length() < 2
            ) {
                continue
            }

            val longitude =
                center.optDouble(
                    0,
                    Double.NaN
                )

            val latitude =
                center.optDouble(
                    1,
                    Double.NaN
                )

            if (
                longitude.isNaN() ||
                latitude.isNaN()
            ) {
                continue
            }

            val id =
                feature.optString(
                    "id",
                    "result_$index"
                )

            val name =
                feature.optString(
                    "text"
                )
                    .ifBlank {

                        feature.optString(
                            "place_name",
                            "Nieznane miejsce"
                        )
                    }

            val fullName =
                feature.optString(
                    "place_name",
                    name
                )

            /*
             * Najpierw próbujemy pobrać
             * kategorię POI.
             */
            val properties =
                feature.optJSONObject(
                    "properties"
                )

            val categories =
                properties
                    ?.optJSONArray(
                        "categories"
                    )

            val category =
                if (
                    categories != null &&
                    categories.length() > 0
                ) {

                    categories.optString(
                        0
                    )

                } else {

                    ""
                }

            val placeTypeNames =
                feature.optJSONArray(
                    "place_type_name"
                )

            val placeType =
                feature.optJSONArray(
                    "place_type"
                )

            val type =
                when {

                    category.isNotBlank() -> {

                        category
                    }

                    placeTypeNames != null &&
                            placeTypeNames.length() > 0 -> {

                        placeTypeNames.optString(
                            0
                        )
                    }

                    placeType != null &&
                            placeType.length() > 0 -> {

                        placeType.optString(
                            0
                        )
                    }

                    else -> ""
                }

            results.add(
                MapSearchResult(
                    id =
                        id,

                    name =
                        name,

                    fullName =
                        fullName,

                    type =
                        type,

                    position =
                        LatLng(
                            latitude,
                            longitude
                        )
                )
            )
        }

        return MapSearchResponse(
            results =
                results
        )
    }
}