package app.hoop.location

import app.hoop.domain.AddressResolver
import app.hoop.domain.geo.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import kotlin.coroutines.resume

/**
 * Finds the street with CoreLocation's geocoder (Apple's service sees the coordinates). Any failure is "no street".
 * Written but never compiled (no Xcode here).
 */
class IosAddressResolver : AddressResolver {
    override suspend fun streetAt(position: GeoPoint): String? = suspendCancellableCoroutine { continuation ->
        val geocoder = CLGeocoder()
        continuation.invokeOnCancellation { geocoder.cancelGeocode() }
        geocoder.reverseGeocodeLocation(CLLocation(latitude = position.latitude, longitude = position.longitude)) { placemarks, error ->
            val place = if (error == null) placemarks?.firstOrNull() as? platform.CoreLocation.CLPlacemark else null
            val street = place?.thoroughfare?.takeIf { it.isNotBlank() }
            val text = when {
                street != null -> listOfNotNull(street, place?.subThoroughfare?.takeIf { it.isNotBlank() }).joinToString(" ")
                else -> (place?.name ?: place?.locality)?.takeIf { it.isNotBlank() }
            }
            if (continuation.isActive) continuation.resume(text)
        }
    }
}
