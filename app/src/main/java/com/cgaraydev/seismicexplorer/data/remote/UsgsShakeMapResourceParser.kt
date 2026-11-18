package com.cgaraydev.seismicexplorer.data.remote

import com.cgaraydev.seismicexplorer.domain.model.ShakeMapProduct
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
import java.net.URI
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Selecciona una versión activa/preferida de ShakeMap y descubre referencias seguras.
 * NO descarga, decodifica, descomprime ni interpreta las imágenes, rejillas o estaciones.
 */
internal object UsgsShakeMapResourceParser {
    private val supported = listOf(
        "download/intensity.jpg" to ShakeMapResourceKind.INTENSITY_MAP,
        "download/intensity_overlay.png" to ShakeMapResourceKind.INTENSITY_OVERLAY,
        "download/grid.xml" to ShakeMapResourceKind.GRID_XML,
        "download/stationlist.json" to ShakeMapResourceKind.STATION_OBSERVATIONS
    )

    fun extract(element: JsonElement?): ShakeMapProduct? {
        val candidates = (element as? JsonArray)?.mapIndexedNotNull { index, item ->
            val product = item as? JsonObject ?: return@mapIndexedNotNull null
            if (product.string("status").equals("DELETE", ignoreCase = true)) {
                return@mapIndexedNotNull null
            }
            val properties = product["properties"] as? JsonObject
            if (properties?.string("map-status").equals("CANCELLED", ignoreCase = true)) {
                return@mapIndexedNotNull null
            }
            if (properties?.string("event-type")?.uppercase() in
                setOf("TEST", "SCENARIO", "HEARTBEAT")
            ) {
                return@mapIndexedNotNull null
            }
            IndexedProduct(index, product)
        } ?: return null

        // ComCat prefiere mayor preferredWeight; ante empate, mayor updateTime.
        // Si faltan ambos metadatos, mantenemos el orden de la fuente.
        val selected = candidates.maxWithOrNull(
            compareBy<IndexedProduct> { it.data.long("preferredWeight") ?: Long.MIN_VALUE }
                .thenBy { it.data.long("updateTime") ?: Long.MIN_VALUE }
                .thenBy { -it.index }
        )?.data ?: return null

        val contents = selected["contents"] as? JsonObject
        val resources = supported.mapNotNull { (path, kind) ->
            val metadata = contents?.get(path) as? JsonObject ?: return@mapNotNull null
            val url = metadata.string("url")?.takeIf { isTrustedResourceUrl(it, path) }
                ?: return@mapNotNull null
            val size = metadata.long("length")?.takeIf { it >= 0L }
            ShakeMapResource(
                kind = kind,
                url = url,
                contentType = metadata.string("contentType"),
                lengthBytes = size
            )
        }

        return ShakeMapProduct(
            source = selected.string("source"),
            code = selected.string("code"),
            updatedAtEpochMillis = selected.long("updateTime")?.takeIf { it > 0L },
            mapStatus = (selected["properties"] as? JsonObject)?.string("map-status"),
            resources = resources
        )
    }

    private data class IndexedProduct(val index: Int, val data: JsonObject)

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull

    private fun isTrustedResourceUrl(raw: String, contentPath: String): Boolean {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals("earthquake.usgs.gov", ignoreCase = true) ||
            uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
            (uri.port != -1 && uri.port != 443)
        ) return false
        val path = uri.rawPath ?: return false
        if ('%' in path || path.split('/').any { it == ".." || it == "." }) return false
        if (!path.startsWith("/realtime/product/shakemap/") &&
            !path.startsWith("/archive/product/shakemap/")
        ) return false
        return path.endsWith("/$contentPath")
    }
}
