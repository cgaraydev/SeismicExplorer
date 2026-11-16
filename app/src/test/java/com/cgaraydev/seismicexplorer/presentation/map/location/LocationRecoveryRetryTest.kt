package com.cgaraydev.seismicexplorer.presentation.map.location

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationRecoveryRetryTest {

    @Test
    fun timeoutDoesNotPreventSuccessfulNextAttempt() = runTest {
        val timedOut = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                { awaitCancellation() }
            ),
            timeoutMillis = 50L
        )

        val recovered = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                { "network-fix" }
            ),
            timeoutMillis = 50L
        )

        assertNull(timedOut)
        assertEquals("network-fix", recovered)
    }
}
