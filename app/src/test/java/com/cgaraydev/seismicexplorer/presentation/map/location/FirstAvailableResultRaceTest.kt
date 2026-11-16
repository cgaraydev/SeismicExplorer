package com.cgaraydev.seismicexplorer.presentation.map.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirstAvailableResultRaceTest {

    @Test
    fun simultaneousFixes_canChooseWinnerWithoutChannelError() = runTest {
        val release = CompletableDeferred<Unit>()

        val result = async {
            firstAvailableResult(
                candidates = listOf<suspend () -> String?>(
                    { release.await(); "google" },
                    { release.await(); "native" }
                ),
                timeoutMillis = 1_000L
            )
        }

        runCurrent()
        release.complete(Unit)

        assertTrue(result.await() in setOf("google", "native"))
    }
}
