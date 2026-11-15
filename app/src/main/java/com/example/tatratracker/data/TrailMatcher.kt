package com.example.tatratracker.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import com.example.tatratracker.model.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class TrailColorProgress(
    val color: String,
    val totalLengthMeters: Double,
    val completedLengthMeters: Double
) {
    val completedFraction: Double
        get() = fraction(
            completedLengthMeters,
            totalLengthMeters
        )
}

data class TrailNameProgress(
    val name: String,
    val colors: List<String>,
    val totalLengthMeters: Double,
    val completedLengthMeters: Double
) {
    val completedFraction: Double
        get() = fraction(
            completedLengthMeters,
            totalLengthMeters
        )
}

data class TrailMatchResult(
    val matchedGeoJson: String,
    val totalLengthMeters: Double,
    val matchedLengthMeters: Double,
    val matchedWayCount: Int,
    val matchedTrackCount: Int,
    val colorProgress: List<TrailColorProgress>,
    val trailProgress: List<TrailNameProgress>
) {
    val completedFraction: Double
        get() = fraction(
            matchedLengthMeters,
            totalLengthMeters
        )
}

/** Szlak z pliku szlaków: nazwa, kolory wspólne dla całego szlaku i łączna długość. */
data class TrailInfo(
    val name: String,
    val colors: List<String>,
    val lengthMeters: Double
)

private fun fraction(
    part: Double,
    total: Double
): Double {

    return if (total > 0.0) {
        part / total
    } else {
        0.0
    }
}

class TrailMatcher(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private data class Point(
        val lat: Double,
        val lon: Double
    )

    /*
     * Jeden odcinek (way) z pliku szlaków.
     *
     * points są już zagęszczone, więc
     * każdy kawałek ma najwyżej
     * maxTrailPieceLengthMeters.
     */
    private class TrailFeature(
        val wayId: String,
        val colors: List<String>,
        val names: List<String>,
        val points: List<Point>,
        val segmentLengths: DoubleArray,
        val lengthMeters: Double
    )

    private data class FeatureProperties(
        val wayId: String,
        val colors: List<String>,
        val names: List<String>
    )

    private data class CellKey(
        val x: Int,
        val y: Int
    )

    /*
     * Kawałek szlaku między punktami
     * segmentIndex i segmentIndex + 1
     * w Feature o numerze featureIndex.
     */
    private data class SegmentRef(
        val featureIndex: Int,
        val segmentIndex: Int
    )

    private class TrailNetwork(
        val features: List<TrailFeature>,
        val spatialIndex: Map<CellKey, List<SegmentRef>>,
        val totalLengthMeters: Double,
        val colorLengths: Map<String, Double>,
        val trailLengths: Map<String, Double>,
        val trailColors: Map<String, List<String>>
    )

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
     * Długie odcinki OSM dzielimy na krótsze
     * kawałki, żeby ślad dotykający tylko
     * końca odcinka nie zaliczał go w całości.
     */
    private val maxTrailPieceLengthMeters =
        25.0

    /*
     * Rozmiar komórki indeksu przestrzennego.
     *
     * 0.002 stopnia to w Tatrach mniej więcej
     * 150-220 metrów.
     */
    private val gridCellSizeDegrees =
        0.002

    private val colorOrder =
        listOf(
            "red",
            "blue",
            "green",
            "yellow",
            "black"
        )

    /*
     * =====================================================
     * PUBLICZNA FUNKCJA
     * =====================================================
     */

    /*
     * Ślady przychodzą po jednym,
     * więc w pamięci nie trzeba trzymać
     * punktów wszystkich aktywności naraz.
     */
    suspend fun matchTracks(
        tracks: Flow<List<TrackPoint>>
    ): TrailMatchResult {

        return withContext(
            Dispatchers.Default
        ) {

            val network =
                getOrLoadNetwork()

            /*
             * Klucz:
             *
             * index Feature -> numery zaliczonych
             * kawałków tej Feature.
             */
            val matchedSegments =
                mutableMapOf<
                        Int,
                        MutableSet<Int>
                        >()

            var matchedTrackCount =
                0

            tracks.collect { track ->

                val matchedAnything =
                    matchTrack(
                        network =
                            network,

                        track =
                            track,

                        matchedSegments =
                            matchedSegments
                    )

                if (matchedAnything) {
                    matchedTrackCount++
                }
            }

            buildResult(
                network =
                    network,

                matchedSegments =
                    matchedSegments,

                matchedTrackCount =
                    matchedTrackCount
            )
        }
    }

    private fun matchTrack(
        network: TrailNetwork,
        track: List<TrackPoint>,
        matchedSegments:
        MutableMap<Int, MutableSet<Int>>
    ): Boolean {

        var matchedAnything =
            false

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

            val candidates =
                findCandidateSegments(
                    network = network,
                    start = trackStart,
                    end = trackEnd
                )

            candidates.forEach { candidate ->

                val feature =
                    network.features[
                        candidate.featureIndex
                    ]

                if (
                    trailSegmentMatchesTrack(
                        trackStart =
                            trackStart,
                        trackEnd =
                            trackEnd,
                        trailStart =
                            feature.points[
                                candidate.segmentIndex
                            ],
                        trailEnd =
                            feature.points[
                                candidate.segmentIndex + 1
                            ]
                    )
                ) {

                    matchedSegments
                        .getOrPut(
                            candidate.featureIndex
                        ) {
                            mutableSetOf()
                        }
                        .add(
                            candidate.segmentIndex
                        )

                    matchedAnything =
                        true
                }
            }
        }

        return matchedAnything
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

        return synchronized(
            networkLock
        ) {

            cachedNetwork
                ?: loadNetwork().also { network ->

                    cachedNetwork =
                        network
                }
        }
    }

    private fun loadNetwork():
            TrailNetwork {

        val features =
            loadFeatures()

        val colorLengths =
            mutableMapOf<String, Double>()

        val trailLengths =
            mutableMapOf<String, Double>()

        /*
         * Kolor szlaku to kolor wspólny
         * dla wszystkich jego odcinków.
         *
         * Odcinek wspólny np. czerwonego
         * i zielonego szlaku ma oba kolory,
         * ale cały czerwony szlak ma
         * tylko czerwony.
         */
        val trailColors =
            mutableMapOf<String, Set<String>>()

        features.forEach { feature ->

            feature.colors.forEach { color ->

                colorLengths[color] =
                    (colorLengths[color] ?: 0.0) +
                            feature.lengthMeters
            }

            feature.names.forEach { name ->

                trailLengths[name] =
                    (trailLengths[name] ?: 0.0) +
                            feature.lengthMeters

                trailColors[name] =
                    trailColors[name]
                        ?.intersect(
                            feature.colors.toSet()
                        )
                        ?: feature.colors.toSet()
            }
        }

        return TrailNetwork(
            features =
                features,

            spatialIndex =
                buildSpatialIndex(
                    features
                ),

            totalLengthMeters =
                features.sumOf {
                    it.lengthMeters
                },

            colorLengths =
                colorLengths,

            trailLengths =
                trailLengths,

            trailColors =
                trailColors.mapValues { (_, colors) ->
                    colors.sortedBy(
                        ::colorSortIndex
                    )
                }
        )
    }

    private fun loadFeatures():
            List<TrailFeature> {

        val result =
            mutableListOf<TrailFeature>()

        appContext.assets
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

        var colors =
            emptyList<String>()

        var names =
            emptyList<String>()

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

                    val properties =
                        readProperties(
                            reader
                        )

                    if (
                        properties.wayId
                            .isNotBlank()
                    ) {
                        wayId =
                            properties.wayId
                    }

                    colors =
                        properties.colors

                    names =
                        properties.names
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

        val densePoints =
            densify(
                finalPoints
            )

        val segmentLengths =
            DoubleArray(
                densePoints.lastIndex
            ) { index ->

                distanceMeters(
                    densePoints[index],
                    densePoints[index + 1]
                )
            }

        return TrailFeature(
            wayId =
                wayId,

            colors =
                colors,

            names =
                names,

            points =
                densePoints,

            segmentLengths =
                segmentLengths,

            lengthMeters =
                segmentLengths.sum()
        )
    }

    private fun readProperties(
        reader: JsonReader
    ): FeatureProperties {

        var wayId =
            ""

        var colors =
            emptyList<String>()

        var names =
            emptyList<String>()

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

                "colors" -> {

                    colors =
                        readStringArray(
                            reader
                        )
                }

                "names" -> {

                    names =
                        readStringArray(
                            reader
                        )
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        return FeatureProperties(
            wayId =
                wayId,

            colors =
                colors,

            names =
                names
        )
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

    private fun readStringArray(
        reader: JsonReader
    ): List<String> {

        if (
            reader.peek() !=
            JsonToken.BEGIN_ARRAY
        ) {
            reader.skipValue()
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        reader.beginArray()

        while (
            reader.hasNext()
        ) {

            readFlexibleString(
                reader
            )
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    result.add(it)
                }
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

    private fun densify(
        points: List<Point>
    ): List<Point> {

        val result =
            mutableListOf(
                points.first()
            )

        for (
        index
        in 0 until points.lastIndex
        ) {

            val start =
                points[index]

            val end =
                points[index + 1]

            val pieces =
                ceil(
                    distanceMeters(
                        start,
                        end
                    ) /
                            maxTrailPieceLengthMeters
                )
                    .toInt()
                    .coerceAtLeast(1)

            /*
             * Na odległościach kilkuset metrów
             * interpolacja liniowa współrzędnych
             * jest wystarczająco dokładna.
             */
            for (
            piece
            in 1 until pieces
            ) {

                val t =
                    piece.toDouble() /
                            pieces

                result.add(
                    Point(
                        lat =
                            start.lat +
                                    (end.lat - start.lat) * t,
                        lon =
                            start.lon +
                                    (end.lon - start.lon) * t
                    )
                )
            }

            result.add(
                end
            )
        }

        return result
    }

    /*
     * =====================================================
     * INDEKS PRZESTRZENNY
     * =====================================================
     */

    private fun buildSpatialIndex(
        features: List<TrailFeature>
    ): Map<CellKey, List<SegmentRef>> {

        val index =
            mutableMapOf<
                    CellKey,
                    MutableList<SegmentRef>
                    >()

        features.forEachIndexed {
                featureIndex,
                feature ->

            for (
            segmentIndex
            in 0 until feature.points.lastIndex
            ) {

                val start =
                    feature.points[
                        segmentIndex
                    ]

                val end =
                    feature.points[
                        segmentIndex + 1
                    ]

                val minX =
                    gridX(
                        min(
                            start.lon,
                            end.lon
                        )
                    )

                val maxX =
                    gridX(
                        max(
                            start.lon,
                            end.lon
                        )
                    )

                val minY =
                    gridY(
                        min(
                            start.lat,
                            end.lat
                        )
                    )

                val maxY =
                    gridY(
                        max(
                            start.lat,
                            end.lat
                        )
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
                                SegmentRef(
                                    featureIndex =
                                        featureIndex,
                                    segmentIndex =
                                        segmentIndex
                                )
                            )
                    }
                }
            }
        }

        return index
    }

    private fun findCandidateSegments(
        network: TrailNetwork,
        start: Point,
        end: Point
    ): Set<SegmentRef> {

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
            mutableSetOf<SegmentRef>()

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
                    ?.let { segments ->

                        candidates.addAll(
                            segments
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
        Map<Int, Set<Int>>,
        matchedTrackCount: Int
    ): TrailMatchResult {

        val featuresJson =
            JSONArray()

        var totalMatchedLength =
            0.0

        var matchedWayCount =
            0

        val colorMatchedLengths =
            mutableMapOf<String, Double>()

        val trailMatchedLengths =
            mutableMapOf<String, Double>()

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
            val featureMatchedLength =
                sorted.sumOf { segmentIndex ->
                    feature.segmentLengths[
                        segmentIndex
                    ]
                }

            totalMatchedLength +=
                featureMatchedLength

            matchedWayCount++

            feature.colors.forEach { color ->

                colorMatchedLengths[color] =
                    (colorMatchedLengths[color] ?: 0.0) +
                            featureMatchedLength
            }

            feature.names.forEach { name ->

                trailMatchedLengths[name] =
                    (trailMatchedLengths[name] ?: 0.0) +
                            featureMatchedLength
            }

            /*
             * Łączymy sąsiednie zaliczone
             * kawałki z powrotem w dłuższe
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

        val colorProgress =
            network.colorLengths
                .map { (color, totalLength) ->

                    TrailColorProgress(
                        color =
                            color,

                        totalLengthMeters =
                            totalLength,

                        completedLengthMeters =
                            colorMatchedLengths[color]
                                ?: 0.0
                    )
                }
                .sortedBy {
                    colorSortIndex(
                        it.color
                    )
                }

        val trailProgress =
            network.trailLengths
                .map { (name, totalLength) ->

                    TrailNameProgress(
                        name =
                            name,

                        colors =
                            network.trailColors[name]
                                .orEmpty(),

                        totalLengthMeters =
                            totalLength,

                        completedLengthMeters =
                            trailMatchedLengths[name]
                                ?: 0.0
                    )
                }
                .sortedWith(
                    compareByDescending<TrailNameProgress> {
                        it.completedFraction
                    }.thenBy {
                        it.name
                    }
                )

        return TrailMatchResult(
            matchedGeoJson =
                collection.toString(),

            totalLengthMeters =
                network.totalLengthMeters,

            matchedLengthMeters =
                totalMatchedLength,

            matchedWayCount =
                matchedWayCount,

            matchedTrackCount =
                matchedTrackCount,

            colorProgress =
                colorProgress,

            trailProgress =
                trailProgress
        )
    }

    private fun colorSortIndex(
        color: String
    ): Int {

        val index =
            colorOrder.indexOf(
                color
            )

        return if (index >= 0) {
            index
        } else {
            colorOrder.size
        }
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

    /*
     * =====================================================
     * SZLAK POD PALCEM (karta szlaku na mapie)
     * =====================================================
     */

    /**
     * Szlaki biegnące odcinkiem najbliższym podanemu miejscu, nie dalej niż
     * [toleranceMeters]. Pusta lista – w pliku szlaków nie ma tam żadnego odcinka.
     * Nie wymaga dopasowania śladów; pierwsze wywołanie wczytuje plik szlaków.
     */
    suspend fun findTrailsNear(
        latitude: Double,
        longitude: Double,
        toleranceMeters: Double
    ): List<TrailInfo> {
        return withContext(Dispatchers.Default) {
            val network = getOrLoadNetwork()

            val feature =
                findNearestFeature(network, Point(latitude, longitude), toleranceMeters)
                    ?: return@withContext emptyList()

            feature.names.distinct().map { name ->
                TrailInfo(
                    name = name,
                    colors = network.trailColors[name].orEmpty(),
                    lengthMeters = network.trailLengths[name] ?: 0.0
                )
            }
        }
    }

    /** Wszystkie odcinki szlaku o podanej nazwie jako FeatureCollection linii. */
    suspend fun trailGeoJson(name: String): String {
        return withContext(Dispatchers.Default) {
            val featuresJson = JSONArray()

            getOrLoadNetwork()
                .features
                .filter { feature -> name in feature.names && feature.points.size >= 2 }
                .forEach { feature ->
                    addMatchedRun(
                        featuresJson = featuresJson,
                        feature = feature,
                        startSegment = 0,
                        endSegment = feature.points.lastIndex - 1
                    )
                }

            JSONObject()
                .put("type", "FeatureCollection")
                .put("features", featuresJson)
                .toString()
        }
    }

    private fun findNearestFeature(
        network: TrailNetwork,
        point: Point,
        toleranceMeters: Double
    ): TrailFeature? {
        val latTolerance = toleranceMeters / 111_320.0

        val lonMetersPerDegree = 111_320.0 * cos(Math.toRadians(point.lat))

        val lonTolerance =
            if (lonMetersPerDegree > 1.0) {
                toleranceMeters / lonMetersPerDegree
            } else {
                latTolerance
            }

        val pointXY = toXY(point, point.lat)

        var nearest: TrailFeature? = null
        var nearestDistance = toleranceMeters

        // Ten sam odcinek leży w kilku komórkach indeksu – liczymy go raz.
        val checked = mutableSetOf<SegmentRef>()

        for (x in gridX(point.lon - lonTolerance)..gridX(point.lon + lonTolerance)) {
            for (y in gridY(point.lat - latTolerance)..gridY(point.lat + latTolerance)) {
                network.spatialIndex[CellKey(x = x, y = y)]?.forEach { ref ->
                    if (!checked.add(ref)) {
                        return@forEach
                    }

                    val feature = network.features[ref.featureIndex]

                    val distance =
                        pointToSegmentDistance(
                            point = pointXY,
                            start = toXY(feature.points[ref.segmentIndex], point.lat),
                            end = toXY(feature.points[ref.segmentIndex + 1], point.lat)
                        )

                    if (distance <= nearestDistance) {
                        nearestDistance = distance
                        nearest = feature
                    }
                }
            }
        }

        return nearest
    }

    companion object {

        /*
         * Sieć jest wspólna dla wszystkich instancji,
         * żeby plik szlaków parsować raz
         * na uruchomienie aplikacji.
         */
        @Volatile
        private var cachedNetwork:
                TrailNetwork? = null

        private val networkLock =
            Any()
    }
}
