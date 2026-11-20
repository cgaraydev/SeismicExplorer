package com.cgaraydev.seismicexplorer.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "earthquakes",
    indices = [
        Index(value = ["occurredAtEpochMillis"])
    ]
)
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val magnitude: Double?,
    val place: String?,
    val latitude: Double,
    val longitude: Double,
    val depthKilometers: Double?,
    val occurredAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val detailUrl: String?,
    val magType: String? = null,
    val reviewStatus: String? = null,
    val feltReports: Int? = null,
    val communityIntensity: Double? = null,
    val modeledIntensity: Double? = null,
    val pagerAlertLevel: String? = null,
    val tsunamiIndicator: Int? = null,
    val significance: Int? = null,
    val network: String? = null,
    val networkEventCode: String? = null,
    val associatedIds: String? = null,
    val contributingNetworks: String? = null,
    val publishedProductCodes: String? = null,
    val stationCount: Int? = null,
    val nearestStationDistanceDegrees: Double? = null,
    val timeResidualRmsSeconds: Double? = null,
    val azimuthalGapDegrees: Double? = null,
    val detailGeoJsonUrl: String? = null
)
