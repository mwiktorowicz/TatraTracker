package com.example.tatratracker.health

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/*
 * Dane treningu z Samsung Health przepisane z klas SDK na zwykłe klasy Kotlina.
 * Dzięki temu mapper i jego testy nie zależą od SDK ani od Androida.
 * Przepisanie robi SamsungHealthClient.
 */

/** Jeden punkt danych ćwiczenia (HealthDataPoint). Może zawierać kilka sesji. */
data class HealthExerciseRecord(
    val uid: String,
    /** Aplikacja, która zapisała trening, np. "com.sec.android.app.shealth". */
    val appId: String?,
    /** Nazwa PredefinedExerciseType z punktu danych, np. "HIKING". */
    val exerciseType: String?,
    val zoneOffset: ZoneOffset?,
    val sessions: List<HealthSession>,
)

/** Sesja ćwiczenia (ExerciseSession). */
data class HealthSession(
    val startTime: Instant,
    val endTime: Instant,
    /** Czas bez pauz. */
    val duration: Duration?,
    /** Nazwa PredefinedExerciseType z sesji, np. "HIKING". */
    val exerciseType: String?,
    val customTitle: String?,
    val distanceMeters: Double?,
    val calories: Double,
    val minAltitude: Double?,
    val maxAltitude: Double?,
    val meanHeartRate: Double?,
    val maxHeartRate: Double?,
    val maxSpeed: Double?,
    val route: List<HealthRoutePoint>,
    val log: List<HealthLogEntry>,
)

/** Punkt trasy (ExerciseLocation), WGS84. */
data class HealthRoutePoint(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
)

/** Wpis logu sesji (ExerciseLog), ok. 1 na minutę. */
data class HealthLogEntry(
    val timestamp: Instant,
    val heartRate: Double?,
    /** m/s */
    val speed: Double?,
)
