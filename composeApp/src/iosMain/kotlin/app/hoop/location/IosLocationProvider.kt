@file:OptIn(ExperimentalForeignApi::class)

package app.hoop.location

import app.hoop.domain.LocationProvider
import app.hoop.domain.common.DomainError
import app.hoop.domain.common.DomainResult
import app.hoop.domain.geo.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Reads the device position with CoreLocation, asking for the "when in use" permission first.
 * Requires `NSLocationWhenInUseUsageDescription` in the app's Info.plist.
 * NOTE: written without an Xcode toolchain available, so it has not been compiled yet.
 */
class IosLocationProvider : LocationProvider {
    // The delegate and manager are retained here: CoreLocation only keeps a weak reference to the delegate.
    private var session: Session? = null

    override suspend fun requestCurrentLocation(): DomainResult<GeoPoint> =
        suspendCancellableCoroutine { continuation ->
            val newSession = Session { result -> if (continuation.isActive) continuation.resume(result) }
            session = newSession
            continuation.invokeOnCancellation { session = null }
            newSession.start()
        }

    private class Session(private val onResult: (DomainResult<GeoPoint>) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
        private val manager = CLLocationManager()

        init {
            manager.delegate = this
        }

        fun start() = handle(manager.authorizationStatus)

        private fun handle(status: CLAuthorizationStatus) {
            when (status) {
                kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
                kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> onResult(DomainResult.failure(DomainError.LocationPermissionDenied))
                else -> onResult(DomainResult.failure(DomainError.LocationUnavailable))
            }
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) handle(manager.authorizationStatus)
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation
            if (location == null) {
                onResult(DomainResult.failure(DomainError.LocationUnavailable))
            } else {
                val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
                onResult(GeoPoint.create(latitude, longitude))
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onResult(DomainResult.failure(DomainError.LocationUnavailable))
        }
    }
}
