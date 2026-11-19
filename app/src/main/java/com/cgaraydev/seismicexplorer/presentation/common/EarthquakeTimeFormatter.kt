package com.cgaraydev.seismicexplorer.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dos representaciones del MISMO instante; ninguna infiere la zona civil del epicentro. */
object EarthquakeTimeFormatter {
    fun local(instant: Instant, deviceZone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm O", locale)
            .withZone(deviceZone)
            .format(instant)

    fun utc(instant: Instant, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm 'UTC'", locale)
            .withZone(ZoneOffset.UTC)
            .format(instant)
}
