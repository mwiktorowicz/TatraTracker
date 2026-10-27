package com.example.tatratracker.model

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double = 0.0,

    // Czas pomiaru zapisany w pliku FIT
    val timestampSeconds: Long? = null,

    // Tętno w danym punkcie
    val heartRate: Int? = null,

    // Prędkość w danym punkcie w m/s
    val speedMs: Double? = null
)