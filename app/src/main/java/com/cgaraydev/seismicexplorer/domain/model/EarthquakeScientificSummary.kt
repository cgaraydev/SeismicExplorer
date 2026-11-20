package com.cgaraydev.seismicexplorer.domain.model

/**
 * Resumen cientifico PUBLICADO por USGS en el GeoJSON Summary.
 * No equivale a productos descargados, danos observados ni alertas oficiales.
 * Null significa dato ausente o no valido, nunca cero.
 */
data class EarthquakeScientificSummary(
    val feltReports: Int? = null,
    val communityIntensity: Double? = null,
    val modeledIntensity: Double? = null,
    val pagerAlertLevel: String? = null,
    val tsunamiIndicator: Int? = null,
    val significance: Int? = null,
    val network: String? = null,
    val networkEventCode: String? = null,
    val associatedIds: String? = null,
    val contributingNetworks: String? = null,
    val publishedProductCodes: String? = null,
    val stationCount: Int? = null,
    val nearestStationDistanceDegrees: Double? = null,
    val timeResidualRmsSeconds: Double? = null,
    val azimuthalGapDegrees: Double? = null,
    val detailGeoJsonUrl: String? = null
)
