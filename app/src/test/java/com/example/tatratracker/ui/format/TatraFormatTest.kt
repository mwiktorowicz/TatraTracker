package com.example.tatratracker.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TatraFormatTest {

    private val polish = Locale.forLanguageTag("pl-PL")
    private val english = Locale.US

    @Test
    fun distance_belowOneKilometer_isInMeters() {
        assertEquals("0 m", TatraFormat.distance(0.0, polish))
        assertEquals("850 m", TatraFormat.distance(850.4, polish))
        assertEquals("999 m", TatraFormat.distance(999.4, polish))
    }

    @Test
    fun distance_fromOneKilometer_isInKilometersWithOneDecimal() {
        assertEquals("1,0 km", TatraFormat.distance(999.6, polish))
        assertEquals("1,0 km", TatraFormat.distance(1000.0, polish))
        assertEquals("21,8 km", TatraFormat.distance(21_824.0, polish))
        assertEquals("21,9 km", TatraFormat.distance(21_850.0, polish))
    }

    @Test
    fun distanceExact_hasTwoDecimals() {
        assertEquals("21,82 km", TatraFormat.distanceExact(21_824.0, polish))
        assertEquals("1,00 km", TatraFormat.distanceExact(1000.0, polish))
        assertEquals("850 m", TatraFormat.distanceExact(850.0, polish))
    }

    @Test
    fun decimalSeparator_followsLocale() {
        assertEquals("21.8 km", TatraFormat.distance(21_824.0, english))
        assertEquals("21.82 km", TatraFormat.distanceExact(21_824.0, english))
        assertEquals("4.2 km/h", TatraFormat.speed(4.2 / 3.6, english))
        assertEquals("4,2 km/h", TatraFormat.speed(4.2 / 3.6, polish))
    }

    @Test
    fun distance_missing_isNoData() {
        assertEquals(TatraFormat.NO_DATA, TatraFormat.distance(null, polish))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.distance(-1.0, polish))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.distance(Double.NaN, polish))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.distanceExact(null, polish))
    }

    @Test
    fun duration_inWords() {
        assertEquals("0 min", TatraFormat.duration(0))
        assertEquals("0 min", TatraFormat.duration(59))
        assertEquals("45 min", TatraFormat.duration(45 * 60L))
        assertEquals("7 h", TatraFormat.duration(7 * 3600L))
        assertEquals("7 h 50 min", TatraFormat.duration(7 * 3600L + 50 * 60 + 18))
        assertEquals("25 h 1 min", TatraFormat.duration(25 * 3600L + 60))
    }

    @Test
    fun durationMillis_matchesSeconds() {
        assertEquals("7 h 50 min", TatraFormat.durationMillis((7 * 3600L + 50 * 60) * 1000 + 999))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.durationMillis(null))
    }

    @Test
    fun duration_missing_isNoData() {
        assertEquals(TatraFormat.NO_DATA, TatraFormat.duration(null))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.duration(-1))
    }

    @Test
    fun clockDuration() {
        assertEquals("7:50:18", TatraFormat.clockDuration(7 * 3600L + 50 * 60 + 18))
        assertEquals("0:05:07", TatraFormat.clockDuration(5 * 60L + 7))
        assertEquals("0:00:00", TatraFormat.clockDuration(0))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.clockDuration(-5))
    }

    @Test
    fun elevation_isWholeMeters() {
        assertEquals("0 m", TatraFormat.elevation(0))
        assertEquals("1450 m", TatraFormat.elevation(1450))
        assertEquals("1450 m", TatraFormat.elevation(1449.6))
        assertEquals("812 m", TatraFormat.elevation(812.4))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.elevation(null as Int?))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.elevation(Double.NaN))
    }

    @Test
    fun altitude_aboveSeaLevel() {
        assertEquals("2499 m n.p.m.", TatraFormat.altitude(2499.2))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.altitude(0.0))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.altitude(null))
    }

    @Test
    fun speed() {
        assertEquals("4,2 km/h", TatraFormat.speed(1.1667, polish))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.speed(0.0, polish))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.speed(null, polish))
    }

    @Test
    fun pace() {
        assertEquals("12:30 /km", TatraFormat.pace(750.0))
        assertEquals("12:31 /km", TatraFormat.pace(750.6))
        assertEquals("5:05 /km", TatraFormat.pace(305.0))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.pace(0.0))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.pace(Double.POSITIVE_INFINITY))
    }

    @Test
    fun heartRate() {
        assertEquals("127 bpm", TatraFormat.heartRate(127))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.heartRate(0))
        assertEquals(TatraFormat.NO_DATA, TatraFormat.heartRate(null))
    }
}
