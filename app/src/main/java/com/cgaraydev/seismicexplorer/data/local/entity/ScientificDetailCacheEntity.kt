package com.cgaraydev.seismicexplorer.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** GeoJSON Detail validado. No almacena ni descarga los archivos de los productos. */
@Entity(tableName = "scientific_detail_cache")
data class ScientificDetailCacheEntity(
    @PrimaryKey val eventId: String,
    val sourceUpdatedAtEpochMillis: Long,
    val fetchedAtEpochMillis: Long,
    val payloadJson: String
)
