package com.cgaraydev.seismicexplorer.data.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapStyleProviderTest {
    @Test
    fun selectsLightAndDarkBasemaps() {
        assertEquals(
            MapStyleProvider.OPEN_FREE_MAP,
            MapStyleProvider.forDarkTheme(false)
        )
        assertEquals(
            MapStyleProvider.OPEN_FREE_MAP_DARK,
            MapStyleProvider.forDarkTheme(true)
        )
        assertEquals(
            "https://tiles.openfreemap.org/styles/dark",
            MapStyleProvider.OPEN_FREE_MAP_DARK.styleUrl
        )
    }
}
