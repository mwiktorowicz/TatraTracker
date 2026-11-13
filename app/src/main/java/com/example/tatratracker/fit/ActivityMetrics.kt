package com.example.tatratracker.fit

/*
 * Wspólne obliczenia dla importu plików i synchronizacji z Samsung Health.
 */

/** Maksymalny czas całkowity liczony z punktów; dłuższy uznajemy za błędny znacznik czasu. */
const val MAX_ELAPSED_FROM_POINTS_MILLIS = 24L * 60L * 60L * 1000L

data class ActivityDurations(
    /** Od startu do mety, z postojami. */
    val totalMillis: Long,
    /** Bez pauz. */
    val movingMillis: Long,
)

/**
 * Ustala oba czasy aktywności. Wartość pusta lub niedodatnia oznacza brak.
 * Jeśli któregoś czasu brak, oba są równe. Czas całkowity nigdy nie jest
 * krótszy niż czas w ruchu.
 */
fun resolveDurations(totalMillis: Long?, movingMillis: Long?): ActivityDurations {
    val total = totalMillis?.takeIf { it > 0L }
    val moving = movingMillis?.takeIf { it > 0L }

    val resolvedMoving = moving ?: total ?: 0L
    val resolvedTotal = maxOf(total ?: resolvedMoving, resolvedMoving)

    return ActivityDurations(totalMillis = resolvedTotal, movingMillis = resolvedMoving)
}

/**
 * Czas między pierwszym a ostatnim znacznikiem czasu punktów (w ms).
 * Zwraca null, gdy znaczników jest mniej niż dwa albo wynik przekracza 24 h.
 */
fun elapsedMillisFromPoints(timestampsMillis: List<Long?>): Long? {
    val timestamps = timestampsMillis.filterNotNull()
    if (timestamps.size < 2) {
        return null
    }

    val elapsed = timestamps.max() - timestamps.min()
    return elapsed.takeIf { it in 1..MAX_ELAPSED_FROM_POINTS_MILLIS }
}

fun speedToPaceSecondsPerKm(speedMetersPerSecond: Double): Double {
    if (speedMetersPerSecond <= 0.0) {
        return 0.0
    }
    return 1000.0 / speedMetersPerSecond
}
