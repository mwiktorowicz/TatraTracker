package com.example.tatratracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tatratracker.achievements.AchievementKind
import com.example.tatratracker.achievements.ActivityRecord
import com.example.tatratracker.achievements.ActivityRecordKind
import com.example.tatratracker.ui.format.TatraFormat

// Wspólny wygląd osiągnięć i rekordów na ekranie „Kolekcja” i na planszy podsumowania.

internal fun achievementIcon(kind: AchievementKind): ImageVector {
    return when (kind) {
        AchievementKind.FIRST_HUT,
        AchievementKind.ALL_POLISH_HUTS -> Icons.Outlined.Cottage

        AchievementKind.PEAKS_5,
        AchievementKind.PEAKS_10,
        AchievementKind.PEAKS_25 -> Icons.Outlined.Landscape

        AchievementKind.FIRST_PEAK_2000 -> Icons.Outlined.Terrain
        AchievementKind.DISTANCE_100_KM_YEAR -> Icons.Outlined.Hiking
        AchievementKind.FIRST_COMPLETED_TRAIL -> Icons.Outlined.Route
    }
}

internal fun recordIcon(kind: ActivityRecordKind): ImageVector {
    return when (kind) {
        ActivityRecordKind.DISTANCE -> Icons.Outlined.Straighten
        ActivityRecordKind.ALTITUDE -> Icons.Outlined.TrendingUp
    }
}

internal fun recordTitle(kind: ActivityRecordKind): String {
    return when (kind) {
        ActivityRecordKind.DISTANCE -> "Najdłuższa wędrówka"
        ActivityRecordKind.ALTITUDE -> "Najwyższy punkt"
    }
}

internal fun recordValue(record: ActivityRecord): String {
    return when (record.kind) {
        ActivityRecordKind.DISTANCE -> TatraFormat.distanceExact(record.value)
        ActivityRecordKind.ALTITUDE -> TatraFormat.altitude(record.value)
    }
}
