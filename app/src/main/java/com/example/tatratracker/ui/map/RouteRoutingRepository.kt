package com.example.tatratracker.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class PlannedRoute(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationSeconds: Int,
    val legCount: Int
)

data class RouteCalculationResponse(
    val route: PlannedRoute? = null,
    val error: String? = null
)

class RouteRoutingRepository {

    companion object {

        /*
         * Zezwalamy na wszystkie poziomy
         * trudności SAC.
         */
        private const val MAX_HIKING_DIFFICULTY =
            6

        /*
         * Maksymalna odległość, w której
         * Valhalla może szukać sieci
         * przy naszym punkcie.
         */
        private const val LOCATION_RADIUS_METERS =
            150

        /*
         * Ważne dla:
         *
         * - szczytów
         * - przełęczy
         * - jaskiń
         * - końców szlaków
         */
        private const val MINIMUM_REACHABILITY =
            1

        /*
         * Nie akceptujemy sytuacji,
         * w której Valhalla kończy trasę
         * bardzo daleko od wybranego punktu.
         */
        private const val MAX_ACCEPTED_SNAP_DISTANCE_METERS =
            400.0
    }

    private val endpoints =
        listOf(
            "https://valhalla1.openstreetmap.de/route",
            "https://valhalla.openstreetmap.de/route"
        )

    suspend fun calculateRoute(
        state: RoutePlanningState
    ): RouteCalculationResponse {

        val start =
            state.start.point
                ?: return RouteCalculationResponse(
                    error = "Najpierw wybierz punkt startowy."
                )

        val end =
            state.end.point
                ?: return RouteCalculationResponse(
                    error = "Najpierw wybierz punkt docelowy."
                )

        /*
         * =====================================================
         * START -> PRZYSTANKI -> CEL
         * =====================================================
         */

        val routePoints =
            buildList {

                add(start)

                state.waypoints
                    .mapNotNull {
                        it.point
                    }
                    .forEach {
                        add(it)
                    }

                add(end)
            }

        return withContext(
            Dispatchers.IO
        ) {

            var lastError:
                    String? = null

            endpoints.forEach { endpoint ->

                val response =
                    tryCalculate(
                        endpoint = endpoint,
                        routePoints = routePoints
                    )

                if (
                    response.route != null
                ) {

                    return@withContext response
                }

                lastError =
                    response.error
            }

            RouteCalculationResponse(
                error =
                    lastError
                        ?: "Nie udało się wyznaczyć trasy."
            )
        }
    }

    private fun tryCalculate(
        endpoint: String,
        routePoints: List<RoutePlanPoint>
    ): RouteCalculationResponse {

        var connection:
                HttpURLConnection? = null

        return try {

            /*
             * =================================================
             * PUNKTY
             * =================================================
             */

            val locations =
                JSONArray()

            routePoints.forEach { point ->

                locations.put(
                    createValhallaLocation(
                        point
                    )
                )
            }

            /*
             * =================================================
             * PEDESTRIAN COSTING
             * =================================================
             *
             * Tutaj jest najważniejsza zmiana.
             *
             * shortest = true
             *
             * oraz ustawiamy wszystkie dodatkowe
             * mnożniki na 1.0 / 0, żeby algorytm
             * nie preferował dziwnych objazdów.
             */

            val pedestrianOptions =
                JSONObject()
                    .put(
                        "max_hiking_difficulty",
                        MAX_HIKING_DIFFICULTY
                    )

                    /*
                     * NAJWAŻNIEJSZE:
                     *
                     * preferuj trasę najkrótszą
                     * przestrzennie.
                     */
                    .put(
                        "shortest",
                        true
                    )

                    /*
                     * Standardowo Valhalla
                     * lekko faworyzuje walkways.
                     *
                     * My chcemy neutralny koszt.
                     */
                    .put(
                        "walkway_factor",
                        1.0
                    )

                    /*
                     * Nie penalizujemy alejek.
                     */
                    .put(
                        "alley_factor",
                        1.0
                    )

                    /*
                     * Nie penalizujemy driveway.
                     */
                    .put(
                        "driveway_factor",
                        1.0
                    )

                    /*
                     * Schody nie mają powodować
                     * wyboru wielokilometrowego
                     * objazdu.
                     */
                    .put(
                        "step_penalty",
                        0
                    )

                    /*
                     * Prom nie ma znaczenia
                     * w Tatrach i nie chcemy,
                     * żeby wpływał na koszt.
                     */
                    .put(
                        "use_ferry",
                        0.0
                    )

            val costingOptions =
                JSONObject()
                    .put(
                        "pedestrian",
                        pedestrianOptions
                    )

            /*
             * =================================================
             * DIRECTIONS
             * =================================================
             */

            val directionsOptions =
                JSONObject()
                    .put(
                        "units",
                        "kilometers"
                    )
                    .put(
                        "narrative",
                        false
                    )

            /*
             * =================================================
             * REQUEST
             * =================================================
             */

            val requestBody =
                JSONObject()
                    .put(
                        "locations",
                        locations
                    )
                    .put(
                        "costing",
                        "pedestrian"
                    )
                    .put(
                        "costing_options",
                        costingOptions
                    )
                    .put(
                        "directions_options",
                        directionsOptions
                    )
                    .put(
                        "alternates",
                        0
                    )

            /*
             * =================================================
             * HTTP
             * =================================================
             */

            connection =
                URL(
                    endpoint
                )
                    .openConnection()
                        as HttpURLConnection

            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.connectTimeout =
                12_000

            connection.readTimeout =
                20_000

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "X-Client-Id",
                "TatraTracker-dev"
            )

            connection
                .outputStream
                .bufferedWriter()
                .use { writer ->

                    writer.write(
                        requestBody.toString()
                    )
                }

            val responseCode =
                connection.responseCode

            val inputStream =
                if (
                    responseCode in 200..299
                ) {

                    connection.inputStream

                } else {

                    connection.errorStream
                }

            val responseBody =
                inputStream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            if (
                responseCode !in 200..299
            ) {

                return RouteCalculationResponse(
                    error =
                        parseError(
                            responseBody = responseBody,
                            responseCode = responseCode
                        )
                )
            }

            parseRoute(
                json = responseBody,
                requestedPoints = routePoints
            )

        } catch (
            exception: Exception
        ) {

            RouteCalculationResponse(
                error =
                    "Błąd połączenia z routingiem: " +
                            (
                                    exception.message
                                        ?: "nieznany błąd"
                                    )
            )

        } finally {

            connection
                ?.disconnect()
        }
    }

    /*
     * =========================================================
     * PUNKT VALHALLA
     * =========================================================
     */

    private fun createValhallaLocation(
        point: RoutePlanPoint
    ): JSONObject {

        return JSONObject()
            .put(
                "lat",
                point.position.latitude
            )
            .put(
                "lon",
                point.position.longitude
            )
            .put(
                "type",
                "break"
            )
            .put(
                "minimum_reachability",
                MINIMUM_REACHABILITY
            )
            .put(
                "radius",
                LOCATION_RADIUS_METERS
            )
            .put(
                "rank_candidates",
                true
            )
            .put(
                "name",
                point.name
            )
    }

    /*
     * =========================================================
     * ODCZYT TRASY
     * =========================================================
     */

    private fun parseRoute(
        json: String,
        requestedPoints: List<RoutePlanPoint>
    ): RouteCalculationResponse {

        return try {

            val root =
                JSONObject(json)

            val trip =
                root.optJSONObject(
                    "trip"
                )
                    ?: return RouteCalculationResponse(
                        error =
                            root.optString(
                                "error",
                                "Serwer nie zwrócił trasy."
                            )
                    )

            /*
             * =================================================
             * SUMMARY
             * =================================================
             */

            val summary =
                trip.optJSONObject(
                    "summary"
                )

            val distanceKm =
                summary
                    ?.optDouble(
                        "length",
                        0.0
                    )
                    ?: 0.0

            val durationSeconds =
                (
                        summary
                            ?.optDouble(
                                "time",
                                0.0
                            )
                            ?: 0.0
                        )
                    .roundToInt()

            /*
             * =================================================
             * LEGS
             * =================================================
             */

            val legs =
                trip.optJSONArray(
                    "legs"
                )
                    ?: return RouteCalculationResponse(
                        error =
                            "Serwer nie zwrócił geometrii trasy."
                    )

            val expectedLegCount =
                requestedPoints.size -
                        1

            if (
                legs.length() !=
                expectedLegCount
            ) {

                return RouteCalculationResponse(
                    error =
                        "Routing zwrócił nieprawidłową liczbę odcinków."
                )
            }

            val routePoints =
                mutableListOf<LatLng>()

            /*
             * =================================================
             * KAŻDY LEG
             * =================================================
             */

            for (
            legIndex in
            0 until legs.length()
            ) {

                val leg =
                    legs.optJSONObject(
                        legIndex
                    )
                        ?: return RouteCalculationResponse(
                            error =
                                "Nie udało się odczytać odcinka ${legIndex + 1}."
                        )

                val encodedShape =
                    leg.optString(
                        "shape"
                    )

                if (
                    encodedShape.isBlank()
                ) {

                    return RouteCalculationResponse(
                        error =
                            "Brakuje geometrii odcinka ${legIndex + 1}."
                    )
                }

                val decoded =
                    decodePolyline6(
                        encodedShape
                    )

                if (
                    decoded.size < 2
                ) {

                    return RouteCalculationResponse(
                        error =
                            "Nie udało się odczytać odcinka ${legIndex + 1}."
                    )
                }

                /*
                 * =================================================
                 * SPRAWDZENIE SNAPOWANIA
                 * =================================================
                 */

                val requestedStart =
                    requestedPoints[
                        legIndex
                    ]
                        .position

                val requestedEnd =
                    requestedPoints[
                        legIndex + 1
                    ]
                        .position

                val actualStart =
                    decoded.first()

                val actualEnd =
                    decoded.last()

                val startSnapDistance =
                    distanceMeters(
                        requestedStart,
                        actualStart
                    )

                val endSnapDistance =
                    distanceMeters(
                        requestedEnd,
                        actualEnd
                    )

                if (
                    startSnapDistance >
                    MAX_ACCEPTED_SNAP_DISTANCE_METERS
                ) {

                    return RouteCalculationResponse(
                        error =
                            "Punkt „${requestedPoints[legIndex].name}” " +
                                    "jest zbyt daleko od sieci szlaków."
                    )
                }

                if (
                    endSnapDistance >
                    MAX_ACCEPTED_SNAP_DISTANCE_METERS
                ) {

                    return RouteCalculationResponse(
                        error =
                            "Punkt „${requestedPoints[legIndex + 1].name}” " +
                                    "jest zbyt daleko od sieci szlaków."
                    )
                }

                /*
                 * =================================================
                 * ŁĄCZENIE ODCINKÓW
                 * =================================================
                 */

                if (
                    routePoints.isEmpty()
                ) {

                    routePoints.addAll(
                        decoded
                    )

                } else {

                    routePoints.addAll(
                        decoded.drop(
                            1
                        )
                    )
                }
            }

            if (
                routePoints.size < 2
            ) {

                return RouteCalculationResponse(
                    error =
                        "Nie udało się odczytać przebiegu trasy."
                )
            }

            /*
             * =================================================
             * KONTROLA ABSURDALNEGO OBJAZDU
             * =================================================
             */

            val directMinimumKm =
                calculateDirectMinimumDistanceKm(
                    requestedPoints
                )

            /*
             * Jeżeli routing jest ponad 5 razy
             * dłuższy od sumy prostych pomiędzy
             * punktami, traktujemy go jako
             * podejrzany.
             *
             * Nie odrzucamy normalnych górskich
             * serpentyn, ale 34 km zamiast kilku
             * kilometrów zostanie wychwycone.
             */
            if (
                directMinimumKm >
                0.5 &&
                distanceKm >
                directMinimumKm *
                5.0
            ) {

                return RouteCalculationResponse(
                    error =
                        "Valhalla znalazła nienaturalnie długi objazd " +
                                "(${String.format("%.1f", distanceKm)} km). " +
                                "Brakuje prawidłowego połączenia w jej grafie szlaków."
                )
            }

            RouteCalculationResponse(
                route =
                    PlannedRoute(
                        points =
                            routePoints,

                        distanceKm =
                            distanceKm,

                        durationSeconds =
                            durationSeconds,

                        legCount =
                            legs.length()
                    )
            )

        } catch (
            exception: Exception
        ) {

            RouteCalculationResponse(
                error =
                    "Nie udało się odczytać odpowiedzi routingu."
            )
        }
    }

    /*
     * =========================================================
     * POLYLINE6
     * =========================================================
     */

    private fun decodePolyline6(
        encoded: String
    ): List<LatLng> {

        val result =
            mutableListOf<LatLng>()

        var index =
            0

        var latitude =
            0

        var longitude =
            0

        while (
            index <
            encoded.length
        ) {

            /*
             * LATITUDE
             */

            var shift =
                0

            var value =
                0

            var byte:
                    Int

            do {

                if (
                    index >=
                    encoded.length
                ) {

                    return result
                }

                byte =
                    encoded[
                        index++
                    ]
                        .code -
                            63

                value =
                    value or
                            (
                                    (
                                            byte and
                                                    0x1F
                                            ) shl
                                            shift
                                    )

                shift +=
                    5

            } while (
                byte >=
                0x20
            )

            val latitudeChange =
                if (
                    value and 1 !=
                    0
                ) {

                    (value shr 1)
                        .inv()

                } else {

                    value shr
                            1
                }

            latitude +=
                latitudeChange

            /*
             * LONGITUDE
             */

            shift =
                0

            value =
                0

            do {

                if (
                    index >=
                    encoded.length
                ) {

                    return result
                }

                byte =
                    encoded[
                        index++
                    ]
                        .code -
                            63

                value =
                    value or
                            (
                                    (
                                            byte and
                                                    0x1F
                                            ) shl
                                            shift
                                    )

                shift +=
                    5

            } while (
                byte >=
                0x20
            )

            val longitudeChange =
                if (
                    value and 1 !=
                    0
                ) {

                    (value shr 1)
                        .inv()

                } else {

                    value shr
                            1
                }

            longitude +=
                longitudeChange

            result.add(
                LatLng(
                    latitude /
                            1_000_000.0,

                    longitude /
                            1_000_000.0
                )
            )
        }

        return result
    }

    /*
     * =========================================================
     * MINIMALNA ODLEGŁOŚĆ POMIĘDZY PUNKTAMI
     * =========================================================
     */

    private fun calculateDirectMinimumDistanceKm(
        points: List<RoutePlanPoint>
    ): Double {

        if (
            points.size <
            2
        ) {

            return 0.0
        }

        var totalMeters =
            0.0

        for (
        index in
        0 until points.lastIndex
        ) {

            totalMeters +=
                distanceMeters(
                    points[index]
                        .position,

                    points[index + 1]
                        .position
                )
        }

        return totalMeters /
                1000.0
    }

    /*
     * =========================================================
     * HAVERSINE
     * =========================================================
     */

    private fun distanceMeters(
        first: LatLng,
        second: LatLng
    ): Double {

        val earthRadius =
            6_371_000.0

        val lat1 =
            Math.toRadians(
                first.latitude
            )

        val lat2 =
            Math.toRadians(
                second.latitude
            )

        val deltaLat =
            Math.toRadians(
                second.latitude -
                        first.latitude
            )

        val deltaLon =
            Math.toRadians(
                second.longitude -
                        first.longitude
            )

        val a =
            sin(
                deltaLat /
                        2.0
            ) *
                    sin(
                        deltaLat /
                                2.0
                    ) +
                    cos(
                        lat1
                    ) *
                    cos(
                        lat2
                    ) *
                    sin(
                        deltaLon /
                                2.0
                    ) *
                    sin(
                        deltaLon /
                                2.0
                    )

        val c =
            2.0 *
                    atan2(
                        sqrt(a),
                        sqrt(
                            1.0 - a
                        )
                    )

        return earthRadius *
                c
    }

    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    private fun parseError(
        responseBody: String,
        responseCode: Int
    ): String {

        return try {

            val root =
                JSONObject(
                    responseBody
                )

            val message =
                root.optString(
                    "error"
                )
                    .ifBlank {

                        root.optString(
                            "error_message"
                        )
                    }
                    .ifBlank {

                        root.optString(
                            "message"
                        )
                    }

            if (
                message.isBlank()
            ) {

                "Routing zwrócił HTTP $responseCode."

            } else {

                message
            }

        } catch (
            exception: Exception
        ) {

            "Routing zwrócił HTTP $responseCode."
        }
    }
}