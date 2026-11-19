package com.cgaraydev.seismicexplorer.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageResult
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResource
import com.cgaraydev.seismicexplorer.domain.model.ShakeMapResourceKind
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CachedShakeMapImageRepositoryTest {
    private val resource = ShakeMapResource(ShakeMapResourceKind.INTENSITY_MAP,
        "https://earthquake.usgs.gov/realtime/product/shakemap/us123/us/123/download/intensity.jpg",
        "image/jpeg", 7)
    private val image = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 1, 2, 3)

    @Test fun cachedImageDoesNotTriggerAnotherHttpRequest() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val requests = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            requests.incrementAndGet()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(image.toResponseBody("image/jpeg".toMediaType())).build()
        }).build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            val first = repository.load(resource) as ShakeMapImageResult.Available
            val second = repository.load(resource) as ShakeMapImageResult.Available
            assertEquals(false, first.fromCache)
            assertEquals(true, second.fromCache)
            assertEquals(1, requests.get())
        } finally { dir.deleteRecursively() }
    }

    @Test fun oversizedMetadataIsRejectedBeforeNetwork() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val calls = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            calls.incrementAndGet()
            throw java.io.IOException("Should never run")
        }.build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            val result = repository.load(resource.copy(lengthBytes =
                (CachedShakeMapImageRepository.MAX_JPEG_BYTES + 1).toLong()))
            assertEquals(com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.TOO_LARGE,
                (result as ShakeMapImageResult.Failed).reason)
            assertEquals(0, calls.get())
        } finally { dir.deleteRecursively() }
    }

    @Test fun staleCachedImageIsExplicitOnConnectionFailure() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val calls = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            if (calls.incrementAndGet() > 1) throw java.io.IOException("Offline")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(image.toResponseBody("image/jpeg".toMediaType())).build()
        }).build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            assertTrue(repository.load(resource) is ShakeMapImageResult.Available)
            dir.listFiles()!!.single().setLastModified(System.currentTimeMillis() - 16 * 60_000L)
            val offline = repository.load(resource) as ShakeMapImageResult.Available
            assertEquals(true, offline.fromCache)
            assertEquals(true, offline.isStale)
            assertEquals(2, calls.get())
        } finally { dir.deleteRecursively() }
    }

    @Test fun rejectedUrlNeverMakesHttpRequest() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val requests = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            throw java.io.IOException("Should never run")
        }.build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            val invalid = resource.copy(url = "https://evil.example/download/intensity.jpg")
            val failure = repository.load(invalid) as ShakeMapImageResult.Failed
            assertEquals(0, requests.get())
            assertEquals(com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.INVALID_RESOURCE,
                failure.reason)
        } finally { dir.deleteRecursively() }
    }
}
