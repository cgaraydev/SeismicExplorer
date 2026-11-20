package com.cgaraydev.seismicexplorer.data.local.mapper

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.EarthquakeScientificSummary
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class EarthquakeScientificSummaryLocalMapperTest {
    @Test
    fun allScientificFields_surviveRoomEntityRoundTrip() {
        val scientific = EarthquakeScientificSummary(
            feltReports = 0, communityIntensity = 3.4, modeledIntensity = 5.6,
            pagerAlertLevel = "green", tsunamiIndicator = 0,
            significance = 290, network = "us", networkEventCode = "7000test",
            associatedIds = ",us7000test,", contributingNetworks = ",us,",
            publishedProductCodes = ",origin,shakemap,", stationCount = 23,
            nearestStationDistanceDegrees = 2.4, timeResidualRmsSeconds = 0.72,
            azimuthalGapDegrees = 129.0,
            detailGeoJsonUrl = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us7000test.geojson"
        )
        val event = Earthquake(
            id = "us7000test", magnitude = 4.8, place = "Chile",
            coordinates = GeoCoordinates(-33.4, -71.6), depthKilometers = 15.0,
            occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
            updatedAt = Instant.parse("2026-11-14T10:05:00Z"),
            detailUrl = null, scientific = scientific
        )
        assertEquals(event, event.toEntity().toDomain())
    }

    @Test
    fun missingScientificFields_remainNullable() {
        val event = Earthquake(
            id = "without-metadata", magnitude = null, place = null,
            coordinates = GeoCoordinates(0.0, 0.0), depthKilometers = null,
            occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
            updatedAt = Instant.parse("2026-11-14T10:00:00Z"),
            detailUrl = null
        )
        assertEquals(EarthquakeScientificSummary(), event.toEntity().toDomain().scientific)
    }
}
