package com.cgaraydev.seismicexplorer.data.map

enum class MapStyleProvider(
    val styleUrl: String
) {
    OPEN_FREE_MAP(
        "https://tiles.openfreemap.org/styles/liberty"
    ),
    OPEN_FREE_MAP_FIORD(
        "https://tiles.openfreemap.org/styles/fiord"
    );

    companion object {
        fun forDarkTheme(dark: Boolean): MapStyleProvider =
            if (dark) OPEN_FREE_MAP_FIORD else OPEN_FREE_MAP
    }
}