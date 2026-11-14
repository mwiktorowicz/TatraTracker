package com.example.tatratracker.export

import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.TrackPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ActivityGpxExporterTest {

    private val warsaw = ZoneId.of("Europe/Warsaw")

    // 06.11.2026 08:15:00 czasu polskiego = 07:15:00 UTC
    private val startMillis =
        ZonedDateTime.of(2026, 11, 6, 8, 15, 0, 0, warsaw).toInstant().toEpochMilli()

    private fun activity(name: String = "Wędrówka 06.11.2026") =
        ActivityEntity(id = 1, timestamp = startMillis, name = name)

    private fun point(
        latitude: Double = 49.2312345,
        longitude: Double = 20.0087654,
        altitude: Double? = null,
        timestampSeconds: Long? = null,
        heartRate: Int? = null
    ) = TrackPointEntity(
        activityId = 1,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitude,
        timestampSeconds = timestampSeconds,
        heartRate = heartRate
    )

    @Test
    fun fullPoint_hasElevationTimeAndHeartRate() {
        val gpx = ActivityGpxExporter.buildGpx(
            activity(),
            listOf(point(altitude = 1012.34, timestampSeconds = startMillis / 1000, heartRate = 132))
        )

        val expectedPoint = """
            |      <trkpt lat="49.2312345" lon="20.0087654">
            |        <ele>1012.3</ele>
            |        <time>2026-11-06T07:15:00Z</time>
            |        <extensions>
            |          <gpxtpx:TrackPointExtension>
            |            <gpxtpx:hr>132</gpxtpx:hr>
            |          </gpxtpx:TrackPointExtension>
            |        </extensions>
            |      </trkpt>
            """.trimMargin()

        assertTrue(gpx, gpx.contains(expectedPoint))
        assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(gpx.contains("""xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1""""))
        assertTrue(gpx.contains("<time>2026-11-06T07:15:00Z</time>\n  </metadata>"))
        assertTrue(gpx.trimEnd().endsWith("</gpx>"))
    }

    @Test
    fun pointWithoutOptionalData_isSelfClosing() {
        val gpx = ActivityGpxExporter.buildGpx(activity(), listOf(point()))

        assertTrue(gpx, gpx.contains("""      <trkpt lat="49.2312345" lon="20.0087654" />"""))
        assertFalse(gpx.contains("<ele>"))
        assertFalse(gpx.contains("<extensions>"))
        // jedyny <time> to czas startu w metadata
        assertEquals(1, Regex("<time>").findAll(gpx).count())
    }

    @Test
    fun eachOptionalField_isWrittenOnlyWhenPresent() {
        val gpx = ActivityGpxExporter.buildGpx(
            activity(),
            listOf(
                point(altitude = 950.0),
                point(timestampSeconds = 1_800_000_000L),
                point(heartRate = 0),
                point(heartRate = 101)
            )
        )

        assertEquals(1, Regex("<ele>").findAll(gpx).count())
        // metadata + jeden punkt
        assertEquals(2, Regex("<time>").findAll(gpx).count())
        assertTrue(gpx.contains("<time>2027-01-15T08:00:00Z</time>"))
        // tętno 0 = brak pomiaru
        assertEquals(1, Regex("<gpxtpx:hr>").findAll(gpx).count())
        assertTrue(gpx.contains("<gpxtpx:hr>101</gpxtpx:hr>"))
        assertEquals(4, Regex("<trkpt ").findAll(gpx).count())
    }

    @Test
    fun zeroAltitude_isTreatedAsMissing() {
        val gpx = ActivityGpxExporter.buildGpx(
            activity(),
            listOf(
                point(altitude = 0.0),
                point(altitude = 0.0, timestampSeconds = startMillis / 1000),
                point(altitude = 1012.0)
            )
        )

        assertFalse(gpx, gpx.contains("<ele>0.0</ele>"))
        assertEquals(1, Regex("<ele>").findAll(gpx).count())
        assertTrue(gpx.contains("<ele>1012.0</ele>"))
        // sama zerowa wysokość = punkt bez dodatkowych danych
        assertTrue(gpx, gpx.contains("""      <trkpt lat="49.2312345" lon="20.0087654" />"""))
        assertTrue(gpx.contains("<time>2026-11-06T07:15:00Z</time>\n      </trkpt>"))
    }

    @Test
    fun specialCharactersInName_areEscapedInMetadataAndTrack() {
        val gpx = ActivityGpxExporter.buildGpx(
            activity(name = "Rysy & <Wysoka> \"z 'Bulą'\""),
            listOf(point())
        )

        val escaped = "Rysy &amp; &lt;Wysoka&gt; &quot;z &apos;Bulą&apos;&quot;"
        assertEquals(2, Regex(Regex.escape("<name>$escaped</name>")).findAll(gpx).count())
        assertFalse(gpx.contains("<Wysoka>"))
    }

    @Test
    fun emptyTrack_givesReadableError() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ActivityGpxExporter.buildGpx(activity(), emptyList())
        }

        assertEquals(ActivityGpxExporter.EMPTY_TRACK_MESSAGE, error.message)
    }

    @Test
    fun suggestedFileName_usesNameAndLocalDate() {
        assertEquals(
            "wedrowka_06_11_2026_2026-11-06.gpx",
            ActivityGpxExporter.suggestedFileName("Wędrówka 06.11.2026", startMillis, warsaw)
        )
    }

    @Test
    fun suggestedFileName_withoutUsableName_hasFallback() {
        assertEquals(
            "tatratracker_aktywnosc_2026-11-06.gpx",
            ActivityGpxExporter.suggestedFileName("  ***  ", startMillis, warsaw)
        )
    }
}
