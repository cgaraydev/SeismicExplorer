package com.cgaraydev.seismicexplorer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsgsEarthquakeCollectionDto(
    val features: List<UsgsEarthquakeFeatureDto>,
    val metadata: UsgsEarthquakeMetadataDto? = null
)

@Serializable
data class UsgsEarthquakeMetadataDto(
    val generated: Long? = null,
    val count: Int? = null,
    val status: Int? = null
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
    val type: String? = null,
    // Códigos y estados publicados por el proveedor; no son equivalentes a "Richter".
    val magType: String? = null,
    val status: String? = null,
    // Los datos de Summary no son automaticamente mediciones locales ni danos confirmados.
    val felt: Int? = null,
    val cdi: Double? = null,
    val mmi: Double? = null,
    val alert: String? = null,
    val tsunami: Int? = null,
    val sig: Int? = null,
    val net: String? = null,
    val code: String? = null,
    val ids: String? = null,
    val sources: String? = null,
    val types: String? = null,
    val nst: Int? = null,
    val dmin: Double? = null,
    val rms: Double? = null,
    val gap: Double? = null,
    // Distinto de url, que conduce a la pagina oficial del evento.
    val detail: String? = null
)

@Serializable
data class UsgsEarthquakeGeometryDto(
    val type: String? = null,
    val coordinates: List<Double?>? = null
)
