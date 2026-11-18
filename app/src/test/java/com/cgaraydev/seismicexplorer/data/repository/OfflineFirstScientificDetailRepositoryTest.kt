package com.cgaraydev.seismicexplorer.data.repository

import com.cgaraydev.seismicexplorer.data.local.dao.ScientificDetailCacheDao
import com.cgaraydev.seismicexplorer.data.local.entity.ScientificDetailCacheEntity
import com.cgaraydev.seismicexplorer.data.remote.UsgsScientificDetailParser
import com.cgaraydev.seismicexplorer.data.remote.api.UsgsScientificDetailApi
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailFailure
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailResult
import com.cgaraydev.seismicexplorer.domain.model.ScientificDetailSource
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstScientificDetailRepositoryTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()
    private val cache = FakeCache()
    private val api = FakeApi()
    private val repository = OfflineFirstScientificDetailRepository(
        api, cache, UsgsScientificDetailParser(Json),
        Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    )

    @Test fun firstFetchThenCacheWithoutSecondNetworkCall() = runBlocking {
        api.payload = detail("us123")
        val first = repository.getDetail("us123") as ScientificDetailResult.Available
        val second = repository.getDetail("us123") as ScientificDetailResult.Available
        assertEquals(ScientificDetailSource.NETWORK, first.source)
        assertEquals(ScientificDetailSource.CACHE, second.source)
        assertFalse(second.isStale)
        assertEquals(listOf("shakemap"), second.detail.availableProductTypes)
        assertEquals(1, second.detail.availableProductCount)
        assertEquals(1, api.calls)
        assertEquals(detail("us123"), cache.rows["us123"]?.payloadJson)
    }

    @Test fun staleCacheIsReturnedWhenNetworkFails() = runBlocking {
        cache.rows["us123"] = ScientificDetailCacheEntity(
            "us123", 100L, now - OfflineFirstScientificDetailRepository.CACHE_TTL_MILLIS - 1,
            detail("us123", 100L)
        )
        api.failure = IOException("sin conexión")
        val result = repository.getDetail("us123") as ScientificDetailResult.Available
        assertEquals(ScientificDetailSource.CACHE, result.source)
        assertTrue(result.isStale)
    }

    @Test fun olderCachedEventMustRefreshWhenFeedHasNewerRevision() = runBlocking {
        cache.rows["us123"] = ScientificDetailCacheEntity("us123", 100L, now, detail("us123", 100L))
        api.payload = detail("us123", 200L)
        val result = repository.getDetail("us123", 200L) as ScientificDetailResult.Available
        assertEquals(ScientificDetailSource.NETWORK, result.source)
        assertEquals(200L, result.detail.sourceUpdatedAtEpochMillis)
        assertEquals(1, api.calls)
    }

    @Test fun differentEventIdDoesNotOverwriteCache() = runBlocking {
        api.payload = detail("other")
        val result = repository.getDetail("us123")
        assertEquals(ScientificDetailResult.Failed(ScientificDetailFailure.INVALID_RESPONSE), result)
        assertNull(cache.rows["us123"])
    }

    @Test fun invalidIdIsNotSentToNetwork() = runBlocking {
        val result = repository.getDetail("../other")
        assertEquals(ScientificDetailResult.Failed(ScientificDetailFailure.INVALID_EVENT_ID), result)
        assertEquals(0, api.calls)
    }

    @Test fun oversizedResponseIsRejectedWithoutCacheWrites() = runBlocking {
        api.payload = "x".repeat(OfflineFirstScientificDetailRepository.MAX_DETAIL_BYTES + 1)
        val result = repository.getDetail("us123")
        assertEquals(ScientificDetailResult.Failed(ScientificDetailFailure.RESPONSE_TOO_LARGE), result)
        assertTrue(cache.rows.isEmpty())
    }

    @Test fun definitive404DoesNotReturnStaleCachedProducts() = runBlocking {
        cache.rows["us123"] = ScientificDetailCacheEntity(
            "us123", 100L, now - OfflineFirstScientificDetailRepository.CACHE_TTL_MILLIS - 1,
            detail("us123", 100L)
        )
        api.failure = retrofit2.HttpException(retrofit2.Response.error<Any>(
            404, "not found".toResponseBody("text/plain".toMediaType())
        ))
        val result = repository.getDetail("us123")
        assertEquals(ScientificDetailResult.Failed(ScientificDetailFailure.EVENT_NOT_FOUND), result)
    }

    @Test fun cancellationIsNeverConvertedIntoOfflineFallback() = runBlocking {
        api.failure = CancellationException("cancelado")
        val error = runCatching { repository.getDetail("us123") }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    private fun detail(id: String, updated: Long = 1700000003000L) = """{
        "type":"Feature","id":"$id","properties":{
            "type":"earthquake","updated":$updated,
            "products":{"shakemap":[{"status":"UPDATE","source":"us"}]}
        }
    }""".trimIndent()

    private class FakeApi : UsgsScientificDetailApi {
        var payload = ""
        var calls = 0
        var failure: Exception? = null
        override suspend fun getDetail(eventId: String): ResponseBody {
            calls++
            failure?.let { throw it }
            return payload.toResponseBody("application/json".toMediaType())
        }
    }

    private class FakeCache : ScientificDetailCacheDao {
        val rows = mutableMapOf<String, ScientificDetailCacheEntity>()
        override suspend fun get(eventId: String): ScientificDetailCacheEntity? = rows[eventId]
        override suspend fun upsert(entity: ScientificDetailCacheEntity) { rows[entity.eventId] = entity }
        override suspend fun deleteOlderThan(cutoffMillis: Long) {
            rows.values.removeAll { it.fetchedAtEpochMillis < cutoffMillis }
        }
        override suspend fun keepLatest(maxEntries: Int) {
            val kept = rows.values.sortedWith(
                compareByDescending<ScientificDetailCacheEntity> { it.fetchedAtEpochMillis }
                    .thenBy { it.eventId }
            ).take(maxEntries).map { it.eventId }.toSet()
            rows.keys.retainAll(kept)
        }
    }
}
