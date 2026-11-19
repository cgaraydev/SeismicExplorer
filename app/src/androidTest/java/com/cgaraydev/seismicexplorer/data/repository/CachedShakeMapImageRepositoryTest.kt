package com.cgaraydev.seismicexplorer.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.graphics.Bitmap
import com.cgaraydev.seismicexplorer.core.image.ShakeMapJpegDecoder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        "image/jpeg", null)
    // Un JPEG decodificable, no la cabecera de siete bytes usada en 3.5.5.
    private val image: ByteArray by lazy {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        } finally { bitmap.recycle() }
    }
    private val truncated = byteArrayOf(0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0, 1, 2, 3)

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

    @Test fun invalidDownloadedJpegIsNotCachedAndNextAttemptCanRecover() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val requests = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val bytes = if (requests.incrementAndGet() == 1) truncated else image
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(bytes.toResponseBody("image/jpeg".toMediaType())).build()
        }.build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            assertEquals(false, ShakeMapJpegDecoder.isValid(truncated))
            val first = repository.load(resource) as ShakeMapImageResult.Failed
            assertEquals(com.cgaraydev.seismicexplorer.domain.model.ShakeMapImageFailure.INVALID_IMAGE,
                first.reason)
            assertEquals(0, dir.listFiles()?.size ?: 0)
            val second = repository.load(resource) as ShakeMapImageResult.Available
            assertEquals(false, second.fromCache)
            assertTrue(ShakeMapJpegDecoder.isValid(second.bytes))
            assertEquals(2, requests.get())
        } finally { dir.deleteRecursively() }
    }

    @Test fun corruptedCachedJpegIsDeletedAndDownloadedAgain() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val requests = AtomicInteger(0)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(image.toResponseBody("image/jpeg".toMediaType())).build()
        }.build()
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            assertTrue(repository.load(resource) is ShakeMapImageResult.Available)
            dir.listFiles()!!.single().writeBytes(truncated)
            val second = repository.load(resource) as ShakeMapImageResult.Available
            assertEquals(false, second.fromCache)
            assertEquals(2, requests.get())
            assertTrue(ShakeMapJpegDecoder.isValid(dir.listFiles()!!.single().readBytes()))
        } finally { dir.deleteRecursively() }
    }

    @Test fun cancellingCoroutineCancelsInFlightOkHttpCall() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, "shakemap_test_${System.nanoTime()}")
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            started.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!chain.call().isCanceled() && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            if (chain.call().isCanceled()) cancelled.countDown()
            throw IOException("interrupted")
        }.build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = CachedShakeMapImageRepository.forTesting(dir, http)
            val job = scope.launch { repository.load(resource) }
            assertTrue("OkHttp debe comenzar", started.await(5, TimeUnit.SECONDS))
            withTimeout(5_000) { job.cancelAndJoin() }
            assertTrue("La cancelación debe llegar hasta OkHttp",
                cancelled.await(5, TimeUnit.SECONDS))
            assertEquals(0, dir.listFiles()?.size ?: 0)
        } finally { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel(); dir.deleteRecursively() }
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
