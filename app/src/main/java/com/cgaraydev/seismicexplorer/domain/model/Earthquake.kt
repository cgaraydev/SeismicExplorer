package com.cgaraydev.seismicexplorer.domain.model

import java.time.Instant

data class Earthquake(
    val id: String,
    val magnitude: Double?,
    val place: String?,
    val coordinates: GeoCoordinates,
    val depthKilometers: Double?,
    val occurredAt: Instant,
    val updatedAt: Instant,
    val detailUrl: String?,
    val magType: String? = null,
    val reviewStatus: String? = null
) {
    init {
        require(id.isNotBlank()) {
            "Earthquake ID cannot be blank"
        }

        require(magnitude == null || magnitude.isFinite()) {
            "Magnitude must be finite when available"
        }

        require(depthKilometers == null || depthKilometers.isFinite()) {
            "Depth must be finite when available"
        }
    }
}