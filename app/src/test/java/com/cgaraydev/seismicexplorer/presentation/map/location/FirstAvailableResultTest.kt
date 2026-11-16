package com.cgaraydev.seismicexplorer.presentation.map.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirstAvailableResultTest {

    @Test
    fun firstNonNullResultWinsWithoutWaitingForSlowProvider() = runTest {
        var slowProviderCancelled = false

        val result = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                {
                    try {
                        awaitCancellation()
                    } finally {
                        slowProviderCancelled = true
                    }
                },
                { "native-network-fix" }
            ),
            timeoutMillis = 10_000L
        )

        assertEquals("native-network-fix", result)
        assertTrue(slowProviderCancelled)
    }

    @Test
    fun nullFromOneProviderDoesNotDiscardOtherResult() = runTest {
        val result = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                { null },
                { "google-fix" }
            ),
            timeoutMillis = 10_000L
        )

        assertEquals("google-fix", result)
    }

    @Test
    fun allNullResultsFinishWithoutWaitingForGlobalTimeout() = runTest {
        val result = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                { null },
                { null }
            ),
            timeoutMillis = 10_000L
        )

        assertNull(result)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun timeoutCancelsEveryPendingProvider() = runTest {
        var firstCancelled = false
        var secondCancelled = false

        val result = firstAvailableResult(
            candidates = listOf<suspend () -> String?>(
                {
                    try {
                        awaitCancellation()
                    } finally {
                        firstCancelled = true
                    }
                },
                {
                    try {
                        awaitCancellation()
                    } finally {
                        secondCancelled = true
                    }
                }
            ),
            timeoutMillis = 500L
        )

        assertNull(result)
        assertEquals(500L, testScheduler.currentTime)
        assertTrue(firstCancelled)
        assertTrue(secondCancelled)
    }

    @Test
    fun callerCancellationPropagatesAndCancelsProvider() = runTest {
        val started = CompletableDeferred<Unit>()
        var cancelled = false

        val request = launch {
            firstAvailableResult(
                candidates = listOf<suspend () -> String?>(
                    {
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                ),
                timeoutMillis = 10_000L
            )
        }

        runCurrent()
        started.await()
        request.cancelAndJoin()

        assertTrue(request.isCancelled)
        assertTrue(cancelled)
    }

    @Test
    fun noProvidersReturnsImmediately() = runTest {
        val result = firstAvailableResult<String>(
            candidates = emptyList(),
            timeoutMillis = 10_000L
        )
        assertNull(result)
        assertEquals(0L, testScheduler.currentTime)
    }
}
