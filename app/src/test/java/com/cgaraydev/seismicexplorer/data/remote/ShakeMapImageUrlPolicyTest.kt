package com.cgaraydev.seismicexplorer.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeMapImageUrlPolicyTest {
    private val good = "https://earthquake.usgs.gov/realtime/product/shakemap/us123/us/1700000000000/download/intensity.jpg"
    @Test fun validUsGovImageIsAccepted() {
        assertTrue(ShakeMapImageUrlPolicy.accepts(good))
        assertTrue(ShakeMapImageUrlPolicy.accepts(good.replace("/realtime/", "/archive/")))
        assertTrue(ShakeMapImageUrlPolicy.accepts(good.replace("/realtime/product/", "/product/")))
        assertTrue(ShakeMapImageUrlPolicy.accepts(
            "https://earthquake.usgs.gov/product/shakemap/uw61535367~SEA/uw/1563320390893/download/intensity.jpg"
        ))
    }
    @Test fun acceptsPdlUrnStructureForRecentUsgsProducts() {
        val prefix = "https://earthquake.usgs.gov/pdl/products/" +
            "urn:usgs-product:us:shakemap:us7000tz37:1795036656107/contents/"
        assertTrue(ShakeMapImageUrlPolicy.accepts(prefix + "download/intensity.jpg"))
        listOf("download/intensity.jpg", "download/intensity_overlay.png",
            "download/grid.xml", "download/stationlist.json").forEach { suffix ->
            assertTrue(ShakeMapImageUrlPolicy.acceptsResource(prefix + suffix, suffix))
        }
        assertFalse(ShakeMapImageUrlPolicy.accepts(prefix + "download/grid.xml"))
        assertFalse(ShakeMapImageUrlPolicy.acceptsResource(
            prefix + "download/unexpected.zip", "download/unexpected.zip"))
        assertTrue(ShakeMapImageUrlPolicy.accepts(
            prefix.replace(":us:shakemap:us7000tz37:", ":ak:shakemap:ak025xx:") +
                "download/intensity.jpg"))
        assertTrue(ShakeMapImageUrlPolicy.accepts(
            prefix.replace(":us:shakemap:us7000tz37:", ":ci:shakemap:ci41363167:") +
                "download/intensity.jpg"))
    }

    @Test fun rejectsMaliciousOrMalformedPdlResources() {
        val url = "https://earthquake.usgs.gov/pdl/products/" +
            "urn:usgs-product:us:shakemap:us7000tz37:1795036656107/contents/download/intensity.jpg"
        listOf(
            url.replace("https:", "http:"),
            url.replace("earthquake.usgs.gov", "earthquake.usgs.gov.attacker.test"),
            url.replace("https://", "https://user@"),
            url.replace("earthquake.usgs.gov/", "earthquake.usgs.gov:8443/"),
            url.replace(":shakemap:", ":moment-tensor:"),
            url.replace("urn:usgs-product:", "urn:example-product:"),
            url.replace(":us:shakemap:", "::shakemap:"),
            url.replace(":1795036656107/", ":bad-timestamp/"),
            url.replace("/contents/", "/wrong/") ,
            url.replace("/contents/", "//contents/"),
            url.replace("/pdl/products/", "/pdl/products/../"),
            url.replace("/contents/", "/contents/%2e%2e/"),
            url.replace("intensity.jpg", "intensity.jpg.exe"),
            url + "?redirect=1",
            url + "#anchor"
        ).forEach { assertFalse(it, ShakeMapImageUrlPolicy.accepts(it)) }
    }

    @Test fun untrustedOrUnexpectedResourcesAreRejected() {
        listOf(
            good.replace("earthquake.usgs.gov", "earthquake.usgs.gov.attacker.com"),
            good.replace("https:", "http:"),
            good.replace("intensity.jpg", "grid.xml"),
            good.replace("/realtime/product/", "/other/product/"),
            "https://earthquake.usgs.gov/product/shakemap/us123/us/1700000000000/download/intensity.jpg?x=1",
            good + "?redirect=1", good + "#fragment",
            good.replace("/us123/", "/../"),
            "https://user@earthquake.usgs.gov/realtime/product/shakemap/us123/us/123/download/intensity.jpg",
            "https://earthquake.usgs.gov:8443/realtime/product/shakemap/us123/us/123/download/intensity.jpg",
        ).forEach { assertFalse(it, ShakeMapImageUrlPolicy.accepts(it)) }
    }
}
