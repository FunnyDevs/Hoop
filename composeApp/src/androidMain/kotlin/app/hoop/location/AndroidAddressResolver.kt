package app.hoop.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import app.hoop.domain.AddressResolver
import app.hoop.domain.geo.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Finds the street with the system [Geocoder] (on most devices it asks Google's service, so the coordinates
 * leave the phone towards it). Any failure, including no network, is "no street".
 */
class AndroidAddressResolver(private val context: Context) : AddressResolver {
    override suspend fun streetAt(position: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            lookup(geocoder, position)?.let(::streetOf)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun lookup(geocoder: Geocoder, position: GeoPoint): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(position.latitude, position.longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) { if (continuation.isActive) continuation.resume(addresses.firstOrNull()) }
                    override fun onError(errorMessage: String?) { if (continuation.isActive) continuation.resume(null) }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            withContext(Dispatchers.IO) { geocoder.getFromLocation(position.latitude, position.longitude, 1)?.firstOrNull() }
        }

    /** "Via Tadino 12" when the number is known, else the street, else the place name. */
    private fun streetOf(address: Address): String? {
        val street = address.thoroughfare?.takeIf { it.isNotBlank() }
        return when {
            street != null -> listOfNotNull(street, address.subThoroughfare?.takeIf { it.isNotBlank() }).joinToString(" ")
            else -> (address.featureName ?: address.locality)?.takeIf { it.isNotBlank() }
        }
    }
}
