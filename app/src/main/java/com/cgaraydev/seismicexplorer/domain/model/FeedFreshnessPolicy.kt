package com.cgaraydev.seismicexplorer.domain.model

import java.time.Duration

object FeedFreshnessPolicy {

    fun shouldRefresh(
        window: RecentWindow,
        lastSuccessEpochMillis: Long?,
        nowEpochMillis: Long
    ): Boolean {
        if (lastSuccessEpochMillis == null) return true

        // Un registro futuro puede indicar que cambió
        // el reloj del dispositivo.
        if (lastSuccessEpochMillis > nowEpochMillis) {
            return true
        }

        val ageMillis = nowEpochMillis - lastSuccessEpochMillis

        return ageMillis >= maxAge(window).toMillis()
    }

    private fun maxAge(window: RecentWindow): Duration {
        return when (window) {
            RecentWindow.LAST_DAY -> Duration.ofMinutes(5)
            RecentWindow.LAST_WEEK -> Duration.ofMinutes(15)
            RecentWindow.LAST_30_DAYS -> Duration.ofMinutes(60)
        }
    }
}