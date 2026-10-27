package com.example.tatratracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_places",
    indices = [
        Index(
            value = ["category"]
        )
    ]
)
data class SavedPlaceEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,

    val name: String,

    val category: String,

    val latitude: Double,

    val longitude: Double,

    val note: String? = null,

    val createdAt: Long =
        System.currentTimeMillis()
)

object SavedPlaceCategory {

    const val HUT =
        "HUT"

    const val MOUNTAIN =
        "MOUNTAIN"

    const val OTHER =
        "OTHER"

    val all =
        listOf(
            HUT,
            MOUNTAIN,
            OTHER
        )

    fun label(
        category: String
    ): String {

        return when (category) {

            HUT ->
                "Schronisko"

            MOUNTAIN ->
                "Szczyt / przełęcz"

            else ->
                "Inne"
        }
    }
}