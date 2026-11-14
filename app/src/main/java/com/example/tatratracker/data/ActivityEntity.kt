package com.example.tatratracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val timestamp: Long,
    val name: String,
    val sourceFileName: String = "",

    // Czas całkowity: od startu do mety, z postojami
    val durationMillis: Long = 0L,

    // Czas w ruchu: bez pauz; z niego liczone są średnia prędkość i tempo
    @ColumnInfo(defaultValue = "0")
    val movingDurationMillis: Long = 0L,

    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,

    val avgSpeedMs: Double = 0.0,
    val maxSpeedMs: Double = 0.0,

    val avgPaceSecPerKm: Double = 0.0,
    val maxPaceSecPerKm: Double = 0.0,

    val minAltitudeMeters: Double = 0.0,
    val maxAltitudeMeters: Double = 0.0,

    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,

    // Notatka użytkownika; edytowalna razem z nazwą, synchronizacja jej nie nadpisuje
    val note: String? = null
)