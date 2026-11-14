package com.example.tatratracker.export

import com.example.tatratracker.data.ActivityEntity
import com.example.tatratracker.data.TrackPointEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Ślad pojedynczej aktywności jako GPX 1.1: jeden `trk` z jednym `trkseg`.
 * Wysokość, czas (UTC, ISO 8601) i tętno (`gpxtpx:hr`) są zapisywane tylko wtedy,
 * gdy punkt je ma. Wysokość równa dokładnie 0.0 oznacza brak pomiaru (tak zapisuje ją
 * import FIT/TCX i synchronizacja z Samsung Health), więc też jest pomijana.
 */
object ActivityGpxExporter {

    const val EMPTY_TRACK_MESSAGE =
        "Ta aktywność nie ma punktów GPS, więc nie ma czego zapisać do pliku GPX."

    private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Punkty muszą być w kolejności nagrania. Pusty ślad kończy się wyjątkiem z [EMPTY_TRACK_MESSAGE]. */
    fun buildGpx(
        activity: ActivityEntity,
        points: List<TrackPointEntity>
    ): String {
        require(points.isNotEmpty()) { EMPTY_TRACK_MESSAGE }

        val name = GpxXml.escapeXml(activity.name)

        return buildString {
            appendLine(GpxXml.XML_DECLARATION)
            appendLine(GpxXml.openGpxTag(listOf(GpxXml.TRACK_POINT_EXTENSION)))

            appendLine("  <metadata>")
            appendLine("    <name>$name</name>")
            if (activity.timestamp > 0L) {
                appendLine("    <time>${formatTime(Instant.ofEpochMilli(activity.timestamp))}</time>")
            }
            appendLine("  </metadata>")

            appendLine("  <trk>")
            appendLine("    <name>$name</name>")
            appendLine("    <trkseg>")
            points.forEach { point -> appendTrackPoint(point) }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    /** Np. „Wędrówka 06.11.2026” z 6 listopada → `wedrowka_06_11_2026_2026-11-06.gpx`. */
    fun suggestedFileName(
        activityName: String,
        timestampMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val baseName = GpxXml.fileNameBase(text = activityName, fallback = "tatratracker_aktywnosc")
        val date = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).format(fileDateFormatter)

        return "${baseName}_$date.gpx"
    }

    private fun StringBuilder.appendTrackPoint(point: TrackPointEntity) {
        val opening =
            "      <trkpt lat=\"${GpxXml.formatCoordinate(point.latitude)}\" " +
                    "lon=\"${GpxXml.formatCoordinate(point.longitude)}\""

        // Import zapisuje brak wysokości jako 0.0, a w Tatrach takiej wysokości nie ma.
        val altitude = point.altitudeMeters?.takeIf { it != 0.0 }
        val time = point.timestampSeconds
        // Tętno ≤ 0 traktujemy jak brak pomiaru.
        val heartRate = point.heartRate?.takeIf { it > 0 }

        if (altitude == null && time == null && heartRate == null) {
            appendLine("$opening />")
            return
        }

        appendLine("$opening>")

        // Kolejność elementów zgodna ze schematem GPX: ele, time, extensions.
        if (altitude != null) {
            appendLine("        <ele>${GpxXml.formatElevation(altitude)}</ele>")
        }
        if (time != null) {
            appendLine("        <time>${formatTime(Instant.ofEpochSecond(time))}</time>")
        }
        if (heartRate != null) {
            appendLine("        <extensions>")
            appendLine("          <gpxtpx:TrackPointExtension>")
            appendLine("            <gpxtpx:hr>$heartRate</gpxtpx:hr>")
            appendLine("          </gpxtpx:TrackPointExtension>")
            appendLine("        </extensions>")
        }

        appendLine("      </trkpt>")
    }

    // ISO 8601 w UTC z dokładnością do sekundy, np. 2026-11-06T07:15:00Z.
    private fun formatTime(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS))
    }
}
