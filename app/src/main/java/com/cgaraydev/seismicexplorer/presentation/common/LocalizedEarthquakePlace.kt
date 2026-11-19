package com.cgaraydev.seismicexplorer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.cgaraydev.seismicexplorer.R

/** Traduce la descripción en presentación sin alterar el texto USGS persistido. */
@Composable
fun localizedEarthquakePlace(raw: String?): String =
    EarthquakePlaceFormatter.format(raw, LocalConfiguration.current.locales[0])
        ?: stringResource(R.string.home_event_place_unknown)
