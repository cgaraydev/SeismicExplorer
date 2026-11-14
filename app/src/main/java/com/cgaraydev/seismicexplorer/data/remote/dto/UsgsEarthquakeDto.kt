package com.cgaraydev.seismicexplorer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsgsEarthquakeCollectionDto(
    val features: List<UsgsEarthquakeFeatureDto>
)

@Serializable
data class UsgsEarthquakeFeatureDto(
    val id: String? = null,
    val properties: UsgsEarthquakePropertiesDto? = null,
    val geometry: UsgsEarthquakeGeometryDto? = null
)

@Serializable
data class UsgsEarthquakePropertiesDto(
    val mag: Double? = null,
    val place: String? = null,
    val time: Long? = null,
    val updated: Long? = null,
    val url: String? = null,
    val type: String? = null
)

@Serializable
data class UsgsEarthquakeGeometryDto(
    val type: String? = null,
    val coordinates: List<Double?>? = null
)