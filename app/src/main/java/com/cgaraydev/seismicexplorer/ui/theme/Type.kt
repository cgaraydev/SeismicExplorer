package com.cgaraydev.seismicexplorer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font as DeviceFont
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleDownloadFont
import androidx.compose.ui.text.googlefonts.R as GoogleFontResources
import androidx.compose.ui.unit.sp

internal val AtlasGoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = GoogleFontResources.array.com_google_android_gms_fonts_certs
)

private val manrope = GoogleFont("Manrope")

@OptIn(ExperimentalTextApi::class)
private val manropeFamily = FontFamily(
    GoogleDownloadFont(googleFont = manrope, fontProvider = AtlasGoogleFontProvider, weight = FontWeight.Normal),
    DeviceFont(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Normal),
    GoogleDownloadFont(googleFont = manrope, fontProvider = AtlasGoogleFontProvider, weight = FontWeight.Medium),
    DeviceFont(DeviceFontFamilyName("sans-serif-medium"), weight = FontWeight.Medium),
    GoogleDownloadFont(googleFont = manrope, fontProvider = AtlasGoogleFontProvider, weight = FontWeight.SemiBold),
    DeviceFont(DeviceFontFamilyName("sans-serif"), weight = FontWeight.SemiBold),
    GoogleDownloadFont(googleFont = manrope, fontProvider = AtlasGoogleFontProvider, weight = FontWeight.Bold),
    DeviceFont(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold)
)

private fun atlasTypography(family: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = family, fontSize = 54.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontFamily = family, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontFamily = family, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = family, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = family, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = family, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = family, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = family, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = family, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = family, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

// Variante 100% local: útil para dispositivos sin Google Play Services y previews offline.
val Typography = atlasTypography(FontFamily.SansSerif)
internal val ManropeTypography = atlasTypography(manropeFamily)
