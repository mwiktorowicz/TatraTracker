package com.example.tatratracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_routes"
)
data class PlannedRouteEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    val createdAt: Long,

    val distanceMeters: Double,
    val durationSeconds: Int,

    val ascentMeters: Int? = null,
    val descentMeters: Int? = null,

    val minElevationMeters: Int? = null,
    val maxElevationMeters: Int? = null,

    val startName: String? = null,
    val endName: String? = null
)