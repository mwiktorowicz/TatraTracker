package com.example.tatratracker.data

data class PlannedRouteDraft(
    val name: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val ascentMeters: Int? = null,
    val descentMeters: Int? = null,
    val minElevationMeters: Int? = null,
    val maxElevationMeters: Int? = null,
    val startName: String? = null,
    val endName: String? = null,
    val points: List<PlannedRouteDraftPoint>
)

data class PlannedRouteDraftPoint(
    val pointType: String,
    val orderIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val distanceMeters: Double? = null,
    val name: String? = null
)