package com.cgaraydev.seismicexplorer.data.remote

import java.net.URI

/** Defensa adicional: nunca confiar solamente en una URL extraída de GeoJSON. */
object ShakeMapImageUrlPolicy {
    fun accepts(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals("earthquake.usgs.gov", ignoreCase = true) ||
            uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
            (uri.port != -1 && uri.port != 443)) return false
        val path = uri.rawPath ?: return false
        if ('%' in path || path.split('/').any { it == "." || it == ".." }) return false
        val prefix = path.startsWith("/product/shakemap/") ||
            path.startsWith("/realtime/product/shakemap/") ||
            path.startsWith("/archive/product/shakemap/")
        if (!prefix || !path.endsWith("/download/intensity.jpg")) return false
        val middle = path.removePrefix("/product/shakemap/")
            .removePrefix("/realtime/product/shakemap/")
            .removePrefix("/archive/product/shakemap/")
            .removeSuffix("/download/intensity.jpg")
        return middle.split('/').size == 3 && middle.split('/').all {
            it.isNotBlank() && it.matches(Regex("[A-Za-z0-9_~-]+"))
        }
    }
}
