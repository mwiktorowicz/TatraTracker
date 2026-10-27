package com.example.tatratracker.data

import android.content.Context
import android.util.JsonReader
import com.example.tatratracker.model.TrackPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class TrailMatchResult(
    val matchedGeoJson: String,
    val matchedLengthMeters: Double,
    val matchedSegmentCount: Int,
    val matchedWayCount: Int
)

class TrailMatcher(
    private val context: Context
) {

    private data class Point(
        val lat: Double,
        val lon: Double
    )

    private data class TrailFeature(
        val wayId: String,
        val points: List<Point>,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    private data class CellKey(
        val x: Int,
        val y: Int
    )

    private data class TrailNetwork(
        val features: List<TrailFeature>,
        val spatialIndex: Map<CellKey, List<Int>>
    )

    private var cachedNetwork:
            TrailNetwork? = null

    /*
     * =====================================================
     * USTAWIENIA MATCHINGU
     * =====================================================
     */

    /*
     * Maksymalna odległość śladu GPS
     * od szlaku.
     *
     * 25 metrów jest rozsądnym punktem
     * startowym dla GPS z telefonu/zegarka
     * w górach.
     */
    private val matchToleranceMeters =
        25.0

    /*
     * Nie wystarczy, że dwie linie są blisko.
     *
     * Muszą również biec mniej więcej
     * w tym samym kierunku.
     *
     * Chroni to np. przed zaliczeniem
     * przecinającego się prostopadle szlaku.
     */
    private val maxAngleDifferenceDegrees =
        55.0

    /*
     * Jeżeli między dwoma kolejnymi punktami
     * GPS jest ogromna dziura, nie traktujemy
     * jej jako rzeczywiście przebytej linii.
     */
    private val maxTrackSegmentLengthMeters =
        250.0

    /*
     * Rozmiar komórki indeksu przestrzennego.
     *
     * 0.002 stopnia to w Tatrach mniej więcej
     * 150-220 metrów.
     */
    private val gridCellSizeDegrees =
        0.002

    /*
     * =====================================================
     * PUBLICZNA FUNKCJA
     * =====================================================
     */

    fun matchTracks(
        tracks: List<List<TrackPoint>>
    ): TrailMatchResult {

        val network =
            getOrLoadNetwork()

        /*
         * Klucz:
         *
         * index Feature -> numery zaliczonych
         * małych segmentów tej Feature.
         */
        val matchedSegments =
            mutableMapOf<
                    Int,
                    MutableSet<Int>
                    >()

        tracks.forEach { track ->

            if (track.size < 2) {
                return@forEach
            }

            for (
            trackIndex
            in 0 until track.lastIndex
            ) {

                val trackStart =
                    Point(
                        lat =
                            track[trackIndex].lat,
                        lon =
                            track[trackIndex].lon
                    )

                val trackEnd =
                    Point(
                        lat =
                            track[trackIndex + 1].lat,
                        lon =
                            track[trackIndex + 1].lon
                    )

                val trackLength =
                    distanceMeters(
                        trackStart,
                        trackEnd
                    )

                /*
                 * Ignorujemy:
                 *
                 * - praktycznie zerowy ruch,
                 * - ogromne dziury w zapisie GPS.
                 */
                if (
                    trackLength < 1.0 ||
                    trackLength >
                    maxTrackSegmentLengthMeters
                ) {
                    continue
                }

                val candidateFeatures =
                    findCandidateFeatures(
                        network = network,
                        start = trackStart,
                        end = trackEnd
                    )

                candidateFeatures.forEach {
                        featureIndex ->

                    val feature =
                        network.features[
                            featureIndex
                        ]

                    for (
                    segmentIndex
                    in 0 until
                            feature.points.lastIndex
                    ) {

                        val trailStart =
                            feature.points[
                                segmentIndex
                            ]

                        val trailEnd =
                            feature.points[
                                segmentIndex + 1
                            ]

                        if (
                            trailSegmentMatchesTrack(
                                trackStart =
                                    trackStart,
                                trackEnd =
                                    trackEnd,
                                trailStart =
                                    trailStart,
                                trailEnd =
                                    trailEnd
                            )
                        ) {

                            matchedSegments
                                .getOrPut(
                                    featureIndex
                                ) {
                                    mutableSetOf()
                                }
                                .add(
                                    segmentIndex
                                )
                        }
                    }
                }
            }
        }

        return buildResult(
            network =
                network,

            matchedSegments =
                matchedSegments
        )
    }

    /*
     * =====================================================
     * WCZYTYWANIE SIECI TATR
     * =====================================================
     */

    private fun getOrLoadNetwork():
            TrailNetwork {

        cachedNetwork?.let {
            return it
        }

        val features =
            loadFeatures()

        val spatialIndex =
            buildSpatialIndex(
                features
            )

        val network =
            TrailNetwork(
                features =
                    features,

                spatialIndex =
                    spatialIndex
            )

        cachedNetwork =
            network

        return network
    }

    private fun loadFeatures():
            List<TrailFeature> {

        val result =
            mutableListOf<TrailFeature>()

        context.assets
            .open(
                "tatry_szlaki.geojson"
            )
            .use { inputStream ->

                JsonReader(
                    InputStreamReader(
                        inputStream,
                        Charsets.UTF_8
                    )
                ).use { reader ->

                    reader.beginObject()

                    while (
                        reader.hasNext()
                    ) {

                        when (
                            reader.nextName()
                        ) {

                            "features" -> {

                                readFeaturesArray(
                                    reader =
                                        reader,

                                    result =
                                        result
                                )
                            }

                            else -> {
                                reader.skipValue()
                            }
                        }
                    }

                    reader.endObject()
                }
            }

        return result
    }

    private fun readFeaturesArray(
        reader: JsonReader,
        result: MutableList<TrailFeature>
    ) {

        reader.beginArray()

        while (
            reader.hasNext()
        ) {

            readFeature(
                reader
            )?.let { feature ->

                result.add(
                    feature
                )
            }
        }

        reader.endArray()
    }

    private fun readFeature(
        reader: JsonReader
    ): TrailFeature? {

        var wayId =
            ""

        var points:
                List<Point>? = null

        reader.beginObject()

        while (
            reader.hasNext()
        ) {

            when (
                reader.nextName()
            ) {

                "id" -> {

                    wayId =
                        readFlexibleString(
                            reader
                        )
                }

                "properties" -> {

                    val propertyWayId =
                        readProperties(
                            reader
                        )

                    if (
                        propertyWayId
                            .isNotBlank()
                    ) {
                        wayId =
                            propertyWayId
                    }
                }

                "geometry" -> {

                    points =
                        readGeometry(
                            reader
                        )
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        val finalPoints =
            points
                ?: return null

        if (
            finalPoints.size < 2
        ) {
            return null
        }

        val minLat =
            finalPoints.minOf {
                it.lat
            }

        val maxLat =
            finalPoints.maxOf {
                it.lat
            }

        val minLon =
            finalPoints.minOf {
                it.lon
            }

        val maxLon =
            finalPoints.maxOf {
                it.lon
            }

        return TrailFeature(
            wayId =
                wayId,

            points =
                finalPoints,

            minLat =
                minLat,

            maxLat =
                maxLat,

            minLon =
                minLon,

            maxLon =
                maxLon
        )
    }

    private fun readProperties(
        reader: JsonReader
    ): String {

        var wayId =
            ""

        reader.beginObject()

        while (
            reader.hasNext()
        ) {

            when (
                reader.nextName()
            ) {

                "@id" -> {

                    wayId =
                        readFlexibleString(
                            reader
                        )
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        return wayId
    }

    private fun readGeometry(
        reader: JsonReader
    ): List<Point>? {

        var type =
            ""

        var points:
                List<Point>? = null

        reader.beginObject()

        while (
            reader.hasNext()
        ) {

            when (
                reader.nextName()
            ) {

                "type" -> {

                    type =
                        reader.nextString()
                }

                "coordinates" -> {

                    points =
                        readCoordinates(
                            reader
                        )
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        if (
            type != "LineString"
        ) {
            return null
        }

        return points
    }

    private fun readCoordinates(
        reader: JsonReader
    ): List<Point> {

        val result =
            mutableListOf<Point>()

        reader.beginArray()

        while (
            reader.hasNext()
        ) {

            reader.beginArray()

            val longitude =
                reader.nextDouble()

            val latitude =
                reader.nextDouble()

            /*
             * Jeżeli GeoJSON miałby również
             * wysokość Z, ignorujemy ją.
             */
            while (
                reader.hasNext()
            ) {
                reader.skipValue()
            }

            reader.endArray()

            result.add(
                Point(
                    lat =
                        latitude,
                    lon =
                        longitude
                )
            )
        }

        reader.endArray()

        return result
    }

    private fun readFlexibleString(
        reader: JsonReader
    ): String {

        return try {

            reader.nextString()

        } catch (_: Exception) {

            reader.skipValue()
            ""
        }
    }

    /*
     * =====================================================
     * INDEKS PRZESTRZENNY
     * =====================================================
     */

    private fun buildSpatialIndex(
        features: List<TrailFeature>
    ): Map<CellKey, List<Int>> {

        val index =
            mutableMapOf<
                    CellKey,
                    MutableList<Int>
                    >()

        features.forEachIndexed {
                featureIndex,
                feature ->

            val minX =
                gridX(
                    feature.minLon
                )

            val maxX =
                gridX(
                    feature.maxLon
                )

            val minY =
                gridY(
                    feature.minLat
                )

            val maxY =
                gridY(
                    feature.maxLat
                )

            for (
            x in minX..maxX
            ) {

                for (
                y in minY..maxY
                ) {

                    index
                        .getOrPut(
                            CellKey(
                                x = x,
                                y = y
                            )
                        ) {
                            mutableListOf()
                        }
                        .add(
                            featureIndex
                        )
                }
            }
        }

        return index
    }

    private fun findCandidateFeatures(
        network: TrailNetwork,
        start: Point,
        end: Point
    ): Set<Int> {

        val averageLatitude =
            (
                    start.lat +
                            end.lat
                    ) / 2.0

        val latTolerance =
            matchToleranceMeters /
                    111_320.0

        val lonMetersPerDegree =
            111_320.0 *
                    cos(
                        Math.toRadians(
                            averageLatitude
                        )
                    )

        val lonTolerance =
            if (
                lonMetersPerDegree >
                1.0
            ) {

                matchToleranceMeters /
                        lonMetersPerDegree

            } else {

                latTolerance
            }

        val minLat =
            min(
                start.lat,
                end.lat
            ) -
                    latTolerance

        val maxLat =
            max(
                start.lat,
                end.lat
            ) +
                    latTolerance

        val minLon =
            min(
                start.lon,
                end.lon
            ) -
                    lonTolerance

        val maxLon =
            max(
                start.lon,
                end.lon
            ) +
                    lonTolerance

        val minX =
            gridX(
                minLon
            )

        val maxX =
            gridX(
                maxLon
            )

        val minY =
            gridY(
                minLat
            )

        val maxY =
            gridY(
                maxLat
            )

        val candidates =
            mutableSetOf<Int>()

        for (
        x in minX..maxX
        ) {

            for (
            y in minY..maxY
            ) {

                network
                    .spatialIndex[
                    CellKey(
                        x = x,
                        y = y
                    )
                ]
                    ?.let { indices ->

                        candidates.addAll(
                            indices
                        )
                    }
            }
        }

        return candidates
    }

    private fun gridX(
        longitude: Double
    ): Int {

        return floor(
            longitude /
                    gridCellSizeDegrees
        ).toInt()
    }

    private fun gridY(
        latitude: Double
    ): Int {

        return floor(
            latitude /
                    gridCellSizeDegrees
        ).toInt()
    }

    /*
     * =====================================================
     * SPRAWDZANIE DOPASOWANIA
     * =====================================================
     */

    private fun trailSegmentMatchesTrack(
        trackStart: Point,
        trackEnd: Point,
        trailStart: Point,
        trailEnd: Point
    ): Boolean {

        val distance =
            segmentDistanceMeters(
                trackStart,
                trackEnd,
                trailStart,
                trailEnd
            )

        if (
            distance >
            matchToleranceMeters
        ) {
            return false
        }

        val trailLength =
            distanceMeters(
                trailStart,
                trailEnd
            )

        val trackLength =
            distanceMeters(
                trackStart,
                trackEnd
            )

        /*
         * Dla bardzo krótkich fragmentów
         * porównywanie kąta jest niestabilne.
         */
        if (
            trailLength < 4.0 ||
            trackLength < 4.0
        ) {
            return true
        }

        val angleDifference =
            directionDifferenceDegrees(
                trackStart,
                trackEnd,
                trailStart,
                trailEnd
            )

        return angleDifference <=
                maxAngleDifferenceDegrees
    }

    /*
     * =====================================================
     * GEOMETRIA
     * =====================================================
     */

    private data class XY(
        val x: Double,
        val y: Double
    )

    private fun toXY(
        point: Point,
        referenceLatitude: Double
    ): XY {

        val metersPerDegreeLon =
            111_320.0 *
                    cos(
                        Math.toRadians(
                            referenceLatitude
                        )
                    )

        return XY(
            x =
                point.lon *
                        metersPerDegreeLon,

            y =
                point.lat *
                        111_320.0
        )
    }

    private fun segmentDistanceMeters(
        a1: Point,
        a2: Point,
        b1: Point,
        b2: Point
    ): Double {

        val referenceLatitude =
            (
                    a1.lat +
                            a2.lat +
                            b1.lat +
                            b2.lat
                    ) / 4.0

        val aa1 =
            toXY(
                a1,
                referenceLatitude
            )

        val aa2 =
            toXY(
                a2,
                referenceLatitude
            )

        val bb1 =
            toXY(
                b1,
                referenceLatitude
            )

        val bb2 =
            toXY(
                b2,
                referenceLatitude
            )

        if (
            segmentsIntersect(
                aa1,
                aa2,
                bb1,
                bb2
            )
        ) {
            return 0.0
        }

        return minOf(
            pointToSegmentDistance(
                aa1,
                bb1,
                bb2
            ),

            pointToSegmentDistance(
                aa2,
                bb1,
                bb2
            ),

            pointToSegmentDistance(
                bb1,
                aa1,
                aa2
            ),

            pointToSegmentDistance(
                bb2,
                aa1,
                aa2
            )
        )
    }

    private fun pointToSegmentDistance(
        point: XY,
        start: XY,
        end: XY
    ): Double {

        val dx =
            end.x -
                    start.x

        val dy =
            end.y -
                    start.y

        val lengthSquared =
            dx * dx +
                    dy * dy

        if (
            lengthSquared <=
            0.000001
        ) {

            return sqrt(
                (
                        point.x -
                                start.x
                        ).pow(2) +
                        (
                                point.y -
                                        start.y
                                ).pow(2)
            )
        }

        val projection =
            (
                    (
                            point.x -
                                    start.x
                            ) * dx +
                            (
                                    point.y -
                                            start.y
                                    ) * dy
                    ) /
                    lengthSquared

        val t =
            projection.coerceIn(
                0.0,
                1.0
            )

        val closestX =
            start.x +
                    t * dx

        val closestY =
            start.y +
                    t * dy

        return sqrt(
            (
                    point.x -
                            closestX
                    ).pow(2) +
                    (
                            point.y -
                                    closestY
                            ).pow(2)
        )
    }

    private fun segmentsIntersect(
        a: XY,
        b: XY,
        c: XY,
        d: XY
    ): Boolean {

        val o1 =
            orientation(
                a,
                b,
                c
            )

        val o2 =
            orientation(
                a,
                b,
                d
            )

        val o3 =
            orientation(
                c,
                d,
                a
            )

        val o4 =
            orientation(
                c,
                d,
                b
            )

        return (
                o1 * o2 < 0.0 &&
                        o3 * o4 < 0.0
                )
    }

    private fun orientation(
        a: XY,
        b: XY,
        c: XY
    ): Double {

        return (
                b.x -
                        a.x
                ) *
                (
                        c.y -
                                a.y
                        ) -
                (
                        b.y -
                                a.y
                        ) *
                (
                        c.x -
                                a.x
                        )
    }

    private fun directionDifferenceDegrees(
        a1: Point,
        a2: Point,
        b1: Point,
        b2: Point
    ): Double {

        val referenceLatitude =
            (
                    a1.lat +
                            a2.lat +
                            b1.lat +
                            b2.lat
                    ) / 4.0

        val aa1 =
            toXY(
                a1,
                referenceLatitude
            )

        val aa2 =
            toXY(
                a2,
                referenceLatitude
            )

        val bb1 =
            toXY(
                b1,
                referenceLatitude
            )

        val bb2 =
            toXY(
                b2,
                referenceLatitude
            )

        val angleA =
            Math.toDegrees(
                atan2(
                    aa2.y -
                            aa1.y,

                    aa2.x -
                            aa1.x
                )
            )

        val angleB =
            Math.toDegrees(
                atan2(
                    bb2.y -
                            bb1.y,

                    bb2.x -
                            bb1.x
                )
            )

        var difference =
            abs(
                angleA -
                        angleB
            )

        while (
            difference >
            180.0
        ) {
            difference -=
                180.0
        }

        if (
            difference >
            90.0
        ) {
            difference =
                180.0 -
                        difference
        }

        return abs(
            difference
        )
    }

    private fun distanceMeters(
        a: Point,
        b: Point
    ): Double {

        val referenceLatitude =
            (
                    a.lat +
                            b.lat
                    ) / 2.0

        val aa =
            toXY(
                a,
                referenceLatitude
            )

        val bb =
            toXY(
                b,
                referenceLatitude
            )

        return sqrt(
            (
                    aa.x -
                            bb.x
                    ).pow(2) +
                    (
                            aa.y -
                                    bb.y
                            ).pow(2)
        )
    }

    /*
     * =====================================================
     * BUDOWANIE WYNIKU
     * =====================================================
     */

    private fun buildResult(
        network: TrailNetwork,
        matchedSegments:
        Map<Int, Set<Int>>
    ): TrailMatchResult {

        val featuresJson =
            JSONArray()

        var totalLength =
            0.0

        var segmentCount =
            0

        matchedSegments.forEach {
                (
                    featureIndex,
                    segmentIndices
                ) ->

            if (
                segmentIndices.isEmpty()
            ) {
                return@forEach
            }

            val feature =
                network.features[
                    featureIndex
                ]

            val sorted =
                segmentIndices
                    .sorted()

            /*
             * Liczenie faktycznie zaliczonej
             * długości.
             */
            sorted.forEach {
                    segmentIndex ->

                if (
                    segmentIndex <
                    feature.points.lastIndex
                ) {

                    totalLength +=
                        distanceMeters(
                            feature.points[
                                segmentIndex
                            ],

                            feature.points[
                                segmentIndex + 1
                            ]
                        )

                    segmentCount++
                }
            }

            /*
             * Łączymy sąsiednie zaliczone
             * segmenty z powrotem w dłuższe
             * linie.
             *
             * Dzięki temu MapLibre dostanie
             * znacznie mniej geometrii.
             */
            var runStart =
                sorted.first()

            var runEnd =
                runStart

            for (
            index
            in 1 until sorted.size
            ) {

                val current =
                    sorted[index]

                if (
                    current ==
                    runEnd + 1
                ) {

                    runEnd =
                        current

                } else {

                    addMatchedRun(
                        featuresJson =
                            featuresJson,

                        feature =
                            feature,

                        startSegment =
                            runStart,

                        endSegment =
                            runEnd
                    )

                    runStart =
                        current

                    runEnd =
                        current
                }
            }

            addMatchedRun(
                featuresJson =
                    featuresJson,

                feature =
                    feature,

                startSegment =
                    runStart,

                endSegment =
                    runEnd
            )
        }

        val collection =
            JSONObject()
                .put(
                    "type",
                    "FeatureCollection"
                )
                .put(
                    "features",
                    featuresJson
                )

        return TrailMatchResult(
            matchedGeoJson =
                collection.toString(),

            matchedLengthMeters =
                totalLength,

            matchedSegmentCount =
                segmentCount,

            matchedWayCount =
                matchedSegments.size
        )
    }

    private fun addMatchedRun(
        featuresJson: JSONArray,
        feature: TrailFeature,
        startSegment: Int,
        endSegment: Int
    ) {

        if (
            startSegment < 0 ||
            endSegment >=
            feature.points.lastIndex
        ) {
            return
        }

        val coordinates =
            JSONArray()

        for (
        pointIndex
        in startSegment..
                (endSegment + 1)
        ) {

            val point =
                feature.points[
                    pointIndex
                ]

            coordinates.put(
                JSONArray()
                    .put(
                        point.lon
                    )
                    .put(
                        point.lat
                    )
            )
        }

        val geometry =
            JSONObject()
                .put(
                    "type",
                    "LineString"
                )
                .put(
                    "coordinates",
                    coordinates
                )

        val properties =
            JSONObject()
                .put(
                    "way_id",
                    feature.wayId
                )

        val jsonFeature =
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

        featuresJson.put(
            jsonFeature
        )
    }
}