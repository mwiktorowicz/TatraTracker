package com.example.tatratracker.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Wspólny zapis liczb na ekranach: dystans, czas, wysokość, prędkość, tempo, tętno.
 * Separator dziesiętny według [Locale] (domyślnie ustawienia urządzenia).
 *
 * Brak wartości (null, NaN, liczba ujemna) daje zawsze [NO_DATA]. Przy wysokości n.p.m.,
 * prędkości, tempie i tętnie także zero oznacza brak danych. Dystans, czas i przewyższenie
 * równe zero są poprawną wartością – ekran, który traktuje zero jako brak, sam podaje null.
 */
object TatraFormat {

    const val NO_DATA = "Brak danych"

    private val timeOfDayFormatter = DateTimeFormatter.ofPattern("H:mm:ss", Locale.ROOT)

    /** „850 m” poniżej 1000 m, od 1000 m „21,8 km”. */
    fun distance(meters: Double?, locale: Locale = Locale.getDefault()): String {
        return formatDistance(meters, kilometerDecimals = 1, locale = locale)
    }

    /** Wariant dokładny „21,82 km” – tylko główna wartość w szczegółach aktywności i trasy. */
    fun distanceExact(meters: Double?, locale: Locale = Locale.getDefault()): String {
        return formatDistance(meters, kilometerDecimals = 2, locale = locale)
    }

    /** Czas słownie: „45 min”, „7 h”, „7 h 50 min” (sekundy obcinane do pełnych minut). */
    fun duration(seconds: Long?): String {
        if (seconds == null || seconds < 0) {
            return NO_DATA
        }

        val totalMinutes = seconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0L -> "$minutes min"
            minutes == 0L -> "$hours h"
            else -> "$hours h $minutes min"
        }
    }

    /** [duration] dla sekund zapisanych jako Int (trasy zaplanowane). */
    fun duration(seconds: Int?): String {
        return duration(seconds?.toLong())
    }

    /** [duration] dla czasu w milisekundach. */
    fun durationMillis(millis: Long?): String {
        return duration(millis?.div(1000))
    }

    /** Czas zegarowy „7:50:18”. */
    fun clockDuration(seconds: Long?): String {
        if (seconds == null || seconds < 0) {
            return NO_DATA
        }

        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val remainingSeconds = seconds % 60

        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    /** Godzina zegarowa „7:05:12” z czasu Unix w sekundach, w strefie [zoneId] (domyślnie urządzenia). */
    fun timeOfDay(epochSeconds: Long?, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (epochSeconds == null || epochSeconds <= 0L) {
            return NO_DATA
        }

        return Instant.ofEpochSecond(epochSeconds).atZone(zoneId).format(timeOfDayFormatter)
    }

    /** Wysokość albo przewyższenie: „1450 m”. */
    fun elevation(meters: Double?): String {
        if (meters == null || !meters.isFinite()) {
            return NO_DATA
        }

        return "${meters.roundToInt()} m"
    }

    /** [elevation] dla wartości całkowitej. */
    fun elevation(meters: Int?): String {
        return elevation(meters?.toDouble())
    }

    /** Wysokość nad poziomem morza: „1450 m n.p.m.”. Zero = brak danych. */
    fun altitude(meters: Double?): String {
        if (meters == null || !meters.isFinite() || meters <= 0.0) {
            return NO_DATA
        }

        return "${meters.roundToInt()} m n.p.m."
    }

    /** Prędkość z m/s: „4,2 km/h”. */
    fun speed(metersPerSecond: Double?, locale: Locale = Locale.getDefault()): String {
        if (metersPerSecond == null || !metersPerSecond.isFinite() || metersPerSecond <= 0.0) {
            return NO_DATA
        }

        return String.format(locale, "%.1f km/h", metersPerSecond * 3.6)
    }

    /** Tempo z sekund na kilometr: „12:30 /km”. */
    fun pace(secondsPerKm: Double?): String {
        if (secondsPerKm == null || !secondsPerKm.isFinite() || secondsPerKm <= 0.0) {
            return NO_DATA
        }

        val totalSeconds = secondsPerKm.roundToLong()

        return String.format(Locale.ROOT, "%d:%02d /km", totalSeconds / 60, totalSeconds % 60)
    }

    /** Tętno: „127 bpm”. */
    fun heartRate(bpm: Int?): String {
        if (bpm == null || bpm <= 0) {
            return NO_DATA
        }

        return "$bpm bpm"
    }

    private fun formatDistance(meters: Double?, kilometerDecimals: Int, locale: Locale): String {
        if (meters == null || !meters.isFinite() || meters < 0.0) {
            return NO_DATA
        }

        // Próg liczony po zaokrągleniu, żeby 999,6 m nie dało „1000 m”.
        val roundedMeters = meters.roundToLong()
        if (roundedMeters < 1000) {
            return "$roundedMeters m"
        }

        return String.format(locale, "%.${kilometerDecimals}f km", meters / 1000.0)
    }
}
