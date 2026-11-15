package com.cgaraydev.seismicexplorer.presentation.map.location

import java.util.concurrent.TimeUnit

/** Valida la edad de una ubicación utilizando el reloj monotónico del dispositivo. */
internal object LocationAgePolicy {
    fun isFresh(
        locationElapsedNanos: Long,
        nowElapsedNanos: Long,
        maxAgeMs: Long
    ): Boolean {
        if (maxAgeMs < 0 || locationElapsedNanos > nowElapsedNanos) return false
        val age = nowElapsedNanos - locationElapsedNanos
        return age in 0L..TimeUnit.MILLISECONDS.toNanos(maxAgeMs)
    }
}
