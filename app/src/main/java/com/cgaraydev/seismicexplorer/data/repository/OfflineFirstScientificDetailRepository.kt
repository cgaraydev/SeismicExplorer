package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.ScientificDetailCacheDao
import com.cgaraydev.seismicexplorer.data.local.entity.ScientificDetailCacheEntity
import com.cgaraydev.seismicexplorer.data.remote.UsgsScientificDetailParser
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsScientificDetailApi
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetail
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource
import com.cgaraydev.seismicexplorer.domain.repository.ScientificDetailRepository
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException

/**
 * GeoJSON Detail bajo demanda. Nunca se llama al abrir los feeds.
 * Un unico mutex evita descargas duplicadas; el maximo de entradas limita el disco.
 */
@Singleton
class OfflineFirstScientificDetailRepository @Inject constructor(
    private val api: UsgsScientificDetailApi,
    private val cache: ScientificDetailCacheDao,
    private val parser: UsgsScientificDetailParser,
    private val clock: Clock
) : ScientificDetailRepository {

    private val mutex = Mutex()
    private val validId = Regex("^[A-Za-z0-9_-]{1,80}$")

    override suspend fun getDetail(
        eventId: String,
        knownUpdatedAtEpochMillis: Long?,
        forceRefresh: Boolean
    ): ScientificDetailResult {
        if (!validId.matches(eventId)) {
            return ScientificDetailResult.Failed(ScientificDetailFailure.INVALID_EVENT_ID)
        }
        return mutex.withLock { load(eventId, knownUpdatedAtEpochMillis, forceRefresh) }
    }

    private suspend fun load(
        eventId: String,
        knownUpdatedAtEpochMillis: Long?,
        forceRefresh: Boolean
    ): ScientificDetailResult {
        val now = clock.millis()
        val existing = cache.get(eventId)?.let { entity ->
            val parsed = parseOffMain(entity.payloadJson, eventId)
            if (parsed == null || parsed.sourceUpdatedAtEpochMillis != entity.sourceUpdatedAtEpochMillis) {
                null
            } else {
                entity to parsed
            }
        }

        val cachedAge = existing?.first?.let { now - it.fetchedAtEpochMillis }
        val recentCache = cachedAge != null && cachedAge >= 0L && cachedAge <= CACHE_TTL_MILLIS
        val sameOrNewer = knownUpdatedAtEpochMillis == null ||
            (existing != null && existing.second.sourceUpdatedAtEpochMillis >= knownUpdatedAtEpochMillis)
        if (!forceRefresh && existing != null && recentCache && sameOrNewer) {
            return available(existing.first, existing.second, ScientificDetailSource.CACHE, false)
        }

        val raw = try {
            api.getDetail(eventId).use { body -> readBounded(body) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpException) {
            if (exception.code() == 404 || exception.code() == 410) {
                return ScientificDetailResult.Failed(ScientificDetailFailure.EVENT_NOT_FOUND)
            }
            return staleOrFailure(existing, cachedAge, ScientificDetailFailure.SERVICE)
        } catch (exception: ResponseTooLargeException) {
            return staleOrFailure(existing, cachedAge, ScientificDetailFailure.RESPONSE_TOO_LARGE)
        } catch (exception: IOException) {
            return staleOrFailure(existing, cachedAge, ScientificDetailFailure.CONNECTION)
        }

        val parsed = parseOffMain(raw, eventId)
            ?: return staleOrFailure(existing, cachedAge, ScientificDetailFailure.INVALID_RESPONSE)

        // No degradar un evento ya conocido aunque el servidor devuelva una revision anterior.
        // Una copia caducada (>7 dias) se conserva en disco, pero no se presenta como vigente.
        if (existing != null &&
            parsed.sourceUpdatedAtEpochMillis < existing.second.sourceUpdatedAtEpochMillis
        ) {
            return if (cachedAge != null && cachedAge >= 0L && cachedAge <= CACHE_RETENTION_MILLIS) {
                available(existing.first, existing.second, ScientificDetailSource.CACHE, true)
            } else {
                ScientificDetailResult.Failed(ScientificDetailFailure.INVALID_RESPONSE)
            }
        }

        val entity = ScientificDetailCacheEntity(
            eventId = eventId,
            sourceUpdatedAtEpochMillis = parsed.sourceUpdatedAtEpochMillis,
            fetchedAtEpochMillis = now,
            payloadJson = raw
        )
        cache.upsert(entity)
        cache.deleteOlderThan(now - CACHE_RETENTION_MILLIS)
        cache.keepLatest(MAX_CACHE_ENTRIES)
        val isStale = knownUpdatedAtEpochMillis != null &&
            parsed.sourceUpdatedAtEpochMillis < knownUpdatedAtEpochMillis
        return available(entity, parsed, ScientificDetailSource.NETWORK, isStale)
    }

    /** El parsing de JSON, tanto de red como de Room, nunca se ejecuta en Main. */
    private suspend fun parseOffMain(
        raw: String,
        eventId: String
    ): UsgsScientificDetailParser.Parsed? = withContext(Dispatchers.Default) {
        try {
            parser.parse(raw, eventId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: RuntimeException) {
            // Payload malformado: no debe provocar crash ni romper la politica offline-first.
            null
        }
    }

    private fun staleOrFailure(
        existing: Pair<ScientificDetailCacheEntity, UsgsScientificDetailParser.Parsed>?,
        ageMillis: Long?,
        reason: ScientificDetailFailure
    ): ScientificDetailResult {
        if (existing != null && ageMillis != null &&
            ageMillis >= 0L && ageMillis <= CACHE_RETENTION_MILLIS
        ) {
            return available(existing.first, existing.second, ScientificDetailSource.CACHE, true)
        }
        return ScientificDetailResult.Failed(reason)
    }

    private fun available(
        entity: ScientificDetailCacheEntity,
        parsed: UsgsScientificDetailParser.Parsed,
        source: ScientificDetailSource,
        stale: Boolean
    ) = ScientificDetailResult.Available(
        detail = ScientificDetail(
            eventId = parsed.eventId,
            sourceUpdatedAtEpochMillis = parsed.sourceUpdatedAtEpochMillis,
            fetchedAtEpochMillis = entity.fetchedAtEpochMillis,
            availableProductTypes = parsed.availableProductTypes,
            availableProductCount = parsed.availableProductCount,
            shakeMap = parsed.shakeMap
        ),
        source = source,
        isStale = stale
    )

    private suspend fun readBounded(body: ResponseBody): String = withContext(Dispatchers.IO) {
        if (body.contentLength() > MAX_DETAIL_BYTES) throw ResponseTooLargeException()
        val result = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val chunk = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(chunk)
                if (read < 0) break
                if (result.size() + read > MAX_DETAIL_BYTES) throw ResponseTooLargeException()
                result.write(chunk, 0, read)
            }
        }
        String(result.toByteArray(), Charsets.UTF_8)
    }

    private class ResponseTooLargeException : IOException()

    companion object {
        const val MAX_DETAIL_BYTES = 2 * 1024 * 1024
        const val MAX_CACHE_ENTRIES = 64
        val CACHE_TTL_MILLIS: Long = Duration.ofMinutes(15).toMillis()
        val CACHE_RETENTION_MILLIS: Long = Duration.ofDays(7).toMillis()
    }
}
