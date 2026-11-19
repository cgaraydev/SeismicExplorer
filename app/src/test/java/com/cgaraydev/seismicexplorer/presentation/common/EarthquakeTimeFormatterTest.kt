package com.cgaraydev.seismicexplorer.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class EarthquakeTimeFormatterTest {
    private val spanish = Locale.forLanguageTag("es-CL")

    @Test fun showsUtcBelowTheSameInstantInDeviceTimeZone() {
        val instant = Instant.parse("2026-11-19T15:42:00Z")
        val local = EarthquakeTimeFormatter.local(instant, ZoneId.of("America/Santiago"), spanish)
        val utc = EarthquakeTimeFormatter.utc(instant, spanish)
        assertTrue("La hora local debe usar la zona del dispositivo", local.contains("12:42"))
        assertTrue("La referencia universal debe conservar el instante", utc.contains("15:42 UTC"))
    }

    @Test fun differentDatesAcrossTimeZonesDoNotChangeInstant() {
        val instant = Instant.parse("2026-01-01T00:10:00Z")
        val local = EarthquakeTimeFormatter.local(instant, ZoneId.of("America/Santiago"), spanish)
        val utc = EarthquakeTimeFormatter.utc(instant, spanish)
        assertTrue(local.contains("2025"))
        assertTrue(local.contains("21:10"))
        assertTrue(utc.contains("2026"))
        assertTrue(utc.contains("00:10 UTC"))
    }
}
