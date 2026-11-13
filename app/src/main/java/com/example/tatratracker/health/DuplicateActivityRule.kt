package com.example.tatratracker.health

import com.example.tatratracker.data.ActivityEntity
import kotlin.math.abs

/**
 * Trening z Samsung Health jest duplikatem, jeśli w bazie jest już aktywność
 * zaimportowana z pliku (FIT/TCX), która wystartowała najwyżej 5 minut wcześniej
 * lub później i ma dystans różniący się o mniej niż 5% dystansu treningu.
 */
object DuplicateActivityRule {

    const val MAX_START_DIFFERENCE_MILLIS = 5L * 60L * 1000L
    const val MAX_DISTANCE_DIFFERENCE_RATIO = 0.05

    fun isDuplicateOfFileActivity(
        startMillis: Long,
        distanceMeters: Double,
        existing: List<ActivityEntity>,
    ): Boolean = existing.any { activity ->
        isFileActivity(activity) &&
            abs(activity.timestamp - startMillis) <= MAX_START_DIFFERENCE_MILLIS &&
            isSimilarDistance(distanceMeters, activity.distanceMeters)
    }

    private fun isFileActivity(activity: ActivityEntity): Boolean =
        !activity.sourceFileName.startsWith(SamsungHealthSessionMapper.SOURCE_ID_PREFIX)

    private fun isSimilarDistance(referenceMeters: Double, otherMeters: Double): Boolean {
        if (referenceMeters <= 0.0) {
            return otherMeters <= 0.0
        }
        return abs(otherMeters - referenceMeters) / referenceMeters < MAX_DISTANCE_DIFFERENCE_RATIO
    }
}
