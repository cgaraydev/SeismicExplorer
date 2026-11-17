package com.cgaraydev.seismicexplorer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val AtlasLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    surfaceTint = LightPrimary,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

internal val AtlasDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    surfaceTint = DarkPrimary,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

@Composable
fun SeismicExplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    // La consulta del proveedor es bloqueante (@WorkerThread): nunca se ejecuta
    // durante la composición. Mientras se verifica, usamos sans-serif local.
    val canRequestManrope = produceState(
        initialValue = false,
        key1 = applicationContext
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                AtlasGoogleFontProvider.isAvailableOnDevice(applicationContext)
            }.getOrDefault(false)
        }
    }.value
    MaterialTheme(
        colorScheme = if (darkTheme) AtlasDarkColorScheme else AtlasLightColorScheme,
        typography = if (canRequestManrope) ManropeTypography else Typography,
        content = content
    )
}
