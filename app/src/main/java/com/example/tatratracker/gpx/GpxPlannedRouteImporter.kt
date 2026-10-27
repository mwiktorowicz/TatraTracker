package com.example.tatratracker.gpx

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.example.tatratracker.data.PlannedRouteDraft
import com.example.tatratracker.data.PlannedRouteDraftPoint
import com.example.tatratracker.data.PlannedRoutePointType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

class GpxPlannedRouteImporter(
    private val context: Context
) {

    suspend fun importRoute(
        uri: Uri
    ): Result<GpxPlannedRouteImportResult> {

        return withContext(
            Dispatchers.IO
        ) {

            runCatching {

                val fileName =
                    resolveFileName(
                        uri
                    )

                val inputStream =
                    context
                        .contentResolver
                        .openInputStream(
                            uri
                        )
                        ?: error(
                            "Nie udało się otworzyć wybranego pliku."
                        )

                val parsed =
                    inputStream.use {
                            stream ->

                        val parser =
                            Xml.newPullParser()

                        parser.setFeature(
                            XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                            true
                        )

                        parser.setInput(
                            stream,
                            "UTF-8"
                        )

                        parseGpx(
                            parser = parser,
                            fallbackFileName = fileName
                        )
                    }

                GpxPlannedRouteImportResult(
                    fileName = fileName,
                    draft = parsed.draft,
                    geometryPointCount =
                        parsed.geometryPointCount,
                    waypointCount =
                        parsed.waypointCount
                )
            }
        }
    }

    private fun parseGpx(
        parser: XmlPullParser,
        fallbackFileName: String
    ): ParsedGpxRoute {

        val trackPoints =
            mutableListOf<GpxPoint>()

        val routePoints =
            mutableListOf<GpxPoint>()

        val waypoints =
            mutableListOf<GpxPoint>()

        var metadataName: String? =
            null

        var trackName: String? =
            null

        var routeName: String? =
            null

        var insideMetadata =
            false

        var insideTrack =
            false

        var insideRoute =
            false

        var currentPoint:
                MutableGpxPoint? =
            null

        var currentPointKind:
                GpxPointKind? =
            null

        var eventType =
            parser.eventType

        while (
            eventType !=
            XmlPullParser.END_DOCUMENT
        ) {

            when (
                eventType
            ) {

                XmlPullParser.START_TAG -> {

                    when (
                        parser.name
                            .localTagName()
                    ) {

                        "metadata" -> {

                            insideMetadata =
                                true
                        }

                        "trk" -> {

                            insideTrack =
                                true
                        }

                        "rte" -> {

                            insideRoute =
                                true
                        }

                        "trkpt" -> {

                            currentPoint =
                                readPointAttributes(
                                    parser
                                )

                            currentPointKind =
                                GpxPointKind.TRACK
                        }

                        "rtept" -> {

                            currentPoint =
                                readPointAttributes(
                                    parser
                                )

                            currentPointKind =
                                GpxPointKind.ROUTE
                        }

                        "wpt" -> {

                            currentPoint =
                                readPointAttributes(
                                    parser
                                )

                            currentPointKind =
                                GpxPointKind.WAYPOINT
                        }

                        "name" -> {

                            val value =
                                parser
                                    .nextText()
                                    .trim()

                            if (
                                value.isNotBlank()
                            ) {

                                when {

                                    currentPoint !=
                                            null -> {

                                        currentPoint.name =
                                            value
                                    }

                                    insideMetadata -> {

                                        if (
                                            metadataName ==
                                            null
                                        ) {

                                            metadataName =
                                                value
                                        }
                                    }

                                    insideTrack -> {

                                        if (
                                            trackName ==
                                            null
                                        ) {

                                            trackName =
                                                value
                                        }
                                    }

                                    insideRoute -> {

                                        if (
                                            routeName ==
                                            null
                                        ) {

                                            routeName =
                                                value
                                        }
                                    }
                                }
                            }
                        }

                        "ele" -> {

                            currentPoint
                                ?.altitudeMeters =
                                parser
                                    .nextText()
                                    .trim()
                                    .toDoubleOrNull()
                        }

                        "time" -> {

                            currentPoint
                                ?.time =
                                parser
                                    .nextText()
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                        }

                        "type" -> {

                            if (
                                currentPoint !=
                                null
                            ) {

                                currentPoint
                                    ?.type =
                                    parser
                                        .nextText()
                                        .trim()
                                        .takeIf {
                                            it.isNotBlank()
                                        }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    when (
                        parser.name
                            .localTagName()
                    ) {

                        "trkpt",
                        "rtept",
                        "wpt" -> {

                            val finishedPoint =
                                currentPoint
                                    ?.toImmutable()

                            if (
                                finishedPoint !=
                                null
                            ) {

                                when (
                                    currentPointKind
                                ) {

                                    GpxPointKind.TRACK ->

                                        trackPoints +=
                                            finishedPoint

                                    GpxPointKind.ROUTE ->

                                        routePoints +=
                                            finishedPoint

                                    GpxPointKind.WAYPOINT ->

                                        waypoints +=
                                            finishedPoint

                                    null ->
                                        Unit
                                }
                            }

                            currentPoint =
                                null

                            currentPointKind =
                                null
                        }

                        "metadata" -> {

                            insideMetadata =
                                false
                        }

                        "trk" -> {

                            insideTrack =
                                false
                        }

                        "rte" -> {

                            insideRoute =
                                false
                        }
                    }
                }
            }

            eventType =
                parser.next()
        }

        val geometry =
            when {

                trackPoints.size >=
                        2 ->

                    trackPoints

                routePoints.size >=
                        2 ->

                    routePoints

                else ->

                    error(
                        "Plik GPX nie zawiera poprawnej trasy. Potrzebne są co najmniej 2 punkty trkpt lub rtept."
                    )
            }

        val sourceName =
            when {

                trackPoints.size >=
                        2 ->
                    trackName

                else ->
                    routeName
            }

        val cleanFileName =
            fallbackFileName
                .substringBeforeLast(
                    '.'
                )
                .trim()

        val plannedRouteName =
            metadataName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: sourceName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                ?: cleanFileName
                    .takeIf {
                        it.isNotBlank()
                    }
                ?: "Importowana trasa GPX"

        return buildDraft(
            routeName =
                plannedRouteName,
            geometry =
                geometry,
            routePoints =
                routePoints,
            waypoints =
                waypoints
        )
    }

    private fun buildDraft(
        routeName: String,
        geometry: List<GpxPoint>,
        routePoints: List<GpxPoint>,
        waypoints: List<GpxPoint>
    ): ParsedGpxRoute {

        val cumulativeDistances =
            calculateCumulativeDistances(
                geometry
            )

        val totalDistanceMeters =
            cumulativeDistances
                .lastOrNull()
                ?: 0.0

        val typedStart =
            waypoints.firstOrNull {
                it.type.isStartType()
            }

        val typedEnd =
            waypoints.firstOrNull {
                it.type.isEndType()
            }

        val typedIntermediateWaypoints =
            waypoints.filter {
                it.type.isWaypointType()
            }

        val start =
            typedStart
                ?: geometry.first()

        val end =
            typedEnd
                ?: geometry.last()

        val intermediateWaypoints =
            when {

                typedIntermediateWaypoints
                    .isNotEmpty() ->

                    typedIntermediateWaypoints

                routePoints.size in
                        3..50 ->

                    routePoints
                        .drop(1)
                        .dropLast(1)
                        .filter {
                            !it.name.isNullOrBlank()
                        }

                else ->
                    emptyList()
            }

        val elevationValues =
            geometry
                .mapNotNull {
                    it.altitudeMeters
                }

        val minElevation =
            elevationValues
                .minOrNull()
                ?.roundToInt()

        val maxElevation =
            elevationValues
                .maxOrNull()
                ?.roundToInt()

        val elevationChange =
            calculateElevationChange(
                geometry
            )

        val durationSeconds =
            calculateDurationSeconds(
                geometry
            )

        val draftPoints =
            mutableListOf<
                    PlannedRouteDraftPoint
                    >()

        geometry.forEachIndexed {
                index,
                point ->

            draftPoints +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.GEOMETRY,
                    orderIndex =
                        index,
                    latitude =
                        point.latitude,
                    longitude =
                        point.longitude,
                    altitudeMeters =
                        point.altitudeMeters,
                    distanceMeters =
                        cumulativeDistances[index]
                )
        }

        draftPoints +=
            PlannedRouteDraftPoint(
                pointType =
                    PlannedRoutePointType.START,
                orderIndex =
                    0,
                latitude =
                    start.latitude,
                longitude =
                    start.longitude,
                altitudeMeters =
                    start.altitudeMeters,
                name =
                    start.name
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Start GPX"
            )

        intermediateWaypoints
            .forEachIndexed {
                    index,
                    waypoint ->

                draftPoints +=
                    PlannedRouteDraftPoint(
                        pointType =
                            PlannedRoutePointType.WAYPOINT,
                        orderIndex =
                            index,
                        latitude =
                            waypoint.latitude,
                        longitude =
                            waypoint.longitude,
                        altitudeMeters =
                            waypoint.altitudeMeters,
                        name =
                            waypoint.name
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Przystanek ${index + 1}"
                    )
            }

        draftPoints +=
            PlannedRouteDraftPoint(
                pointType =
                    PlannedRoutePointType.END,
                orderIndex =
                    0,
                latitude =
                    end.latitude,
                longitude =
                    end.longitude,
                altitudeMeters =
                    end.altitudeMeters,
                name =
                    end.name
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Cel GPX"
            )

        geometry.forEachIndexed {
                index,
                point ->

            val altitude =
                point.altitudeMeters
                    ?: return@forEachIndexed

            draftPoints +=
                PlannedRouteDraftPoint(
                    pointType =
                        PlannedRoutePointType.ELEVATION,
                    orderIndex =
                        index,
                    latitude =
                        point.latitude,
                    longitude =
                        point.longitude,
                    altitudeMeters =
                        altitude,
                    distanceMeters =
                        cumulativeDistances[index]
                )
        }

        val draft =
            PlannedRouteDraft(
                name =
                    routeName,
                distanceMeters =
                    totalDistanceMeters,
                durationSeconds =
                    durationSeconds,
                ascentMeters =
                    elevationChange.first,
                descentMeters =
                    elevationChange.second,
                minElevationMeters =
                    minElevation,
                maxElevationMeters =
                    maxElevation,
                startName =
                    start.name
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Start GPX",
                endName =
                    end.name
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Cel GPX",
                points =
                    draftPoints
            )

        return ParsedGpxRoute(
            draft =
                draft,
            geometryPointCount =
                geometry.size,
            waypointCount =
                intermediateWaypoints.size
        )
    }

    private fun calculateCumulativeDistances(
        points: List<GpxPoint>
    ): List<Double> {

        if (
            points.isEmpty()
        ) {

            return emptyList()
        }

        val result =
            MutableList(
                points.size
            ) {
                0.0
            }

        var total =
            0.0

        for (
        index in
        1 until points.size
        ) {

            total +=
                distanceMeters(
                    first =
                        points[index - 1],
                    second =
                        points[index]
                )

            result[index] =
                total
        }

        return result
    }

    private fun calculateElevationChange(
        points: List<GpxPoint>
    ): Pair<Int?, Int?> {

        val elevations =
            points.mapNotNull {
                it.altitudeMeters
            }

        if (
            elevations.size <
            2
        ) {

            return null to
                    null
        }

        var ascent =
            0.0

        var descent =
            0.0

        var previousElevation =
            elevations.first()

        elevations
            .drop(1)
            .forEach {
                    currentElevation ->

                val difference =
                    currentElevation -
                            previousElevation

                if (
                    abs(
                        difference
                    ) >= 2.0
                ) {

                    if (
                        difference >
                        0.0
                    ) {

                        ascent +=
                            difference

                    } else {

                        descent +=
                            -difference
                    }
                }

                previousElevation =
                    currentElevation
            }

        return ascent
            .roundToInt() to
                descent
                    .roundToInt()
    }

    private fun calculateDurationSeconds(
        points: List<GpxPoint>
    ): Int {

        val firstTime =
            points
                .firstNotNullOfOrNull {
                    parseTimeSeconds(
                        it.time
                    )
                }
                ?: return 0

        val lastTime =
            points
                .asReversed()
                .firstNotNullOfOrNull {
                    parseTimeSeconds(
                        it.time
                    )
                }
                ?: return 0

        val difference =
            lastTime -
                    firstTime

        if (
            difference <= 0L
        ) {

            return 0
        }

        return difference
            .coerceAtMost(
                Int.MAX_VALUE.toLong()
            )
            .toInt()
    }

    private fun parseTimeSeconds(
        value: String?
    ): Long? {

        if (
            value.isNullOrBlank()
        ) {

            return null
        }

        return runCatching {

            Instant
                .parse(
                    value
                )
                .epochSecond

        }.recoverCatching {

            OffsetDateTime
                .parse(
                    value
                )
                .toInstant()
                .epochSecond

        }.getOrNull()
    }

    private fun distanceMeters(
        first: GpxPoint,
        second: GpxPoint
    ): Double {

        val earthRadiusMeters =
            6_371_000.0

        val latitude1 =
            Math.toRadians(
                first.latitude
            )

        val latitude2 =
            Math.toRadians(
                second.latitude
            )

        val deltaLatitude =
            Math.toRadians(
                second.latitude -
                        first.latitude
            )

        val deltaLongitude =
            Math.toRadians(
                second.longitude -
                        first.longitude
            )

        val a =
            sin(
                deltaLatitude /
                        2.0
            ) *
                    sin(
                        deltaLatitude /
                                2.0
                    ) +
                    cos(
                        latitude1
                    ) *
                    cos(
                        latitude2
                    ) *
                    sin(
                        deltaLongitude /
                                2.0
                    ) *
                    sin(
                        deltaLongitude /
                                2.0
                    )

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

        return earthRadiusMeters *
                c
    }

    private fun readPointAttributes(
        parser: XmlPullParser
    ): MutableGpxPoint {

        val latitude =
            parser
                .getAttributeValue(
                    null,
                    "lat"
                )
                ?.toDoubleOrNull()
                ?: error(
                    "Punkt GPX nie zawiera poprawnej szerokości geograficznej."
                )

        val longitude =
            parser
                .getAttributeValue(
                    null,
                    "lon"
                )
                ?.toDoubleOrNull()
                ?: error(
                    "Punkt GPX nie zawiera poprawnej długości geograficznej."
                )

        require(
            latitude in
                    -90.0..90.0
        ) {
            "Niepoprawna szerokość geograficzna w pliku GPX."
        }

        require(
            longitude in
                    -180.0..180.0
        ) {
            "Niepoprawna długość geograficzna w pliku GPX."
        }

        return MutableGpxPoint(
            latitude =
                latitude,
            longitude =
                longitude
        )
    }

    private fun resolveFileName(
        uri: Uri
    ): String {

        val cursor =
            context
                .contentResolver
                .query(
                    uri,
                    arrayOf(
                        OpenableColumns.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )

        cursor?.use {

            if (
                it.moveToFirst()
            ) {

                val index =
                    it.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (
                    index >= 0
                ) {

                    val value =
                        it.getString(
                            index
                        )

                    if (
                        !value.isNullOrBlank()
                    ) {

                        return value
                    }
                }
            }
        }

        return uri
            .lastPathSegment
            ?.substringAfterLast(
                '/'
            )
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "importowana_trasa.gpx"
    }
}

data class GpxPlannedRouteImportResult(
    val fileName: String,
    val draft: PlannedRouteDraft,
    val geometryPointCount: Int,
    val waypointCount: Int
)

private data class ParsedGpxRoute(
    val draft: PlannedRouteDraft,
    val geometryPointCount: Int,
    val waypointCount: Int
)

private enum class GpxPointKind {
    TRACK,
    ROUTE,
    WAYPOINT
}

private data class GpxPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val time: String?,
    val name: String?,
    val type: String?
)

private data class MutableGpxPoint(
    val latitude: Double,
    val longitude: Double,
    var altitudeMeters: Double? = null,
    var time: String? = null,
    var name: String? = null,
    var type: String? = null
) {

    fun toImmutable():
            GpxPoint {

        return GpxPoint(
            latitude =
                latitude,
            longitude =
                longitude,
            altitudeMeters =
                altitudeMeters,
            time =
                time,
            name =
                name,
            type =
                type
        )
    }
}

private fun String?.isStartType():
        Boolean {

    val value =
        this
            ?.trim()
            ?.lowercase()
            ?: return false

    return value in
            setOf(
                "start",
                "begin",
                "departure"
            )
}

private fun String?.isEndType():
        Boolean {

    val value =
        this
            ?.trim()
            ?.lowercase()
            ?: return false

    return value in
            setOf(
                "finish",
                "end",
                "destination"
            )
}

private fun String?.isWaypointType():
        Boolean {

    val value =
        this
            ?.trim()
            ?.lowercase()
            ?: return false

    return value in
            setOf(
                "waypoint",
                "via",
                "intermediate"
            )
}

private fun String?.localTagName():
        String {

    return this
        ?.substringAfterLast(
            ':'
        )
        ?.lowercase()
        .orEmpty()
}