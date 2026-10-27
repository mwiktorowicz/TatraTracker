package com.example.tatratracker.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * =========================================================
 * POJEDYNCZA PRÓBKA PROFILU
 * =========================================================
 */

data class RouteElevationSample(

    /*
     * Odległość od początku trasy.
     */
    val distanceMeters: Double,

    /*
     * Współrzędne próbki.
     */
    val position: LatLng,

    /*
     * Wysokość n.p.m.
     */
    val elevationMeters: Double
)


/*
 * =========================================================
 * GOTOWY PROFIL WYSOKOŚCIOWY
 * =========================================================
 */

data class RouteElevationProfile(

    val samples: List<RouteElevationSample>,

    val totalAscentMeters: Int,

    val totalDescentMeters: Int,

    val highestElevationMeters: Int,

    val lowestElevationMeters: Int
)


data class ElevationProfileResponse(

    val profile: RouteElevationProfile? = null,

    val error: String? = null
)


/*
 * =========================================================
 * REPOSITORY
 * =========================================================
 */

class RouteElevationRepository {

    companion object {

        /*
         * Próbkujemy mniej więcej
         * co 100 metrów.
         */
        private const val MIN_SAMPLE_DISTANCE_METERS =
            100.0

        /*
         * Maksymalna liczba próbek
         * jednej trasy.
         */
        private const val MAX_SAMPLES =
            300

        /*
         * Open-Meteo pozwala do 100,
         * ale używamy 80 żeby URL
         * nie robił się niepotrzebnie ogromny.
         */
        private const val REQUEST_CHUNK_SIZE =
            80

        /*
         * Bardzo małe różnice DEM
         * ignorujemy przy liczeniu
         * sumy podejść / zejść.
         */
        private const val ELEVATION_NOISE_THRESHOLD_METERS =
            2.0

        private const val ENDPOINT =
            "https://api.open-meteo.com/v1/elevation"
    }


    /*
     * =====================================================
     * GŁÓWNA FUNKCJA
     * =====================================================
     */

    suspend fun calculateProfile(
        route: PlannedRoute
    ): ElevationProfileResponse {

        if (
            route.points.size < 2
        ) {

            return ElevationProfileResponse(
                error =
                    "Trasa jest zbyt krótka, aby utworzyć profil wysokościowy."
            )
        }

        return withContext(
            Dispatchers.IO
        ) {

            try {

                /*
                 * Najpierw robimy punkty
                 * rozmieszczone równomiernie
                 * po całej trasie.
                 */
                val sampledLocations =
                    resampleRoute(
                        route.points
                    )

                if (
                    sampledLocations.size < 2
                ) {

                    return@withContext ElevationProfileResponse(
                        error =
                            "Nie udało się przygotować punktów profilu wysokościowego."
                    )
                }

                /*
                 * Pobieramy DEM.
                 */
                val elevations =
                    fetchElevations(
                        sampledLocations.map {
                            it.position
                        }
                    )

                if (
                    elevations.size !=
                    sampledLocations.size
                ) {

                    return@withContext ElevationProfileResponse(
                        error =
                            "Serwer zwrócił niepełne dane wysokościowe."
                    )
                }

                /*
                 * Lekko wygładzamy DEM,
                 * żeby pojedyncze artefakty
                 * nie nabijały sztucznie
                 * podejść i zejść.
                 */
                val smoothedElevations =
                    smoothElevations(
                        elevations
                    )

                val samples =
                    sampledLocations.mapIndexed {
                            index,
                            sampledLocation ->

                        RouteElevationSample(
                            distanceMeters =
                                sampledLocation.distanceMeters,

                            position =
                                sampledLocation.position,

                            elevationMeters =
                                smoothedElevations[index]
                        )
                    }

                val profile =
                    calculateStatistics(
                        samples
                    )

                ElevationProfileResponse(
                    profile =
                        profile
                )

            } catch (
                exception: Exception
            ) {

                ElevationProfileResponse(
                    error =
                        exception.message
                            ?: "Nie udało się pobrać profilu wysokościowego."
                )
            }
        }
    }


    /*
     * =====================================================
     * POBIERANIE WYSOKOŚCI
     * =====================================================
     */

    private fun fetchElevations(
        points: List<LatLng>
    ): List<Double> {

        val elevations =
            mutableListOf<Double>()

        points
            .chunked(
                REQUEST_CHUNK_SIZE
            )
            .forEach { chunk ->

                val chunkElevations =
                    fetchElevationChunk(
                        chunk
                    )

                elevations.addAll(
                    chunkElevations
                )
            }

        return elevations
    }


    private fun fetchElevationChunk(
        points: List<LatLng>
    ): List<Double> {

        if (
            points.isEmpty()
        ) {

            return emptyList()
        }

        /*
         * Open-Meteo:
         *
         * latitude=49.1,49.2,...
         * longitude=19.9,20.0,...
         */

        val latitudes =
            points.joinToString(
                separator = ","
            ) {
                    point ->

                String.format(
                    Locale.US,
                    "%.6f",
                    point.latitude
                )
            }

        val longitudes =
            points.joinToString(
                separator = ","
            ) {
                    point ->

                String.format(
                    Locale.US,
                    "%.6f",
                    point.longitude
                )
            }

        val url =
            URL(
                "$ENDPOINT" +
                        "?latitude=$latitudes" +
                        "&longitude=$longitudes"
            )

        val connection =
            url.openConnection()
                    as HttpURLConnection

        return try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                10_000

            connection.readTimeout =
                15_000

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

            if (
                responseCode !in 200..299
            ) {

                throw IllegalStateException(
                    parseOpenMeteoError(
                        responseBody =
                            responseBody,

                        responseCode =
                            responseCode
                    )
                )
            }

            val root =
                JSONObject(
                    responseBody
                )

            val elevationArray =
                root.optJSONArray(
                    "elevation"
                )
                    ?: throw IllegalStateException(
                        "Brak wysokości w odpowiedzi serwera."
                    )

            if (
                elevationArray.length() !=
                points.size
            ) {

                throw IllegalStateException(
                    "Serwer zwrócił niepełne dane wysokościowe."
                )
            }

            val result =
                mutableListOf<Double>()

            for (
            index in
            0 until elevationArray.length()
            ) {

                val elevation =
                    elevationArray.optDouble(
                        index,
                        Double.NaN
                    )

                if (
                    elevation.isNaN()
                ) {

                    throw IllegalStateException(
                        "Brak wysokości dla części trasy."
                    )
                }

                result.add(
                    elevation
                )
            }

            result

        } finally {

            connection.disconnect()
        }
    }


    /*
     * =====================================================
     * RÓWNOMIERNE PRÓBKOWANIE TRASY
     * =====================================================
     */

    private data class SampledLocation(

        val distanceMeters: Double,

        val position: LatLng
    )


    private fun resampleRoute(
        routePoints: List<LatLng>
    ): List<SampledLocation> {

        if (
            routePoints.size < 2
        ) {

            return emptyList()
        }

        /*
         * Długości kolejnych segmentów.
         */
        val segmentLengths =
            mutableListOf<Double>()

        var totalDistance =
            0.0

        for (
        index in
        0 until routePoints.lastIndex
        ) {

            val length =
                distanceMeters(
                    routePoints[index],
                    routePoints[index + 1]
                )

            segmentLengths.add(
                length
            )

            totalDistance +=
                length
        }

        if (
            totalDistance <= 0.0
        ) {

            return emptyList()
        }

        /*
         * Jeśli trasa jest długa,
         * zwiększamy interwał tak,
         * żeby nie przekroczyć
         * MAX_SAMPLES.
         */
        val sampleDistance =
            max(
                MIN_SAMPLE_DISTANCE_METERS,

                totalDistance /
                        (
                                MAX_SAMPLES -
                                        1
                                )
            )

        val targetDistances =
            mutableListOf<Double>()

        var targetDistance =
            0.0

        while (
            targetDistance <
            totalDistance
        ) {

            targetDistances.add(
                targetDistance
            )

            targetDistance +=
                sampleDistance
        }

        /*
         * Zawsze dokładamy dokładny
         * koniec trasy.
         */
        if (
            targetDistances.isEmpty() ||
            targetDistances.last() <
            totalDistance
        ) {

            targetDistances.add(
                totalDistance
            )
        }

        val result =
            mutableListOf<SampledLocation>()

        var segmentIndex =
            0

        var segmentStartDistance =
            0.0

        targetDistances.forEach {
                requestedDistance ->

            while (
                segmentIndex <
                segmentLengths.lastIndex &&
                segmentStartDistance +
                segmentLengths[segmentIndex] <
                requestedDistance
            ) {

                segmentStartDistance +=
                    segmentLengths[segmentIndex]

                segmentIndex++
            }

            val segmentLength =
                segmentLengths[
                    segmentIndex
                ]

            val start =
                routePoints[
                    segmentIndex
                ]

            val end =
                routePoints[
                    segmentIndex + 1
                ]

            val fraction =
                if (
                    segmentLength <= 0.0
                ) {

                    0.0

                } else {

                    (
                            (
                                    requestedDistance -
                                            segmentStartDistance
                                    ) /
                                    segmentLength
                            )
                        .coerceIn(
                            0.0,
                            1.0
                        )
                }

            val latitude =
                start.latitude +
                        (
                                end.latitude -
                                        start.latitude
                                ) *
                        fraction

            val longitude =
                start.longitude +
                        (
                                end.longitude -
                                        start.longitude
                                ) *
                        fraction

            result.add(
                SampledLocation(
                    distanceMeters =
                        requestedDistance,

                    position =
                        LatLng(
                            latitude,
                            longitude
                        )
                )
            )
        }

        return result
    }


    /*
     * =====================================================
     * WYGŁADZENIE PROFILU
     * =====================================================
     */

    private fun smoothElevations(
        values: List<Double>
    ): List<Double> {

        if (
            values.size < 3
        ) {

            return values
        }

        val result =
            MutableList(
                values.size
            ) {
                    index ->

                values[index]
            }

        /*
         * Prosta średnia 3-punktowa.
         *
         * Pierwszy i ostatni punkt
         * pozostają bez zmian.
         */
        for (
        index in
        1 until values.lastIndex
        ) {

            result[index] =
                (
                        values[index - 1] +
                                values[index] +
                                values[index + 1]
                        ) /
                        3.0
        }

        return result
    }


    /*
     * =====================================================
     * STATYSTYKI
     * =====================================================
     */

    private fun calculateStatistics(
        samples: List<RouteElevationSample>
    ): RouteElevationProfile {

        var ascent =
            0.0

        var descent =
            0.0

        for (
        index in
        1 until samples.size
        ) {

            val difference =
                samples[index].elevationMeters -
                        samples[index - 1].elevationMeters

            when {

                difference >
                        ELEVATION_NOISE_THRESHOLD_METERS -> {

                    ascent +=
                        difference
                }

                difference <
                        -ELEVATION_NOISE_THRESHOLD_METERS -> {

                    descent +=
                        -difference
                }
            }
        }

        val highest =
            samples.maxOf {
                it.elevationMeters
            }

        val lowest =
            samples.minOf {
                it.elevationMeters
            }

        return RouteElevationProfile(
            samples =
                samples,

            totalAscentMeters =
                ascent.roundToInt(),

            totalDescentMeters =
                descent.roundToInt(),

            highestElevationMeters =
                highest.roundToInt(),

            lowestElevationMeters =
                lowest.roundToInt()
        )
    }


    /*
     * =====================================================
     * HAVERSINE
     * =====================================================
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
     * =====================================================
     * ERROR OPEN-METEO
     * =====================================================
     */

    private fun parseOpenMeteoError(
        responseBody: String,
        responseCode: Int
    ): String {

        return try {

            val root =
                JSONObject(
                    responseBody
                )

            root.optString(
                "reason"
            )
                .ifBlank {

                    "Elevation API zwróciło HTTP $responseCode."
                }

        } catch (
            exception: Exception
        ) {

            "Elevation API zwróciło HTTP $responseCode."
        }
    }
}