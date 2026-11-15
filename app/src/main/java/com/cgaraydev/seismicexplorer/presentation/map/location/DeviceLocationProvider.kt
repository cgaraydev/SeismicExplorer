
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed interface CurrentLocationResult {

    data class Available(
        val location: Location
    ) : CurrentLocationResult

    data object PermissionRequired : CurrentLocationResult

    data object LocationDisabled : CurrentLocationResult

    data object Unavailable : CurrentLocationResult
}

class DeviceLocationProvider(
    private val context: Context
) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CurrentLocationResult {
        val hasCoarsePermission = hasPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val hasFinePermission = hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (!hasCoarsePermission && !hasFinePermission) {
            return CurrentLocationResult.PermissionRequired
        }

        val manager = context.getSystemService(
            LocationManager::class.java
        ) ?: return CurrentLocationResult.Unavailable

        return try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                return CurrentLocationResult.LocationDisabled
            }

            val enabledProviders = manager
                .getProviders(true)
                .toSet()

            val candidates = buildList {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    LocationManager.FUSED_PROVIDER in enabledProviders
                ) {
                    add(LocationManager.FUSED_PROVIDER)
                }

                if (
                    LocationManager.NETWORK_PROVIDER in enabledProviders
                ) {
                    add(LocationManager.NETWORK_PROVIDER)
                }

                if (
                    hasFinePermission &&
                    LocationManager.GPS_PROVIDER in enabledProviders
                ) {
                    add(LocationManager.GPS_PROVIDER)
                }
            }

            Log.d(
                TAG,
                "Enabled location providers: $enabledProviders"
            )

            Log.d(
                TAG,
                "Eligible providers: $candidates"
            )

            if (candidates.isEmpty()) {
                Log.w(TAG, "No eligible location provider")
                return CurrentLocationResult.Unavailable
            }

            // Una ubicación muy reciente evita esperar
            // innecesariamente a una nueva estimación.
            val recentLocation = findCachedLocation(
                manager = manager,
                providers = candidates,
                maxAgeMs = IMMEDIATE_CACHE_MAX_AGE_MS
            )

            if (recentLocation != null) {
                Log.d(TAG, "Using very recent cached location")
                return CurrentLocationResult.Available(
                    recentLocation
                )
            }

            // Probar proveedores alternativos cuando uno
            // no consigue calcular la ubicación.
            for (provider in candidates) {
                Log.d(
                    TAG,
                    "Requesting current location: $provider"
                )

                val location = try {
                    withTimeoutOrNull(
                        PROVIDER_TIMEOUT_MS.milliseconds
                    ) {
                        requestCurrentLocation(
                            manager = manager,
                            provider = provider
                        )
                    }
                } catch (_: IllegalArgumentException) {
                    Log.w(
                        TAG,
                        "Location provider became unavailable: $provider"
                    )
                    null
                }

                if (location != null) {
                    Log.d(
                        TAG,
                        "Current location obtained from: $provider"
                    )

                    return CurrentLocationResult.Available(
                        location
                    )
                }

                Log.w(
                    TAG,
                    "No current location from: $provider"
                )
            }

            // Último recurso: usar una posición reciente,
            // pero nunca una localización antigua sin límite.
            val cachedLocation = findCachedLocation(
                manager = manager,
                providers = candidates,
                maxAgeMs = FALLBACK_CACHE_MAX_AGE_MS
            )

            if (cachedLocation != null) {
                Log.d(
                    TAG,
                    "Using recent cached location as fallback"
                )

                CurrentLocationResult.Available(cachedLocation)
            } else {
                Log.w(
                    TAG,
                    "No current or recent cached location available"
                )

                CurrentLocationResult.Unavailable
            }

        } catch (_: SecurityException) {
            Log.w(TAG, "Location permission was revoked")
            CurrentLocationResult.PermissionRequired
        }
    }

    private fun hasPermission(
        permission: String
    ): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        manager: LocationManager,
        provider: String
    ): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()

            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
            }

            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findCachedLocation(
        manager: LocationManager,
        providers: List<String>,
        maxAgeMs: Long
    ): Location? {
        val nowNanos = SystemClock.elapsedRealtimeNanos()

        val maxAgeNanos = TimeUnit.MILLISECONDS
            .toNanos(maxAgeMs)

        return providers
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            .filter { location ->
                val ageNanos = nowNanos -
                        location.elapsedRealtimeNanos

                ageNanos in 0L..maxAgeNanos
            }
            .maxByOrNull {
                it.elapsedRealtimeNanos
            }
    }

    private companion object {
        const val TAG = "DeviceLocationProvider"

        const val PROVIDER_TIMEOUT_MS = 10_000L

        const val IMMEDIATE_CACHE_MAX_AGE_MS = 10_000L

        const val FALLBACK_CACHE_MAX_AGE_MS = 60_000L
    }
}
