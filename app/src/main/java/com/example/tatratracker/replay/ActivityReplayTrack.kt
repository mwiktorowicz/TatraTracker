package com.example.tatratracker.replay

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Punkt śladu do odtwarzania. Brak wartości = null; wysokość 0 i tętno 0 też traktujemy jako brak.
 */
data class ReplayPoint(
    val lat: Double,
    val lon: Double,
    val elevationMeters: Double? = null,
    val timestampSeconds: Long? = null,
    val heartRate: Int? = null,
    val speedMs: Double? = null
)

data class ReplayCoordinate(
    val lat: Double,
    val lon: Double
)

/** Odczyty w bieżącym miejscu odtwarzania. */
data class ReplayReadings(
    val timestampSeconds: Long?,
    /** Dystans od startu z surowych danych – tak samo jak profil w szczegółach aktywności. */
    val distanceMeters: Double,
    val elevationMeters: Double?,
    val heartRate: Int?,
    val speedMs: Double?
)

/** Próbka profilu wysokości; oś pozioma to postęp odtwarzania. */
data class ReplayProfileSample(
    val progressMeters: Double,
    val elevationMeters: Double
)

/** Czas odtworzenia całej wędrówki. */
enum class ReplayTempo(val totalSeconds: Double) {
    SLOW(120.0),
    NORMAL(60.0),
    FAST(30.0)
}

/** O ile metrów postępu przesunąć znacznik po [elapsedNanos] przy danym tempie. */
fun replayStepMeters(totalProgressMeters: Double, tempo: ReplayTempo, elapsedNanos: Long): Double {
    if (totalProgressMeters <= 0.0 || elapsedNanos <= 0L) {
        return 0.0
    }

    return totalProgressMeters * (elapsedNanos / 1_000_000_000.0) / tempo.totalSeconds
}

/**
 * Ślad przygotowany do odtwarzania.
 *
 * Postęp odtwarzania liczony jest po dystansie z filtrem postojów: dystans narasta dopiero,
 * gdy pozycja oddali się o co najmniej [MIN_STEP_METERS] od ostatniego zaliczonego punktu
 * („kotwicy”). Dzięki temu drgania GPS na postoju nie przesuwają postępu i znacznik jedzie
 * ze stałą prędkością wzdłuż trasy. Odczyt dystansu pochodzi z surowych danych (wszystkie
 * punkty), żeby liczby zgadzały się ze szczegółami aktywności.
 */
class ActivityReplayTrack private constructor(
    private val points: List<ReplayPoint>,
    /** Surowy dystans narastający dla każdego punktu. */
    private val rawDistances: DoubleArray,
    /** Indeksy punktów zaliczonych przez filtr (pierwszy i ostatni zawsze). */
    private val anchorIndices: IntArray,
    /** Postęp (dystans po filtrze) w każdej kotwicy; niemalejący. */
    private val anchorProgress: DoubleArray,
    /** Uproszczony ślad do rysowania – podzbiór kotwic. */
    val path: List<ReplayCoordinate>,
    /** Postęp w każdym punkcie [path]. */
    val pathProgress: DoubleArray,
    /** Profil wysokości (pusty, gdy ślad nie ma wysokości). */
    val profile: List<ReplayProfileSample>
) {

    /** Długość odtwarzania (dystans po filtrze postojów). */
    val totalProgressMeters: Double = anchorProgress.last()

    /** Surowy dystans całego śladu. */
    val totalDistanceMeters: Double = rawDistances.last()

    val start: ReplayCoordinate = points.first().toCoordinate()

    val end: ReplayCoordinate = points.last().toCoordinate()

    fun clampProgress(progressMeters: Double): Double {
        if (progressMeters.isNaN()) {
            return 0.0
        }

        return progressMeters.coerceIn(0.0, totalProgressMeters)
    }

    /** Pozycja znacznika: interpolacja między sąsiednimi kotwicami. */
    fun positionAt(progressMeters: Double): ReplayCoordinate {
        val segment = anchorSegmentAt(progressMeters)
        val from = points[anchorIndices[segment.index]]
        val to = points[anchorIndices[segment.index + 1]]

        return ReplayCoordinate(
            lat = from.lat + (to.lat - from.lat) * segment.fraction,
            lon = from.lon + (to.lon - from.lon) * segment.fraction
        )
    }

    /**
     * Odczyty z pełnych danych. Między dwiema kotwicami surowy dystans rośnie proporcjonalnie
     * do postępu; punkt odczytu to ostatni punkt śladu, który ten dystans już osiągnął.
     * Tętno i prędkość z najbliższego w czasie punktu, który je ma, nie dalej niż
     * [READING_WINDOW_SECONDS].
     */
    fun readingsAt(progressMeters: Double): ReplayReadings {
        val segment = anchorSegmentAt(progressMeters)
        val fromIndex = anchorIndices[segment.index]
        val toIndex = anchorIndices[segment.index + 1]

        val distance =
            if (segment.fraction >= 1.0) {
                rawDistances[toIndex]
            } else {
                rawDistances[fromIndex] +
                    (rawDistances[toIndex] - rawDistances[fromIndex]) * segment.fraction
            }

        val index = lastIndexAtOrBelow(rawDistances, distance, fromIndex, toIndex)
        val point = points[index]

        return ReplayReadings(
            timestampSeconds = point.timestampSeconds,
            distanceMeters = distance,
            elevationMeters = point.elevationMeters?.takeIf { it.isFinite() && it > 0.0 },
            heartRate = nearestValue(index) { candidate -> candidate.heartRate?.takeIf { it > 0 } },
            speedMs = nearestValue(index) { candidate ->
                candidate.speedMs?.takeIf { it.isFinite() && it >= 0.0 }
            }
        )
    }

    /** Ostatni punkt [path], do którego dotarł znacznik. */
    fun pathIndexAt(progressMeters: Double): Int {
        val progress = clampProgress(progressMeters)

        return (upperBound(pathProgress, progress) - 1).coerceIn(0, pathProgress.lastIndex)
    }

    /** Wysokość na profilu w danym miejscu (interpolowana), do kursora na wykresie. */
    fun profileElevationAt(progressMeters: Double): Double? {
        if (profile.isEmpty()) {
            return null
        }

        val progress = progressMeters.coerceIn(
            profile.first().progressMeters,
            profile.last().progressMeters
        )

        var low = 0
        var high = profile.lastIndex
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (profile[middle].progressMeters <= progress) {
                low = middle
            } else {
                high = middle
            }
        }

        val from = profile[low]
        val to = profile[high]
        val length = to.progressMeters - from.progressMeters
        if (length <= 0.0) {
            return to.elevationMeters
        }

        val fraction = ((progress - from.progressMeters) / length).coerceIn(0.0, 1.0)

        return from.elevationMeters + (to.elevationMeters - from.elevationMeters) * fraction
    }

    private class AnchorSegment(
        val index: Int,
        val fraction: Double
    )

    private fun anchorSegmentAt(progressMeters: Double): AnchorSegment {
        val progress = clampProgress(progressMeters)

        val index = (upperBound(anchorProgress, progress) - 1).coerceIn(0, anchorProgress.size - 2)
        val length = anchorProgress[index + 1] - anchorProgress[index]

        val fraction =
            if (length <= 0.0) {
                1.0
            } else {
                ((progress - anchorProgress[index]) / length).coerceIn(0.0, 1.0)
            }

        return AnchorSegment(index, fraction)
    }

    private inline fun <T : Any> nearestValue(index: Int, value: (ReplayPoint) -> T?): T? {
        value(points[index])?.let { return it }

        val time = points[index].timestampSeconds ?: return null

        var best: T? = null
        var bestGap = Long.MAX_VALUE

        var before = index - 1
        while (before >= 0) {
            val candidateTime = points[before].timestampSeconds
            if (candidateTime != null) {
                val gap = abs(time - candidateTime)
                if (gap > READING_WINDOW_SECONDS) {
                    break
                }

                val candidate = value(points[before])
                if (candidate != null) {
                    best = candidate
                    bestGap = gap
                    break
                }
            }
            before--
        }

        var after = index + 1
        while (after <= points.lastIndex) {
            val candidateTime = points[after].timestampSeconds
            if (candidateTime != null) {
                val gap = abs(candidateTime - time)
                if (gap > READING_WINDOW_SECONDS || gap >= bestGap) {
                    break
                }

                val candidate = value(points[after])
                if (candidate != null) {
                    best = candidate
                    break
                }
            }
            after++
        }

        return best
    }

    companion object {

        /** Minimalne oddalenie od ostatniego zaliczonego punktu, od którego rośnie postęp. */
        const val MIN_STEP_METERS = 8.0

        /** Jak daleko w czasie szukać tętna i prędkości, gdy bieżący punkt ich nie ma. */
        const val READING_WINDOW_SECONDS = 90L

        /** Górna granica liczby punktów rysowanego śladu. */
        const val MAX_PATH_POINTS = 2000

        /** Górna granica liczby próbek profilu wysokości. */
        const val MAX_PROFILE_SAMPLES = 600

        /** Zwraca null, gdy punktów jest mniej niż dwa. */
        fun build(points: List<ReplayPoint>): ActivityReplayTrack? {
            if (points.size < 2) {
                return null
            }

            val rawDistances = DoubleArray(points.size)
            for (index in 1..points.lastIndex) {
                rawDistances[index] =
                    rawDistances[index - 1] + distanceMeters(points[index - 1], points[index])
            }

            val anchorIndices = ArrayList<Int>()
            val anchorProgress = ArrayList<Double>()
            anchorIndices.add(0)
            anchorProgress.add(0.0)

            var anchor = points.first()
            var progress = 0.0

            for (index in 1..points.lastIndex) {
                val step = distanceMeters(anchor, points[index])
                if (step >= MIN_STEP_METERS) {
                    progress += step
                    anchor = points[index]
                    anchorIndices.add(index)
                    anchorProgress.add(progress)
                }
            }

            // Meta zawsze jest kotwicą, żeby odtwarzanie kończyło się w ostatnim punkcie
            // (także z ostatnią godziną, gdy ślad kończy się postojem).
            if (anchorIndices.last() != points.lastIndex) {
                progress += distanceMeters(anchor, points.last())
                anchorIndices.add(points.lastIndex)
                anchorProgress.add(progress)
            }

            val anchorIndexArray = anchorIndices.toIntArray()
            val anchorProgressArray = anchorProgress.toDoubleArray()

            val pathAnchors = simplifiedAnchors(anchorProgressArray)

            return ActivityReplayTrack(
                points = points,
                rawDistances = rawDistances,
                anchorIndices = anchorIndexArray,
                anchorProgress = anchorProgressArray,
                path = pathAnchors.map { anchorIndex ->
                    points[anchorIndexArray[anchorIndex]].toCoordinate()
                },
                pathProgress = DoubleArray(pathAnchors.size) { position ->
                    anchorProgressArray[pathAnchors[position]]
                },
                profile = buildProfile(points, anchorIndexArray, anchorProgressArray)
            )
        }

        /**
         * Przerzedza kotwice po postępie: kolejny punkt co najmniej co 1/[MAX_PATH_POINTS]
         * długości. Pierwsza i ostatnia kotwica zostają.
         */
        private fun simplifiedAnchors(anchorProgress: DoubleArray): List<Int> {
            val minGap = anchorProgress.last() / MAX_PATH_POINTS
            val kept = ArrayList<Int>()
            kept.add(0)

            var lastKeptProgress = 0.0
            for (index in 1 until anchorProgress.lastIndex) {
                if (anchorProgress[index] - lastKeptProgress >= minGap) {
                    kept.add(index)
                    lastKeptProgress = anchorProgress[index]
                }
            }

            kept.add(anchorProgress.lastIndex)

            return kept
        }

        /**
         * Wysokość z kotwic (bez zer), wygładzona oknem 5 próbek (jak profil w szczegółach
         * aktywności, od 5 próbek), przerzedzona do limitu.
         */
        private fun buildProfile(
            points: List<ReplayPoint>,
            anchorIndices: IntArray,
            anchorProgress: DoubleArray
        ): List<ReplayProfileSample> {
            val raw = ArrayList<ReplayProfileSample>()
            for (anchor in anchorIndices.indices) {
                val elevation = points[anchorIndices[anchor]].elevationMeters
                if (elevation != null && elevation.isFinite() && elevation > 0.0) {
                    raw.add(ReplayProfileSample(anchorProgress[anchor], elevation))
                }
            }

            if (raw.size < 2) {
                return emptyList()
            }

            if (raw.size < 5) {
                return raw
            }

            val smoothed = raw.mapIndexed { index, sample ->
                val from = (index - 2).coerceAtLeast(0)
                val to = (index + 2).coerceAtMost(raw.lastIndex)
                var sum = 0.0
                for (window in from..to) {
                    sum += raw[window].elevationMeters
                }
                sample.copy(elevationMeters = sum / (to - from + 1))
            }

            if (smoothed.size <= MAX_PROFILE_SAMPLES) {
                return smoothed
            }

            val stride = ceil(smoothed.size / MAX_PROFILE_SAMPLES.toDouble()).toInt()
            val sampled = smoothed.filterIndexed { index, _ -> index % stride == 0 }.toMutableList()
            if (sampled.last() != smoothed.last()) {
                sampled.add(smoothed.last())
            }

            return sampled
        }

        private fun ReplayPoint.toCoordinate(): ReplayCoordinate {
            return ReplayCoordinate(lat, lon)
        }

        /** Pierwszy indeks z wartością większą niż [value] (tablica niemalejąca). */
        private fun upperBound(values: DoubleArray, value: Double): Int {
            var low = 0
            var high = values.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (values[middle] <= value) {
                    low = middle + 1
                } else {
                    high = middle
                }
            }
            return low
        }

        /** Największy indeks z przedziału [from, to], którego wartość nie przekracza [value]. */
        private fun lastIndexAtOrBelow(values: DoubleArray, value: Double, from: Int, to: Int): Int {
            var low = from
            var high = to
            while (low < high) {
                val middle = (low + high + 1) ushr 1
                if (values[middle] <= value) {
                    low = middle
                } else {
                    high = middle - 1
                }
            }
            return low
        }

        // Ten sam wzór i promień Ziemi co profil w szczegółach aktywności – dystanse się zgadzają.
        private fun distanceMeters(first: ReplayPoint, second: ReplayPoint): Double {
            val earthRadiusMeters = 6_371_000.0

            val lat1 = Math.toRadians(first.lat)
            val lat2 = Math.toRadians(second.lat)
            val deltaLat = Math.toRadians(second.lat - first.lat)
            val deltaLon = Math.toRadians(second.lon - first.lon)

            val a =
                sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
                    cos(lat1) * cos(lat2) * sin(deltaLon / 2.0) * sin(deltaLon / 2.0)

            return earthRadiusMeters * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        }
    }
}
