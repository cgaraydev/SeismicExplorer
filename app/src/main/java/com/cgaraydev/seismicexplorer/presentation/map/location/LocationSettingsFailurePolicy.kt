package com.cgaraydev.seismicexplorer.presentation.map.location

import com.google.android.gms.location.LocationSettingsStatusCodes

/** Diferencia ajustes corregibles de los que requieren intervención manual. */
internal enum class LocationSettingsFailure {
    RESOLUTION_REQUIRED,
    UNAVAILABLE,
    UNKNOWN
}

internal object LocationSettingsFailurePolicy {
    fun classify(statusCode: Int): LocationSettingsFailure = when (statusCode) {
        LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
            LocationSettingsFailure.RESOLUTION_REQUIRED
        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
            LocationSettingsFailure.UNAVAILABLE
        else -> LocationSettingsFailure.UNKNOWN
    }
}
