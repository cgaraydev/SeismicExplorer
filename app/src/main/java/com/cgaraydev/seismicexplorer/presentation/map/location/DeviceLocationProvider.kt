package com.cgaraydev.seismicexplorer.presentation.map.location

import android.Manifest
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationListenerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed interface CurrentLocationResult {
    data class Available(val location: Location) : CurrentLocationResult
    data object PermissionRequired : CurrentLocationResult
    data object LocationDisabled : CurrentLocationResult
    data class SettingsResolutionRequired(val resolution: PendingIntent) : CurrentLocationResult
    data object SettingsUnavailable : CurrentLocationResult
    data object Unavailable : CurrentLocationResult
}

/**
 * Una sola estimación aproximada para centrar el mapa, sin seguimiento ni
 * ubicación en segundo plano. Solo requiere el permiso aproximado de la app.
 */
class DeviceLocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CurrentLocationResult {
        val hasCoarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasFine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasCoarse && !hasFine) {
            return CurrentLocationResult.PermissionRequired
        }

        val manager = context.getSystemService(LocationManager::class.java)
            ?: return CurrentLocationResult.Unavailable

        return try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                return CurrentLocationResult.LocationDisabled
            }

            val nativeProviders = eligibleNativeProviders(manager, hasFine)
            Log.d(
                TAG,
                "Location enabled; coarse=$hasCoarse fine=$hasFine " +
                    "nativeProviders=$nativeProviders"
            )

            // Leer la caché nativa antes de iniciar una solicitud que puede
            // bloquear varios segundos. La edad máxima es de cinco minutos.
            findCachedNativeLocation(manager, nativeProviders)?.let {
                Log.d(TAG, "Location acquired: native-cache")
                return CurrentLocationResult.Available(it)
            }

            val playServicesAvailable = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

            if (playServicesAvailable) {
                when (val settings = checkLocationSettings()) {
                    LocationSettingsInspection.Ready,
                    LocationSettingsInspection.Unknown -> Unit

                    is LocationSettingsInspection.ResolutionRequired -> {
                        return CurrentLocationResult.SettingsResolutionRequired(settings.resolution)
                    }

                    LocationSettingsInspection.Unavailable -> {
                        return CurrentLocationResult.SettingsUnavailable
                    }
                }
            }

            // La activación de ubicación puede habilitar proveedores durante
            // el chequeo de SettingsClient. No usamos una lista antigua.
            val currentNativeProviders = eligibleNativeProviders(manager, hasFine)
            if (currentNativeProviders != nativeProviders) {
                Log.d(TAG, "Native providers changed: $currentNativeProviders")
            }

            val candidates = buildList<suspend () -> Location?> {
                if (playServicesAvailable) {
                    // getCurrentLocation puede devolver null después de
                    // reactivar la ubicación y vaciarse la caché del sistema.
                    add { tryGoogleLocation() }
                    // Una suscripción acotada ayuda a obtener el primer fix
                    // cuando el proveedor vuelve a iniciar sus sensores.
                    add { tryGoogleUpdates() }
                } else {
                    Log.w(TAG, "Google Play services unavailable; using native providers")
                }

                currentNativeProviders.forEach { provider ->
                    add { tryNativeLocation(manager, provider) }
                    add { tryNativeUpdates(manager, provider) }
                }
            }

            Log.d(TAG, "Active location candidates: ${candidates.size}")

            // La primera posición válida gana. El límite global cancela
            // tanto las consultas únicas como las suscripciones temporales.
            val result = firstAvailableResult(
                candidates = candidates,
                timeoutMillis = OVERALL_TIMEOUT_MS
            )

            if (result != null) {
                Log.d(TAG, "Location acquired from active provider")
                CurrentLocationResult.Available(result)
            } else {
                Log.w(TAG, "No acceptable location within bounded request")
                if (!LocationManagerCompat.isLocationEnabled(manager)) {
                    CurrentLocationResult.LocationDisabled
                } else if (playServicesAvailable && isNetworkAccuracyDisabled()) {
                    // Con permiso aproximado no usamos GPS directamente. Si
                    // Google Location Accuracy está desactivada, no existe
                    // garantía de posición mediante red.
                    CurrentLocationResult.SettingsUnavailable
                } else {
                    // Puede no existir todavía un fix tras reactivar el servicio.
                    CurrentLocationResult.Unavailable
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: SecurityException) {
            Log.w(TAG, "Location permission revoked during request")
            CurrentLocationResult.PermissionRequired
        } catch (exception: Exception) {
            Log.w(TAG, "Unexpected location failure", exception)
            CurrentLocationResult.Unavailable
        }
    }

    /**
     * Comprueba requisitos de la solicitud BALANCED real. El resultado Unknown
     * no bloquea la adquisición: SettingsClient puede fallar transitoriamente
     * aunque los proveedores nativos sigan funcionando.
     */
    private suspend fun checkLocationSettings(): LocationSettingsInspection {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MS
        )
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()

        return try {
            val response = withTimeoutOrNull(SETTINGS_TIMEOUT_MS) {
                LocationServices.getSettingsClient(context)
                    .checkLocationSettings(settingsRequest)
                    .await()
            }
            if (response == null) {
                Log.w(TAG, "Location settings check timed out; trying providers")
                LocationSettingsInspection.Unknown
            } else {
                Log.d(
                    TAG,
                    "Location settings satisfied; " +
                        "networkUsable=${response.locationSettingsStates?.isNetworkLocationUsable}"
                )
                LocationSettingsInspection.Ready
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ResolvableApiException) {
            Log.w(TAG, "Location settings require user resolution")
            LocationSettingsInspection.ResolutionRequired(exception.resolution)
        } catch (exception: ApiException) {
            when (LocationSettingsFailurePolicy.classify(exception.statusCode)) {
                LocationSettingsFailure.UNAVAILABLE -> {
                    Log.w(TAG, "Location settings cannot be resolved automatically")
                    LocationSettingsInspection.Unavailable
                }
                LocationSettingsFailure.RESOLUTION_REQUIRED,
                LocationSettingsFailure.UNKNOWN -> {
                    Log.w(TAG, "Location settings check inconclusive: ${exception.statusCode}")
                    LocationSettingsInspection.Unknown
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Location settings check failed; trying providers", exception)
            LocationSettingsInspection.Unknown
        }
    }

    /** Solo consulta GLA tras no recibir coordenadas; nunca bloquea un fix válido. */
    private suspend fun isNetworkAccuracyDisabled(): Boolean = try {
        val enabled = withTimeoutOrNull(ACCURACY_CHECK_TIMEOUT_MS) {
            LocationServices.getSettingsClient(context)
                .isGoogleLocationAccuracyEnabled()
                .await()
        }
        Log.d(TAG, "Google Location Accuracy enabled: $enabled")
        enabled == false
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Log.w(TAG, "Google Location Accuracy check unavailable", exception)
        false
    }

    private sealed interface LocationSettingsInspection {
        data object Ready : LocationSettingsInspection
        data class ResolutionRequired(val resolution: PendingIntent) : LocationSettingsInspection
        data object Unavailable : LocationSettingsInspection
        data object Unknown : LocationSettingsInspection
    }

    /** Reutiliza la caché de Play Services o solicita una estimación nueva. */
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
            val response = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                LocationResponse(
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(request, cancellationSource.token)
                        .await(cancellationSource)
                )
            }

            val usable = response?.location?.takeIf(::isUsableFix)
            when {
                response == null -> Log.w(TAG, "Google location timeout")
                response.location == null -> Log.w(TAG, "Google location callback returned null")
                usable == null -> Log.w(TAG, "Google location returned an invalid or stale fix")
                else -> Log.d(TAG, "Google location callback returned a valid fix")
            }
            usable
        } catch (error: CancellationException) {
            if (!currentCoroutineContext().isActive) throw error
            Log.w(TAG, "Google location task was cancelled")
            null
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Google location request failed: ${error.javaClass.simpleName}")
            null
        } finally {
            // Idempotente; evita que una solicitud de Google siga activa
            // si otra fuente ganó o expiró el tiempo de espera.
            cancellationSource.cancel()
        }
    }

    /**
     * Obtiene temporalmente actualizaciones de Play Services para cubrir el
     * arranque en frío tras activar la ubicación del sistema. Nunca se mantiene
     * un seguimiento continuo: la primera respuesta válida libera el callback,
     * y la cancelación por tiempo o ciclo de vida también lo libera.
     */
    @SuppressLint("MissingPermission")
    private suspend fun tryGoogleUpdates(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            UPDATE_INTERVAL_MS
        )
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(MAX_CACHE_AGE_MS)
            .setDurationMillis(OVERALL_TIMEOUT_MS)
            .build()

        return try {
            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.locations.lastOrNull(::isUsableFix)
                            ?: return

                        if (continuation.isActive) {
                            client.removeLocationUpdates(this)
                            continuation.resume(location)
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    client.removeLocationUpdates(callback)
                }

                Log.d(TAG, "Requesting Google location updates")
                client.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                ).addOnSuccessListener {
                    // La cancelación puede ocurrir antes de que Play Services
                    // termine de registrar el callback. Desregistramos también
                    // en ese caso para no dejar un listener huérfano.
                    if (!continuation.isActive) {
                        client.removeLocationUpdates(callback)
                    }
                }.addOnFailureListener { error ->
                    if (continuation.isActive) {
                        Log.w(TAG, "Google updates registration failed", error)
                        client.removeLocationUpdates(callback)
                        continuation.resume(null)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Google location updates failed", error)
            null
        }
    }

    /**
     * El callback nativo se desregistra tanto al obtener una posición como
     * al cancelarse la búsqueda. No registra actualizaciones persistentes.
     */
    @SuppressLint("MissingPermission")
    private suspend fun tryNativeUpdates(
        manager: LocationManager,
        provider: String
    ): Location? {
        return try {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListenerCompat {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive && isUsableFix(location)) {
                            manager.removeUpdates(this)
                            continuation.resume(location)
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    manager.removeUpdates(listener)
                }

                if (!continuation.isActive) {
                    return@suspendCancellableCoroutine
                }

                Log.d(TAG, "Requesting native location updates: $provider")
                manager.requestLocationUpdates(
                    provider,
                    UPDATE_INTERVAL_MS,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )

                // Cierra una posible carrera entre cancelación y registro.
                if (!continuation.isActive) {
                    manager.removeUpdates(listener)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            throw error
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Native provider disabled during updates: $provider", error)
            null
        } catch (error: Exception) {
            Log.w(TAG, "Native location updates failed: $provider", error)
            null
        }
    }

    private fun eligibleNativeProviders(
        manager: LocationManager,
        hasFinePermission: Boolean
    ): List<String> {
        val enabled = manager.getProviders(true).toSet()
        return buildList {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                LocationManager.FUSED_PROVIDER in enabled
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (LocationManager.NETWORK_PROVIDER in enabled) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (
                hasFinePermission &&
                LocationManager.GPS_PROVIDER in enabled
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findCachedNativeLocation(
        manager: LocationManager,
        providers: List<String>
    ): Location? {
        return providers.mapNotNull { provider ->
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (_: IllegalArgumentException) {
                null
            }

            location?.takeIf(::isUsableFix)
        }.maxByOrNull { it.elapsedRealtimeNanos }
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryNativeLocation(
        manager: LocationManager,
        provider: String
    ): Location? {
        return try {
            Log.d(TAG, "Requesting native location: $provider")
            val response = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                LocationResponse(
                    requestNativeCurrentLocation(manager, provider)
                )
            }

            val usable = response?.location?.takeIf(::isUsableFix)
            when {
                response == null -> Log.w(TAG, "Native location timeout: $provider")
                response.location == null -> Log.w(TAG, "Native callback returned null: $provider")
                usable == null -> Log.w(TAG, "Native location invalid or stale: $provider")
                else -> Log.d(TAG, "Native location acquired: $provider")
            }
            usable
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Native location failed: $provider (${error.javaClass.simpleName})")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestNativeCurrentLocation(
        manager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }

        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            signal,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
    }

    private fun isUsableFix(location: Location): Boolean =
        LocationFixPolicy.isUsable(
            latitude = location.latitude,
            longitude = location.longitude,
            locationElapsedNanos = location.elapsedRealtimeNanos,
            nowElapsedNanos = SystemClock.elapsedRealtimeNanos(),
            maxAgeMs = MAX_CACHE_AGE_MS
        )

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    private data class LocationResponse(val location: Location?)

    private companion object {
        const val TAG = "DeviceLocationProvider"
        const val MAX_CACHE_AGE_MS = 5 * 60_000L
        const val UPDATE_INTERVAL_MS = 1_000L
        const val GOOGLE_REQUEST_DURATION_MS = 12_000L
        const val PROVIDER_TIMEOUT_MS = 14_000L
        const val OVERALL_TIMEOUT_MS = 16_000L
        const val SETTINGS_TIMEOUT_MS = 2_500L
        const val ACCURACY_CHECK_TIMEOUT_MS = 1_000L
    }
}
