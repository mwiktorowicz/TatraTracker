package com.example.tatratracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivity(
        activity: ActivityEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrackPoints(
        points: List<TrackPointEntity>
    )

    @Query(
        """
        SELECT * FROM activities
        ORDER BY timestamp DESC
        """
    )
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query(
        """
        SELECT * FROM activities
        WHERE id = :activityId
        LIMIT 1
        """
    )
    suspend fun getActivityById(
        activityId: Int
    ): ActivityEntity?

    @Query(
        """
        SELECT * FROM track_points
        WHERE activityId = :activityId
        ORDER BY id ASC
        """
    )
    fun getTrackPointsForActivity(
        activityId: Int
    ): Flow<List<TrackPointEntity>>

    @Query(
        """
        SELECT * FROM track_points
        ORDER BY activityId ASC, id ASC
        """
    )
    fun getAllTrackPoints():
            Flow<List<TrackPointEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM activities
            WHERE sourceFileName = :fileName
        )
        """
    )
    suspend fun activityExists(
        fileName: String
    ): Boolean

    @Query(
        """
        SELECT * FROM activities
        WHERE timestamp BETWEEN :fromMillis AND :toMillis
        """
    )
    suspend fun getActivitiesStartedBetween(fromMillis: Long, toMillis: Long): List<ActivityEntity>

    @Query(
        """
        DELETE FROM activities
        WHERE id = :activityId
        """
    )
    suspend fun deleteActivity(
        activityId: Int
    )

    @Query(
        """
        DELETE FROM activities
        """
    )
    suspend fun deleteAllActivities()
}