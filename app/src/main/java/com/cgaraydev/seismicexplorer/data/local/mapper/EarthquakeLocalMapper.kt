package com.cgaraydev.seismicexplorer.data.local.mapper

import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant

fun Earthquake.toEntity(): EarthquakeEntity {
    return EarthquakeEntity(
        id = id,
        magnitude = magnitude,
        place = place,
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        depthKilometers = depthKilometers,
        occurredAtEpochMillis = occurredAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
        detailUrl = detailUrl
    )
}

fun EarthquakeEntity.toDomain(): Earthquake {
    return Earthquake(
        id = id,
        magnitude = magnitude,
        place = place,
        coordinates = GeoCoordinates(
            latitude = latitude,
            longitude = longitude
        ),
        depthKilometers = depthKilometers,
        occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        detailUrl = detailUrl
    )
}