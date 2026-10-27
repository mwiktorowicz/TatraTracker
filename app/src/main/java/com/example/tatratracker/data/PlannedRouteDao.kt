package com.example.tatratracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedRouteDao {

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertRoute(
        route: PlannedRouteEntity
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPoints(
        points: List<PlannedRoutePointEntity>
    )

    @Update
    suspend fun updateRoute(
        route: PlannedRouteEntity
    )

    @Query(
        """
        SELECT *
        FROM planned_routes
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAllRoutes():
            Flow<List<PlannedRouteEntity>>

    @Query(
        """
        SELECT *
        FROM planned_routes
        WHERE id = :routeId
        LIMIT 1
        """
    )
    suspend fun getRouteById(
        routeId: Long
    ): PlannedRouteEntity?

    @Query(
        """
        SELECT *
        FROM planned_routes
        WHERE id = :routeId
        LIMIT 1
        """
    )
    fun observeRouteById(
        routeId: Long
    ): Flow<PlannedRouteEntity?>

    @Query(
        """
        SELECT *
        FROM planned_route_points
        WHERE routeId = :routeId
        AND pointType = :pointType
        ORDER BY orderIndex ASC
        """
    )
    suspend fun getPointsForRoute(
        routeId: Long,
        pointType: String
    ): List<PlannedRoutePointEntity>

    @Query(
        """
        SELECT *
        FROM planned_route_points
        WHERE routeId = :routeId
        ORDER BY id ASC
        """
    )
    suspend fun getAllPointsForRoute(
        routeId: Long
    ): List<PlannedRoutePointEntity>

    @Query(
        """
        SELECT *
        FROM planned_route_points
        WHERE routeId = :routeId
        ORDER BY id ASC
        """
    )
    fun observeAllPointsForRoute(
        routeId: Long
    ): Flow<List<PlannedRoutePointEntity>>

    @Query(
        """
        DELETE FROM planned_route_points
        WHERE routeId = :routeId
        """
    )
    suspend fun deletePointsForRoute(
        routeId: Long
    )

    @Query(
        """
        DELETE FROM planned_routes
        WHERE id = :routeId
        """
    )
    suspend fun deleteRoute(
        routeId: Long
    )
}