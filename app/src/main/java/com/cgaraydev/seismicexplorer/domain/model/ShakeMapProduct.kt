package com.cgaraydev.seismicexplorer.domain.model

/** Referencias publicadas por USGS, no recursos descargados ni mapas interpretados. */
data class ShakeMapProduct(
    val source: String?,
    val code: String?,
    val updatedAtEpochMillis: Long?,
    val mapStatus: String?,
    val resources: List<ShakeMapResource>
)

enum class ShakeMapResourceKind {
    INTENSITY_MAP,
    INTENSITY_OVERLAY,
    GRID_XML,
    STATION_OBSERVATIONS
}

data class ShakeMapResource(
    val kind: ShakeMapResourceKind,
    val url: String,
    val contentType: String?,
    val lengthBytes: Long?
)
