package com.cgaraydev.seismicexplorer.data.mapper

import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeScientificSummary
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
        detailUrl = eventProperties.url?.trim()?.takeIf { it.isNotEmpty() },
        magType = eventProperties.magType?.trim()?.takeIf { it.isNotEmpty() },
        reviewStatus = eventProperties.status?.trim()?.takeIf { it.isNotEmpty() },
        scientific = EarthquakeScientificSummary(
            feltReports = eventProperties.felt?.takeIf { it >= 0 },
            communityIntensity = eventProperties.cdi?.takeIf {
                it.isFinite() && it in 0.0..12.0
            },
            modeledIntensity = eventProperties.mmi?.takeIf {
                it.isFinite() && it in 0.0..12.0
            },
            pagerAlertLevel = eventProperties.alert?.trim()?.takeIf { it.isNotEmpty() },
            tsunamiIndicator = eventProperties.tsunami?.takeIf { it == 0 || it == 1 },
            significance = eventProperties.sig?.takeIf { it >= 0 },
            network = eventProperties.net?.trim()?.takeIf { it.isNotEmpty() },
            networkEventCode = eventProperties.code?.trim()?.takeIf { it.isNotEmpty() },
            associatedIds = eventProperties.ids?.trim()?.takeIf { it.isNotEmpty() },
            contributingNetworks = eventProperties.sources?.trim()?.takeIf { it.isNotEmpty() },
            publishedProductCodes = eventProperties.types?.trim()?.takeIf { it.isNotEmpty() },
            stationCount = eventProperties.nst?.takeIf { it >= 0 },
            nearestStationDistanceDegrees = eventProperties.dmin?.takeIf {
                it.isFinite() && it >= 0.0
            },
            timeResidualRmsSeconds = eventProperties.rms?.takeIf {
                it.isFinite() && it >= 0.0
            },
            azimuthalGapDegrees = eventProperties.gap?.takeIf {
                it.isFinite() && it in 0.0..360.0
            },
            detailGeoJsonUrl = eventProperties.detail?.trim()?.takeIf { it.isNotEmpty() }
        )
    )
}
