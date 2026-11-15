package com.cgaraydev.seismicexplorer.presentation.map.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed interface CurrentLocationResult {
    data class Available(val location: Location) : CurrentLocationResult
    data object PermissionRequired : CurrentLocationResult
    data object LocationDisabled : CurrentLocationResult
    data object Unavailable : CurrentLocationResult
}

/**
 * Obtiene una posición aproximada para centrar el mapa.
 * No mantiene seguimiento ni solicita ubicación en segundo plano.
 */
class DeviceLocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CurrentLocationResult {
        val hasCoarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasFine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasCoarse && !hasFine) return CurrentLocationResult.PermissionRequired

        val manager = context.getSystemService(LocationManager::class.java)
            ?: return CurrentLocationResult.Unavailable

        return try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                return CurrentLocationResult.LocationDisabled
            }

            val providers = eligibleNativeProviders(manager, hasFine)
            Log.d(TAG, "Eligible native providers: $providers")

            val playServicesStatus = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            Log.d(TAG, "Google Play services status: $playServicesStatus")

            if (playServicesStatus == ConnectionResult.SUCCESS) {
                tryGoogleLocation()?.let {
                    Log.d(TAG, "Location acquired: google-play-services")
                    return CurrentLocationResult.Available(it)
                }
            } else {
                Log.w(TAG, "Google Play services unavailable; using native fallback")
            }

            findCachedNativeLocation(manager, providers)?.let {
                Log.d(TAG, "Location acquired: native-cache")
                return CurrentLocationResult.Available(it)
            }

            requestNativeFallback(manager, providers)?.let {
                Log.d(TAG, "Location acquired: native-provider")
                return CurrentLocationResult.Available(it)
            }

            Log.w(TAG, "No acceptable location from Google or native providers")
            CurrentLocationResult.Unavailable
        } catch (_: SecurityException) {
            Log.w(TAG, "Location permission revoked during request")
            CurrentLocationResult.PermissionRequired
        }
    }

    /**
     * Permite reutilizar una estimación de hasta cinco minutos.
     * Es suficiente para centrar un mapa regional, sin exigir GPS preciso.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    private suspend fun tryGoogleLocation(): Location? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(MAX_CACHE_AGE_MS)
            .setDurationMillis(GOOGLE_REQUEST_DURATION_MS)
            .build()

        val cancellationSource = CancellationTokenSource()

        return try {
            Log.d(TAG, "Requesting location: google-play-services")
            val response = withTimeoutOrNull(GOOGLE_OUTER_TIMEOUT_MS.milliseconds) {
                LocationResponse(
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(request, cancellationSource.token)
                        .await(cancellationSource)
                )
            }

            when {
                response == null -> Log.w(TAG, "Google location timeout")
                response.location == null -> Log.w(TAG, "Google location callback returned null")
                else -> Log.d(TAG, "Google location callback returned a fix")
            }
            response?.location
        } catch (error: CancellationException) {
            if (!currentCoroutineContext().isActive) throw error
            Log.w(TAG, "Google location task was cancelled")
            null
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Google location request failed: ${error.javaClass.simpleName}")
            null
        }
    }

    private fun eligibleNativeProviders(
        manager: LocationManager,
        hasFinePermission: Boolean
    ): List<String> {
        val enabled = manager.getProviders(true).toSet()
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                LocationManager.FUSED_PROVIDER in enabled
            ) add(LocationManager.FUSED_PROVIDER)
            if (LocationManager.NETWORK_PROVIDER in enabled) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (hasFinePermission && LocationManager.GPS_PROVIDER in enabled) {
                add(LocationManager.GPS_PROVIDER)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findCachedNativeLocation(
        manager: LocationManager,
        providers: List<String>
    ): Location? {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        return providers.mapNotNull { provider ->
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (_: IllegalArgumentException) {
                null
            }
            if (location == null) {
                Log.d(TAG, "Native cache missing: $provider")
                null
            } else {
                val ageMs = TimeUnit.NANOSECONDS.toMillis(
                    nowNanos - location.elapsedRealtimeNanos
                )
                Log.d(TAG, "Native cache age: $provider = ${ageMs}ms")
                location.takeIf {
                    LocationAgePolicy.isFresh(
                        locationElapsedNanos = it.elapsedRealtimeNanos,
                        nowElapsedNanos = nowNanos,
                        maxAgeMs = MAX_CACHE_AGE_MS
                    )
                }
            }
        }.maxByOrNull { it.elapsedRealtimeNanos }
    }

    /** Ejecuta los proveedores nativos en paralelo para evitar 10 s + 10 s. */
    private suspend fun requestNativeFallback(
        manager: LocationManager,
        providers: List<String>
    ): Location? = coroutineScope {
        if (providers.isEmpty()) return@coroutineScope null
        providers.map { provider ->
            async {
                try {
                    Log.d(TAG, "Requesting native location: $provider")
                    val response = withTimeoutOrNull(NATIVE_REQUEST_TIMEOUT_MS.milliseconds) {
                        LocationResponse(requestNativeCurrentLocation(manager, provider))
                    }
                    when {
                        response == null -> Log.w(TAG, "Native location timeout: $provider")
                        response.location == null -> Log.w(TAG, "Native callback returned null: $provider")
                        else -> Log.d(TAG, "Native callback returned fix: $provider")
                    }
                    response?.location
                } catch (_: IllegalArgumentException) {
                    Log.w(TAG, "Native provider became unavailable: $provider")
                    null
                }
            }
        }.awaitAll().firstOrNull { it != null }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestNativeCurrentLocation(
        manager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation { cancellation.cancel() }
        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            cancellation,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    private data class LocationResponse(val location: Location?)

    private companion object {
        const val TAG = "DeviceLocationProvider"
        const val MAX_CACHE_AGE_MS = 5 * 60_000L
        const val GOOGLE_REQUEST_DURATION_MS = 12_000L
        const val GOOGLE_OUTER_TIMEOUT_MS = 14_000L
        const val NATIVE_REQUEST_TIMEOUT_MS = 5_000L
    }
}
