package com.example.tatratracker.poi

import android.content.Context
import androidx.room.withTransaction
import com.example.tatratracker.data.AppDatabase
import com.example.tatratracker.data.PoiVisitEntity
import com.example.tatratracker.data.PoiVisitSource
import com.example.tatratracker.data.SavedPlaceCategory
import com.example.tatratracker.data.SavedPlaceEntity
import com.example.tatratracker.model.TrackPoint
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ActivityPoiDetector(
    context: Context
) {

    private val database =
        AppDatabase.getDatabase(
            context.applicationContext
        )

    private val savedPlaceDao =
        database.savedPlaceDao()

    suspend fun detectAndRegister(
        activityId: Int,
        visitedAt: Long,
        trackPoints: List<TrackPoint>
    ): Result<ActivityPoiDetectionResult> {

        return withContext(
            Dispatchers.IO
        ) {

            try {

                val result =
                    detectAndRegisterInternal(
                        activityId = activityId,
                        visitedAt = visitedAt,
                        trackPoints = trackPoints
                    )

                Result.success(
                    result
                )

            } catch (error: Throwable) {

                Result.failure(
                    error
                )
            }
        }
    }

    private suspend fun detectAndRegisterInternal(
        activityId: Int,
        visitedAt: Long,
        trackPoints: List<TrackPoint>
    ): ActivityPoiDetectionResult {

        require(
            activityId > 0
        ) {
            "Niepoprawne ID aktywności."
        }

        if (
            trackPoints.size < 2
        ) {

            return ActivityPoiDetectionResult(
                skippedNoTrack = true
            )
        }

        /*
         * Analizujemy automatycznie tylko Tatry.
         */
        val tatraTrackPoints =
            trackPoints.filter { point ->

                point.lat in
                        TATRA_SOUTH..TATRA_NORTH &&
                        point.lon in
                        TATRA_WEST..TATRA_EAST
            }

        if (
            tatraTrackPoints.isEmpty()
        ) {

            return ActivityPoiDetectionResult(
                skippedOutsideTatras = true
            )
        }

        val bounds =
            buildQueryBounds(
                tatraTrackPoints
            )

        /*
         * Pobranie POI posiada:
         *
         * - retry,
         * - drugi serwer,
         * - opóźnienie między próbami.
         */
        val candidates =
            queryOverpassWithFallback(
                bounds
            )

        val uniqueCandidates =
            deduplicateCandidates(
                candidates
            )

        /*
         * Sprawdzamy rzeczywistą odległość
         * od przebiegu śladu.
         */
        val matchedCandidates =
            uniqueCandidates
                .mapNotNull { candidate ->

                    val distance =
                        minimumDistanceToTrackMeters(
                            candidate = candidate,
                            trackPoints = trackPoints
                        )

                    if (
                        distance <=
                        candidate.maxVisitDistanceMeters
                    ) {

                        MatchedPoiCandidate(
                            candidate = candidate,
                            distanceMeters = distance
                        )

                    } else {

                        null
                    }
                }
                .sortedBy {
                    it.distanceMeters
                }

        if (
            matchedCandidates.isEmpty()
        ) {

            return ActivityPoiDetectionResult()
        }

        val existingPlaces =
            savedPlaceDao
                .getAllPlacesOnce()
                .toMutableList()

        var newPlaces =
            0

        var newVisits =
            0

        val detectedNames =
            mutableListOf<String>()

        matchedCandidates.forEach { matched ->

            val candidate =
                matched.candidate

            val foundPlace =
                findExistingPlace(
                    candidate = candidate,
                    places = existingPlaces
                )

            val resolvedPlace =
                if (
                    foundPlace == null
                ) {

                    val effectiveVisitedAt =
                        if (
                            visitedAt > 0L
                        ) {

                            visitedAt

                        } else {

                            System.currentTimeMillis()
                        }

                    val newPlace =
                        SavedPlaceEntity(
                            name =
                                candidate.name,
                            category =
                                candidate.category,
                            latitude =
                                candidate.latitude,
                            longitude =
                                candidate.longitude,
                            note =
                                "Wykryto automatycznie na podstawie aktywności • ${candidate.typeLabel}",
                            createdAt =
                                effectiveVisitedAt
                        )

                    val insertResult =
                        database.withTransaction {

                            val placeId =
                                savedPlaceDao
                                    .insertPlace(
                                        newPlace
                                    )

                            val visitId =
                                savedPlaceDao
                                    .insertVisit(
                                        PoiVisitEntity(
                                            placeId =
                                                placeId,
                                            activityId =
                                                activityId,
                                            visitedAt =
                                                effectiveVisitedAt,
                                            source =
                                                PoiVisitSource.ACTIVITY
                                        )
                                    )

                            placeId to visitId
                        }

                    val createdPlace =
                        newPlace.copy(
                            id =
                                insertResult.first
                        )

                    existingPlaces +=
                        createdPlace

                    newPlaces++

                    if (
                        insertResult.second != -1L
                    ) {

                        newVisits++
                    }

                    createdPlace

                } else {

                    val effectiveVisitedAt =
                        if (
                            visitedAt > 0L
                        ) {

                            visitedAt

                        } else {

                            System.currentTimeMillis()
                        }

                    /*
                     * Dzięki UNIQUE(placeId, activityId)
                     * ta sama aktywność nie nabije
                     * tego samego miejsca ponownie.
                     */
                    val visitId =
                        savedPlaceDao
                            .insertVisit(
                                PoiVisitEntity(
                                    placeId =
                                        foundPlace.id,
                                    activityId =
                                        activityId,
                                    visitedAt =
                                        effectiveVisitedAt,
                                    source =
                                        PoiVisitSource.ACTIVITY
                                )
                            )

                    if (
                        visitId != -1L
                    ) {

                        newVisits++
                    }

                    foundPlace
                }

            detectedNames +=
                resolvedPlace.name
        }

        return ActivityPoiDetectionResult(
            matchedPlaces =
                matchedCandidates.size,
            newPlaces =
                newPlaces,
            newVisits =
                newVisits,
            detectedNames =
                detectedNames.distinct()
        )
    }

    /*
     * =====================================================
     * OVERPASS — RETRY + FALLBACK
     * =====================================================
     */

    private suspend fun queryOverpassWithFallback(
        bounds: PoiQueryBounds
    ): List<PoiCandidate> {

        val query =
            buildOverpassQuery(
                bounds
            )

        val errors =
            mutableListOf<String>()

        OVERPASS_ENDPOINTS
            .forEachIndexed {
                    endpointIndex,
                    endpoint ->

                repeat(
                    ATTEMPTS_PER_ENDPOINT
                ) {
                        attemptIndex ->

                    try {

                        return queryOverpassOnce(
                            endpoint = endpoint,
                            query = query
                        )

                    } catch (
                        error: Throwable
                    ) {

                        val attemptNumber =
                            attemptIndex + 1

                        errors +=
                            buildString {

                                append(
                                    endpoint
                                )

                                append(
                                    " — próba "
                                )

                                append(
                                    attemptNumber
                                )

                                append(
                                    ": "
                                )

                                append(
                                    error.message
                                        ?: error
                                            .javaClass
                                            .simpleName
                                )
                            }

                        /*
                         * Czekamy przed kolejną próbą
                         * tego samego serwera.
                         */
                        if (
                            attemptIndex <
                            ATTEMPTS_PER_ENDPOINT - 1
                        ) {

                            delay(
                                RETRY_DELAY_MS *
                                        attemptNumber
                            )
                        }
                    }
                }

                /*
                 * Pierwszy serwer nie odpowiedział.
                 * Krótka przerwa przed fallbackiem.
                 */
                if (
                    endpointIndex <
                    OVERPASS_ENDPOINTS.lastIndex
                ) {

                    delay(
                        FALLBACK_DELAY_MS
                    )
                }
            }

        throw IOException(
            buildString {

                append(
                    "Nie udało się pobrać danych POI z serwerów Overpass."
                )

                if (
                    errors.isNotEmpty()
                ) {

                    append(
                        "\n"
                    )

                    append(
                        errors.joinToString(
                            separator =
                                "\n"
                        )
                    )
                }
            }
        )
    }

    private fun queryOverpassOnce(
        endpoint: String,
        query: String
    ): List<PoiCandidate> {

        val encodedBody =
            "data=" +
                    URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8.name()
                    )

        val connection =
            URL(
                endpoint
            )
                .openConnection()
                    as HttpURLConnection

        try {

            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.useCaches =
                false

            connection.connectTimeout =
                CONNECT_TIMEOUT_MS

            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            /*
             * Publiczne instancje Overpass
             * powinny wiedzieć, kto wykonuje
             * zapytanie.
             */
            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
            )

            connection
                .outputStream
                .bufferedWriter(
                    Charsets.UTF_8
                )
                .use { writer ->

                    writer.write(
                        encodedBody
                    )
                }

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

            val responseText =
                stream
                    ?.bufferedReader(
                        Charsets.UTF_8
                    )
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            if (
                responseCode !in 200..299
            ) {

                throw IOException(
                    buildString {

                        append(
                            "HTTP "
                        )

                        append(
                            responseCode
                        )

                        if (
                            responseText.isNotBlank()
                        ) {

                            append(
                                " — "
                            )

                            append(
                                responseText
                                    .replace(
                                        '\n',
                                        ' '
                                    )
                                    .take(
                                        160
                                    )
                            )
                        }
                    }
                )
            }

            if (
                responseText.isBlank()
            ) {

                throw IOException(
                    "Serwer zwrócił pustą odpowiedź."
                )
            }

            return parseOverpassResponse(
                responseText
            )

        } finally {

            connection.disconnect()
        }
    }

    private fun buildOverpassQuery(
        bounds: PoiQueryBounds
    ): String {

        val south =
            formatCoordinate(
                bounds.south
            )

        val west =
            formatCoordinate(
                bounds.west
            )

        val north =
            formatCoordinate(
                bounds.north
            )

        val east =
            formatCoordinate(
                bounds.east
            )

        val bbox =
            "$south,$west,$north,$east"

        return """
            [out:json][timeout:15];
            (
              nwr["tourism"="alpine_hut"]["name"]($bbox);
              nwr["tourism"="wilderness_hut"]["name"]($bbox);
              nwr["natural"="peak"]["name"]($bbox);
              nwr["natural"="saddle"]["name"]($bbox);
              nwr["mountain_pass"="yes"]["name"]($bbox);
            );
            out center;
        """.trimIndent()
    }

    private fun parseOverpassResponse(
        responseText: String
    ): List<PoiCandidate> {

        val root =
            JSONObject(
                responseText
            )

        val elements =
            root.optJSONArray(
                "elements"
            )
                ?: return emptyList()

        val result =
            mutableListOf<PoiCandidate>()

        for (
        index in
        0 until elements.length()
        ) {

            val element =
                elements.optJSONObject(
                    index
                )
                    ?: continue

            val tags =
                element.optJSONObject(
                    "tags"
                )
                    ?: continue

            val name =
                firstNonBlank(
                    tags.optString(
                        "name:pl"
                    ),
                    tags.optString(
                        "name"
                    ),
                    tags.optString(
                        "name:sk"
                    )
                )
                    ?: continue

            val coordinates =
                readElementCoordinates(
                    element
                )
                    ?: continue

            val tourism =
                tags.optString(
                    "tourism"
                )

            val natural =
                tags.optString(
                    "natural"
                )

            val mountainPass =
                tags.optString(
                    "mountain_pass"
                )

            val candidate =
                when {

                    tourism ==
                            "alpine_hut" -> {

                        PoiCandidate(
                            name =
                                name,
                            category =
                                SavedPlaceCategory.HUT,
                            latitude =
                                coordinates.first,
                            longitude =
                                coordinates.second,
                            maxVisitDistanceMeters =
                                HUT_VISIT_DISTANCE_METERS,
                            typeLabel =
                                "schronisko"
                        )
                    }

                    tourism ==
                            "wilderness_hut" -> {

                        PoiCandidate(
                            name =
                                name,
                            category =
                                SavedPlaceCategory.HUT,
                            latitude =
                                coordinates.first,
                            longitude =
                                coordinates.second,
                            maxVisitDistanceMeters =
                                HUT_VISIT_DISTANCE_METERS,
                            typeLabel =
                                "schronisko"
                        )
                    }

                    natural ==
                            "peak" -> {

                        PoiCandidate(
                            name =
                                name,
                            category =
                                SavedPlaceCategory.MOUNTAIN,
                            latitude =
                                coordinates.first,
                            longitude =
                                coordinates.second,
                            maxVisitDistanceMeters =
                                PEAK_VISIT_DISTANCE_METERS,
                            typeLabel =
                                "szczyt"
                        )
                    }

                    natural ==
                            "saddle" -> {

                        PoiCandidate(
                            name =
                                name,
                            category =
                                SavedPlaceCategory.MOUNTAIN,
                            latitude =
                                coordinates.first,
                            longitude =
                                coordinates.second,
                            maxVisitDistanceMeters =
                                PASS_VISIT_DISTANCE_METERS,
                            typeLabel =
                                "przełęcz"
                        )
                    }

                    mountainPass ==
                            "yes" -> {

                        PoiCandidate(
                            name =
                                name,
                            category =
                                SavedPlaceCategory.MOUNTAIN,
                            latitude =
                                coordinates.first,
                            longitude =
                                coordinates.second,
                            maxVisitDistanceMeters =
                                PASS_VISIT_DISTANCE_METERS,
                            typeLabel =
                                "przełęcz"
                        )
                    }

                    else ->
                        null
                }

            candidate?.let {

                result +=
                    it
            }
        }

        return result
    }

    private fun readElementCoordinates(
        element: JSONObject
    ): Pair<Double, Double>? {

        val directLatitude =
            element.optDouble(
                "lat",
                Double.NaN
            )

        val directLongitude =
            element.optDouble(
                "lon",
                Double.NaN
            )

        if (
            directLatitude.isFinite() &&
            directLongitude.isFinite()
        ) {

            return directLatitude to
                    directLongitude
        }

        val center =
            element.optJSONObject(
                "center"
            )
                ?: return null

        val centerLatitude =
            center.optDouble(
                "lat",
                Double.NaN
            )

        val centerLongitude =
            center.optDouble(
                "lon",
                Double.NaN
            )

        if (
            !centerLatitude.isFinite() ||
            !centerLongitude.isFinite()
        ) {

            return null
        }

        return centerLatitude to
                centerLongitude
    }

    /*
     * =====================================================
     * MATCHING POI DO ŚLADU
     * =====================================================
     */

    private fun minimumDistanceToTrackMeters(
        candidate: PoiCandidate,
        trackPoints: List<TrackPoint>
    ): Double {

        if (
            trackPoints.isEmpty()
        ) {

            return Double.MAX_VALUE
        }

        if (
            trackPoints.size == 1
        ) {

            return haversineMeters(
                latitude1 =
                    candidate.latitude,
                longitude1 =
                    candidate.longitude,
                latitude2 =
                    trackPoints.first().lat,
                longitude2 =
                    trackPoints.first().lon
            )
        }

        var minimumDistance =
            Double.MAX_VALUE

        for (
        index in
        1 until trackPoints.size
        ) {

            val previous =
                trackPoints[
                    index - 1
                ]

            val current =
                trackPoints[
                    index
                ]

            val distance =
                distanceFromPointToSegmentMeters(
                    pointLatitude =
                        candidate.latitude,
                    pointLongitude =
                        candidate.longitude,
                    startLatitude =
                        previous.lat,
                    startLongitude =
                        previous.lon,
                    endLatitude =
                        current.lat,
                    endLongitude =
                        current.lon
                )

            if (
                distance <
                minimumDistance
            ) {

                minimumDistance =
                    distance
            }

            if (
                minimumDistance <= 2.0
            ) {

                break
            }
        }

        return minimumDistance
    }

    private fun distanceFromPointToSegmentMeters(
        pointLatitude: Double,
        pointLongitude: Double,
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {

        val start =
            localMeters(
                latitude =
                    startLatitude,
                longitude =
                    startLongitude,
                originLatitude =
                    pointLatitude,
                originLongitude =
                    pointLongitude
            )

        val end =
            localMeters(
                latitude =
                    endLatitude,
                longitude =
                    endLongitude,
                originLatitude =
                    pointLatitude,
                originLongitude =
                    pointLongitude
            )

        val dx =
            end.first -
                    start.first

        val dy =
            end.second -
                    start.second

        val lengthSquared =
            dx * dx +
                    dy * dy

        if (
            lengthSquared <=
            0.000001
        ) {

            return hypot(
                start.first,
                start.second
            )
        }

        val projection =
            -(
                    start.first * dx +
                            start.second * dy
                    ) /
                    lengthSquared

        val t =
            projection.coerceIn(
                0.0,
                1.0
            )

        val nearestX =
            start.first +
                    t * dx

        val nearestY =
            start.second +
                    t * dy

        return hypot(
            nearestX,
            nearestY
        )
    }

    private fun localMeters(
        latitude: Double,
        longitude: Double,
        originLatitude: Double,
        originLongitude: Double
    ): Pair<Double, Double> {

        val originLatitudeRadians =
            Math.toRadians(
                originLatitude
            )

        val x =
            Math.toRadians(
                longitude -
                        originLongitude
            ) *
                    cos(
                        originLatitudeRadians
                    ) *
                    EARTH_RADIUS_METERS

        val y =
            Math.toRadians(
                latitude -
                        originLatitude
            ) *
                    EARTH_RADIUS_METERS

        return x to y
    }

    /*
     * =====================================================
     * DEDUPLIKACJA
     * =====================================================
     */

    private fun deduplicateCandidates(
        candidates: List<PoiCandidate>
    ): List<PoiCandidate> {

        val result =
            mutableListOf<PoiCandidate>()

        candidates.forEach { candidate ->

            val candidateName =
                normalizeName(
                    candidate.name
                )

            val duplicate =
                result.any { existing ->

                    val distance =
                        haversineMeters(
                            latitude1 =
                                candidate.latitude,
                            longitude1 =
                                candidate.longitude,
                            latitude2 =
                                existing.latitude,
                            longitude2 =
                                existing.longitude
                        )

                    val sameNormalizedName =
                        candidateName ==
                                normalizeName(
                                    existing.name
                                )

                    candidate.category ==
                            existing.category &&
                            (
                                    (
                                            sameNormalizedName &&
                                                    distance <=
                                                    150.0
                                            ) ||
                                            distance <=
                                            20.0
                                    )
                }

            if (
                !duplicate
            ) {

                result +=
                    candidate
            }
        }

        return result
    }

    private fun findExistingPlace(
        candidate: PoiCandidate,
        places: List<SavedPlaceEntity>
    ): SavedPlaceEntity? {

        val candidateName =
            normalizeName(
                candidate.name
            )

        return places
            .map { place ->

                val distance =
                    haversineMeters(
                        latitude1 =
                            candidate.latitude,
                        longitude1 =
                            candidate.longitude,
                        latitude2 =
                            place.latitude,
                        longitude2 =
                            place.longitude
                    )

                place to distance
            }
            .filter { pair ->

                val place =
                    pair.first

                val distance =
                    pair.second

                val sameName =
                    candidateName ==
                            normalizeName(
                                place.name
                            )

                candidate.category ==
                        place.category &&
                        (
                                (
                                        sameName &&
                                                distance <=
                                                500.0
                                        ) ||
                                        distance <=
                                        EXISTING_PLACE_POSITION_TOLERANCE_METERS
                                )
            }
            .minByOrNull {
                it.second
            }
            ?.first
    }

    private fun normalizeName(
        value: String
    ): String {

        val lower =
            value
                .trim()
                .lowercase(
                    Locale.ROOT
                )
                .replace(
                    "ł",
                    "l"
                )

        val withoutAccents =
            Normalizer
                .normalize(
                    lower,
                    Normalizer.Form.NFD
                )
                .replace(
                    Regex(
                        "\\p{Mn}+"
                    ),
                    ""
                )

        return withoutAccents
            .replace(
                Regex(
                    "[^a-z0-9]+"
                ),
                ""
            )
    }

    /*
     * =====================================================
     * BBOX
     * =====================================================
     */

    private fun buildQueryBounds(
        trackPoints: List<TrackPoint>
    ): PoiQueryBounds {

        val minLatitude =
            trackPoints.minOf {
                it.lat
            }

        val maxLatitude =
            trackPoints.maxOf {
                it.lat
            }

        val minLongitude =
            trackPoints.minOf {
                it.lon
            }

        val maxLongitude =
            trackPoints.maxOf {
                it.lon
            }

        return PoiQueryBounds(
            south =
                max(
                    TATRA_SOUTH,
                    minLatitude -
                            QUERY_MARGIN_DEGREES
                ),
            west =
                max(
                    TATRA_WEST,
                    minLongitude -
                            QUERY_MARGIN_DEGREES
                ),
            north =
                min(
                    TATRA_NORTH,
                    maxLatitude +
                            QUERY_MARGIN_DEGREES
                ),
            east =
                min(
                    TATRA_EAST,
                    maxLongitude +
                            QUERY_MARGIN_DEGREES
                )
        )
    }

    /*
     * =====================================================
     * GEO
     * =====================================================
     */

    private fun haversineMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {

        val latitude1Radians =
            Math.toRadians(
                latitude1
            )

        val latitude2Radians =
            Math.toRadians(
                latitude2
            )

        val deltaLatitude =
            Math.toRadians(
                latitude2 -
                        latitude1
            )

        val deltaLongitude =
            Math.toRadians(
                longitude2 -
                        longitude1
            )

        val sinLatitude =
            sin(
                deltaLatitude /
                        2.0
            )

        val sinLongitude =
            sin(
                deltaLongitude /
                        2.0
            )

        val a =
            sinLatitude *
                    sinLatitude +
                    cos(
                        latitude1Radians
                    ) *
                    cos(
                        latitude2Radians
                    ) *
                    sinLongitude *
                    sinLongitude

        val c =
            2.0 *
                    atan2(
                        sqrt(
                            a
                        ),
                        sqrt(
                            1.0 -
                                    a
                        )
                    )

        return EARTH_RADIUS_METERS *
                c
    }

    private fun formatCoordinate(
        value: Double
    ): String {

        return String.format(
            Locale.US,
            "%.6f",
            value
        )
    }

    private fun firstNonBlank(
        vararg values: String
    ): String? {

        return values
            .firstOrNull {
                it.isNotBlank()
            }
            ?.trim()
    }

    companion object {

        /*
         * Główny serwer + fallback.
         */
        private val OVERPASS_ENDPOINTS =
            listOf(
                "https://overpass-api.de/api/interpreter",
                "https://overpass.private.coffee/api/interpreter"
            )

        private const val USER_AGENT =
            "TatraTracker-Android/1.0"

        /*
         * 2 próby na serwer:
         *
         * główny 2x
         * fallback 2x
         *
         * Maksymalnie 4 próby.
         */
        private const val ATTEMPTS_PER_ENDPOINT =
            2

        private const val RETRY_DELAY_MS =
            900L

        private const val FALLBACK_DELAY_MS =
            700L

        private const val CONNECT_TIMEOUT_MS =
            15_000

        private const val READ_TIMEOUT_MS =
            25_000

        private const val EARTH_RADIUS_METERS =
            6_371_000.0

        private const val TATRA_SOUTH =
            49.05

        private const val TATRA_NORTH =
            49.36

        private const val TATRA_WEST =
            19.55

        private const val TATRA_EAST =
            20.45

        private const val QUERY_MARGIN_DEGREES =
            0.003

        private const val HUT_VISIT_DISTANCE_METERS =
            110.0

        private const val PEAK_VISIT_DISTANCE_METERS =
            55.0

        private const val PASS_VISIT_DISTANCE_METERS =
            65.0

        private const val EXISTING_PLACE_POSITION_TOLERANCE_METERS =
            60.0
    }
}

data class ActivityPoiDetectionResult(

    val matchedPlaces: Int = 0,

    val newPlaces: Int = 0,

    val newVisits: Int = 0,

    val detectedNames: List<String> =
        emptyList(),

    val skippedOutsideTatras: Boolean =
        false,

    val skippedNoTrack: Boolean =
        false
)

private data class PoiQueryBounds(

    val south: Double,

    val west: Double,

    val north: Double,

    val east: Double
)

private data class PoiCandidate(

    val name: String,

    val category: String,

    val latitude: Double,

    val longitude: Double,

    val maxVisitDistanceMeters: Double,

    val typeLabel: String
)

private data class MatchedPoiCandidate(

    val candidate: PoiCandidate,

    val distanceMeters: Double
)