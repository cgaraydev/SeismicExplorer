package com.cgaraydev.seismicexplorer.data.local.dao

data class EarthquakeVersion(
    val id: String,
    val updatedAtEpochMillis: Long,
    val magType: String?,
    val reviewStatus: String?
)