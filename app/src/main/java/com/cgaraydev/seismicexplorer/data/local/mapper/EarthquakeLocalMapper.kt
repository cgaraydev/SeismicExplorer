package com.cgaraydev.seismicexplorer.data.local.mapper

import com.cgaraydev.seismicexplorer.data.local.entity.EarthquakeEntity
import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeScientificSummary
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
        detailUrl = detailUrl,
        magType = magType,
        reviewStatus = reviewStatus,
        feltReports = scientific.feltReports,
        communityIntensity = scientific.communityIntensity,
        modeledIntensity = scientific.modeledIntensity,
        pagerAlertLevel = scientific.pagerAlertLevel,
        tsunamiIndicator = scientific.tsunamiIndicator,
        significance = scientific.significance,
        network = scientific.network,
        networkEventCode = scientific.networkEventCode,
        associatedIds = scientific.associatedIds,
        contributingNetworks = scientific.contributingNetworks,
        publishedProductCodes = scientific.publishedProductCodes,
        stationCount = scientific.stationCount,
        nearestStationDistanceDegrees = scientific.nearestStationDistanceDegrees,
        timeResidualRmsSeconds = scientific.timeResidualRmsSeconds,
        azimuthalGapDegrees = scientific.azimuthalGapDegrees,
        detailGeoJsonUrl = scientific.detailGeoJsonUrl
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
        detailUrl = detailUrl,
        magType = magType,
        reviewStatus = reviewStatus,
        scientific = EarthquakeScientificSummary(
            feltReports = feltReports,
            communityIntensity = communityIntensity,
            modeledIntensity = modeledIntensity,
            pagerAlertLevel = pagerAlertLevel,
            tsunamiIndicator = tsunamiIndicator,
            significance = significance,
            network = network,
            networkEventCode = networkEventCode,
            associatedIds = associatedIds,
            contributingNetworks = contributingNetworks,
            publishedProductCodes = publishedProductCodes,
            stationCount = stationCount,
            nearestStationDistanceDegrees = nearestStationDistanceDegrees,
            timeResidualRmsSeconds = timeResidualRmsSeconds,
            azimuthalGapDegrees = azimuthalGapDegrees,
            detailGeoJsonUrl = detailGeoJsonUrl
        )
    )
}
