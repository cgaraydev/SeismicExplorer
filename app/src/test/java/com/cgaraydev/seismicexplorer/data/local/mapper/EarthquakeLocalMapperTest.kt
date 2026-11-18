package com.cgaraydev.seismicexplorer.data.local.mapper

import com.cgaraydev.seismicexplorer.domain.model.Earthquake
import com.cgaraydev.seismicexplorer.domain.model.GeoCoordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class EarthquakeLocalMapperTest {

    @Test
    fun domainToEntity_preservesCoordinatesAndTimestamps() {
        val earthquake = sampleEarthquake()

        val entity = earthquake.toEntity()

        assertEquals(-33.4, entity.latitude, 0.0)
        assertEquals(-71.6, entity.longitude, 0.0)
        assertEquals(
            earthquake.occurredAt.toEpochMilli(),
            entity.occurredAtEpochMillis
        )
    }

    @Test
    fun roundTrip_preservesDomainModelAndNullableFields() {
        val original = sampleEarthquake().copy(
            magnitude = null,
            place = null,
            depthKilometers = null,
            detailUrl = null
        )

        val restored = original.toEntity().toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun roundTrip_preservesMagnitudeTypeAndReviewStatus() {
        val original = sampleEarthquake().copy(
            magType = "mww",
            reviewStatus = "reviewed"
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    private fun sampleEarthquake() = Earthquake(
        id = "us123",
        magnitude = 4.5,
        place = "Coast of Chile",
        coordinates = GeoCoordinates(
            latitude = -33.4,
            longitude = -71.6
        ),
        depthKilometers = 12.5,
        occurredAt = Instant.parse("2026-11-14T10:00:00Z"),
        updatedAt = Instant.parse("2026-11-14T10:05:00Z"),
        detailUrl = "https://earthquake.usgs.gov/"
    )
}