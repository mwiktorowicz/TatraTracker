package com.example.tatratracker.data

import com.example.tatratracker.model.TrackPoint

object TrackPointMapper {

    fun toEntity(
        point: TrackPoint,
        activityId: Int
    ): TrackPointEntity {

        return TrackPointEntity(
            activityId = activityId,
            latitude = point.lat,
            longitude = point.lon,
            altitudeMeters = point.ele,
            timestampSeconds = point.timestampSeconds,
            heartRate = point.heartRate,
            speedMs = point.speedMs
        )
    }

    fun toModel(
        entity: TrackPointEntity
    ): TrackPoint {

        return TrackPoint(
            lat = entity.latitude,
            lon = entity.longitude,
            ele = entity.altitudeMeters ?: 0.0,
            timestampSeconds = entity.timestampSeconds,
            heartRate = entity.heartRate,
            speedMs = entity.speedMs
        )
    }

    fun toEntities(
        points: List<TrackPoint>,
        activityId: Int
    ): List<TrackPointEntity> {

        return points.map { point ->
            toEntity(
                point = point,
                activityId = activityId
            )
        }
    }

    fun toModels(
        entities: List<TrackPointEntity>
    ): List<TrackPoint> {

        return entities.map { entity ->
            toModel(entity)
        }
    }
}