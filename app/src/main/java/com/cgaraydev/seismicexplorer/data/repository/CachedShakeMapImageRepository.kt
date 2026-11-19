package com.cgaraydev.seismicexplorer.data.repository

import android.content.Context
import com.cgaraydev.seismicexplorer.data.remote.ShakeMapImageUrlPolicy
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageResult
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
import com.cgaraydev.seismicexplorer.domain.repository.ShakeMapImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Caché separada de Room. Solo JPEG de intensidad, con descarga iniciada por el usuario. */
@Singleton
class CachedShakeMapImageRepository private constructor(
    private val directory: File,
    private val http: OkHttpClient
) : ShakeMapImageRepository {
    @Inject constructor(@ApplicationContext context: Context) : this(
        File(context.cacheDir, "usgs_shakemap_jpeg"), defaultClient()
    )

    /** Permite probar la política con un interceptor de OkHttp y sin Internet. */
    companion object {
        const val MAX_JPEG_BYTES = 4 * 1024 * 1024
        private const val TTL_MS = 15L * 60_000L
        private const val RETENTION_MS = 7L * 24L * 60L * 60_000L
        private const val MAX_FILES = 12
        private fun defaultClient() = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        internal fun forTesting(directory: File, http: OkHttpClient) =
            CachedShakeMapImageRepository(directory, http)
    }

    private val mutex = Mutex()

    override suspend fun load(resource: ShakeMapResource): ShakeMapImageResult = withContext(Dispatchers.IO) {
        if (resource.kind != ShakeMapResourceKind.INTENSITY_MAP ||
            !ShakeMapImageUrlPolicy.accepts(resource.url)) {
            return@withContext ShakeMapImageResult.Failed(ShakeMapImageFailure.INVALID_RESOURCE)
        }
        if ((resource.lengthBytes ?: 0L) > MAX_JPEG_BYTES) {
            return@withContext ShakeMapImageResult.Failed(ShakeMapImageFailure.TOO_LARGE)
        }
        mutex.withLock { getOrFetch(resource.url) }
    }

    private fun getOrFetch(url: String): ShakeMapImageResult {
        directory.mkdirs()
        val target = File(directory, hashedName(url))
        val now = System.currentTimeMillis()
        val age = now - target.lastModified()
        val canUseCache = target.isFile && age in 0..RETENTION_MS &&
            target.length() in 3..MAX_JPEG_BYTES.toLong()
        val cached = if (canUseCache) runCatching { target.readBytes() }
            .getOrNull()?.takeIf(::isJpeg) else null
        if (cached != null && age <= TTL_MS) {
            return ShakeMapImageResult.Available(cached, fromCache = true, isStale = false)
        }
        return try {
            val request = Request.Builder().url(url).header("Accept", "image/jpeg").get().build()
            http.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> Unit
                    404, 410 -> return ShakeMapImageResult.Failed(ShakeMapImageFailure.NOT_FOUND)
                    else -> return staleOrError(cached, ShakeMapImageFailure.SERVICE)
                }
                val body = response.body ?: return staleOrError(cached, ShakeMapImageFailure.INVALID_IMAGE)
                val type = body.contentType()
                if (type != null && (type.type != "image" || type.subtype != "jpeg")) {
                    return staleOrError(cached, ShakeMapImageFailure.INVALID_IMAGE)
                }
                if (body.contentLength() > MAX_JPEG_BYTES) {
                    return staleOrError(cached, ShakeMapImageFailure.TOO_LARGE)
                }
                val output = ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val chunk = ByteArray(8192)
                    while (true) {
                        val length = input.read(chunk)
                        if (length < 0) break
                        if (output.size() + length > MAX_JPEG_BYTES) {
                            return staleOrError(cached, ShakeMapImageFailure.TOO_LARGE)
                        }
                        output.write(chunk, 0, length)
                    }
                }
                val bytes = output.toByteArray()
                if (!isJpeg(bytes)) return staleOrError(cached, ShakeMapImageFailure.INVALID_IMAGE)
                // Escritura best effort: una caché no debe impedir ver un recurso ya obtenido.
                runCatching {
                    val temp = File.createTempFile("map_", ".tmp", directory)
                    try {
                        temp.writeBytes(bytes)
                        if (!temp.renameTo(target)) {
                            target.writeBytes(bytes)
                        }
                        target.setLastModified(now)
                    } finally { temp.delete() }
                    prune(now)
                }
                ShakeMapImageResult.Available(bytes, fromCache = false, isStale = false)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            staleOrError(cached, ShakeMapImageFailure.CONNECTION)
        }
    }

    private fun staleOrError(cached: ByteArray?, reason: ShakeMapImageFailure): ShakeMapImageResult =
        if (cached != null) ShakeMapImageResult.Available(cached, fromCache = true, isStale = true)
        else ShakeMapImageResult.Failed(reason)

    private fun prune(now: Long) {
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "jpg" } ?: return
        files.filter { now - it.lastModified() > RETENTION_MS }.forEach { it.delete() }
        directory.listFiles()?.filter { it.isFile && it.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }?.drop(MAX_FILES)?.forEach { it.delete() }
    }

    private fun isJpeg(bytes: ByteArray): Boolean = bytes.size >= 3 &&
        (bytes[0].toInt() and 255) == 255 && (bytes[1].toInt() and 255) == 216 &&
        (bytes[2].toInt() and 255) == 255

    private fun hashedName(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + ".jpg"
    }
}
