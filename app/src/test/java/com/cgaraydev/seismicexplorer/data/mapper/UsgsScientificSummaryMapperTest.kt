package com.cgaraydev.seismicexplorer.data.mapper

import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeCollectionDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeFeatureDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakeGeometryDto
import com.cgaraydev.seismicexplorer.data.remote.dto.UsgsEarthquakePropertiesDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsgsScientificSummaryMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun summaryJson_preservesPublishedScientificFields() {
        val raw = """{
            "type": "FeatureCollection", "features": [{
                "id": "us7000test",
                "properties": {
                    "type": "earthquake", "mag": 6.1, "place": "Coast of Chile",
                    "time": 1700000000000, "updated": 1700000010000,
                    "url": "https://earthquake.usgs.gov/earthquakes/eventpage/us7000test",
                    "detail": "https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us7000test.geojson",
                    "felt": 26, "cdi": 4.8, "mmi": 5.2,
                    "alert": "yellow", "tsunami": 0, "sig": 610,
                    "net": "us", "code": "7000test",
                    "ids": ",us7000test,ak123,", "sources": ",us,ak,",
                    "types": ",origin,shakemap,dyfi,",
                    "nst": 92, "dmin": 1.25, "rms": 0.61, "gap": 68.0
                },
                "geometry": {"type": "Point", "coordinates": [-71.6,-33.4,10.0]}
            }]
        }""".trimIndent()

        val feature = json.decodeFromString<UsgsEarthquakeCollectionDto>(raw).toDomain().single()
        val summary = feature.scientific
        assertEquals(26, summary.feltReports)
        assertEquals(4.8, summary.communityIntensity!!, 0.0)
        assertEquals(5.2, summary.modeledIntensity!!, 0.0)
        assertEquals("yellow", summary.pagerAlertLevel)
        assertEquals(0, summary.tsunamiIndicator)
        assertEquals(610, summary.significance)
        assertEquals("us", summary.network)
        assertEquals("7000test", summary.networkEventCode)
        assertEquals(",us7000test,ak123,", summary.associatedIds)
        assertEquals(",us,ak,", summary.contributingNetworks)
        assertEquals(",origin,shakemap,dyfi,", summary.publishedProductCodes)
        assertEquals(92, summary.stationCount)
        assertEquals(1.25, summary.nearestStationDistanceDegrees!!, 0.0)
        assertEquals(0.61, summary.timeResidualRmsSeconds!!, 0.0)
        assertEquals(68.0, summary.azimuthalGapDegrees!!, 0.0)
        assertEquals("https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us7000test.geojson",
            summary.detailGeoJsonUrl)
        assertEquals("https://earthquake.usgs.gov/earthquakes/eventpage/us7000test", feature.detailUrl)
    }

    @Test
    fun absentFields_remainUnknownRatherThanZero() {
        val summary = event(UsgsEarthquakePropertiesDto(
            mag = 4.0, type = "earthquake", time = 1700000000000,
            updated = 1700000001000
        )).scientific
        assertNull(summary.feltReports)
        assertNull(summary.tsunamiIndicator)
        assertNull(summary.communityIntensity)
        assertNull(summary.significance)
        assertNull(summary.network)
        assertNull(summary.detailGeoJsonUrl)
    }

    @Test
    fun invalidOptionalFields_areDiscardedWithoutDroppingEvent() {
        val summary = event(UsgsEarthquakePropertiesDto(
            mag = 4.0, type = "earthquake", time = 1700000000000,
            updated = 1700000001000, felt = -1,
            cdi = Double.NaN, mmi = Double.POSITIVE_INFINITY,
            alert = " ", tsunami = 2, sig = -1, net = " ",
            nst = -5, dmin = -2.0, rms = Double.NaN, gap = 361.0,
            detail = " "
        )).scientific
        assertNull(summary.feltReports)
        assertNull(summary.communityIntensity)
        assertNull(summary.modeledIntensity)
        assertNull(summary.pagerAlertLevel)
        assertNull(summary.tsunamiIndicator)
        assertNull(summary.significance)
        assertNull(summary.network)
        assertNull(summary.stationCount)
        assertNull(summary.nearestStationDistanceDegrees)
        assertNull(summary.timeResidualRmsSeconds)
        assertNull(summary.azimuthalGapDegrees)
        assertNull(summary.detailGeoJsonUrl)
    }

    private fun event(properties: UsgsEarthquakePropertiesDto) =
        UsgsEarthquakeCollectionDto(features = listOf(
            UsgsEarthquakeFeatureDto(
                id = "us-test", properties = properties,
                geometry = UsgsEarthquakeGeometryDto(
                    type = "Point", coordinates = listOf(-71.6, -33.4, 10.0)
                )
            )
        )).toDomain().single()
}
