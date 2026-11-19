package com.cgaraydev.seismicexplorer.data.remote

import java.net.URI

/**
 * Política única para las referencias ShakeMap publicadas por USGS.
 * El parser usa acceptsResource() para descubrir los cuatro recursos soportados.
 * El descargador usa accepts(): SOLO autoriza el JPEG de intensidad.
 */
object ShakeMapImageUrlPolicy {
    private val knownResources = setOf(
        "download/intensity.jpg",
        "download/intensity_overlay.png",
        "download/grid.xml",
        "download/stationlist.json"
    )

    private val legacyPrefixes = listOf(
        "/product/shakemap/",
        "/realtime/product/shakemap/",
        "/archive/product/shakemap/"
    )
    private const val PDL_PREFIX = "/pdl/products/"

    private val legacySegment = Regex("[A-Za-z0-9_~-]+")
    // URN: source, product type, product code, updateTime.
    // No inferir el código del producto desde el ID del evento del catálogo.
    private val pdlIdentifier = Regex(
        "urn:usgs-product:[A-Za-z0-9_-]{1,32}:shakemap:[A-Za-z0-9_~.-]{1,160}:[0-9]{1,17}"
    )

    /** Defensa independiente antes de cualquier descarga de imagen. */
    fun accepts(url: String): Boolean = acceptsResource(url, "download/intensity.jpg")

    /** Comparte exactamente la misma validación para el inventario GeoJSON. */
    internal fun acceptsResource(url: String, contentPath: String): Boolean {
        if (contentPath !in knownResources) return false

        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals("earthquake.usgs.gov", ignoreCase = true) ||
            uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
            (uri.port != -1 && uri.port != 443)
        ) return false

        val path = uri.rawPath ?: return false
        // Nunca aceptar codificación de ruta, segmentos vacíos, traversal ni sufijos arbitrarios.
        if ('%' in path || path.split('/').drop(1).any { it.isEmpty() || it == "." || it == ".." }) {
            return false
        }
        val legacySuffix = "/$contentPath"
        if (path.endsWith(legacySuffix)) {
            val legacyPrefix = legacyPrefixes.firstOrNull(path::startsWith)
            if (legacyPrefix != null) {
                val body = path.removePrefix(legacyPrefix).removeSuffix(legacySuffix)
                val parts = body.split('/')
                return parts.size == 3 && parts.all(legacySegment::matches)
            }
        }

        // Estructura de URLs PDL observada en ShakeMap recientes de ComCat:
        // /pdl/products/urn:usgs-product:{source}:shakemap:{code}:{updateTime}
        //     /contents/download/intensity.jpg
        val pdlSuffix = "/contents/$contentPath"
        if (!path.startsWith(PDL_PREFIX) || !path.endsWith(pdlSuffix)) return false
        val urn = path.removePrefix(PDL_PREFIX).removeSuffix(pdlSuffix)
        return pdlIdentifier.matches(urn)
    }
}
