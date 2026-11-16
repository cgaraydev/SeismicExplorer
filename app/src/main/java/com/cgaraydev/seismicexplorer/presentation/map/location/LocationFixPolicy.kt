package com.cgaraydev.seismicexplorer.presentation.map.location

/** Verifica la validez de una posición y su edad sin depender de APIs Android. */
internal object LocationFixPolicy {
    fun isUsable(
        latitude: Double,
        longitude: Double,
        locationElapsedNanos: Long,
        nowElapsedNanos: Long,
        maxAgeMs: Long
    ): Boolean {
        return latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0 &&
            LocationAgePolicy.isFresh(
                locationElapsedNanos = locationElapsedNanos,
                nowElapsedNanos = nowElapsedNanos,
                maxAgeMs = maxAgeMs
            )
    }
}
