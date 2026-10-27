package com.example.tatratracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "poi_visits",
    foreignKeys = [
        ForeignKey(
            entity = SavedPlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["placeId"]
        ),
        Index(
            value = ["activityId"]
        ),
        Index(
            value = [
                "placeId",
                "activityId"
            ],
            unique = true
        )
    ]
)
data class PoiVisitEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,

    val placeId: Long,

    val activityId: Int? = null,

    val visitedAt: Long,

    val source: String
)

object PoiVisitSource {

    const val MANUAL =
        "MANUAL"

    const val ACTIVITY =
        "ACTIVITY"
}