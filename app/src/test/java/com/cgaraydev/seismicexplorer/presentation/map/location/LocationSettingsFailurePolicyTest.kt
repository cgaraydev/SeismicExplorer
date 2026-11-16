package com.cgaraydev.seismicexplorer.presentation.map.location

import com.google.android.gms.location.LocationSettingsStatusCodes
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSettingsFailurePolicyTest {

    @Test
    fun resolvableStatusRequestsUserAction() {
        assertEquals(
            LocationSettingsFailure.RESOLUTION_REQUIRED,
            LocationSettingsFailurePolicy.classify(
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED
            )
        )
    }

    @Test
    fun unavailableStatusIsNotTreatedAsResolvable() {
        assertEquals(
            LocationSettingsFailure.UNAVAILABLE,
            LocationSettingsFailurePolicy.classify(
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
            )
        )
    }

    @Test
    fun unknownStatusDoesNotBlockNativeFallback() {
        assertEquals(
            LocationSettingsFailure.UNKNOWN,
            LocationSettingsFailurePolicy.classify(-12345)
        )
    }
}
