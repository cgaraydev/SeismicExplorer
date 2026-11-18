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
    val reviewStatus: String? = null
)