package com.example.tatratracker.replay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityReplayTrackTest {

    // Ten sam promień Ziemi co w ActivityReplayTrack: 1° szerokości = ok. 111 195 m.
    private val metersPerDegreeLat = 6_371_000.0 * Math.PI / 180.0
    private val baseLat = 49.2
    private val baseLon = 19.95
    private val metersPerDegreeLon = metersPerDegreeLat * Math.cos(Math.toRadians(baseLat))

    private fun point(
        northMeters: Double,
        eastMeters: Double = 0.0,
        elevation: Double? = null,
        time: Long? = null,
        heartRate: Int? = null,
        speed: Double? = null
    ): ReplayPoint {
        return ReplayPoint(
            lat = baseLat + northMeters / metersPerDegreeLat,
            lon = baseLon + eastMeters / metersPerDegreeLon,
            elevationMeters = elevation,
            timestampSeconds = time,
            heartRate = heartRate,
            speedMs = speed
        )
    }

    /** Drgania GPS do 3 m wokół miejsca (north, 0) – żadne dwa punkty nie są dalej niż 8 m. */
    private fun jitterAround(northMeters: Double, count: Int): List<ReplayPoint> {
        val offsets = listOf(3.0 to 0.0, 0.0 to 3.0, -3.0 to 0.0, 0.0 to -3.0, 2.0 to 2.0, -2.0 to -2.0)
        return List(count) { index ->
            val (north, east) = offsets[index % offsets.size]
            point(northMeters + north, east)
        }
    }

    @Test
    fun build_needsAtLeastTwoPoints() {
        assertNull(ActivityReplayTrack.build(emptyList()))
        assertNull(ActivityReplayTrack.build(listOf(point(0.0))))
        assertNotNull(ActivityReplayTrack.build(listOf(point(0.0), point(10.0))))
    }

    @Test
    fun jitterAroundOnePlace_doesNotAdvanceProgress() {
        val points = listOf(point(0.0)) + jitterAround(0.0, 300) + point(0.0)

        val track = ActivityReplayTrack.build(points)!!

        assertEquals(0.0, track.totalProgressMeters, 1e-9)
        // Surowy dystans zawiera drgania – filtr dotyczy tylko postępu odtwarzania.
        assertTrue(track.totalDistanceMeters > 500.0)

        val start = points.first()
        val position = track.positionAt(track.totalProgressMeters)
        assertEquals(start.lat, position.lat, 1e-12)
        assertEquals(start.lon, position.lon, 1e-12)
    }

    @Test
    fun stopInTheMiddle_progressFollowsRouteAndRawDistanceKeepsJitter() {
        // Co 10 m (każdy punkt zaliczony), postój z drganiami w 500 m, dalej do 1000 m.
        val points =
            (0..50).map { step -> point(step * 10.0) } +
                jitterAround(500.0, 300) +
                (51..100).map { step -> point(step * 10.0) }

        val track = ActivityReplayTrack.build(points)!!

        assertEquals(1000.0, track.totalProgressMeters, 0.5)
        assertTrue(track.totalDistanceMeters > 1500.0)

        // Na końcu odczyt dystansu równa się surowemu dystansowi całego śladu.
        val atEnd = track.readingsAt(track.totalProgressMeters)
        assertEquals(track.totalDistanceMeters, atEnd.distanceMeters, 1e-9)
    }

    @Test
    fun movingForward_progressGrowsOnlyAfterEightMeters() {
        // Co 3 m: zaliczone 0, 9 i 18 m (pierwsze co najmniej 8 m od poprzedniego), meta 21 m.
        val points = (0..7).map { step -> point(step * 3.0) }

        val track = ActivityReplayTrack.build(points)!!

        assertEquals(21.0, track.totalProgressMeters, 0.01)
        assertEquals(4, track.path.size)
        assertEquals(9.0, track.pathProgress[1], 0.01)
        assertEquals(18.0, track.pathProgress[2], 0.01)
    }

    @Test
    fun positionAt_interpolatesAndClamps() {
        val start = point(0.0)
        val end = point(100.0)
        val track = ActivityReplayTrack.build(listOf(start, end))!!

        val middle = track.positionAt(track.totalProgressMeters / 2)
        assertEquals((start.lat + end.lat) / 2, middle.lat, 1e-9)

        assertEquals(start.lat, track.positionAt(-50.0).lat, 1e-12)
        assertEquals(end.lat, track.positionAt(1_000_000.0).lat, 1e-12)
        assertEquals(start.lat, track.positionAt(Double.NaN).lat, 1e-12)
    }

    @Test
    fun readings_comeFromPointReachedByDistance() {
        val points = (0..10).map { index ->
            point(index * 10.0, elevation = 1000.0 + index, time = 1_000L + index)
        }
        val track = ActivityReplayTrack.build(points)!!

        val readings = track.readingsAt(31.0)

        assertEquals(1_003L, readings.timestampSeconds)
        assertEquals(1003.0, readings.elevationMeters!!, 1e-9)
        assertEquals(31.0, readings.distanceMeters, 0.01)
    }

    @Test
    fun heartRateAndSpeed_fromNearestPointWithinNinetySeconds() {
        val points = (0..200).map { index ->
            point(
                northMeters = index * 10.0,
                time = index.toLong(),
                heartRate = if (index == 0) 120 else if (index == 200) 150 else null,
                speed = if (index == 0) 1.5 else null
            )
        }
        val track = ActivityReplayTrack.build(points)!!

        // 60 s po pierwszym pomiarze, 140 s przed drugim.
        val near = track.readingsAt(60 * 10.0 + 1.0)
        assertEquals(120, near.heartRate)
        assertEquals(1.5, near.speedMs!!, 1e-9)

        // 100 s od obu pomiarów – poza oknem.
        val far = track.readingsAt(100 * 10.0 + 1.0)
        assertNull(far.heartRate)
        assertNull(far.speedMs)

        // Bliżej drugiego pomiaru.
        val late = track.readingsAt(150 * 10.0 + 1.0)
        assertEquals(150, late.heartRate)
    }

    @Test
    fun missingValues_giveNullInsteadOfError() {
        val points = (0..10).map { index -> point(index * 10.0, elevation = if (index == 5) 0.0 else null) }
        val track = ActivityReplayTrack.build(points)!!

        val readings = track.readingsAt(51.0)

        assertNull(readings.timestampSeconds)
        assertNull(readings.elevationMeters)
        assertNull(readings.heartRate)
        assertNull(readings.speedMs)
        assertTrue(track.profile.isEmpty())
        assertNull(track.profileElevationAt(50.0))
    }

    @Test
    fun longTrack_isSimplifiedForDrawing() {
        // 30 km co 1 m (jak trening z zegarka, 1 punkt/s).
        val points = (0..30_000).map { meter -> point(meter.toDouble(), elevation = 1000.0 + meter / 100.0) }

        val track = ActivityReplayTrack.build(points)!!

        assertTrue(track.path.size in 1500..ActivityReplayTrack.MAX_PATH_POINTS + 1)
        assertEquals(points.first().lat, track.path.first().lat, 1e-12)
        assertEquals(points.last().lat, track.path.last().lat, 1e-12)
        assertTrue(track.profile.size in 2..ActivityReplayTrack.MAX_PROFILE_SAMPLES + 1)
        assertEquals(track.totalProgressMeters, track.profile.last().progressMeters, 1e-9)
    }

    @Test
    fun pathIndexAt_findsLastReachedPoint() {
        // Punkty rysowanego śladu w 0, 9, 18 i 21 m.
        val points = (0..7).map { step -> point(step * 3.0) }
        val track = ActivityReplayTrack.build(points)!!

        assertEquals(0, track.pathIndexAt(0.0))
        assertEquals(0, track.pathIndexAt(8.0))
        assertEquals(1, track.pathIndexAt(10.0))
        assertEquals(track.path.lastIndex, track.pathIndexAt(track.totalProgressMeters))
    }

    @Test
    fun profileElevation_isInterpolatedAndClamped() {
        val points = listOf(
            point(0.0, elevation = 1000.0),
            point(100.0, elevation = 1100.0)
        )
        val track = ActivityReplayTrack.build(points)!!

        assertEquals(1050.0, track.profileElevationAt(track.totalProgressMeters / 2)!!, 0.01)
        assertEquals(1000.0, track.profileElevationAt(-10.0)!!, 1e-9)
        assertEquals(1100.0, track.profileElevationAt(10_000.0)!!, 1e-9)
    }

    @Test
    fun replayStep_coversWholeTrackInChosenTempo() {
        val oneSecond = 1_000_000_000L

        assertEquals(1000.0 / 60.0, replayStepMeters(1000.0, ReplayTempo.NORMAL, oneSecond), 1e-9)
        assertEquals(1000.0 / 120.0, replayStepMeters(1000.0, ReplayTempo.SLOW, oneSecond), 1e-9)
        assertEquals(1000.0 / 30.0, replayStepMeters(1000.0, ReplayTempo.FAST, oneSecond), 1e-9)

        var progress = 0.0
        // 6000 klatek po 10 ms = 60 s.
        repeat(6000) { progress += replayStepMeters(1000.0, ReplayTempo.NORMAL, 10_000_000L) }
        assertEquals(1000.0, progress, 1e-6)

        assertEquals(0.0, replayStepMeters(0.0, ReplayTempo.NORMAL, oneSecond), 1e-12)
    }
}
