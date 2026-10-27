package com.example.tatratracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("activityId")
    ]
)
data class TrackPointEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Aktywność, do której należy punkt
    val activityId: Int,

    // Pozycja GPS
    val latitude: Double,
    val longitude: Double,

    // Wysokość n.p.m.
    val altitudeMeters: Double? = null,

    // Czas punktu zapisany w FIT
    val timestampSeconds: Long? = null,

    // Tętno
    val heartRate: Int? = null,

    // Prędkość w m/s
    val speedMs: Double? = null
)