package com.example.tatratracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_route_points",
    foreignKeys = [
        ForeignKey(
            entity = PlannedRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routeId"),
        Index(
            value = [
                "routeId",
                "pointType",
                "orderIndex"
            ],
            unique = true
        )
    ]
)
data class PlannedRoutePointEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val routeId: Long,

    val pointType: String,
    val orderIndex: Int,

    val latitude: Double,
    val longitude: Double,

    val altitudeMeters: Double? = null,
    val distanceMeters: Double? = null,

    val name: String? = null
)

object PlannedRoutePointType {
    const val GEOMETRY = "GEOMETRY"
    const val START = "START"
    const val WAYPOINT = "WAYPOINT"
    const val END = "END"
    const val ELEVATION = "ELEVATION"
}