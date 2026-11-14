package com.cgaraydev.seismicexplorer.data.mapper

import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant

fun UsgsEarthquakeCollectionDto.toDomain(): List<Earthquake> {
    return features.mapNotNull { feature ->
        feature.toDomainOrNull()
    }
}

private fun UsgsEarthquakeFeatureDto.toDomainOrNull(): Earthquake? {
    val eventId = id?.takeIf { it.isNotBlank() } ?: return null
    val eventProperties = properties ?: return null
    val eventGeometry = geometry ?: return null

    if (!eventProperties.type.equals("earthquake", ignoreCase = true)) {
        return null
    }

    if (eventGeometry.type != "Point") {
        return null
    }

    val coordinateValues = eventGeometry.coordinates ?: return null
    val longitude = coordinateValues.getOrNull(0) ?: return null
    val latitude = coordinateValues.getOrNull(1) ?: return null
    val depth = coordinateValues.getOrNull(2)

    if (!latitude.isFinite() || latitude !in -90.0..90.0) {
        return null
    }

    if (!longitude.isFinite() || longitude !in -180.0..180.0) {
        return null
    }

    val magnitude = eventProperties.mag

    if (magnitude != null && !magnitude.isFinite()) {
        return null
    }

    if (depth != null && !depth.isFinite()) {
        return null
    }

    val occurredAt = eventProperties.time ?: return null
    val updatedAt = eventProperties.updated ?: return null

    return Earthquake(
        id = eventId,
        magnitude = magnitude,
        place = eventProperties.place?.trim()?.takeIf { it.isNotEmpty() },
        coordinates = GeoCoordinates(
            latitude = latitude,
            longitude = longitude
        ),
        depthKilometers = depth,
        occurredAt = Instant.ofEpochMilli(occurredAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        detailUrl = eventProperties.url?.trim()?.takeIf { it.isNotEmpty() }
    )
}