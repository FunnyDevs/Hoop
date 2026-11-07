package app.hoop.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal as CompatCancellationSignal
import app.hoop.domain.LocationProvider
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Reads the device position with the platform `LocationManager` (no Google Play Services needed).
 * Asks for the location permission first. Approximate location is enough for Hoop.
 */
class AndroidLocationProvider(
    private val context: Context,
    private val permissions: ActivityPermissionRequester,
) : LocationProvider {

    override suspend fun requestCurrentLocation(): DomainResult<GeoPoint> {
        if (!hasPermission()) {
            val granted = permissions.request(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            if (granted.values.none { it }) return DomainResult.failure(DomainError.LocationPermissionDenied)
        }
        val location = try {
            findLocation()
        } catch (e: SecurityException) {
            return DomainResult.failure(DomainError.LocationPermissionDenied)
        } ?: return DomainResult.failure(DomainError.LocationUnavailable)
        return GeoPoint.create(location.latitude, location.longitude)
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun findLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = PROVIDERS.filter { manager.isProviderEnabled(it) }
        providers.firstNotNullOfOrNull { provider -> manager.getLastKnownLocation(provider)?.takeIf(::isFresh) }?.let { return it }
        for (provider in providers) {
            val fresh = withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { currentLocation(manager, provider) }
            if (fresh != null) return fresh
        }
        // Better an old position than none.
        return providers.firstNotNullOfOrNull { manager.getLastKnownLocation(it) }
    }

    private suspend fun currentLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CompatCancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }

    private fun isFresh(location: Location): Boolean {
        val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / NANOS_PER_MILLI
        return ageMillis <= MAX_CACHED_AGE_MILLIS
    }

    private companion object {
        // Network first: a quick approximate fix is enough; GPS is the fallback.
        val PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        const val FIX_TIMEOUT_MILLIS = 8_000L
        const val MAX_CACHED_AGE_MILLIS = 120_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
