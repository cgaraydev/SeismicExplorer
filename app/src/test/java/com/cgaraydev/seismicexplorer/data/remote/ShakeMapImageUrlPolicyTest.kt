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
