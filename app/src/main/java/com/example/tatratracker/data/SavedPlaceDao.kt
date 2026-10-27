package com.example.tatratracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPlace(
        place: SavedPlaceEntity
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertVisit(
        visit: PoiVisitEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM saved_places
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAllPlaces():
            Flow<List<SavedPlaceEntity>>

    @Query(
        """
        SELECT *
        FROM saved_places
        ORDER BY id ASC
        """
    )
    suspend fun getAllPlacesOnce():
            List<SavedPlaceEntity>

    @Query(
        """
        SELECT *
        FROM poi_visits
        ORDER BY visitedAt DESC, id DESC
        """
    )
    fun getAllVisits():
            Flow<List<PoiVisitEntity>>

    @Query(
        """
        SELECT *
        FROM poi_visits
        WHERE placeId = :placeId
        ORDER BY visitedAt DESC, id DESC
        """
    )
    suspend fun getVisitsForPlace(
        placeId: Long
    ): List<PoiVisitEntity>

    @Query(
        """
        SELECT *
        FROM saved_places
        WHERE id = :placeId
        LIMIT 1
        """
    )
    suspend fun getPlaceById(
        placeId: Long
    ): SavedPlaceEntity?

    @Query(
        """
        DELETE FROM saved_places
        WHERE id = :placeId
        """
    )
    suspend fun deletePlace(
        placeId: Long
    )
}