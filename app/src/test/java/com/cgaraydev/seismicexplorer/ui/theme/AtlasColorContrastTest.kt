package com.cgaraydev.seismicexplorer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasColorContrastTest {

    @Test fun lightThemeTextOnSurface_meetsWcagAaForNormalText() {
        assertTrue(contrast(AtlasLightColorScheme.onSurface, AtlasLightColorScheme.surface) >= 4.5)
        assertTrue(contrast(AtlasLightColorScheme.onSurfaceVariant, AtlasLightColorScheme.background) >= 4.5)
    }

    @Test fun darkThemeTextOnSurface_meetsWcagAaForNormalText() {
        assertTrue(contrast(AtlasDarkColorScheme.onSurface, AtlasDarkColorScheme.surface) >= 4.5)
        assertTrue(contrast(AtlasDarkColorScheme.onSurfaceVariant, AtlasDarkColorScheme.background) >= 4.5)
    }

    @Test fun principalButtonsHaveReadableText() {
        assertTrue(contrast(AtlasLightColorScheme.onPrimary, AtlasLightColorScheme.primary) >= 4.5)
        assertTrue(contrast(AtlasDarkColorScheme.onPrimary, AtlasDarkColorScheme.primary) >= 4.5)
    }

    private fun contrast(a: Color, b: Color): Double {
        val light = maxOf(a.luminance().toDouble(), b.luminance().toDouble())
        val dark = minOf(a.luminance().toDouble(), b.luminance().toDouble())
        return (light + 0.05) / (dark + 0.05)
    }
}
